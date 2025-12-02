from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
import json
import re


class JSONCleanerMiddleware(BaseHTTPMiddleware):
    """Middleware to clean JSON request bodies by removing control characters"""
    
    @staticmethod
    def clean_control_characters(text: str) -> str:
        """
        Remove control characters from JSON string, but preserve:
        - \n (newline)
        - \t (tab)
        - \r (carriage return) - only when followed by \n
        """
        # Remove control characters (0x00-0x1F) except \n (0x0A), \t (0x09), and \r (0x0D)
        # But we need to be careful - we should only remove them from string values, not from the JSON structure
        # For simplicity, we'll remove them globally but preserve \n and \t
        cleaned = []
        for char in text:
            code = ord(char)
            # Keep printable characters (>= 32), newline (10), tab (9), and carriage return (13)
            if code >= 32 or code in (9, 10, 13):
                cleaned.append(char)
            # Skip other control characters
        return ''.join(cleaned)
    
    async def dispatch(self, request: Request, call_next):
        # Only process JSON requests
        content_type = request.headers.get("content-type", "")
        if content_type.startswith("application/json"):
            try:
                # Read body
                body = await request.body()
                
                if body:
                    # Decode body
                    try:
                        body_str = body.decode("utf-8")
                    except UnicodeDecodeError:
                        # Try with error handling
                        body_str = body.decode("utf-8", errors="replace")
                    
                    # First, try to clean control characters
                    cleaned_body = self.clean_control_characters(body_str)
                    
                    # Try to parse and re-serialize to ensure valid JSON
                    try:
                        json_data = json.loads(cleaned_body)
                        # Re-serialize to ensure clean, valid JSON
                        cleaned_body = json.dumps(json_data, ensure_ascii=False, separators=(',', ':'))
                    except json.JSONDecodeError as e:
                        # If JSON is still invalid after cleaning, try one more aggressive clean
                        # Remove all control characters except \n and \t
                        more_cleaned = re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F]', '', cleaned_body)
                        try:
                            json_data = json.loads(more_cleaned)
                            cleaned_body = json.dumps(json_data, ensure_ascii=False, separators=(',', ':'))
                        except json.JSONDecodeError:
                            # If still invalid, let FastAPI handle the error with the cleaned version
                            cleaned_body = more_cleaned
                    
                    # Create new request with cleaned body
                    # Replace the request body stream
                    cleaned_bytes = cleaned_body.encode("utf-8")
                    
                    async def receive():
                        return {
                            "type": "http.request",
                            "body": cleaned_bytes,
                            "more_body": False
                        }
                    
                    # Replace the receive function
                    request._receive = receive
                    
                    # Also update the scope to reflect the new body length
                    if "headers" in request.scope:
                        # Update content-length header
                        headers = list(request.scope["headers"])
                        for i, (key, value) in enumerate(headers):
                            if key.lower() == b"content-length":
                                headers[i] = (key, str(len(cleaned_bytes)).encode())
                                break
                        else:
                            # Add content-length header if it doesn't exist
                            headers.append((b"content-length", str(len(cleaned_bytes)).encode()))
                        request.scope["headers"] = headers
                
            except Exception as e:
                # If cleaning fails, proceed with original request
                # FastAPI will handle the error
                pass
        
        response = await call_next(request)
        return response

