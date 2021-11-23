package cn.edu.sustech.cs307.dto;

import java.util.Objects;

public class CourseSection {
    /**
     *For example, it can represent the id of section "No.1 Chinese class of database principle"
     */
    public int id;
    public String courseId;
    public int semesterId;
    /**
     * if the course name is "database principle", the name here could be "No.1 Chinese class", "No.1 English class" ...
     */
    public String name;

    public int totalCapacity, leftCapacity;

    public CourseSection(int id, String courseId, int semesterId, String name, int totalCapacity, int leftCapacity) {
        this.id = id;
        this.courseId = courseId;
        this.semesterId = semesterId;
        this.name = name;
        this.totalCapacity = totalCapacity;
        this.leftCapacity = leftCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CourseSection section = (CourseSection) o;
        return id == section.id && totalCapacity == section.totalCapacity && leftCapacity == section.leftCapacity
                && name.equals(section.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, totalCapacity, leftCapacity);
    }
}
