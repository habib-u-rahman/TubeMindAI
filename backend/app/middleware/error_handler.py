from fastapi import Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.requests import Request as StarletteRequest
import json
import traceback


async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Handle validation errors with better error messages"""
    errors = []
    for error in exc.errors():
        field = " -> ".join(str(loc) for loc in error.get("loc", []))
        message = error.get("msg", "Validation error")
        error_type = error.get("type", "validation_error")
        
        # Handle JSON decode errors specifically
        if error_type == "json_invalid":
            # Try to get more details from the error
            error_ctx = error.get("ctx", {})
            error_msg = error_ctx.get("error", "") if error_ctx else ""
            
            if "control character" in error_msg.lower():
                message = f"Invalid JSON: Control character found at position {field}. Please remove any invisible or special characters. Use /api/test/test-json to debug your JSON."
            else:
                message = f"Invalid JSON format at position {field}. Please check your request body. Use /api/test/test-json endpoint to see exactly what's wrong."
        
        errors.append({
            "field": field,
            "message": message,
            "type": error_type
        })
    
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "detail": errors,
            "message": "Validation error. Please check your input.",
            "hint": "Make sure your JSON is properly formatted and Content-Type header is 'application/json'"
        }
    )


async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """Handle HTTP exceptions"""
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "detail": exc.detail,
            "message": exc.detail
        }
    )


async def json_decode_exception_handler(request: Request, exc: Exception):
    """Handle JSON decode errors"""
    error_msg = str(exc)
    if "JSON" in error_msg or "json" in error_msg.lower() or "decode" in error_msg.lower():
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "detail": "Invalid JSON format in request body",
                "message": "Please ensure your request body is valid JSON. Check for: trailing commas, missing quotes, or control characters.",
                "hint": "Make sure Content-Type is 'application/json' and your JSON is properly formatted"
            }
        )
    return None


async def general_exception_handler(request: Request, exc: Exception):
    """Handle general exceptions"""
    # Check if it's a JSON decode error
    json_error = await json_decode_exception_handler(request, exc)
    if json_error:
        return json_error
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "detail": str(exc),
            "message": "An internal server error occurred"
        }
    )

