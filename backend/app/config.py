from pydantic_settings import BaseSettings
from typing import List
from pathlib import Path


class Settings(BaseSettings):
    # Database
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "294bibah"
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: str = "5432"
    POSTGRES_DB_NAME: str = "tubemindai_db"
    
    @property
    def DATABASE_URL(self) -> str:
        return f"postgresql://{self.POSTGRES_USER}:{self.POSTGRES_PASSWORD}@{self.POSTGRES_HOST}:{self.POSTGRES_PORT}/{self.POSTGRES_DB_NAME}"
    
    # JWT
    SECRET_KEY: str = "your-secret-key-change-in-production-min-32-chars"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 24 hours (1440 minutes) - better UX
    
    # Application
    APP_NAME: str = "TubeMind AI API"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    # CORS - Allow all origins for development (Android apps don't send Origin header, but this ensures compatibility)
    ALLOWED_ORIGINS: List[str] = ["*"]  # Allow all origins for development
    
    # Email (for OTP)
    SMTP_HOST: str = "smtp.gmail.com"
    SMTP_PORT: int = 587
    SMTP_USER: str = "tubemindai343@gmail.com"
    SMTP_PASSWORD: str = "vknr teqb tjsx stlp"
    
    # APIs
    YOUTUBE_API_KEY: str = ""  # Optional: For YouTube Data API v3 (yt-dlp works without it)
    AI_API_KEY: str = "AIzaSyClNOQEM9XEuRCqVX-J6OgRKk9tyZnjSi0"  # Google Gemini API key - Set directly in code for now
    
    # File Uploads
    UPLOAD_DIR: str = "uploads"  # Directory for storing uploaded PDFs
    MAX_FILE_SIZE: int = 50 * 1024 * 1024  # 50MB max file size
    
    class Config:
        # Load .env file from backend directory (parent of app directory)
        env_file = str(Path(__file__).parent.parent / ".env")
        env_file_encoding = "utf-8"
        case_sensitive = True


settings = Settings()

