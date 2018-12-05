package com.project2.server;

import java.util.*;

public class Reminder {
    Timer timer;
    int num = 2;

    public Reminder(int seconds) {
        timer = new Timer();
        timer.schedule(new RemindTask(), 1000, seconds*1000);
    }

    class RemindTask extends TimerTask {
        public void run() {
            if (num > 0) {
                num--;
                System.out.println(num);
            } else {
                num = 2;
                System.out.println("Time's up");
                timer.cancel();
            }
        }
    }

    public static void main(String args[]) {
        new Reminder(1);
        System.out.println("Task scheduled.");
    }
}