import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.config import settings
from typing import Optional


def send_otp_email(email: str, otp_code: str, purpose: str = "signup") -> bool:
    """
    Send OTP code to user's email
    """
    try:
        # Create message
        msg = MIMEMultipart()
        msg['From'] = settings.SMTP_USER
        msg['To'] = email
        msg['Subject'] = "TubeMind AI - OTP Verification Code"
        
        # Email body based on purpose
        if purpose == "signup":
            body = f"""
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #00D4FF;">Welcome to TubeMind AI!</h2>
                <p>Thank you for registering. Please use the following OTP code to verify your email:</p>
                <div style="background-color: #1A1A1A; color: #00D4FF; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; border-radius: 8px; margin: 20px 0;">
                    {otp_code}
                </div>
                <p>This code will expire in 1 hour.</p>
                <p>If you didn't request this code, please ignore this email.</p>
                <hr>
                <p style="color: #808080; font-size: 12px;">TubeMind AI Team</p>
            </body>
            </html>
            """
        else:  # forgot_password
            body = f"""
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #00D4FF;">Password Reset Request</h2>
                <p>You requested to reset your password. Please use the following OTP code:</p>
                <div style="background-color: #1A1A1A; color: #00D4FF; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; border-radius: 8px; margin: 20px 0;">
                    {otp_code}
                </div>
                <p>This code will expire in 1 hour.</p>
                <p>If you didn't request a password reset, please ignore this email.</p>
                <hr>
                <p style="color: #808080; font-size: 12px;">TubeMind AI Team</p>
            </body>
            </html>
            """
        
        msg.attach(MIMEText(body, 'html'))
        
        # Connect to SMTP server and send
        server = smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT)
        server.starttls()
        server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
        text = msg.as_string()
        server.sendmail(settings.SMTP_USER, email, text)
        server.quit()
        
        return True
    except Exception as e:
        print(f"Error sending email: {str(e)}")
        return False

