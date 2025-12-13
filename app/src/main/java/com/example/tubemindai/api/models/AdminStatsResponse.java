package com.example.tubemindai.api.models;

public class AdminStatsResponse {
    private UsersStats users;
    private VideosStats videos;
    private ChatsStats chats;

    public UsersStats getUsers() {
        return users;
    }

    public void setUsers(UsersStats users) {
        this.users = users;
    }

    public VideosStats getVideos() {
        return videos;
    }

    public void setVideos(VideosStats videos) {
        this.videos = videos;
    }

    public ChatsStats getChats() {
        return chats;
    }

    public void setChats(ChatsStats chats) {
        this.chats = chats;
    }

    public static class UsersStats {
        private int total;
        private int active;
        private int verified;
        private int new_today;
        private int new_last_7_days;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getActive() { return active; }
        public void setActive(int active) { this.active = active; }
        public int getVerified() { return verified; }
        public void setVerified(int verified) { this.verified = verified; }
        public int getNewToday() { return new_today; }
        public void setNewToday(int new_today) { this.new_today = new_today; }
        public int getNewLast7Days() { return new_last_7_days; }
        public void setNewLast7Days(int new_last_7_days) { this.new_last_7_days = new_last_7_days; }
    }

    public static class VideosStats {
        private int total;
        private int with_notes;
        private int new_today;
        private int new_last_7_days;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getWithNotes() { return with_notes; }
        public void setWithNotes(int with_notes) { this.with_notes = with_notes; }
        public int getNewToday() { return new_today; }
        public void setNewToday(int new_today) { this.new_today = new_today; }
        public int getNewLast7Days() { return new_last_7_days; }
        public void setNewLast7Days(int new_last_7_days) { this.new_last_7_days = new_last_7_days; }
    }

    public static class ChatsStats {
        private int total;
        private int new_today;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getNewToday() { return new_today; }
        public void setNewToday(int new_today) { this.new_today = new_today; }
    }

    public static class PDFsStats {
        private int total;
        private int with_notes;
        private int new_today;
        private int new_last_7_days;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getWithNotes() { return with_notes; }
        public void setWithNotes(int with_notes) { this.with_notes = with_notes; }
        public int getNewToday() { return new_today; }
        public void setNewToday(int new_today) { this.new_today = new_today; }
        public int getNewLast7Days() { return new_last_7_days; }
        public void setNewLast7Days(int new_last_7_days) { this.new_last_7_days = new_last_7_days; }
    }

    public static class PDFChatsStats {
        private int total;
        private int new_today;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getNewToday() { return new_today; }
        public void setNewToday(int new_today) { this.new_today = new_today; }
    }

    private PDFsStats pdfs;
    private PDFChatsStats pdf_chats;

    public PDFsStats getPdfs() { return pdfs; }
    public void setPdfs(PDFsStats pdfs) { this.pdfs = pdfs; }
    public PDFChatsStats getPdfChats() { return pdf_chats; }
    public void setPdfChats(PDFChatsStats pdf_chats) { this.pdf_chats = pdf_chats; }
}

