package com.example.tubemindai.api.models;

import java.util.List;

public class PDFChatHistoryListResponse {
    private List<PDFChatHistoryItem> histories;
    private int total;

    public List<PDFChatHistoryItem> getHistories() { return histories; }
    public void setHistories(List<PDFChatHistoryItem> histories) { this.histories = histories; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public static class PDFChatHistoryItem {
        private int pdf_id;
        private String pdf_name;
        private String last_message;
        private String last_message_time;
        private int message_count;

        public int getPdfId() { return pdf_id; }
        public void setPdfId(int pdf_id) { this.pdf_id = pdf_id; }
        public String getPdfName() { return pdf_name; }
        public void setPdfName(String pdf_name) { this.pdf_name = pdf_name; }
        public String getLastMessage() { return last_message; }
        public void setLastMessage(String last_message) { this.last_message = last_message; }
        public String getLastMessageTime() { return last_message_time; }
        public void setLastMessageTime(String last_message_time) { this.last_message_time = last_message_time; }
        public int getMessageCount() { return message_count; }
        public void setMessageCount(int message_count) { this.message_count = message_count; }
    }
}

