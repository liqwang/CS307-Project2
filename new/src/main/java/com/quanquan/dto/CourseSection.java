package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseSection {
    public Integer id;

    public Course course;

    public Semester semester;

    public String name;

    public Integer totalCapacity, leftCapacity;
}
