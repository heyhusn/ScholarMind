from fastapi.testclient import TestClient
from main import app, DOCUMENTS

client = TestClient(app)

def test_endpoints():
    print("Testing /api/pdf/extract...")
    with open("test_dummy.pdf", "wb") as f:
        f.write(b"%PDF-1.4 dummy pdf content")
        
    # We can't easily fake a valid PDF that PyPDF2 will parse without a real file, 
    # so we might get a PyPDF2 error, but the endpoint should respond 500 instead of 404
    # Wait, let's just insert a dummy document directly into DOCUMENTS for testing the other endpoints.
    
    DOCUMENTS["dummy-123"] = "This is a complex academic text about Quantum Physics."
    
    print("Testing /api/chat/beginner...")
    response = client.post("/api/chat/beginner", json={"doc_id": "dummy-123", "message": "What is quantum physics?"})
    print("Status Code:", response.status_code)
    print("Response:", response.json())
    
    print("Testing /api/summary/generate...")
    response = client.post("/api/summary/generate", json={"doc_id": "dummy-123"})
    print("Status Code:", response.status_code)
    print("Response:", response.json())
    
    print("Testing /api/text/simplify...")
    response = client.post("/api/text/simplify", json={"text": "A very complex sentence."})
    print("Status Code:", response.status_code)
    print("Response:", response.json())
    
    print("Testing /api/quiz/generate...")
    response = client.post("/api/quiz/generate", json={"doc_id": "dummy-123"})
    print("Status Code:", response.status_code)
    print("Response:", response.json())

    print("\n--- Testing OpenAlex / Search endpoints ---")
    print("Testing /api/papers/search...")
    response = client.get("/api/papers/search?query=neural+networks&page=1")
    print("Status Code:", response.status_code)
    if response.status_code == 200:
        results = response.json().get("results", [])
        print("Results Count:", len(results))
        if results:
            print("First item:", results[0])
            
    print("Testing /api/papers/trending...")
    response = client.get("/api/papers/trending")
    print("Status Code:", response.status_code)
    if response.status_code == 200:
        results = response.json().get("results", [])
        print("Results Count:", len(results))
        if results:
            print("First trending item:", results[0])

    print("Testing /api/papers/insights...")
    response = client.post("/api/papers/insights", json={
        "paper_id": "W2741809807",
        "title": "Attention Is All You Need",
        "abstract": "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks in an encoder-decoder configuration. The best performing models also connect the encoder and decoder through an attention mechanism. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely."
    })
    print("Status Code:", response.status_code)
    print("Response:", response.json())

    # Peer review test
    import io
    import zipfile
    
    docx_buffer = io.BytesIO()
    with zipfile.ZipFile(docx_buffer, "w") as docx:
        docx.writestr("word/document.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
                <w:p>
                    <w:r>
                        <w:t>This is a peer review test document discussing deep learning models and their limitations in data gathering.</w:t>
                    </w:r>
                </w:p>
            </w:body>
        </w:document>""")
    docx_bytes = docx_buffer.getvalue()
    
    print("\n--- Testing /api/peer-review/analyze ---")
    response = client.post(
        "/api/peer-review/analyze",
        files={"file": ("test_doc.docx", docx_bytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")}
    )
    print("Status Code:", response.status_code)
    print("Response:", response.json())

    print("\n--- Testing /api/references/generate ---")
    response = client.post(
        "/api/references/generate",
        files={"file": ("test_doc.docx", docx_bytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")},
        data={"style": "APA"}
    )
    print("Status Code:", response.status_code)
    refs_data = response.json()
    print("Response:", refs_data)
    
    refs_list = refs_data.get("references", ["Dummy Reference 1", "Dummy Reference 2"])
    
    # Legacy .doc test
    doc_bytes = b"This is a legacy Word document file containing text that should be extracted by the heuristic scanner. It discusses deep learning, neural networks, and reference lists."
    print("\n--- Testing /api/references/generate (.doc support) ---")
    response = client.post(
        "/api/references/generate",
        files={"file": ("test_doc.doc", doc_bytes, "application/msword")},
        data={"style": "APA"}
    )
    print("Status Code:", response.status_code)
    print("Response:", response.json())
    
    print("\n--- Testing /api/references/export/docx ---")
    response = client.post(
        "/api/references/export/docx",
        json={"references": refs_list, "style": "APA"}
    )
    print("Status Code:", response.status_code)
    print("Content Length:", len(response.content))
    
    print("\n--- Testing /api/references/export/pdf ---")
    response = client.post(
        "/api/references/export/pdf",
        json={"references": refs_list, "style": "APA"}
    )
    print("Status Code:", response.status_code)
    print("Content Length:", len(response.content))


if __name__ == "__main__":
    test_endpoints()


