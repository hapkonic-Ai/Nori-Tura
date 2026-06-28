from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql://localhost:5432/nonitura"
    JWT_SECRET: str = "your-secret-key-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_HOURS: int = 24 * 30  # 30 days
    
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
    
    # SMS (2Factor.in / MSG91)
    SMS_SENDER_ID: str = "NONITU"
    
    # Firebase
    FIREBASE_CREDENTIALS_JSON: str = ""
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
