package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import implement.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.*;

@ParametersAreNonnullByDefault
public class MyCourseService implements CourseService {
    Connection con;
    {
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //完成√
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        String sql="insert into course(id, name, credit, class_hour, is_pf, prerequisite) values (?,?,?,?,?,?)";
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
        try{
            Util.update(con,sql, courseId,
                                         courseName,
                                         credit,
                                         classHour,
                                         grading==Course.CourseGrading.PASS_OR_FAIL,
                                         preStr
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    //完成√
    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        String sql="insert into section (course_id,semester_id,name,total_capacity,left_capacity) values (?,?,?,?,?)";
        return Util.addAndGetKey(con,sql, courseId,
                                          semesterId,
                                          sectionName,
                                          totalCapacity,
                                          totalCapacity);
    }

    //完成√
    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        String sql="insert into section_class(section_id, instructor_id, day_of_week, class_begin, class_end, location, week_list) values (?,?,?,?,?,?,?)";
        try {
            return Util.addAndGetKey(con,sql,sectionId,
                                             instructorId,
                                             dayOfWeek.getValue(),
                                             classStart,
                                             classEnd,
                                             location,
                                             con.createArrayOf("smallint", weekList.toArray()));
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }

    //完成√
    @Override
    public void removeCourse(String courseId) {
        try{String sql1= """
                delete from section_class where id in(select section_class.id
                from section_class join section s on s.id = section_class.section_id
                join public.course c on c.id = s.course_id
                where c.id=?);""";
            if(Util.update(con,sql1,courseId)==0){
                throw new EntityNotFoundException();
            }
            //删CourseSectionClass
            String sql2= """
                    delete from student_section where student_section.section_id in(select s.id
                    from section s join public.course c on c.id = s.course_id
                    where c.id=?);""";
            if(Util.update(con,sql2,courseId)==0){
                throw new EntityNotFoundException();
            }
            //删student_section
            String sql3= """
                    delete from section where course_id=?;""";
            if(Util.update(con,sql3,courseId)==0){
                throw new EntityNotFoundException();
            }
            //删section
            String sql4="delete from major_course where course_id=?";
            if(Util.update(con,sql4,courseId)==0){
                throw new EntityNotFoundException();
            }
            //删major_course
            String sql5="delete from course where id=?";
            if(Util.update(con,sql5,courseId)==0){
                throw new EntityNotFoundException();
            }
            //删course
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //完成√
    @Override
    public void removeCourseSection(int sectionId) {
        try{
            String sql1= """
                    delete from section_class where id in(select section_class.id
                    from section_class join section s on s.id = section_class.section_id
                    where s.id=?);""";
            if(Util.update(con,sql1,sectionId)==0){
                throw new EntityNotFoundException();
            }
            String sql2="delete from student_section where section_id=?";
            if(Util.update(con,sql2,sectionId)==0){
                throw new EntityNotFoundException();
            }
            String sql3="delete from section where id=?";
            if(Util.update(con,sql3,sectionId)==0){
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //完成√
    @Override
    public void removeCourseSectionClass(int classId) {
        try{
            String sql3="delete from section_class where id=?";
            if(Util.update(con,sql3,classId)==0){
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //完成√
    @Override
    public List<Course> getAllCourses() {
        String sql= """
                    select id,
                           name,
                           credit,
                           class_hour "classHour",
                           is_pf grading
                    from course;""";
        return Util.query(Course.class,con,sql);
    }

    //完成√
    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        String sql= """
                    select id,
                           name,
                           total_capacity "totalCapacity",
                           left_capacity "leftCapacity"
                    from section
                    where course_id=? and semester_id=?""";
        ArrayList<CourseSection> res = Util.query(CourseSection.class, con, sql, courseId, semesterId);
        if(res.isEmpty()){
            throw new EntityNotFoundException();
        }
        return res;
    }

    //完成√
    @Override
    public Course getCourseBySection(int sectionId) {
        String sql = """
                select distinct c.id,class_hour "classHour",
                    c.name,credit,is_pf grading
                from course c join section s on c.id = s.course_id
                where s.id=?""";
        ArrayList<Course> res = Util.query(Course.class, con, sql, sectionId);
        if (res.isEmpty()) {
            throw new EntityNotFoundException();
        }
        return res.get(0);
    }

    //完成√
    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        String sql = """
                select id,
                       instructor_id instructor,
                       day_of_week "dayOfWeek",
                       week_list "weekList",
                       class_begin "classBegin",
                       class_end "classEnd",
                       location
                from section_class
                where id=?;""";
        ArrayList<CourseSectionClass> res = Util.query(CourseSectionClass.class, con, sql, sectionId);
        if (res.isEmpty()) {
            throw new EntityNotFoundException();
        }
        return res;
    }

    //完成√
    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        String sql = """
                    select section_id
                    from section_class
                    where id=?
                """;
        ArrayList<Integer> id = Util.querySingle(con, sql, classId);
        if (id.isEmpty()) {
            throw new EntityNotFoundException();
        }
        sql = """
                select id,
                       name,
                       left_capacity "leftCapacity",
                       total_capacity "totalCapacity"
                from section
                where id=?""";
        return Util.query(CourseSection.class, con, sql, id.get(0)).get(0);
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        ArrayList<Student> cs=new ArrayList<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql= """
                    select * from
                    (select *
                    from (select s.id
                    from course c join section s on c.id = s.course_id
                    join public.semester s2 on s2.id = s.semester_id\s
                    where c.id=? and s2.id=?) le join student_section on section_id=le.id) mid
                    join student stu on mid.student_id=stu.id join major m on stu.major_id = m.id
                    join department d on d.id = m.department_id;
                    """;
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1,courseId);
            ps.setInt(2,semesterId);
            ResultSet rs = ps.executeQuery();
            ps.close();
            Department dp=new Department();
            dp.id = rs.getInt(7);
            dp.name = rs.getString(8);
            Major major=new Major();
            major.id = rs.getInt(2);
            major.name = rs.getString(6);
            major.department = dp;
            while (rs.next()){
                Student student = new Student();
                student.id = rs.getInt(1);
                String f_name = rs.getString(2);
                String l_name = rs.getString(3);
                String name;
                if(f_name.charAt(0) >= 'A' && f_name.charAt(0) <= 'Z')
                    name = f_name + " " + l_name;
                else name = f_name + l_name;
                student.fullName = name;
                student.enrolledDate = rs.getDate(4);
                student.major = major;
                cs.add(student);
            }
            return cs;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
