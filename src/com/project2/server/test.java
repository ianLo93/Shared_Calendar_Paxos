package com.project2.server;

public class test {
    public static void main(String[] args) {
        Event e1 = new Event(0, "Schedule", "", "", "", "", new String[]{"1", "2"});
        Event e2 = new Event(0, "Schedule", "", "", "", "", new String[]{"1", "2"});
        if (e1.equals(e2)) System.out.println("They are equal");
    }
}
