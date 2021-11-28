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
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
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
        return 0;
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

    }

    @Override
    public void removeCourseSection(int sectionId) {

    }

    @Override
    public void removeCourseSectionClass(int classId) {

    }

    @Override
    public List<Course> getAllCourses() {
        return null;
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
