from typing import List, Optional

from pydantic import BaseModel


class AIRequest(BaseModel):
    prompt: str


class AIResponse(BaseModel):
    response: str


class DocumentRequest(BaseModel):
    doc_id: str
    user_id: Optional[str] = None
    context: Optional[str] = None


class ChatRequest(BaseModel):
    doc_id: str
    message: str
    user_id: Optional[str] = None
    context: Optional[str] = None



class SimplifyRequest(BaseModel):
    text: str


class FlashcardItem(BaseModel):
    question: str
    answer: str


class FlashcardResponse(BaseModel):
    flashcards: List[FlashcardItem]


class TextResponse(BaseModel):
    text: str


class PaperSection(BaseModel):
    key: str
    title: str
    content: str


class PaperAnalysis(BaseModel):
    doc_id: str
    title: str
    authors: str
    year: str
    venue: str
    field: str
    citation_count: int
    citation_impact: str
    citation_score: int
    ai_overview_title: str
    ai_overview_body: str
    sections: List[PaperSection] = []
    abstract: Optional[str] = None
    methodology: Optional[str] = None
    results: Optional[str] = None
    conclusion: Optional[str] = None
    citations_list: Optional[str] = None


class QuizQuestion(BaseModel):
    id: str
    question: str
    options: List[str]
    correct_answer_index: int
    explanation: str


class QuizResponse(BaseModel):
    questions: List[QuizQuestion]


class OpenAlexPaper(BaseModel):
    id: str
    title: str
    authors: str
    year: str
    venue: str
    citation_count: int
    doi: Optional[str] = None
    is_open_access: bool
    open_access_pdf: Optional[str] = None
    primary_topic: Optional[str] = None
    abstract: Optional[str] = None
    keywords: Optional[str] = None
    publisher: Optional[str] = None
    funders: Optional[str] = None
    awards: Optional[str] = None
    domain: Optional[str] = None
    field_name: Optional[str] = None
    subfield: Optional[str] = None
    sdgs: Optional[str] = None
    countries: Optional[str] = None
    continents: Optional[str] = None
    language: Optional[str] = None


class OpenAlexSearchResponse(BaseModel):
    count: int
    page: int
    per_page: int
    results: List[OpenAlexPaper]


class PaperInsightsRequest(BaseModel):
    paper_id: str
    title: str
    abstract: str
    authors: Optional[str] = None


class PaperInsightsResponse(BaseModel):
    summary: str
    key_findings: List[str]
    applications: List[str]
    limitations: List[str]


class PeerReviewResponse(BaseModel):
    limitations: List[str]
    technical_flaws: List[str]
    questions: List[str]


class ReferencesResponse(BaseModel):
    references: List[str]


class ExportReferencesRequest(BaseModel):
    references: List[str]
    style: str  # APA, MLA, IEEE, Chicago


