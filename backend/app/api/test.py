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

