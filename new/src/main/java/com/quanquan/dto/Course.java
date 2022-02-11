package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    public enum CourseGrading {
        PASS_OR_FAIL, HUNDRED_MARK_SCORE
    }

    // A courseId is valid if it has been added to the system with addCourse()
    // Do not check whether it is 'CS307' or '307CS'
    public String id;

    public String name;

    public Integer credit;

    public Integer classHour;

    public CourseGrading grading;

    public String prerequisite;
}
