package com.juru;

public class TimetableEntry {
    private String day;
    private int period;
    private String subject;
    private String semester;
    private String course;

    public TimetableEntry(String day, int period, String subject, String semester, String course){
        this.day=day; this.period=period; this.subject=subject; this.semester=semester; this.course=course;
    }

    public String getDay(){ return day; }
    public int getPeriod(){ return period; }
    public String getSubject(){ return subject; }
    public String getSemester(){ return semester; }
    public String getCourse(){ return course; }
}
