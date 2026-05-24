from fastapi import FastAPI

app = FastAPI(title="AI Service for Workflow Policies")

@app.get("/")
async def root():
    return {"message": "AI Service is running"}
