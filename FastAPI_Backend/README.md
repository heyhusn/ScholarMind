# Scholar Mind Backend

FastAPI backend for the Scholar Mind app. It powers PDF analysis, AI chat, summaries, quizzes, flashcards, peer review, reference generation, exports, and OpenAlex research paper search.

## Deployment Status

This backend has been deployed on Vercel.

- Production API: `https://your-vercel-deployment-url.vercel.app`
- API docs: `https://your-vercel-deployment-url.vercel.app/docs`
- Health check: `https://your-vercel-deployment-url.vercel.app/`

Replace the placeholder URL above with the actual Vercel production URL for this project.

Expected health check response:

```json
{
  "message": "Welcome to Scholar Mind AI Backend"
}
```

## Features

- PDF upload and text extraction
- AI-powered paper analysis
- Temporary in-memory document context
- Beginner, technical, and freeform chat modes
- Flashcard, quiz, summary, and podcast script generation
- Text simplification
- OpenAlex paper search and trending papers
- Paper insights
- Peer review analysis
- Reference generation
- DOCX and PDF reference exports

## Project Structure

```text
scholar_mind_backend/
|-- main.py
|-- ai.py
|-- models.py
|-- pdf.py
|-- requirements.txt
|-- vercel.json
|-- test_endpoints.py
`-- README.md
```

## Requirements

- Python 3.10 or newer
- `pip`
- DeepSeek-compatible API key
- Optional OpenAlex API key

Environment variables:

```env
DEEPSEEK_API_KEY=your_api_key_here
OPENALEX_API_KEY=optional_key_here
```

For local development, place these in a `.env` file. For production, add them in the Vercel project settings.

## Local Setup

Open the backend folder:

```powershell
cd scholar_mind_backend
```

Create and activate a virtual environment:

```powershell
python -m venv .venv
.venv\Scripts\Activate
```

Install dependencies:

```powershell
pip install -r requirements.txt
```

Create a `.env` file:

```env
DEEPSEEK_API_KEY=your_api_key_here
OPENALEX_API_KEY=optional_key_here
```

Run the local FastAPI server:

```powershell
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Open the local API docs:

```text
http://127.0.0.1:8000/docs
```

## Vercel Deployment

The project is configured for Vercel with [vercel.json](vercel.json).

```json
{
  "version": 2,
  "builds": [
    {
      "src": "main.py",
      "use": "@vercel/python"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "main.py"
    }
  ]
}
```

### Deploy From The CLI

Install the Vercel CLI:

```powershell
npm i -g vercel
```

Log in:

```powershell
vercel login
```

Deploy from the backend folder:

```powershell
vercel
```

Deploy to production:

```powershell
vercel --prod
```

### Production Environment Variables

Add these variables in the Vercel dashboard under Project Settings > Environment Variables:

- `DEEPSEEK_API_KEY`
- `OPENALEX_API_KEY` if you use authenticated OpenAlex requests

After adding or changing environment variables, redeploy the project.

## Main Endpoints

### Health

- `GET /` - backend health check

### Paper Processing

- `POST /api/pdf/extract` - extract text from a PDF
- `POST /api/pdf/analyze` - analyze a PDF and return structured results
- `GET /api/pdf/analysis/{doc_id}` - fetch saved analysis

### Chat And Learning

- `POST /api/chat/beginner`
- `POST /api/chat/technical`
- `POST /api/chat/freeform`
- `POST /api/flashcards/generate`
- `POST /api/podcast/generate`
- `POST /api/summary/generate`
- `POST /api/text/simplify`
- `POST /api/quiz/generate`

### Research Discovery

- `GET /api/papers/search`
- `GET /api/papers/trending`
- `GET /api/papers/{paper_id}`
- `POST /api/papers/insights`

### Review And References

- `POST /api/peer-review/analyze`
- `POST /api/references/generate`
- `POST /api/references/export/docx`
- `POST /api/references/export/pdf`

## How The Backend Works

1. The client uploads a PDF.
2. [pdf.py](pdf.py) extracts the document text.
3. [ai.py](ai.py) sends the text to the AI layer for structured paper analysis.
4. [main.py](main.py) repairs malformed AI JSON when needed.
5. If parsing still fails, the backend returns fallback analysis data.
6. The stored paper context is reused for chat, quizzes, summaries, flashcards, peer review, and references.

## Testing

Run the endpoint test script after starting the local server:

```powershell
python test_endpoints.py
```

For the deployed backend, open:

```text
https://your-vercel-deployment-url.vercel.app/docs
```

Then test endpoints directly from Swagger UI.

## Important Notes

- CORS is currently open with `allow_origins=["*"]`.
- Document analysis context is stored in memory, so it can be lost when the server restarts or a serverless function instance changes.
- Vercel deployments are serverless, so avoid relying on long-lived local memory for permanent data.
- Some fallback analysis data can be rebuilt from Firestore if `user_id` is available.
- PDF and DOCX files are supported for peer review and reference generation.

## Troubleshooting

### Local server does not start

Check that:

- the virtual environment is activated
- dependencies are installed
- `uvicorn` is available
- required environment variables are present

### AI requests fail

Check that:

- `DEEPSEEK_API_KEY` is set locally or in Vercel
- the key is valid
- the deployment has been redeployed after adding environment variables

### OpenAlex search fails

Check that:

- the API is reachable
- internet access is working
- `OPENALEX_API_KEY` is correct if you are using one

### PDF upload fails

Check that:

- the uploaded file is a valid `.pdf`
- the file is not corrupted
- the request is sent as multipart form data

### Vercel deployment works but API requests fail

Check that:

- `DEEPSEEK_API_KEY` is configured in Vercel
- the latest production deployment is active
- [vercel.json](vercel.json) still points all routes to `main.py`
- the frontend is calling the production Vercel URL, not `localhost`

## Useful Files

- [main.py](main.py) - FastAPI app and routes
- [ai.py](ai.py) - AI generation logic
- [models.py](models.py) - request and response models
- [pdf.py](pdf.py) - PDF and DOCX extraction helpers
- [test_endpoints.py](test_endpoints.py) - endpoint test script
- [vercel.json](vercel.json) - Vercel deployment configuration
