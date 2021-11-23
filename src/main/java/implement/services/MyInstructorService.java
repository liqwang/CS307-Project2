package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyInstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try (Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into instructor (id,first_name,last_name) values (?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,userId);
            ps.setString(2,firstName);
            ps.setString(3,lastName);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        ArrayList<CourseSection> result = new ArrayList<>();
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            String sql="select sec.*\n" +
                        "from instructor i\n" +
                        "     join section_class sc on i.id = sc.instructor_id\n" +
                        "                          and i.id=?\n" +
                        "     join section sec on sc.section_id=sec.id\n" +
                        "     join semester sem on sec.semester_id = sem.id\n" +
                        "                      and sec.id=?;";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,instructorId);
            ps.setInt(2,semesterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id=rs.getInt(1);
                String courseId=rs.getString(2);
                String name=rs.getString(4);
                int totalCapacity=rs.getInt(5);
                int leftCapacity=rs.getInt(6);
                result.add(new CourseSection(id,courseId,semesterId,name,totalCapacity,leftCapacity));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }
}