package com.quanquan.service.impl;

import com.quanquan.database.SQLDataSource;
import com.quanquan.dto.Instructor;
import com.quanquan.dto.Student;
import com.quanquan.dto.User;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.service.UserService;
import util.Util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    //✔
    @Override
    public void removeUser(int userId) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()){
            String sql1 = "delete from student where id = ?";
            String sql2 = "delete from instructor where id = ?";
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
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            ArrayList<User> users = new ArrayList<>();
            String sql1 = """
                    select enrolled_date "enrolledDate",
                           major_id "major",
                           id,
                           full_name "fullName"
                    from student;""";
            ArrayList<Student> stu = Util.query(Student.class, con, sql1);

            String sql2 = """
                    select id,
                           full_name "fullName"
                    from instructor;""";
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
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            String sql1 = "select * from student where id = ?";
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
