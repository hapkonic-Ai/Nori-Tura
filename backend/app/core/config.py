from pydantic_settings import BaseSettings
from pydantic import validator
from functools import lru_cache
import os


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql://localhost:5432/nonitura"
    JWT_SECRET: str = "your-secret-key-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_HOURS: int = 2  # Reduced from 30 days; role-based expiry in security.py
    ENVIRONMENT: str = "development"
    EXTRA_CORS_ORIGINS: str = ""  # comma-separated list of additional CORS origins

    # OTP
    TWO_FACTOR_API_KEY: str = ""
    MSG91_AUTH_KEY: str = ""
    OTP_EXPIRY_MINUTES: int = 5
    OTP_LENGTH: int = 6

    # AI
    OPENAI_API_KEY: str = ""
    ANTHROPIC_API_KEY: str = ""
    AI_MODEL: str = "gpt-4o"

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = ""
    CLOUDINARY_API_KEY: str = ""
    CLOUDINARY_API_SECRET: str = ""

    # Meta WhatsApp
    META_WA_TOKEN: str = ""
    META_WA_PHONE_ID: str = ""

    # SMS
    SMS_SENDER_ID: str = "NONITU"

    # Firebase
    FIREBASE_CREDENTIALS_JSON: str = ""

    # RAG endpoints (optional — features degrade gracefully if not set)
    RAG_DIAGNOSIS_URL: str = ""   # e.g. https://your-rag.example.com/query/diagnosis
    RAG_CONSENT_URL: str = ""     # e.g. https://your-rag.example.com/query/consent
    RAG_API_KEY: str = ""         # shared bearer token for your RAG service
    RAG_TIMEOUT_SECONDS: int = 10

    @validator("JWT_SECRET")
    def validate_jwt_secret(cls, v, values):
        env = values.get("ENVIRONMENT", os.getenv("ENVIRONMENT", "development"))
        if env == "production":
            if v == "your-secret-key-change-in-production":
                raise ValueError("JWT_SECRET must be changed from the default in production")
            if len(v) < 32:
                raise ValueError("JWT_SECRET must be at least 32 characters in production")
        return v

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
