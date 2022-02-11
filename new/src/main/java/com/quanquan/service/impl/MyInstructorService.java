package com.quanquan.service.impl;

import com.quanquan.dto.CourseSection;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.exception.IntegrityViolationException;
import com.quanquan.service.InstructorService;
import com.quanquan.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@Service
public class MyInstructorService implements InstructorService {

    @Autowired
    DataSource dataSource;
    //完成√
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try(Connection con=dataSource.getConnection()){
            String fullName;
            if(firstName.charAt(0) >= 'A' && firstName.charAt(0) <= 'Z') fullName = firstName + " " + lastName;
            else fullName = firstName + lastName;
            String sql="insert into mybatis.instructor (id,full_name) values (?,?)";
            Util.update(con,sql,userId,fullName);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    //完成√
    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        try (Connection con=dataSource.getConnection()){
            String sql = """
                    select i.id,
                           sec.name,
                           left_capacity "leftCapacity",
                           total_capacity "totalCapacity"
                    from mybatis.instructor i
                         join mybatis.section_class sc on i.id = sc.instructor_id
                                              and i.id=?
                         join mybatis.section sec on sc.section_id=sec.id
                         join mybatis.semester sem on sec.semester_id = sem.id
                                          and sec.id=?;""";
            //这里的queryRes有重复的CourseSection，因为join了section_class
            ArrayList<CourseSection> queryRes = Util.query(CourseSection.class, con, sql, instructorId, semesterId);
            if (queryRes.isEmpty()) {
                throw new EntityNotFoundException();
            }
            return queryRes.stream().distinct().collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}