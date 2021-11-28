package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class MyCourseService implements CourseService {

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()){
            String sql="insert into course(id, name, credit, class_hour, is_pf, prerequisite) values (?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            String preStr = prerequisite==null?null:prerequisite.when(new Prerequisite.Cases<>() {
                @Override
                public String match(AndPrerequisite self) {
                    String[] children = self.terms.stream()
                            .map(term -> term.when(this))
                            .toArray(String[]::new);
                    return '(' + String.join(" AND ", children) + ')';
                }

                @Override
                public String match(OrPrerequisite self) {
                    String[] children = self.terms.stream()
                            .map(term -> term.when(this))
                            .toArray(String[]::new);
                    return '(' + String.join(" OR ", children) + ')';
                }

                @Override
                public String match(CoursePrerequisite self) {
                    return self.courseID;
                }
            });
            ps.setString(1,courseId);
            ps.setString(2,courseName);
            ps.setInt(3,credit);
            ps.setInt(4,classHour);
            ps.setBoolean(5,grading==Course.CourseGrading.PASS_OR_FAIL);
            ps.setString(6,preStr);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into section (course_id,semester_id,name,total_capacity,left_capacity) values (?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,courseId);
            ps.setInt(2,semesterId);
            ps.setString(3,sectionName);
            ps.setInt(4,totalCapacity);
            ps.setInt(5,totalCapacity);//新插入 剩余名额为满的
            ps.executeUpdate();
            ResultSet rs= ps.getGeneratedKeys();
            return rs.getInt(1);
        }catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into section_class(section_id, instructor_id, day_of_week, class_begin, class_end, location, week_list) values (?,?,?,?,?,?,?)";
            //PreparedStatement ps=con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            PreparedStatement ps = con.prepareStatement(sql);//这样写会不会有bug?
            ps.setInt(1,sectionId);
            ps.setInt(2,instructorId);
            ps.setInt(3,dayOfWeek.getValue());
            ps.setInt(4,classStart);
            ps.setInt(5,classEnd);
            ps.setString(6,location);
            ps.setArray(7, (Array)weekList);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql1="delete from course where id=?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ps1.setString(1,courseId);
            ps1.executeUpdate();
            String sql4="delete from major_course where course_id=?";
            PreparedStatement ps4 = con.prepareStatement(sql4);
            ps4.setString(1,courseId);
            ps4.executeUpdate();

            String sql2="delete from section where course_id=?";
            PreparedStatement ps2 = con.prepareStatement(sql2,PreparedStatement.RETURN_GENERATED_KEYS);
            ps2.setString(1,courseId);
            ps2.executeUpdate();
            ResultSet rs=ps2.getGeneratedKeys();
            int sectionId=rs.getInt(1);
            String sql5="delete from student_section where section_id=?";
            PreparedStatement ps5 = con.prepareStatement(sql5);
            ps5.setInt(1,sectionId);
            ps5.executeUpdate();
            String sql6="delete from semester where id=?";
            PreparedStatement ps6 = con.prepareStatement(sql6);
            ps6.setInt(1,sectionId);
            ps6.executeUpdate();


            String sql3="delete from section_class where section_id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3,PreparedStatement.RETURN_GENERATED_KEYS);
            ps3.setInt(1,sectionId);
            ps3.executeUpdate();
            ResultSet rs2=ps3.getGeneratedKeys();
            int section_class_id=rs2.getInt(1);
            String sql7="delete from instructor where id=?";
            PreparedStatement ps7 = con.prepareStatement(sql7);
            ps7.setInt(1,section_class_id);
            ps7.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {

            String sql2="delete from section where id=?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1,sectionId);
            ps2.executeUpdate();
            String sql5="delete from student_section where section_id=?";
            PreparedStatement ps5 = con.prepareStatement(sql5);
            ps5.setInt(1,sectionId);
            ps5.executeUpdate();
            String sql6="delete from semester where id=?";
            PreparedStatement ps6 = con.prepareStatement(sql6);
            ps6.setInt(1,sectionId);
            ps6.executeUpdate();

            String sql3="delete from section_class where section_id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3,PreparedStatement.RETURN_GENERATED_KEYS);
            ps3.setInt(1,sectionId);
            ps3.executeUpdate();
            ResultSet rs2=ps3.getGeneratedKeys();
            int section_class_id=rs2.getInt(1);
            String sql7="delete from instructor where id=?";
            PreparedStatement ps7 = con.prepareStatement(sql7);
            ps7.setInt(1,section_class_id);
            ps7.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql3="delete from section_class where id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3);
            ps3.setInt(1,classId);
            ps3.executeUpdate();
            String sql7="delete from instructor where id=?";
            PreparedStatement ps7 = con.prepareStatement(sql7);
            ps7.setInt(1,classId);
            ps7.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        ArrayList<Course> result = new ArrayList<>();
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            String sql="select * from course;";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String id=rs.getString(1);
                String name=rs.getString(2);
                int credit=rs.getInt(3);
                int classHour=rs.getInt(4);
                boolean is_pf=rs.getBoolean(5);
                Course.CourseGrading grading;
                if(is_pf){
                    grading= Course.CourseGrading.PASS_OR_FAIL;
                }else {
                    grading= Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
                String prerequisite=rs.getString(6);
                result.add(new Course(id,name,credit,classHour,grading,prerequisite));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        return null;
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
