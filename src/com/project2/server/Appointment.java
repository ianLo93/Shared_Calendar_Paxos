package com.project2.server;

import java.io.*;
import java.util.Comparator;

public class Appointment {
    private String name;
    private String day;
    private String start;
    private String end;
    private String[] participants;

    public Appointment(String name_, String day_, String start_, String end_, String[] participants_){
        this.name = name_;
        this.day = day_;
        this.start = start_;
        this.end = end_;
        this.participants = participants_;
    }

    public String getName() { return name; }
    public String getDay() { return day; }
    public String getStartTime() { return start; }
    public String getEndTime() { return end; }
    public String[] getParticipants() { return participants; }

    public static Comparator<Appointment> timeComparator = new Comparator<Appointment>() {
        @Override
        public int compare(Appointment m1, Appointment m2) {
            int lex1 = m1.getDay().compareTo(m2.getDay());
            if (lex1 != 0) return lex1;
            int lex2 = Local.parse_time(m1.getStartTime())-Local.parse_time(m2.getStartTime());
            if (lex2 != 0) return lex2;
            return m1.getName().compareTo(m2.getName());
        }
    };

    @Override
    public String toString(){
        StringBuilder m = new StringBuilder(name + " " + day+ " " + start + " " + end + " ");
        int i=0;
        for (; i<participants.length-1; i++){
            m.append(participants[i]+",");
        }
        m.append(participants[i]);
        return m.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Appointment other = (Appointment) obj;
        if (!this.name.equals(other.name)) return false;
        else return true;
    }

}
