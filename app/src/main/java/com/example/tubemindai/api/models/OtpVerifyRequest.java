package com.example.tubemindai.api.models;

public class OtpVerifyRequest {
    private String email;
    private String otp_code;
    private String purpose;

    public OtpVerifyRequest(String email, String otp_code, String purpose) {
        this.email = email;
        this.otp_code = otp_code;
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otp_code;
    }

    public void setOtpCode(String otp_code) {
        this.otp_code = otp_code;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

