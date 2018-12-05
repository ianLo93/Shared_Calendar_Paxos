package com.project2.server;

import java.util.*;

public class Reminder {
    Timer timer;
    int num = 2;
    static int k = 2;

    public Reminder(int seconds) {
        timer = new Timer();
        timer.schedule(new RemindTask(), seconds*1000, 1000);
        k--;
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
                if (k > 0) new Reminder(1);
            }
        }
    }

    public static void main(String args[]) {
        new Reminder(1);
        System.out.println("Task scheduled.");
    }
}