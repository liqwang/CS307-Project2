package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseTable {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseTableEntry {
        /**
         * Course full name: String.format("%s[%s]", course.name, section.name)
         */
        public String courseFullName;
        /**
         * The section class's instructor
         */
        public Instructor instructor;
        /**
         * The class's begin and end time (e.g. 3 and 4).
         */
        public Short classBegin, classEnd;
        /**
         * The class location.
         */
        public String location;
    }

    /**
     * Stores all courses(encapsulated by CourseTableEntry) according to DayOfWeek.
     * The key should always be from MONDAY to SUNDAY, if the student has no course for any of the days, put an empty list.
     */
    public Map<DayOfWeek, Set<CourseTableEntry>> table;
}
