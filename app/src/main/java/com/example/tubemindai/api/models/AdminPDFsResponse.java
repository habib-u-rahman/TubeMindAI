package com.example.tubemindai.api.models;

import java.util.List;

public class AdminPDFsResponse {
    private List<AdminPDFItem> pdfs;
    private int total;
    private int skip;
    private int limit;

    public List<AdminPDFItem> getPdfs() { return pdfs; }
    public void setPdfs(List<AdminPDFItem> pdfs) { this.pdfs = pdfs; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSkip() { return skip; }
    public void setSkip(int skip) { this.skip = skip; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public static class AdminPDFItem {
        private int id;
        private String file_name;
        private Integer file_size;
        private Integer page_count;
        private int user_id;
        private String user_name;
        private String user_email;
        private boolean has_notes;
        private int chat_count;
        private String created_at;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getFileName() { return file_name; }
        public void setFileName(String file_name) { this.file_name = file_name; }
        public Integer getFileSize() { return file_size; }
        public void setFileSize(Integer file_size) { this.file_size = file_size; }
        public Integer getPageCount() { return page_count; }
        public void setPageCount(Integer page_count) { this.page_count = page_count; }
        public int getUserId() { return user_id; }
        public void setUserId(int user_id) { this.user_id = user_id; }
        public String getUserName() { return user_name; }
        public void setUserName(String user_name) { this.user_name = user_name; }
        public String getUserEmail() { return user_email; }
        public void setUserEmail(String user_email) { this.user_email = user_email; }
        public boolean hasNotes() { return has_notes; }
        public void setHasNotes(boolean has_notes) { this.has_notes = has_notes; }
        public int getChatCount() { return chat_count; }
        public void setChatCount(int chat_count) { this.chat_count = chat_count; }
        public String getCreatedAt() { return created_at; }
        public void setCreatedAt(String created_at) { this.created_at = created_at; }
    }
}

