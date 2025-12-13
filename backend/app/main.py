from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from app.config import settings
from app.database import engine, Base
from app.middleware.error_handler import (
    validation_exception_handler,
    http_exception_handler,
    general_exception_handler
)
from app.middleware.json_cleaner import JSONCleanerMiddleware

# Import all models to ensure they're registered
from app.models import User, OTP, Video, Note, Chat, PDF, PDFChat

# Create database tables
Base.metadata.create_all(bind=engine)

# Create FastAPI app
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    debug=settings.DEBUG
)

# Add exception handlers
app.add_exception_handler(RequestValidationError, validation_exception_handler)
app.add_exception_handler(StarletteHTTPException, http_exception_handler)
app.add_exception_handler(Exception, general_exception_handler)

# JSON cleaner middleware (to handle malformed JSON)
app.add_middleware(JSONCleanerMiddleware)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    return {
        "message": "Welcome to TubeMind AI API",
        "version": settings.APP_VERSION,
        "status": "running"
    }


@app.get("/health")
async def health_check():
    return {"status": "healthy"}


# Import and include routers
from app.api import auth, video, admin, pdf

app.include_router(auth.router, prefix="/api/auth", tags=["Authentication"])
app.include_router(video.router, prefix="/api/video", tags=["Video Notes"])
app.include_router(pdf.router, prefix="/api/pdf", tags=["PDF Notes"])
app.include_router(admin.router, prefix="/api", tags=["Admin"])

