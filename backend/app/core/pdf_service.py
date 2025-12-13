"""
PDF Service for extracting text from PDF files
"""
import os
import tempfile
from typing import Optional, Tuple
from pathlib import Path
from app.config import settings


def extract_text_from_pdf(pdf_file_path: str) -> Optional[Tuple[str, int]]:
    """
    Extract text from PDF file
    Returns: (extracted_text, page_count) or None if extraction fails
    """
    try:
        # Try pdfplumber first (better for complex PDFs)
        try:
            import pdfplumber
            text_parts = []
            page_count = 0
            
            with pdfplumber.open(pdf_file_path) as pdf:
                page_count = len(pdf.pages)
                for page in pdf.pages:
                    page_text = page.extract_text()
                    if page_text:
                        text_parts.append(page_text)
            
            extracted_text = "\n\n".join(text_parts)
            
            if settings.DEBUG:
                print(f"DEBUG: Extracted {len(extracted_text)} characters from {page_count} pages using pdfplumber")
            
            return extracted_text, page_count
            
        except ImportError:
            # Fallback to PyPDF2
            try:
                import PyPDF2
                text_parts = []
                page_count = 0
                
                with open(pdf_file_path, 'rb') as file:
                    pdf_reader = PyPDF2.PdfReader(file)
                    page_count = len(pdf_reader.pages)
                    
                    for page_num in range(page_count):
                        page = pdf_reader.pages[page_num]
                        page_text = page.extract_text()
                        if page_text:
                            text_parts.append(page_text)
                
                extracted_text = "\n\n".join(text_parts)
                
                if settings.DEBUG:
                    print(f"DEBUG: Extracted {len(extracted_text)} characters from {page_count} pages using PyPDF2")
                
                return extracted_text, page_count
                
            except ImportError:
                if settings.DEBUG:
                    print("DEBUG: ERROR - No PDF library installed. Install pdfplumber or PyPDF2")
                return None
                
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error extracting text from PDF: {str(e)}")
        return None


def save_uploaded_pdf(file_content: bytes, filename: str, user_id: int) -> Optional[str]:
    """
    Save uploaded PDF file to disk
    Returns: file_path or None if save fails
    """
    try:
        # Create uploads directory if it doesn't exist
        uploads_dir = Path(settings.UPLOAD_DIR if hasattr(settings, 'UPLOAD_DIR') else "uploads")
        uploads_dir.mkdir(parents=True, exist_ok=True)
        
        # Create user-specific directory
        user_dir = uploads_dir / f"user_{user_id}"
        user_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate unique filename
        file_path = user_dir / filename
        
        # Save file
        with open(file_path, 'wb') as f:
            f.write(file_content)
        
        if settings.DEBUG:
            print(f"DEBUG: Saved PDF to: {file_path}")
        
        return str(file_path)
        
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error saving PDF file: {str(e)}")
        return None


def delete_pdf_file(file_path: str) -> bool:
    """
    Delete PDF file from disk
    Returns: True if deleted, False otherwise
    """
    try:
        if os.path.exists(file_path):
            os.remove(file_path)
            if settings.DEBUG:
                print(f"DEBUG: Deleted PDF file: {file_path}")
            return True
        return False
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error deleting PDF file: {str(e)}")
        return False

