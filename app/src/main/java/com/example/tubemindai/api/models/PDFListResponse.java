package com.example.tubemindai.api.models;

import java.util.List;

public class PDFListResponse {
    private List<PDFResponse> pdfs;
    private int total;

    public List<PDFResponse> getPdfs() { return pdfs; }
    public void setPdfs(List<PDFResponse> pdfs) { this.pdfs = pdfs; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

