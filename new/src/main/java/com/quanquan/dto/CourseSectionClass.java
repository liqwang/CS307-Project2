package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.Set;

/**
 * The relationship between {@code CourseSectionClass} with {@code CourseSection} is:
 * One CourseSection usually has two CourseSectionClass
 * the one is theory class, the other is lab class
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseSectionClass {
    public Integer id;
    public Instructor instructor;
    public DayOfWeek dayOfWeek; // We ensure the test semesters begin with Monday.
    // The given elements in weekList are sorted.
    // CourseSectionClasses in same courseSection may have different week list.
    public Set<Short> weekList;
    // The time quantum of start and end (closed interval).
    // For example: classStart is 3 while classEnd is 4
    public Short classBegin, classEnd;
    public String location;
}
