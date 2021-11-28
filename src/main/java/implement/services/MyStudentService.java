package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class MyStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {

    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        //同时要修改表section中的left_capacity
        return null;
    }
    private boolean passAllPreCourse(Connection con,int studentId,int sectionId){
        //1.获取所有通过的课的id
        try {
            String sql="select course_id\n" +
                        "from section\n" +
                        "where id in(\n" +
                        "    select section_id\n" +
                        "    from student_section\n" +
                        "    where student_id=? and is_passed=true\n" +
                        ");";
            PreparedStatement ps = con.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
        //2.判断是否满足先修课条件
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        //同时要修改表section中的left_capacity
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        //同时要修改表section中的left_capacity
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        return null;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {
        return null;
    }
}
