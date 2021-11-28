package cn.edu.sustech.cs307.dto;

import java.util.Objects;

public class Course {

    public Course(String id, String name, int credit, int classHour, CourseGrading grading, String prerequisite) {
        this.id = id;
        this.name = name;
        this.credit = credit;
        this.classHour = classHour;
        this.grading = grading;
        this.prerequisite = prerequisite;
    }

    public enum CourseGrading {
        PASS_OR_FAIL, HUNDRED_MARK_SCORE
    }

    public Course(String id, String name, int credit, int classHour, CourseGrading grading, String prerequisite) {
        this.id = id;
        this.name = name;
        this.credit = credit;
        this.classHour = classHour;
        this.grading = grading;
        this.prerequisite = prerequisite;
    }

    // A courseId is valid if it has been added to the system with addCourse()
    // Do not check whether it is 'CS307' or '307CS'
    public String id;

    public String name;

    public int credit;

    public int classHour;

    public CourseGrading grading;
//    /**
//     * 额外属性：一个课程应该有开课院系
//     */
    public String prerequisite;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Course course = (Course) o;
        return credit == course.credit && classHour == course.classHour && id.equals(course.id)
                && name.equals(course.name) && grading == course.grading;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, credit, classHour, grading);
    }
}
