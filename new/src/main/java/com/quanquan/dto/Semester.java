package com.quanquan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.Objects;

/**
 * In our benchmark, there won't be overlapped semesters.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Semester {
    public Integer id;

    public String name;

    /**
     * If the end date is before the start date, you need give an illegal Argument Error
     * If the date is ridiculous, such as 1900-1-1 or 3000-1-1, it should not give error.
     */
    public Date begin, end;
}
