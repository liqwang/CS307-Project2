package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyMajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into major(name, department_id) values (?,?)";
            PreparedStatement ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,name);
            ps.setInt(2,departmentId);
            ps.executeUpdate();
            ResultSet rs=ps.getGeneratedKeys();
            ps.close();
            return rs.getInt(1);
        }catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override //删三个 student? student_section, major_courses
    public void removeMajor(int majorId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql1="delete from major where id=?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ps1.setInt(1,majorId);
            ps1.executeUpdate();
            String sql2="delete from major_course where major_id=?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1,majorId);
            ps2.executeUpdate();
            String sql3="delete from student where major_id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3);
            ps3.setInt(1,majorId);
            ps3.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        ArrayList<Major> majors = new ArrayList<>();
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            String sql="select *\n" +
                    "from major left join department d on d.id = major.department_id;";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id=rs.getInt(1);
                String name=rs.getString(2);
                Department department=new Department(rs.getInt(3),rs.getString(4));
                majors.add(new Major(id,name,department) );
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return majors;
    }

    @Override
    public Major getMajor(int majorId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="select *\n" +
                    "from major left join department d on d.id = major.department_id where major.id=?;";
            PreparedStatement ps3 = con.prepareStatement(sql);
            ps3.setInt(1,majorId);
            ResultSet rs=ps3.executeQuery();
            Department dp=new Department(rs.getInt(3),rs.getString(4));
            return new Major(rs.getInt(1),rs.getString(2),dp);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) { //这两个没看懂
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
        }catch (SQLException throwables) {
        }
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
