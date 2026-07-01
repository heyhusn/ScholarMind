import pdfplumber
from fastapi import UploadFile
import io
import re


PDF_TEXT_SETTINGS = [
    {"layout": False, "x_tolerance": 1, "y_tolerance": 3},
    {"layout": False, "x_tolerance": 0.5, "y_tolerance": 3},
    {"layout": False, "x_tolerance": 2, "y_tolerance": 3},
    {"layout": True, "x_tolerance": 1, "y_tolerance": 3},
]


def _clean_extracted_text(text: str) -> str:
    """Normalize extractor noise without removing meaningful word spaces."""
    lines = []
    for line in (text or "").replace("\r\n", "\n").replace("\r", "\n").split("\n"):
        lines.append(re.sub(r"[ \t]+", " ", line).strip())
    cleaned = "\n".join(lines)
    return re.sub(r"\n{3,}", "\n\n", cleaned).strip()


def _text_quality_score(text: str) -> float:
    """
    Prefer extraction candidates with natural word spacing.

    Bad PDF text often has long alphabetic runs such as
    "Understandingandreasoningoverlongvideosposes". Extremely loose spacing is
    also bad, so candidates with many single-letter tokens are penalized.
    """
    normalized = re.sub(r"\s+", " ", (text or "").strip())
    if not normalized:
        return float("-inf")

    alpha_tokens = re.findall(r"[A-Za-z]+", normalized)
    if not alpha_tokens:
        return len(normalized)

    token_count = len(alpha_tokens)
    average_token_length = sum(len(token) for token in alpha_tokens) / token_count
    long_tokens = sum(1 for token in alpha_tokens if len(token) >= 24)
    very_long_tokens = sum(1 for token in alpha_tokens if len(token) >= 40)
    single_letter_ratio = sum(1 for token in alpha_tokens if len(token) == 1) / token_count
    whitespace_ratio = sum(1 for char in normalized if char.isspace()) / len(normalized)

    return (
        len(normalized)
        + whitespace_ratio * 700
        - max(0, average_token_length - 8) * 80
        - long_tokens * 120
        - very_long_tokens * 300
        - max(0, single_letter_ratio - 0.25) * 900
    )


def _words_to_lines(words: list[dict]) -> str:
    if not words:
        return ""

    sorted_words = sorted(
        words,
        key=lambda word: (round(float(word.get("top", 0)) / 3), word.get("x0", 0)),
    )
    lines = []
    current_line = []
    current_top = None

    for word in sorted_words:
        top = float(word.get("top", 0))
        if current_top is None or abs(top - current_top) <= 3:
            current_line.append(word)
            current_top = top if current_top is None else (current_top + top) / 2
            continue

        ordered_line = sorted(current_line, key=lambda item: item.get("x0", 0))
        lines.append(" ".join(str(item.get("text", "")) for item in ordered_line))
        current_line = [word]
        current_top = top

    if current_line:
        ordered_line = sorted(current_line, key=lambda item: item.get("x0", 0))
        lines.append(" ".join(str(item.get("text", "")) for item in ordered_line))

    return "\n".join(line for line in lines if line.strip())


def _extract_best_page_text(page) -> str:
    candidates = []

    # Fast path: try standard extraction first. It is usually perfect.
    default_text = page.extract_text()
    if default_text and len(default_text.strip()) > 100:
        if _text_quality_score(default_text) > 50:
            return default_text

    if default_text:
        candidates.append(default_text)

    for settings in PDF_TEXT_SETTINGS:
        try:
            page_text = page.extract_text(**settings)
            if page_text:
                candidates.append(page_text)
        except Exception:
            pass

    # Only run the expensive word-sorting extraction if candidate quality is low
    best_so_far = max(candidates, key=_text_quality_score) if candidates else ""
    if not best_so_far or _text_quality_score(best_so_far) < 50:
        for x_tolerance in (1, 0.5, 2):
            try:
                words = page.extract_words(x_tolerance=x_tolerance, y_tolerance=3, use_text_flow=True)
                word_text = _words_to_lines(words)
                if word_text:
                    candidates.append(word_text)
            except Exception:
                pass

    if not candidates:
        return ""

    return max(candidates, key=_text_quality_score)

async def extract_text_from_pdf(file: UploadFile) -> str:
    """
    Extracts text from an uploaded PDF file using pdfplumber.
    pdfplumber is much better than PyPDF2 at reading academic papers
    with complex layouts, columns, and tables.
    """
    try:
        content = await file.read()
        pdf_file = io.BytesIO(content)
        pages_text = []
        with pdfplumber.open(pdf_file) as pdf:
            for page in pdf.pages:
                page_text = _extract_best_page_text(page)
                if page_text:
                    pages_text.append(page_text)
        text = _clean_extracted_text("\n\n".join(pages_text))
        return text if text.strip() else "Error: Could not extract any text from this PDF."
    except Exception as e:
        return f"Error reading PDF: {e}"


def extract_text_from_docx(file_bytes: bytes) -> str:
    """
    Extracts text from an uploaded DOCX file.
    Uses Python's standard library (zipfile and xml.etree.ElementTree)
    to parse the word/document.xml without needing python-docx.
    """
    import zipfile
    import xml.etree.ElementTree as ET
    import io

    try:
        with zipfile.ZipFile(io.BytesIO(file_bytes)) as docx:
            xml_content = docx.read('word/document.xml')
            root = ET.fromstring(xml_content)
            
            ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            text_elements = root.findall('.//w:t', ns)
            
            texts = [elem.text for elem in text_elements if elem.text]
            return "\n".join(texts) if texts else "Error: Could not extract any text from this DOCX."
    except Exception as e:
        return f"Error reading DOCX: {e}"


def extract_text_from_doc(file_bytes: bytes) -> str:
    """
    Extracts text from legacy .doc files.
    First checks if it is actually a renamed .docx file (zip file).
    Otherwise, uses a robust scanner to extract ASCII/UTF-16LE text from the binary.
    """
    import zipfile
    import io
    import re

    # 1. Check if it's actually a ZIP file (e.g. DOCX renamed to DOC)
    if zipfile.is_zipfile(io.BytesIO(file_bytes)):
        return extract_text_from_docx(file_bytes)

    # 2. Heuristic extraction of printable UTF-16LE and CP1252/ASCII runs
    text_pieces = []

    # Find UTF-16LE characters (common in .doc files)
    try:
        decoded_utf16 = file_bytes.decode("utf-16le", errors="ignore")
        utf16_runs = re.findall(r"[\x20-\x7E\u00A0-\u017F\s]{6,}", decoded_utf16)
        cleaned_utf16 = []
        for run in utf16_runs:
            run_cleaned = re.sub(r"\s+", " ", run).strip()
            if len(run_cleaned) > 10 and not all(c in " _-*" for c in run_cleaned):
                cleaned_utf16.append(run_cleaned)
        if cleaned_utf16:
            text_pieces.append("\n".join(cleaned_utf16))
    except Exception:
        pass

    # Find CP1252/ASCII runs
    try:
        decoded_ascii = file_bytes.decode("cp1252", errors="ignore")
        ascii_runs = re.findall(r"[\x20-\x7E\u00A0-\u00FF\s]{6,}", decoded_ascii)
        cleaned_ascii = []
        for run in ascii_runs:
            run_cleaned = re.sub(r"\s+", " ", run).strip()
            if len(run_cleaned) > 10 and not all(c in " _-*" for c in run_cleaned):
                cleaned_ascii.append(run_cleaned)
        if cleaned_ascii:
            text_pieces.append("\n".join(cleaned_ascii))
    except Exception:
        pass

    if not text_pieces:
        return "Error: Could not extract readable text from this legacy .doc file."

    # Take the one that extracted more content
    extracted_text = max(text_pieces, key=len)

    # Clean up and filter lines
    lines = []
    for line in extracted_text.split("\n"):
        line = re.sub(r"[^\x20-\x7E\u00A0-\u00FF\u0100-\u017F\s.,;:!?()\'\"-]", "", line)
        line = re.sub(r"\s+", " ", line).strip()
        if len(line) > 15:
            lines.append(line)

    final_text = "\n\n".join(lines)
    return final_text if final_text.strip() else "Error: Could not extract readable text from this legacy .doc file."


