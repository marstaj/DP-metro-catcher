package cz.marstaj.metrocatcher.model;

/**
 * Created by mastajner on 09/02/14.
 */
public class MyTime {

    int hours = 0;
    int minutes = 0;
    int seconds = 0;

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public void setMinutesFromMidnight(int numberOfMinutesFromMidnight) {
        hours = numberOfMinutesFromMidnight / 60;
        minutes = numberOfMinutesFromMidnight - hours * 60;
        seconds = 0;
    }

    public void setFromStrWithoutDelimiter(String stringTimeWithoutDelimiter) {
        switch (stringTimeWithoutDelimiter.length()) {
            case 1: {
                hours = 0;
                minutes = Integer.valueOf(stringTimeWithoutDelimiter);
                seconds = 0;
                return;
            }
            case 2: {
                hours = 0;
                minutes = Integer.valueOf(stringTimeWithoutDelimiter);
                seconds = 0;
                return;
            }
            case 3: {
                hours = Integer.valueOf(stringTimeWithoutDelimiter.substring(0, 1));
                minutes = Integer.valueOf(stringTimeWithoutDelimiter.substring(1, 3));
                seconds = 0;
                return;
            }
            case 4: {
                hours = Integer.valueOf(stringTimeWithoutDelimiter.substring(0, 2));
                minutes = Integer.valueOf(stringTimeWithoutDelimiter.substring(2, 4));
                seconds = 0;
                return;
            }

        }
    }

    public String getStringBasicTimeWithDelimiter() {
        String hour;
        String minute;

        if (hours < 10) {
            hour = "0" + hours;
        } else {
            hour = String.valueOf(hours);
        }
        if (minutes < 10) {
            minute = "0" + minutes;
        } else {
            minute = String.valueOf(minutes);
        }

        return hour + ":" + minute;
    }

    public String getStringTimeWithDelimiter() {
        String second;

        if (seconds < 10) {
            second = "0" + seconds;
        } else {
            second = String.valueOf(seconds);
        }

        return getStringBasicTimeWithDelimiter() + ":" + second;
    }
}
