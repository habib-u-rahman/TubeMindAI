from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
import json
import re

router = APIRouter()


@router.post("/test-json")
async def test_json(request: Request):
    """
    Test endpoint to check if JSON is being received correctly
    Use this to debug JSON format issues before calling the actual API
    """
    try:
        body = await request.body()
        body_str = body.decode("utf-8", errors='replace')
        
        # Show character details around error position
        def show_char_details(text, pos):
            start = max(0, pos - 20)
            end = min(len(text), pos + 20)
            context = text[start:end]
            char_at_pos = text[pos] if pos < len(text) else "EOF"
            char_code = ord(char_at_pos) if pos < len(text) else None
            
            return {
                "context": context,
                "char_at_position": char_at_pos,
                "char_code": char_code,
                "is_control": char_code and char_code < 32 and char_code not in [9, 10, 13] if char_code else False
            }
        
        # Try to parse JSON
        try:
            json_data = json.loads(body_str)
            return {
                "success": True,
                "message": "✅ JSON is valid!",
                "received_data": json_data,
                "body_length": len(body_str),
                "content_type": request.headers.get("content-type"),
                "next_step": "You can now use this JSON in the actual API endpoints"
            }
        except json.JSONDecodeError as e:
            char_info = show_char_details(body_str, e.pos) if hasattr(e, 'pos') else None
            
            # Find control characters
            control_chars = []
            for i, char in enumerate(body_str):
                code = ord(char)
                if code < 32 and code not in [9, 10, 13]:  # Exclude tab, newline, carriage return
                    control_chars.append({
                        "position": i,
                        "char": repr(char),
                        "code": code
                    })
            
            return JSONResponse(
                status_code=400,
                content={
                    "success": False,
                    "message": "❌ Invalid JSON",
                    "error": str(e),
                    "error_message": e.msg if hasattr(e, 'msg') else str(e),
                    "error_position": e.pos if hasattr(e, 'pos') else None,
                    "character_info": char_info,
                    "control_characters_found": control_chars[:10],  # First 10 control chars
                    "body_preview": body_str[:200] if len(body_str) > 200 else body_str,
                    "body_length": len(body_str),
                    "content_type": request.headers.get("content-type"),
                    "hint": "Common issues: trailing commas, missing quotes, control characters, invalid escape sequences",
                    "example_correct_json": {
                        "name": "John Doe",
                        "email": "john@example.com",
                        "password": "password123"
                    }
                }
            )
    except UnicodeDecodeError as e:
        return JSONResponse(
            status_code=400,
            content={
                "success": False,
                "message": "Invalid encoding",
                "error": str(e),
                "hint": "Make sure your request body is UTF-8 encoded"
            }
        )
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "message": "Error processing request",
                "error": str(e)
            }
        )


@router.get("/example-request")
async def example_request():
    """
    Get example of correct JSON format for register endpoint
    """
    return {
        "endpoint": "POST /api/auth/register",
        "content_type": "application/json",
        "example_request_body": {
            "name": "John Doe",
            "email": "john@example.com",
            "password": "password123"
        },
        "curl_example": 'curl -X POST "http://localhost:8000/api/auth/register" -H "Content-Type: application/json" -d \'{"name":"John Doe","email":"john@example.com","password":"password123"}\'',
        "python_example": """
import requests

response = requests.post(
    "http://localhost:8000/api/auth/register",
    json={
        "name": "John Doe",
        "email": "john@example.com",
        "password": "password123"
    }
)
print(response.json())
        """
    }


@router.post("/test-validation-error")
async def test_validation_error(request: Request):
    """
    Test endpoint to see what validation errors look like
    Send an invalid request to see the error format
    """
    from fastapi import Request as FastAPIRequest
    from fastapi.exceptions import RequestValidationError
    from app.schemas.auth import UserLogin
    
    try:
        body = await request.body()
        body_str = body.decode("utf-8", errors='replace')
        
        try:
            json_data = json.loads(body_str)
        except json.JSONDecodeError as e:
            return JSONResponse(
                status_code=400,
                content={
                    "error": "Invalid JSON",
                    "message": str(e),
                    "hint": "Your JSON is malformed. Check for syntax errors."
                }
            )
        
        # Try to validate with Pydantic
        try:
            login_data = UserLogin(**json_data)
            return {
                "success": True,
                "message": "Request is valid!",
                "data": {
                    "email": login_data.email,
                    "password": "***"  # Don't return password
                }
            }
        except Exception as e:
            # This will show what validation errors look like
            if hasattr(e, 'errors'):
                return JSONResponse(
                    status_code=422,
                    content={
                        "error": "Validation failed",
                        "raw_errors": e.errors() if hasattr(e, 'errors') else str(e),
                        "message": "This is what a validation error looks like",
                        "received_data": json_data
                    }
                )
            else:
                return JSONResponse(
                    status_code=400,
                    content={
                        "error": str(e),
                        "received_data": json_data
                    }
                )
                
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={
                "error": "Server error",
                "message": str(e)
            }
        )


@router.post("/validate-login")
async def validate_login(request: Request):
    """
    Validate login request format before calling the actual login endpoint
    Helps debug 422 validation errors
    """
    try:
        body = await request.body()
        body_str = body.decode("utf-8", errors='replace')
        
        try:
            json_data = json.loads(body_str)
            
            # Check required fields
            required_fields = ["email", "password"]
            missing_fields = [field for field in required_fields if field not in json_data]
            
            errors = []
            warnings = []
            
            if missing_fields:
                errors.append(f"Missing required fields: {', '.join(missing_fields)}")
            
            # Validate email
            if "email" in json_data:
                email = json_data["email"]
                if not isinstance(email, str):
                    errors.append("Email must be a string")
                elif "@" not in email or "." not in email:
                    warnings.append("Email format looks invalid. Should be like: user@example.com")
            else:
                errors.append("Email field is required")
            
            # Validate password
            if "password" in json_data:
                password = json_data["password"]
                if not isinstance(password, str):
                    errors.append("Password must be a string")
                elif len(password) == 0:
                    errors.append("Password cannot be empty")
                elif len(password) < 6:
                    warnings.append("Password should be at least 6 characters long")
            else:
                errors.append("Password field is required")
            
            # Check for extra fields
            allowed_fields = ["email", "password"]
            extra_fields = [field for field in json_data.keys() if field not in allowed_fields]
            if extra_fields:
                warnings.append(f"Extra fields found (will be ignored): {', '.join(extra_fields)}")
            
            if errors:
                return JSONResponse(
                    status_code=400,
                    content={
                        "success": False,
                        "message": "Validation errors found",
                        "errors": errors,
                        "warnings": warnings,
                        "received_data": json_data,
                        "correct_format": {
                            "email": "user@example.com",
                            "password": "password123"
                        }
                    }
                )
            
            return {
                "success": True,
                "message": "✅ Login request format is valid!",
                "received_data": json_data,
                "warnings": warnings if warnings else None,
                "next_step": "You can now call POST /api/auth/login with this data"
            }
            
        except json.JSONDecodeError as e:
            return JSONResponse(
                status_code=400,
                content={
                    "success": False,
                    "message": "Invalid JSON format",
                    "error": str(e),
                    "hint": "Make sure your JSON is properly formatted",
                    "correct_format": {
                        "email": "user@example.com",
                        "password": "password123"
                    }
                }
            )
            
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "message": "Error processing request",
                "error": str(e)
            }
        )