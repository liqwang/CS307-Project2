package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.dto.CourseSection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * 教师
 */
@ParametersAreNonnullByDefault
public interface InstructorService {
    void addInstructor(int userId, String firstName, String lastName);

    List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId);
}
