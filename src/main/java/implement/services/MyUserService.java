package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    @Override
    public void removeUser(int userId) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()){
            String sql1 = "delete from student where id = ?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
                ps1.setInt(1, userId);
                ps1.executeUpdate();
                ps1.close();
            String sql2 = "delete from instructor where id = ?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
                ps2.setInt(1,userId);
                ps2.executeUpdate();
                ps2.close();
        }catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            ArrayList<User> users = new ArrayList<>();
            String sql1 = "select * from student join major on student.major_id = major.id join department d on d.id = major.department_id";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ResultSet rs1 = ps1.executeQuery();
            while(rs1.next()){
                int id = rs1.getInt(1);
                String f_name = rs1.getString(3);
                String l_name = rs1.getString(4);
                Date date = rs1.getDate(5);
                Department department = new Department(rs1.getInt(9), rs1.getString(10));
                Major major = new Major(rs1.getInt(6), rs1.getString(7), department);
                Student student = new Student(id, f_name, l_name, date, major);
                users.add(student);
            }

            String sql2 = "select * from instructor";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ResultSet rs2 = ps2.executeQuery();
            while(rs2.next()){
                int id = rs2.getInt(1);
                String f_name = rs2.getString(2);
                String l_name = rs2.getString(3);
                String name;
                if(f_name.charAt(0) >= 'A' && f_name.charAt(0) <= 'Z'){
                    name = f_name + " " + l_name;
                }
                else name = f_name + l_name;
                Instructor instructor = new Instructor(id, name);
                users.add(instructor);
            }

            return users;
        }catch(SQLException throwables){
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public User getUser(int userId) {
        User user;
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()){
            String sql1 = "select * from student where id = ?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ps1.setInt(1, userId);
            ResultSet rs1 = ps1.executeQuery();
            String sql2 = "select * from instructor where id = ?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ResultSet rs2 = ps2.executeQuery();
            if(rs1.next()){
                int id = rs1.getInt(1);
                String f_name = rs1.getString(3);
                String l_name = rs1.getString(4);
                Date date = rs1.getDate(5);
                Department department = new Department(rs1.getInt(9), rs1.getString(10));
                Major major = new Major(rs1.getInt(6), rs1.getString(7), department);
                user = new Student(id, f_name, l_name, date, major);
                return user;
            }

            else if(rs2.next()){
                int id = rs2.getInt(1);
                String f_name = rs2.getString(2);
                String l_name = rs2.getString(3);
                String name;
                if(f_name.charAt(0) >= 'A' && f_name.charAt(0) <= 'Z'){
                    name = f_name + " " + l_name;
                }
                else name = f_name + l_name;
                user = new Instructor(id, name);
                return user;
            }

            else throw new EntityNotFoundException();

        }catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
