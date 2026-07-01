import os

from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")

# Safely initialize client with a placeholder to prevent startup crashes when keys aren't loaded yet
client = OpenAI(
    api_key=DEEPSEEK_API_KEY or "placeholder_key",
    base_url="https://api.deepseek.com",
    timeout=20.0,
)

MODEL_NAME = "deepseek-chat"


def _call_ai(system_prompt: str, user_prompt: str, timeout_seconds: float = 30.0) -> str:
    """Shared helper to call the AI model with timeout and retry logic."""
    if not DEEPSEEK_API_KEY or DEEPSEEK_API_KEY == "your_deepseek_api_key_here":
        return "Error: DeepSeek API key is not configured in .env file."
    
    max_retries = 2
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model=MODEL_NAME,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                timeout=timeout_seconds,
            )
            return response.choices[0].message.content
        except Exception as e:
            if attempt == max_retries - 1:
                return f"Error: AI API request failed or timed out. Details: {e}"
            print(f"DeepSeek call failed (attempt {attempt + 1}/{max_retries}): {e}. Retrying...")


def analyze_paper(text: str) -> str:
    """
    Takes extracted PDF text and returns structured JSON for the paper.
    The sections list is dynamic, so it only includes headings that actually
    exist in the current paper.
    """
    system_prompt = """You are an expert academic paper analyzer.
Analyze the given paper text and return a SINGLE valid JSON object with no markdown and no extra text.

Return this schema:
{
  "title": "Full paper title",
  "authors": "Author1, Author2 et al.",
  "year": "YYYY",
  "venue": "Conference or Journal name, or 'Unknown'",
  "field": "e.g. Machine Learning, NLP, Computer Vision, etc.",
  "citation_count": 0,
  "citation_impact": "Low | Moderate | Good | High | Excellent",
  "citation_score": 0,
  "ai_overview_title": "One catchy sentence summarizing the core contribution",
  "ai_overview_body": "2-3 sentences explaining what the paper does and why it matters",
  "sections": [
    {
      "key": "abstract",
      "title": "Abstract",
      "content": "Section content"
    }
  ],
  "citations_list": "A formatted numbered list of references, max 15"
}

Rules:
- The sections array must be dynamic and include only sections that actually exist in the paper.
- Preserve the real section meaning. Examples: Abstract, Introduction, Related Work, Methodology, System Design, Experiments, Results, Discussion, Limitations, Conclusion.
- Use short stable lowercase snake_case values for section keys.
- Do not include empty sections.
- If a classic section is missing, do not invent it.
- Always include citations_list as null if references are not found.
- For citation_score: 0-20=Low, 21-40=Moderate, 41-65=Good, 66-85=High, 86-100=Excellent.
- Return ONLY the JSON object.
"""
    truncated_text = text[:8000]
    return _call_ai(system_prompt, f"Paper text:\n{truncated_text}")


def generate_chat_response(context: str, message: str, mode: str) -> str:
    system_prompt = "You are a helpful AI assistant."
    if mode == "beginner":
        system_prompt = "You are an AI assistant that explains concepts very simply to beginners. Use analogies and simple language. Keep it brief."
    elif mode == "technical":
        system_prompt = "You are a highly technical AI assistant. Use precise, academic terminology and go in-depth on the underlying mechanics."
    elif mode == "freeform":
        system_prompt = "You are a helpful AI assistant for Scholar Mind. Answer questions based on the provided document context."

    user_prompt = f"Document Context:\n{context[:4000]}\n\nQuestion: {message}"
    return _call_ai(system_prompt, user_prompt)


def generate_flashcards(context: str) -> str:
    system_prompt = "You are an AI that generates flashcards from educational text. Respond ONLY in valid JSON format as a list of objects with 'question' and 'answer' keys. Do not include markdown formatting."
    user_prompt = f"Generate 5 to 8 flashcards for this text:\n{context[:4000]}"
    return _call_ai(system_prompt, user_prompt)


def generate_podcast_script(context: str) -> str:
    system_prompt = "You are an AI that converts academic text into a lively podcast script between two hosts, Alice and Bob. Alice is curious and asks questions, Bob is knowledgeable and explains things clearly."
    user_prompt = f"Generate a short podcast script based on this text:\n{context[:4000]}"
    return _call_ai(system_prompt, user_prompt)


def generate_summary(context: str) -> str:
    system_prompt = "You are an AI that summarizes academic text concisely. Provide a 3-4 sentence summary."
    user_prompt = f"Summarize this text:\n{context[:4000]}"
    return _call_ai(system_prompt, user_prompt)


def simplify_text(text: str) -> str:
    system_prompt = "You are an AI that simplifies complex academic text. Rewrite the text so it can be easily understood by a high school student."
    user_prompt = f"Simplify this text:\n{text[:4000]}"
    return _call_ai(system_prompt, user_prompt)


def generate_quiz_questions(context: str) -> str:
    system_prompt = """You are an AI that generates educational multiple-choice quizzes from academic text.
Analyze the given context and generate exactly 5 multiple-choice questions to test the reader's comprehension.
Each question must have:
- A unique 'id' (e.g. q1, q2, q3, q4, q5)
- The 'question' text
- Exactly 4 'options' (as a list of strings)
- A 'correct_answer_index' (0-indexed integer pointing to the correct option in the list)
- A brief 'explanation' (1-2 sentences explaining why the correct answer is right based on the context).

Respond ONLY in valid JSON format as a list of objects with the keys 'id', 'question', 'options', 'correct_answer_index', and 'explanation'. Do not include markdown formatting.
"""
    user_prompt = f"Generate a quiz for this academic text:\n{context[:4000]}"
    return _call_ai(system_prompt, user_prompt)


def generate_paper_insights(title: str, abstract: str, authors: str = None) -> str:
    system_prompt = """You are an expert scientific researcher.
Analyze the given paper's title and abstract and generate structured insights.
You must respond with a SINGLE valid JSON object (no markdown code blocks, no extra characters).

The JSON object must follow this exact schema:
{
  "summary": "A clear, high-level summary of the paper's core ideas in 2-3 sentences.",
  "key_findings": [
    "First main key finding or discovery",
    "Second main key finding or discovery",
    "Third main key finding or discovery"
  ],
  "applications": [
    "First practical use-case or application of this research",
    "Second practical use-case or application of this research"
  ],
  "limitations": [
    "First limitation, weakness, or future research direction mentioned",
    "Second limitation, weakness, or future research direction mentioned"
  ]
}

Rules:
- Be objective and ground your insights strictly on the provided abstract.
- Do not add markdown code blocks like ```json.
- Return ONLY the raw JSON object.
"""
    user_prompt = f"Title: {title}\nAuthors: {authors or 'Unknown'}\nAbstract:\n{abstract}"
    return _call_ai(system_prompt, user_prompt)


def generate_peer_review(text: str) -> str:
    system_prompt = """You are an expert scientific peer reviewer.
Analyze the given academic paper text and return a SINGLE valid JSON object with no markdown and no extra text.

The JSON object must follow this exact schema:
{
  "limitations": [
    "First limitation of the paper (e.g. sample size, assumptions, data constraints)",
    "Second limitation of the paper..."
  ],
  "technical_flaws": [
    "First technical flaw or methodological weakness",
    "Second technical flaw..."
  ],
  "questions": [
    "Peer reviewer question 1",
    "Peer reviewer question 2",
    "Peer reviewer question 3",
    "Peer reviewer question 4",
    "Peer reviewer question 5"
  ]
}

Rules:
- Identify realistic limitations, assumptions, and data-gathering boundaries.
- Find potential technical flaws in experimental setup, validation, or conclusions.
- Formulate exactly 5 challenging, constructive questions a peer reviewer would ask the authors.
- Return ONLY the JSON object. Do not wrap in markdown fences.
"""
    truncated_text = text[:8000]
    return _call_ai(system_prompt, f"Paper text:\n{truncated_text}")


def generate_paper_references(text: str, style: str) -> str:
    system_prompt = f"""You are an expert scientific researcher and bibliographer.
Analyze the given academic paper text and generate a list of exactly 8 to 12 high-quality, relevant, and realistic references (citations) that match the scope of this paper.
You must format these citations in the requested citation style: {style} (e.g. APA, MLA, IEEE, Chicago).

Return a SINGLE valid JSON array of strings (no markdown blocks, no extra characters).
For example:
[
  "First citation formatted in {style}",
  "Second citation formatted in {style}"
]

Rules:
- Generate highly realistic citations including real or extremely plausible authors, journals/venues, publication years, and article titles matching the research field.
- Do not add markdown code fences like ```json.
- Return ONLY the raw JSON array.
"""
    truncated_text = text[:6000]
    return _call_ai(system_prompt, f"Paper text:\n{truncated_text}", timeout_seconds=45.0)



