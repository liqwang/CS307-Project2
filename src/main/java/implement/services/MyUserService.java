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
        ArrayList<User> users = new ArrayList<>();
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql1 = "select * from student join major on student.major_id = major.id join department d on d.id = major.department_id";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ResultSet rs1 = ps1.executeQuery();
            while(rs1.next()){
                int id = rs1.getInt(1);
                String f_name = rs1.getString(3);
                String l_name = rs1.getString(4);
                Date date = rs1.getDate(5);
                Department dep = new Department();
                dep.id=rs1.getInt(9);
                dep.name=rs1.getString(10);
                Major maj = new Major();
                maj.id=rs1.getInt(6);
                maj.name=rs1.getString(7);
                maj.department=dep;
                Student stu = new Student();
                stu.id=id;
                stu.fullName=f_name;
                stu.enrolledDate=date;
                stu.major=maj;
                users.add(stu);
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
                Instructor ins = new Instructor();
                ins.id=id;
                ins.fullName=name;
                users.add(ins);
            }
        }catch(SQLException throwables){
            throwables.printStackTrace();
        }
        return users;
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
                Department dep = new Department();
                dep.id=rs1.getInt(9);
                dep.name=rs1.getString(10);
                Major maj = new Major();
                maj.id=rs1.getInt(6);
                maj.name=rs1.getString(7);
                maj.department=dep;
                Student stu = new Student();
                stu.id=id;
                stu.fullName=f_name;
                stu.major=maj;
                stu.enrolledDate=date;
                return stu;
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
                Instructor ins = new Instructor();
                ins.id=id;
                ins.fullName=name;
                return ins;
            }

            else throw new EntityNotFoundException();

        }catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
