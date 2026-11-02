package com.tkyl.lectuplan;

public class EventModel {
    private int id;
    private int stt;
    private String title;       // Tên sự kiện / công việc
    private String date;        // Ngày tổ chức (Định dạng chuẩn: yyyy-MM-dd)
    private String organizer;   // Tên người tổ chức / phân công
    private String semester;    // Học kỳ (HK1, HK2, HK3)
    private int year;           // Năm
    private int week;           // Tuần thứ mấy trong năm

    public EventModel(int id, int stt, String title, String date, String organizer, String semester, int year, int week) {
        this.id = id;
        this.stt = stt;
        this.title = title;
        this.date = date;
        this.organizer = organizer;
        this.semester = semester;
        this.year = year;
        this.week = week;
    }

    public int getId() { return id; }
    public int getStt() { return stt; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getOrganizer() { return organizer; }
    public String getSemester() { return semester; }
    public int getYear() { return year; }
    public int getWeek() { return week; }

    // Định dạng học kỳ chuẩn đại học: ví dụ HK1 2026-2027
    public String getFormattedAcademicSemester() {
        int nextYear = year + 1;
        return semester + " " + year + "-" + nextYear;
    }
}
