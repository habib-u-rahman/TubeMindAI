"""
AI Service for generating notes from video transcripts using Google Gemini API
"""
from typing import Optional, Dict
import json
from app.config import settings


def generate_notes_from_transcript(transcript: str, video_title: str = "") -> Optional[Dict]:
    """
    Generate notes from video transcript using Google Gemini API
    Returns: dict with 'summary', 'key_points', 'bullet_notes' or None if generation fails
    """
    # Validate transcript
    if not transcript or not transcript.strip():
        if settings.DEBUG:
            print("DEBUG: No transcript provided to generate_notes_from_transcript")
        return None
    
    # Check transcript length
    transcript = transcript.strip()
    if len(transcript) < 50:  # Too short to generate meaningful notes
        if settings.DEBUG:
            print(f"DEBUG: Transcript too short ({len(transcript)} chars), cannot generate notes")
        return None
    
    if settings.DEBUG:
        print(f"DEBUG: Generating notes for video: {video_title}")
        print(f"DEBUG: Transcript length: {len(transcript)}")
        print(f"DEBUG: AI_API_KEY present: {bool(settings.AI_API_KEY)}")
        try:
            print(f"DEBUG: Transcript preview (first 200 chars): {transcript[:200]}")
        except UnicodeEncodeError:
            print(f"DEBUG: Transcript contains special characters (preview skipped)")
    
    # Check if AI API key is configured
    if not settings.AI_API_KEY or not settings.AI_API_KEY.strip():
        if settings.DEBUG:
            print("DEBUG: ERROR - No AI_API_KEY found! Please set AI_API_KEY in .env file or environment variables.")
            print("DEBUG: Get your Gemini API key from: https://makersuite.google.com/app/apikey")
        return None
    
    # Try Gemini API
    if settings.DEBUG:
        print("DEBUG: Attempting to use Gemini API...")
    
    result = generate_notes_with_gemini(transcript, video_title)
    
    if result:
        # Validate result has all required fields
        if result.get("summary") and result.get("key_points") and result.get("bullet_notes"):
            if settings.DEBUG:
                print("DEBUG: Gemini API returned notes successfully")
                print(f"DEBUG: Summary length: {len(result.get('summary', ''))}")
                print(f"DEBUG: Key points length: {len(result.get('key_points', ''))}")
                print(f"DEBUG: Bullet notes length: {len(result.get('bullet_notes', ''))}")
            return result
        else:
            if settings.DEBUG:
                print("DEBUG: Gemini API returned incomplete notes (missing fields)")
                print(f"DEBUG: Result keys: {list(result.keys())}")
    else:
        if settings.DEBUG:
            print("DEBUG: ERROR - Gemini API returned None. Check API key and network connection.")
    
    # Return None if generation fails - let caller handle fallback
    return None


def generate_notes_with_gemini(transcript: str, video_title: str = "") -> Optional[Dict]:
    """
    Generate notes using Google Gemini API
    Requires AI_API_KEY (Gemini API key) in settings
    """
    if not settings.AI_API_KEY:
        return None
    
    try:
        import google.generativeai as genai
        
        # Configure Gemini API
        genai.configure(api_key=settings.AI_API_KEY)
        
        # Initialize the model (try best available models)
        # Updated to use newer Gemini 2.x models
        model = None
        model_names = ['gemini-2.5-flash', 'gemini-2.0-flash-001', 'gemini-2.0-flash', 'gemini-pro-latest', 'gemini-flash-latest']
        
        for model_name in model_names:
            try:
                model = genai.GenerativeModel(model_name)
                if settings.DEBUG:
                    print(f"DEBUG: Using model: {model_name}")
                break
            except Exception as e:
                if settings.DEBUG:
                    print(f"DEBUG: Model {model_name} not available: {str(e)}")
                continue
        
        if model is None:
            if settings.DEBUG:
                print("DEBUG: No Gemini model available")
            return None
        
        # Limit transcript length to avoid token limits
        # Gemini 1.5 supports longer context, use more characters for comprehensive analysis
        # Use up to 50000 characters to capture more content
        if len(transcript) > 50000:
            # For very long transcripts, take strategic parts: beginning (intro), middle sample, and end (conclusion)
            intro_part = transcript[:20000]  # First 20k chars (usually intro and main topics)
            middle_sample = transcript[len(transcript)//2 - 5000:len(transcript)//2 + 5000]  # Sample from middle
            end_part = transcript[-20000:]  # Last 20k chars (usually conclusion and summary)
            transcript_limited = intro_part + "\n\n[... middle content ...]\n\n" + middle_sample + "\n\n[... more content ...]\n\n" + end_part
            if settings.DEBUG:
                print(f"DEBUG: Transcript truncated from {len(transcript)} to ~50000 characters (strategic sampling)")
        elif len(transcript) > 30000:
            # For medium-long transcripts, take first and last parts
            first_part = transcript[:20000]
            last_part = transcript[-20000:]
            transcript_limited = first_part + "\n\n[... middle content ...]\n\n" + last_part
            if settings.DEBUG:
                print(f"DEBUG: Transcript truncated from {len(transcript)} to ~40000 characters")
        else:
            transcript_limited = transcript
            if settings.DEBUG:
                print(f"DEBUG: Using full transcript, length: {len(transcript)}")
        
        # Concise but effective prompt for faster generation
        prompt = f"""Analyze this YouTube video transcript and create comprehensive notes.

Video Title: {video_title}

TRANSCRIPT:
{transcript_limited}

Create detailed notes with:

SUMMARY:
Write 3-4 paragraphs (200-300 words) covering:
- Main purpose and themes
- Key messages and takeaways
- Important examples or demonstrations
- Practical applications

KEY POINTS:
List 8-12 key points (each starting with "•"), including:
- Specific details, numbers, names from the video
- Important concepts and explanations
- Tips, warnings, or best practices
- Action items or recommendations

BULLET NOTES:
Create 15-25 detailed bullet points (each starting with "•") covering:
- All main topics and subtopics
- Step-by-step processes or procedures
- Examples, case studies, demonstrations
- Important statistics, data, or measurements
- Tools, resources, or references mentioned
- Definitions and terminology

Format EXACTLY as:
SUMMARY:
[Your summary here]

KEY POINTS:
• [Point 1]
• [Point 2]
[Continue...]

BULLET NOTES:
• [Note 1]
• [Note 2]
[Continue...]

Be specific, accurate, and comprehensive. Use exact details from the transcript."""
        
        # Generate content
        try:
            if settings.DEBUG:
                print(f"DEBUG: Sending prompt to Gemini API (prompt length: {len(prompt)})")
            
            response = model.generate_content(prompt)
            
            if response and response.text:
                if settings.DEBUG:
                    print(f"DEBUG: Gemini API response received, length: {len(response.text)}")
                    try:
                        print(f"DEBUG: Response preview (first 500 chars): {response.text[:500]}")
                    except UnicodeEncodeError:
                        print(f"DEBUG: Response received (contains special characters, preview skipped)")
                
                # Parse the response
                if settings.DEBUG:
                    print(f"DEBUG: Raw response text length: {len(response.text)}")
                    try:
                        print(f"DEBUG: Raw response first 1000 chars:\n{response.text[:1000]}")
                    except UnicodeEncodeError:
                        print(f"DEBUG: Raw response received (contains special characters, preview skipped)")
                
                parsed_result = parse_gemini_response(response.text, video_title)
                
                if settings.DEBUG:
                    print(f"DEBUG: After parsing - parsed_result is None: {parsed_result is None}")
                    if parsed_result:
                        print(f"DEBUG: Parsed result keys: {list(parsed_result.keys())}")
                        print(f"DEBUG: Summary exists: {bool(parsed_result.get('summary'))}, length: {len(parsed_result.get('summary', ''))}")
                        print(f"DEBUG: Key points exists: {bool(parsed_result.get('key_points'))}, length: {len(parsed_result.get('key_points', ''))}")
                        print(f"DEBUG: Bullet notes exists: {bool(parsed_result.get('bullet_notes'))}, length: {len(parsed_result.get('bullet_notes', ''))}")
                        if parsed_result.get('summary'):
                            try:
                                print(f"DEBUG: Summary preview: {parsed_result.get('summary')[:200]}...")
                            except UnicodeEncodeError:
                                print(f"DEBUG: Summary generated (contains special characters, preview skipped)")
                
                # Check if parsing returned None
                if parsed_result is None:
                    if settings.DEBUG:
                        print("DEBUG: ERROR - parse_gemini_response returned None")
                    return None
                
                # Validate parsed result
                if parsed_result.get("summary") and parsed_result.get("key_points") and parsed_result.get("bullet_notes"):
                    # Check if it's not placeholder
                    summary = parsed_result.get("summary", "")
                    if "contains valuable content" in summary.lower() and "watch the video" in summary.lower():
                        if settings.DEBUG:
                            print("DEBUG: WARNING - Parsed result looks like placeholder, this is an error!")
                        return None
                    if settings.DEBUG:
                        print("DEBUG: SUCCESS - All fields present and validated, returning parsed result")
                    return parsed_result
                else:
                    if settings.DEBUG:
                        print("DEBUG: ERROR - Parsed result is missing required fields")
                        print(f"DEBUG: Missing fields - summary: {not parsed_result.get('summary')}, key_points: {not parsed_result.get('key_points')}, bullet_notes: {not parsed_result.get('bullet_notes')}")
                    return None
            else:
                if settings.DEBUG:
                    print("DEBUG: ERROR - Gemini API returned empty response")
                    if response:
                        print(f"DEBUG: Response object type: {type(response)}")
                        if hasattr(response, 'prompt_feedback'):
                            print(f"DEBUG: Prompt feedback: {response.prompt_feedback}")
                return None
        except Exception as api_error:
            if settings.DEBUG:
                print(f"DEBUG: ERROR - Gemini API exception: {str(api_error)}")
                print(f"DEBUG: Error type: {type(api_error).__name__}")
                import traceback
                print(f"DEBUG: Full traceback:\n{traceback.format_exc()}")
                
                # Check for specific error types
                error_str = str(api_error).lower()
                error_type = type(api_error).__name__
                
                if "permissiondenied" in error_type.lower() or "permission denied" in error_str or "leaked" in error_str:
                    print("DEBUG: ERROR - API key is invalid or has been reported as leaked!")
                    print("DEBUG: ERROR - Please get a new API key from: https://makersuite.google.com/app/apikey")
                    print("DEBUG: ERROR - Update AI_API_KEY in backend/app/config.py")
                elif "api key" in error_str or "authentication" in error_str or "permission" in error_str:
                    print("DEBUG: ERROR - API key issue detected! Check if the API key is valid.")
                elif "quota" in error_str or "limit" in error_str:
                    print("DEBUG: ERROR - API quota/limit exceeded!")
                elif "network" in error_str or "connection" in error_str or "timeout" in error_str:
                    print("DEBUG: ERROR - Network/connection issue!")
            return None
            
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: ERROR - Exception in generate_notes_with_gemini: {str(e)}")
            print(f"DEBUG: Error type: {type(e).__name__}")
            import traceback
            print(f"DEBUG: Full traceback:\n{traceback.format_exc()}")
        return None


def parse_gemini_response(response_text: str, video_title: str = "") -> Dict:
    """
    Parse Gemini API response and extract summary, key points, and bullet notes
    """
    try:
        # Split response into sections
        summary = ""
        key_points = ""
        bullet_notes = ""
        
        # Try to find sections
        lines = response_text.split('\n')
        current_section = None
        current_content = []
        
        for line in lines:
            line = line.strip()
            
            if line.upper().startswith('SUMMARY:'):
                current_section = 'summary'
                current_content = []
                # Get text after "SUMMARY:"
                text_after = line[8:].strip()
                if text_after:
                    current_content.append(text_after)
                continue
            elif line.upper().startswith('KEY POINTS:'):
                if current_section == 'summary' and current_content:
                    summary = '\n'.join(current_content).strip()
                current_section = 'key_points'
                current_content = []
                # Get text after "KEY POINTS:"
                text_after = line[11:].strip()
                if text_after and text_after.startswith('•'):
                    current_content.append(text_after)
                continue
            elif line.upper().startswith('BULLET NOTES:'):
                if current_section == 'key_points' and current_content:
                    key_points = '\n'.join(current_content).strip()
                current_section = 'bullet_notes'
                current_content = []
                # Get text after "BULLET NOTES:"
                text_after = line[13:].strip()
                if text_after and text_after.startswith('•'):
                    current_content.append(text_after)
                continue
            elif line and current_section:
                # Accept any content for summary, bullet points for key_points and bullet_notes
                if current_section == 'summary':
                    current_content.append(line)
                elif (line.startswith('•') or line.startswith('-') or line.startswith('*')) or current_section in ['key_points', 'bullet_notes']:
                    current_content.append(line)
        
        # Save the last section
        if current_section == 'summary' and current_content:
            summary = '\n'.join(current_content).strip()
        elif current_section == 'key_points' and current_content:
            key_points = '\n'.join(current_content).strip()
        elif current_section == 'bullet_notes' and current_content:
            bullet_notes = '\n'.join(current_content).strip()
        
        if settings.DEBUG:
            print(f"DEBUG: After parsing sections - summary length: {len(summary)}, key_points length: {len(key_points)}, bullet_notes length: {len(bullet_notes)}")
        
        # If parsing failed, try alternative parsing methods
        if not summary or not key_points or not bullet_notes:
            if settings.DEBUG:
                print("DEBUG: Primary parsing failed, trying alternative methods...")
            
            # Try splitting by double newlines
            parts = response_text.split('\n\n')
            if len(parts) >= 3:
                # Try to identify which part is which
                for part in parts:
                    part_upper = part.upper().strip()
                    if 'SUMMARY' in part_upper and not summary:
                        summary = part.replace('SUMMARY:', '').replace('Summary:', '').strip()
                    elif ('KEY POINT' in part_upper or 'KEYPOINT' in part_upper) and not key_points:
                        key_points = part.replace('KEY POINTS:', '').replace('Key Points:', '').strip()
                    elif ('BULLET' in part_upper and 'NOTE' in part_upper) and not bullet_notes:
                        bullet_notes = part.replace('BULLET NOTES:', '').replace('Bullet Notes:', '').strip()
            
            # If still missing, try regex-based extraction
            if not summary or not key_points or not bullet_notes:
                import re
                # Try to find summary (usually first large block of text)
                summary_match = re.search(r'(?i)summary[:\s]*(.+?)(?=\n\s*(?:KEY|BULLET|$))', response_text, re.DOTALL)
                if summary_match and not summary:
                    summary = summary_match.group(1).strip()
                
                # Try to find key points
                key_points_match = re.search(r'(?i)key\s+points?[:\s]*(.+?)(?=\n\s*(?:BULLET|SUMMARY|$))', response_text, re.DOTALL)
                if key_points_match and not key_points:
                    key_points = key_points_match.group(1).strip()
                
                # Try to find bullet notes
                bullet_match = re.search(r'(?i)bullet\s+notes?[:\s]*(.+?)(?=\n\s*(?:SUMMARY|KEY|$))', response_text, re.DOTALL)
                if bullet_match and not bullet_notes:
                    bullet_notes = bullet_match.group(1).strip()
        
        # Validate that we have real content (not empty or too short) - more lenient validation
        if not summary or len(summary.strip()) < 50:
            if settings.DEBUG:
                print(f"DEBUG: ERROR - Summary is missing or too short ({len(summary) if summary else 0} chars, need at least 50)")
            return None
        
        if not key_points or len(key_points.strip()) < 20:
            if settings.DEBUG:
                print(f"DEBUG: ERROR - Key points are missing or too short ({len(key_points) if key_points else 0} chars, need at least 20)")
            return None
        
        if not bullet_notes or len(bullet_notes.strip()) < 20:
            if settings.DEBUG:
                print(f"DEBUG: ERROR - Bullet notes are missing or too short ({len(bullet_notes) if bullet_notes else 0} chars, need at least 20)")
            return None
        
        # Final validation - check for placeholder patterns
        summary_lower = summary.lower()
        if "contains valuable content" in summary_lower and "watch the video" in summary_lower:
            if settings.DEBUG:
                print("DEBUG: ERROR - Detected placeholder pattern in summary")
            return None
        
        if settings.DEBUG:
            print(f"DEBUG: Successfully parsed all sections")
            print(f"DEBUG: Summary length: {len(summary)}, Key points length: {len(key_points)}, Bullet notes length: {len(bullet_notes)}")
        
        return {
            "summary": summary.strip(),
            "key_points": key_points.strip(),
            "bullet_notes": bullet_notes.strip()
        }
        
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: ERROR - Exception parsing Gemini response: {str(e)}")
            import traceback
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        
        # Return None - let caller handle error properly
        return None


def generate_notes_with_openai(transcript: str, video_title: str = "") -> Optional[Dict]:
    """
    Generate notes using OpenAI API
    Requires OPENAI_API_KEY in settings (or AI_API_KEY)
    """
    if not settings.AI_API_KEY:
        return None
    
    try:
        import openai
        
        # Set API key
        openai.api_key = settings.AI_API_KEY
        
        prompt = f"""Analyze this video transcript and create comprehensive notes.

Video Title: {video_title}

Transcript:
{transcript[:4000]}

Please provide:
1. A concise summary (2-3 paragraphs)
2. Key points (4-6 main points, each on a new line starting with •)
3. Detailed bullet notes (organized by topic, each on a new line starting with •)

Format your response as JSON:
{{
    "summary": "...",
    "key_points": "...",
    "bullet_notes": "..."
}}"""
        
        # Call OpenAI API
        # response = openai.ChatCompletion.create(...)
        # Parse response and return
        
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error generating notes with AI: {str(e)}")
    
    return None


def generate_chat_response(user_message: str, video_transcript: str, video_title: str = "", conversation_history: list = None) -> Optional[str]:
    """
    Generate AI chat response for video Q&A using Google Gemini API
    Requires AI_API_KEY (Gemini API key) in settings
    
    Args:
        user_message: The user's question
        video_transcript: The video transcript for context
        video_title: The video title
        conversation_history: List of previous messages in format [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
    
    Returns:
        AI response string or None if generation fails
    """
    if not settings.AI_API_KEY:
        return None
    
    if not user_message or not user_message.strip():
        return None
    
    try:
        import google.generativeai as genai
        
        # Configure Gemini API
        genai.configure(api_key=settings.AI_API_KEY)
        
        # Initialize the model
        model = None
        model_names = ['gemini-2.5-flash', 'gemini-2.0-flash-001', 'gemini-2.0-flash', 'gemini-pro-latest', 'gemini-flash-latest']
        
        for model_name in model_names:
            try:
                model = genai.GenerativeModel(model_name)
                if settings.DEBUG:
                    print(f"DEBUG: Using model {model_name} for chat")
                break
            except Exception as e:
                if settings.DEBUG:
                    print(f"DEBUG: Model {model_name} not available: {str(e)}")
                continue
        
        if model is None:
            if settings.DEBUG:
                print("DEBUG: No Gemini model available for chat")
            return None
        
        # Limit transcript length for context (use more for chat to have better context)
        transcript_context = video_transcript
        if len(transcript_context) > 30000:
            # For long transcripts, use strategic sampling
            intro_part = transcript_context[:15000]
            end_part = transcript_context[-15000:]
            transcript_context = intro_part + "\n\n[... middle content ...]\n\n" + end_part
            if settings.DEBUG:
                print(f"DEBUG: Transcript truncated for chat context from {len(video_transcript)} to ~30000 characters")
        
        # Build conversation history if provided
        chat_history = []
        if conversation_history:
            for msg in conversation_history[-10:]:  # Keep last 10 messages for context
                role = msg.get("role", "user")
                content = msg.get("content", "")
                if role == "user":
                    chat_history.append({"role": "user", "parts": [content]})
                elif role == "assistant":
                    chat_history.append({"role": "model", "parts": [content]})
        
        # Create system context for chat
        system_context = f"""You are an expert AI assistant helping users understand a YouTube video. You have access to the video transcript and can answer questions about the video content.

Video Title: {video_title}

VIDEO TRANSCRIPT (for context):
{transcript_context[:20000]}

INSTRUCTIONS:
1. Answer questions based ONLY on the information provided in the video transcript
2. If the question cannot be answered from the transcript, politely say so
3. Be accurate, detailed, and helpful in your responses
4. Reference specific parts of the video when relevant
5. If asked about something not in the transcript, acknowledge this limitation
6. Be conversational but informative
7. Use the conversation history to provide context-aware responses"""
        
        # Start chat session with history
        if chat_history:
            # Add system context as first message in history
            history_with_context = [
                {"role": "user", "parts": [system_context]},
                {"role": "model", "parts": ["I understand. I'll help you understand this video based on the transcript. What would you like to know?"]}
            ] + chat_history
            chat = model.start_chat(history=history_with_context)
            response = chat.send_message(user_message)
        else:
            # First message - include system context in prompt
            full_prompt = system_context + f"\n\nUser's Question: {user_message.strip()}\n\nPlease provide a helpful, accurate response based on the video transcript."
            response = model.generate_content(full_prompt)
        
        if response and response.text:
            if settings.DEBUG:
                print(f"DEBUG: Chat response generated successfully, length: {len(response.text)}")
            return response.text.strip()
        else:
            if settings.DEBUG:
                print("DEBUG: Chat response is empty")
            return None
            
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error generating chat response: {str(e)}")
            import traceback
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
        return None

