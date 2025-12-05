"""
AI Service for generating notes from video transcripts using Google Gemini API
"""
from typing import Optional, Dict
import json
from app.config import settings


def generate_notes_from_transcript(transcript: str, video_title: str = "") -> Optional[Dict]:
    """
    Generate notes from video transcript using Google Gemini API
    Returns: dict with 'summary', 'key_points', 'bullet_notes'
    """
    if not transcript:
        if settings.DEBUG:
            print("DEBUG: No transcript provided to generate_notes_from_transcript")
        return None
    
    if settings.DEBUG:
        print(f"DEBUG: Generating notes for video: {video_title}")
        print(f"DEBUG: Transcript length: {len(transcript)}")
        print(f"DEBUG: AI_API_KEY present: {bool(settings.AI_API_KEY)}")
    
    # Try Gemini API first
    if settings.AI_API_KEY:
        if settings.DEBUG:
            print("DEBUG: Attempting to use Gemini API...")
        result = generate_notes_with_gemini(transcript, video_title)
        if result:
            if settings.DEBUG:
                print("DEBUG: Gemini API returned notes successfully")
            return result
        else:
            if settings.DEBUG:
                print("DEBUG: Gemini API returned None, using fallback")
    else:
        if settings.DEBUG:
            print("DEBUG: No AI_API_KEY found, using placeholder notes")
    
    # Fallback to placeholder if no API key or API fails
    return {
        "summary": f"This video titled '{video_title}' contains valuable content. "
                  f"Watch the video to get detailed insights and information.",
        "key_points": "• Watch the video for key insights\n• Take notes while watching\n• Review important sections",
        "bullet_notes": "• Video content analysis\n• Important concepts\n• Practical applications"
    }


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
        model = None
        model_names = ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro']
        
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
        
        # Create comprehensive prompt for better note generation
        prompt = f"""You are an expert note-taker analyzing a YouTube video transcript. Your task is to create comprehensive, detailed, and well-organized notes that capture ALL important information from the video.

Video Title: {video_title}

TRANSCRIPT:
{transcript_limited}

INSTRUCTIONS:
1. Read the ENTIRE transcript carefully and identify ALL key information, concepts, examples, tips, and details.
2. Do NOT skip or miss any important points mentioned in the video - be thorough and comprehensive.
3. Extract specific details, numbers, statistics, names, dates, examples, case studies, and practical applications.
4. Pay attention to:
   - Main topics and subtopics discussed
   - Step-by-step processes or procedures explained
   - Examples, case studies, or real-world applications
   - Tips, tricks, best practices, or warnings mentioned
   - Important numbers, percentages, statistics, or data points
   - Tools, resources, or references mentioned
   - Key definitions, concepts, or terminology explained
   - Comparisons, contrasts, or relationships between concepts
   - Action items, recommendations, or next steps
5. Organize information logically and comprehensively - group related information together.
6. Be detailed and specific - avoid vague or generic statements.

REQUIRED OUTPUT FORMAT:

SUMMARY:
Write a comprehensive summary (3-4 paragraphs) that:
- Captures the main purpose and theme of the video
- Includes the key messages and takeaways
- Mentions important concepts, examples, or case studies discussed
- Provides context and overall understanding
- Be thorough and detailed, don't miss important information

KEY POINTS:
List 8-12 main points, each on a new line starting with "•". Each point should:
- Be specific and detailed (not vague or generic)
- Include important details, examples, numbers, statistics, or data mentioned
- Cover different aspects/topics discussed in the video comprehensively
- Be informative, actionable, and valuable
- Capture ALL major topics, themes, and important concepts covered
- Include any critical information, warnings, or important notes
- Mention specific examples, case studies, or real-world applications when discussed

BULLET NOTES:
Provide comprehensive, detailed bullet notes that:
- Cover ALL important topics, subtopics, and concepts from the video - be exhaustive
- Include specific details, examples, tips, tricks, and practical information
- Organize notes by topic or theme (group related points together logically)
- Each note should start with "•" and be on a new line
- Be extremely thorough - include important details, explanations, examples, applications, and context
- Don't miss ANY significant information from the transcript - capture everything important
- Include any numbers, statistics, percentages, names, dates, or specific examples mentioned
- Cover both theoretical concepts and practical applications in detail
- Include any warnings, tips, best practices, common mistakes, or important notes mentioned
- Capture step-by-step processes, procedures, or methodologies explained
- Include definitions, terminology, or key concepts explained in the video
- Mention tools, resources, websites, books, or references if discussed
- Include comparisons, pros/cons, or relationships between concepts
- Capture action items, recommendations, or next steps if mentioned
- Be detailed enough that someone reading the notes gets comprehensive understanding

IMPORTANT - CRITICAL REQUIREMENTS:
- Be EXTREMELY comprehensive and detailed - don't skip ANY information
- Include ALL important points, examples, statistics, tips, and details from the video
- Don't summarize too much - include specific details, numbers, names, and examples
- Make notes useful for someone who wants to understand the video content fully without watching
- Organize information clearly and logically but be exhaustive in coverage
- Use clear, detailed language - prioritize completeness over brevity
- Capture everything important - it's better to include too much detail than to miss key information
- Pay special attention to: specific examples, case studies, numbers, step-by-step processes, warnings, tips, and actionable advice

Format your response EXACTLY as follows:
SUMMARY:
[Your comprehensive summary here - 3-4 paragraphs with all important details]

KEY POINTS:
• [Detailed point 1 with specific information]
• [Detailed point 2 with specific information]
• [Detailed point 3 with specific information]
• [Continue with 6-10 comprehensive points]

BULLET NOTES:
• [Comprehensive note 1 with details]
• [Comprehensive note 2 with details]
• [Comprehensive note 3 with details]
• [Continue with detailed notes covering ALL topics from the video]

Remember: 
- Be extremely thorough, comprehensive, and detailed - don't miss ANY important information from the video transcript
- Quality and completeness over brevity - include all relevant details, examples, and insights
- Make the notes valuable for someone who wants to fully understand the video content without watching it
- Extract and preserve all important information, examples, statistics, tips, and insights
- Be exhaustive in your analysis - better to include too much detail than to miss important points
- Organize information clearly but be comprehensive in coverage"""
        
        # Generate content
        try:
            response = model.generate_content(prompt)
            
            if response and response.text:
                if settings.DEBUG:
                    print(f"DEBUG: Gemini API response received, length: {len(response.text)}")
                # Parse the response
                parsed_result = parse_gemini_response(response.text, video_title)
                if settings.DEBUG:
                    print(f"DEBUG: Parsed result - summary: {bool(parsed_result.get('summary'))}, key_points: {bool(parsed_result.get('key_points'))}")
                return parsed_result
            else:
                if settings.DEBUG:
                    print("DEBUG: Gemini API returned empty response")
                    if response:
                        print(f"DEBUG: Response object: {response}")
                return None
        except Exception as api_error:
            if settings.DEBUG:
                print(f"DEBUG: Gemini API error: {str(api_error)}")
                import traceback
                print(f"DEBUG: Traceback: {traceback.format_exc()}")
            return None
            
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error generating notes with Gemini: {str(e)}")
            import traceback
            print(f"DEBUG: Traceback: {traceback.format_exc()}")
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
                if line.startswith('•') or (current_section == 'summary' and line):
                    current_content.append(line)
        
        # Save the last section
        if current_section == 'summary' and current_content:
            summary = '\n'.join(current_content).strip()
        elif current_section == 'key_points' and current_content:
            key_points = '\n'.join(current_content).strip()
        elif current_section == 'bullet_notes' and current_content:
            bullet_notes = '\n'.join(current_content).strip()
        
        # If parsing failed, try to extract from the whole response
        if not summary or not key_points or not bullet_notes:
            # Fallback: split by common patterns
            parts = response_text.split('\n\n')
            if len(parts) >= 3:
                summary = parts[0].replace('SUMMARY:', '').strip()
                key_points = parts[1].replace('KEY POINTS:', '').strip()
                bullet_notes = parts[2].replace('BULLET NOTES:', '').strip()
        
        # Ensure we have content
        if not summary:
            summary = f"This video titled '{video_title}' contains valuable content and insights."
        if not key_points:
            key_points = "• Key insights from the video\n• Important concepts discussed\n• Practical applications"
        if not bullet_notes:
            bullet_notes = "• Detailed notes from the video content\n• Important points to remember\n• Additional information"
        
        return {
            "summary": summary,
            "key_points": key_points,
            "bullet_notes": bullet_notes
        }
        
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error parsing Gemini response: {str(e)}")
        
        # Return fallback response
        return {
            "summary": f"This video titled '{video_title}' contains valuable content.",
            "key_points": "• Key insights\n• Important concepts\n• Practical applications",
            "bullet_notes": "• Detailed notes\n• Important points\n• Additional information"
        }


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

