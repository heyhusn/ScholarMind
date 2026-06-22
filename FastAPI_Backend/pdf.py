import pdfplumber
from fastapi import UploadFile
import io

async def extract_text_from_pdf(file: UploadFile) -> str:
    """
    Extracts text from an uploaded PDF file using pdfplumber.
    pdfplumber is much better than PyPDF2 at reading academic papers
    with complex layouts, columns, and tables.
    """
    try:
        content = await file.read()
        pdf_file = io.BytesIO(content)
        text = ""
        with pdfplumber.open(pdf_file) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
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

