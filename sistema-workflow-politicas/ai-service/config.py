import os
from dotenv import load_dotenv

load_dotenv()


def get_ai_config():
    provider = os.getenv("AI_PROVIDER", "gemini")
    api_key = os.getenv("AI_API_KEY", "").strip()
    model = os.getenv("AI_MODEL", "gemini-1.5-flash")
    return provider, api_key, model


def ensure_api_key():
    _, api_key, _ = get_ai_config()
    if not api_key:
        raise ValueError("AI_API_KEY is not configured. Set it in ai-service/.env")
    return api_key
