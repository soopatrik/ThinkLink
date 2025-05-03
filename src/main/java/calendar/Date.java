package main.java.calendar;

/**
 * Date entity as defined in the UML class diagram.
 */
public class Date {
    private int hour;
    private int day;
    private int month;
    private int year;

    public Date(int day, int month, int year) {
        this(0, day, month, year);
    }

    public Date(int hour, int day, int month, int year) {
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    // Create Date from Java's Date
    public static Date fromJavaDate(java.util.Date javaDate) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(javaDate);

        return new Date(
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.MONTH) + 1, // Java months are 0-based
                cal.get(java.util.Calendar.YEAR));
    }

    // Convert to Java's Date
    public java.util.Date toJavaDate() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, day, hour, 0); // Convert to Java's 0-based month
        return cal.getTime();
    }

    // Getters and setters
    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d on %02d/%02d/%04d", hour, 0, day, month, year);
    }
}