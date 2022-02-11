package com.quanquan.service.impl;

import com.quanquan.dto.Department;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.service.DepartmentService;
import com.quanquan.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@ParametersAreNonnullByDefault
@Service
public class MyDepartmentService implements DepartmentService {

    @Autowired
    DataSource dataSource;
    //完成√
    @Override
    public int addDepartment(String name) {
        try (Connection con=dataSource.getConnection()){
            String sql="insert into mybatis.department (name) values (?)";
            return Util.addAndGetKey(con,sql,name);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }
    //完成√
    @Override
    public void removeDepartment(int departmentId) {
        try (Connection con=dataSource.getConnection()){//student_section
            String sql1= """
                delete from mybatis.student_section
                where mybatis.student_section.student_id in(select student_id from mybatis.department join mybatis.major m on department.id = m.department_id
                join mybatis.student s on m.id = s.major_id
                join student_section on s.id = student_section.student_id
                 where department.id=?);""";
            Util.update(con,sql1,departmentId);
            //student
            String sql2= """
                    delete from mybatis.student
                    where student.id in(select student.id from mybatis.department join mybatis.major m on department.id = m.department_id
                    join student s on m.id = s.major_id
                        where department.id=?)""";
            Util.update(con,sql2,departmentId);
            String sql3= """
                    delete from mybatis.major_course
                    where major_id in(select m.id from mybatis.department join mybatis.major m on department.id = m.department_id
                    join major_course mc on m.id = mc.major_id
                        where department.id=?);""";
            Util.update(con,sql3,departmentId);
            String sql4= """
                    delete from mybatis.major
                    where major.id in(select m.id from mybatis.department
                        join major m on department.id = m.department_id
                        where department.id=?);""";
            Util.update(con,sql4,departmentId);
            String sql="delete from mybatis.department where id=?";
            if(Util.update(con,sql,departmentId)==0){
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //完成√
    @Override
    public List<Department> getAllDepartments() {
        try (Connection con=dataSource.getConnection()){
            String sql="select * from mybatis.department";
            return Util.query(Department.class,con,sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    //完成√
    @Override
    public Department getDepartment(int departmentId) {
        try (Connection con=dataSource.getConnection()){
            String sql="select * from mybatis.department where id=?";
            try {
                return Util.query(Department.class, con, sql, departmentId).get(0);
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
