package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.UserService;
import implement.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    Connection con;
    {
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //✔
    @Override
    public void removeUser(int userId) {
        try{
            String sql1 = "delete from student where id = ?";
            String sql2 = "delete from instructor where id = ?";
            if(Util.update(con, sql1, userId)+
               Util.update(con, sql2, userId)==0){
                throw new EntityNotFoundException();
            }
        }catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
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
    }

    @Override
    public User getUser(int userId) {
        String sql1 = "select * from student where id = ?";
        ArrayList<Student> stu = Util.query(Student.class, con ,sql1, userId);
        ArrayList<Instructor> ins = Util.query(Instructor.class, con ,sql1, userId);
        if(!stu.isEmpty()) return stu.get(0);
        else if(!ins.isEmpty()) return ins.get(0);
        else throw new EntityNotFoundException();
    }
}
