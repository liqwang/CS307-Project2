package com.quanquan.service;

import com.quanquan.dto.CourseSection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public interface InstructorService {
    void addInstructor(int userId, String firstName, String lastName);

    List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId);
}
