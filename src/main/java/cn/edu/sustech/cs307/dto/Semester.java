package cn.edu.sustech.cs307.dto;

import com.sun.jdi.connect.Connector;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Objects;

/**
 * In our benchmark, there won't be overlapped semesters.
 */
public class Semester {
    public int id;
    public String name;

    /**
     * If the end date is before the start date, you need give an illegal Argument Error
     * If the date is ridiculous, such as 1900-1-1 or 3000-1-1, it should not give error.
     */
    public Date begin, end;

    public Semester(int id, String name, Date begin, Date end) throws SQLException{
        this.id = id;
        this.name = name;
        if(begin.after(end)) throw new SQLException("illegal Argument Error");
        else{
            this.begin = begin;
            this.end = end;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Semester semester = (Semester) o;
        return id == semester.id && name.equals(semester.name) && begin.equals(semester.begin) && end
                .equals(semester.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, begin, end);
    }
}
