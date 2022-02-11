package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseSearchEntry {
    /**
     * The course of the searched section
     */
    public Course course;

    /**
     * The searched course section
     */
    public CourseSection section;

    /**
     * All classes of the section
     */
    public Set<CourseSectionClass> sectionClasses;

    /**
     * List all course or time conflicting courses' full name, sorted alphabetically.
     * Course full name: String.format("%s[%s]", course.name, section.name)
     * <p>
     * The conflict courses come from the student's enrolled courses (' sections).
     * <p>
     * Course conflict is when multiple sections belong to the same course.
     * Time conflict is when multiple sections have time-overlapping classes.
     */
    public List<String> conflictCourseNames;
}
