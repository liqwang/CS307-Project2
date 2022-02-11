package com.quanquan.service.impl;

import com.quanquan.dto.Instructor;
import com.quanquan.dto.Student;
import com.quanquan.dto.User;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.service.UserService;
import com.quanquan.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MyUserService implements UserService {

    @Autowired
    DataSource dataSource;

    //✔
    @Override
    public void removeUser(int userId) {
        try(Connection con=dataSource.getConnection()){
            String sql1 = "delete from mybatis.student where id = ?";
            String sql2 = "delete from mybatis.instructor where id = ?";
            if(Util.update(con, sql1, userId)+
               Util.update(con, sql2, userId)==0){
                throw new EntityNotFoundException();
            }
        }catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public List<User> getAllUsers() {
        try (Connection con=dataSource.getConnection()){
            ArrayList<User> users = new ArrayList<>();
            String sql1 = """
                    select enrolled_date "enrolledDate",
                           major_id "major",
                           id,
                           full_name "fullName"
                    from mybatis.student;""";
            ArrayList<Student> stu = Util.query(Student.class, con, sql1);

            String sql2 = """
                    select id,
                           full_name "fullName"
                    from mybatis.instructor;""";
            ArrayList<Instructor> ins = Util.query(Instructor.class, con, sql2);
            users.addAll(stu);
            users.addAll(ins);
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Override
    public User getUser(int userId) {
        try (Connection con=dataSource.getConnection()){
            String sql1 = "select * from mybatis.student where id = ?";
            ArrayList<Student> stu = Util.query(Student.class, con ,sql1, userId);
            ArrayList<Instructor> ins = Util.query(Instructor.class, con ,sql1, userId);
            if(!stu.isEmpty()) return stu.get(0);
            else if(!ins.isEmpty()) return ins.get(0);
            else throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
