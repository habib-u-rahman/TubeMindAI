from fastapi import Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.requests import Request as StarletteRequest
from pydantic import ValidationError
import json
import traceback


async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Handle validation errors with better error messages"""
    errors = []
    error_list = exc.errors()
    
    # Debug: Print raw errors if in debug mode
    try:
        from app.config import settings as app_settings
        if app_settings.DEBUG:
            print(f"DEBUG: Validation errors received: {error_list}")
            print(f"DEBUG: Error count: {len(error_list)}")
    except Exception as e:
        print(f"DEBUG: Could not import settings: {e}")
    
    for error in error_list:
        # Extract error details safely
        loc_list = error.get("loc", [])
        field = " -> ".join(str(loc) for loc in loc_list) if loc_list else "body"
        original_msg = error.get("msg", "Validation error")
        error_type = error.get("type", "validation_error")
        error_ctx = error.get("ctx", {}) or {}
        error_input = error.get("input", None)
        
        # Start with original message, then enhance it
        message = str(original_msg) if original_msg else "Validation error"
        
        # Handle specific validation error types with user-friendly messages
        if error_type == "missing":
            field_name = field.split(" -> ")[-1] if " -> " in field else field
            message = f"Field '{field_name}' is required. Please include this field in your request."
        elif error_type == "value_error.email":
            message = f"Invalid email format for field '{field}'. Please provide a valid email address (e.g., user@example.com)."
        elif error_type == "value_error.str.regex":
            if "purpose" in field.lower():
                message = f"Invalid value for '{field}'. Must be either 'signup' or 'forgot_password'."
            else:
                message = f"Invalid format for field '{field}'. Please check the value."
        elif error_type == "value_error.any_str.min_length":
            min_length = error_ctx.get("limit_value", "?") if error_ctx else "?"
            if "password" in field.lower():
                message = f"Password must be at least {min_length} characters long."
            elif "otp" in field.lower():
                message = f"OTP code must be at least {min_length} digits."
            else:
                message = f"Field '{field}' must be at least {min_length} characters long."
        elif error_type == "value_error.any_str.max_length":
            max_length = error_ctx.get("limit_value", "?") if error_ctx else "?"
            message = f"Field '{field}' must not exceed {max_length} characters."
        elif error_type == "type_error.str":
            message = f"Field '{field}' must be a string/text value."
        elif error_type == "type_error.integer":
            message = f"Field '{field}' must be a number/integer."
        elif error_type == "json_invalid":
            error_msg = str(error_ctx.get("error", "")) if error_ctx else ""
            if "control character" in error_msg.lower():
                message = f"Invalid JSON: Control character found. Please remove any invisible or special characters. Use /api/test/test-json to debug your JSON."
            else:
                message = f"Invalid JSON format. Please check your request body. Use /api/test/test-json endpoint to see exactly what's wrong."
        elif error_type == "value_error":
            # Generic value error - try to provide context
            if error_ctx:
                message = f"Invalid value for '{field}': {message}"
        
        # Build error object
        error_obj = {
            "field": field,
            "message": message,
            "type": error_type
        }
        
        # Add input if available (but limit size for security)
        if error_input is not None:
            try:
                input_str = str(error_input)
                if len(input_str) > 100:
                    input_str = input_str[:100] + "..."
                error_obj["input"] = input_str
            except:
                pass
        
        errors.append(error_obj)
    
    # Create a summary message
    if len(errors) == 1:
        summary = errors[0]["message"]
    else:
        field_names = [e['field'].split(" -> ")[-1] for e in errors]
        summary = f"Multiple validation errors found. Please check the following fields: {', '.join(field_names)}"
    
    # Build response content
    response_content = {
        "detail": errors,
        "message": summary,
        "errors": errors,  # Alias for easier access
        "hint": "Make sure your JSON is properly formatted, Content-Type header is 'application/json', and all required fields are included.",
        "status_code": 422
    }
    
    # Add example request format based on the endpoint
    try:
        path = request.url.path
        if "/login" in path:
            response_content["example_request"] = {
                "email": "user@example.com",
                "password": "password123"
            }
        elif "/register" in path:
            response_content["example_request"] = {
                "name": "John Doe",
                "email": "john@example.com",
                "password": "password123"
            }
        elif "/verify-otp" in path:
            response_content["example_request"] = {
                "email": "user@example.com",
                "otp_code": "123456",
                "purpose": "signup"
            }
    except:
        pass
    
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=response_content
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

