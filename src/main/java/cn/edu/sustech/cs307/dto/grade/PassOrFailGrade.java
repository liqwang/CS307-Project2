package cn.edu.sustech.cs307.dto.grade;

import java.time.DateTimeException;
import java.time.DayOfWeek;

public enum PassOrFailGrade implements Grade {
    PASS, FAIL;

    @Override
    public <R> R when(Cases<R> cases) {
        return cases.match(this);
    }
    MONDAY,
    /**
     * The singleton instance for the day-of-week of Tuesday.
     * This has the numeric value of {@code 2}.
     */
    TUESDAY,
    /**
     * The singleton instance for the day-of-week of Wednesday.
     * This has the numeric value of {@code 3}.
     */
    WEDNESDAY,
    /**
     * The singleton instance for the day-of-week of Thursday.
     * This has the numeric value of {@code 4}.
     */

    private static final PassOrFailGrade[] ENUMS = PassOrFailGrade.values();

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code DayOfWeek} from an {@code int} value.
     * <p>
     * {@code DayOfWeek} is an enum representing the 7 days of the week.
     * This factory allows the enum to be obtained from the {@code int} value.
     * The {@code int} value follows the ISO-8601 standard, from 1 (Monday) to 7 (Sunday).
     *
     * @param dayOfWeek  the day-of-week to represent, from 1 (Monday) to 7 (Sunday)
     * @return the day-of-week singleton, not null
     * @throws DateTimeException if the day-of-week is invalid
     */
    public static PassOrFailGrade of(int grade) {
        if (grade !=-1||grade!=-2) {
            throw new DateTimeException("Invalid value for PassOrFailGrade: " + grade);
        }else {
            return ENUMS[];
        }
    }
}
