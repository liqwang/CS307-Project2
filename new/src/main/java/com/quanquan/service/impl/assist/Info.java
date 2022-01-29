package com.quanquan.service.impl.assist;

import cn.edu.sustech.cs307.dto.Course;

import java.time.DayOfWeek;
import java.util.HashSet;

/**
 * searchCourse()的内部类
 */
public class Info{
    public int leftCapacity;
    public String courseId,
                  courseFullName,
                  instructorFullName;
    public DayOfWeek dayOfWeek;
    public short classBegin,classEnd;
    public String location;
    public Course.CourseGrading grading;
    public int sectionId,
            instructorId,
            classId;
    public HashSet<Short> weekList;
}