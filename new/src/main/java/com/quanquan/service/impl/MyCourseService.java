package com.quanquan.service.impl;

import com.quanquan.dto.*;
import com.quanquan.dto.prerequisite.AndPrerequisite;
import com.quanquan.dto.prerequisite.CoursePrerequisite;
import com.quanquan.dto.prerequisite.OrPrerequisite;
import com.quanquan.dto.prerequisite.Prerequisite;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.exception.IntegrityViolationException;
import com.quanquan.service.CourseService;
import com.quanquan.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@Service
public class MyCourseService implements CourseService {

    @Autowired
    DataSource dataSource;

    @Override
    public List<CourseSection> getAllSections() {
        try(Connection con=dataSource.getConnection()){
            String sql="select id," +
                    "          course_id course," +
                    "          semester_id semester," +
                    "          name," +
                    "          total_capacity totalCapacity," +
                    "          left_capacity leftCapacity" +
                    " from mybatis.section";
            return Util.query(CourseSection.class,con,sql);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //完成√
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection con=dataSource.getConnection()){
            String sql="insert into mybatis.course(id, name, credit, class_hour, prerequisite) values (?,?,?,?,?)";
            String preStr = prerequisite==null?null:prerequisite.when(new Prerequisite.Cases<>() {
                    @Override
                    public String match(AndPrerequisite self) {
                        String[] children = self.getTerms().stream()
                                .map(term -> term.when(this))
                                .toArray(String[]::new);
                        return '(' + String.join(" AND ", children) + ')';
                    }

                    @Override
                    public String match(OrPrerequisite self) {
                        String[] children = self.getTerms().stream()
                                .map(term -> term.when(this))
                                .toArray(String[]::new);
                        return '(' + String.join(" OR ", children) + ')';
                    }

                    @Override
                    public String match(CoursePrerequisite self) {
                        return self.getCourseID();
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
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //完成√
    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try (Connection con=dataSource.getConnection()){
            String sql="insert into mybatis.section (course_id,semester_id,name,total_capacity,left_capacity) values (?,?,?,?,?)";
            return Util.addAndGetKey(con,sql, courseId,
                                              semesterId,
                                              sectionName,
                                              totalCapacity,
                                              totalCapacity);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }

    //完成√
    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        try (Connection con=dataSource.getConnection()){
            String sql = "insert into mybatis.section_class(section_id, instructor_id, day_of_week, class_begin, class_end, location, week_list) values (?,?,?,?,?,?,?)";
            return Util.addAndGetKey(con, sql, sectionId,
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
        try(Connection con=dataSource.getConnection()){
            String sql1= """
                delete
                from mybatis.section_class
                where id in (select section_class.id
                             from section_class
                                      join mybatis.section s on s.id = section_class.section_id
                                      join mybatis.course c on c.id = s.course_id
                             where c.id=?);""";
            Util.update(con,sql1,courseId);
            //删CourseSectionClass
            String sql2= """
                    delete from mybatis.student_section where mybatis.student_section.section_id in(select s.id
                    from mybatis.section s join mybatis.course c on c.id = s.course_id
                    where c.id=?);""";
            Util.update(con,sql2,courseId);
            //删student_section
            String sql3= """
                    delete from mybatis.section where course_id=?;""";
            Util.update(con,sql3,courseId);
            //删section
            String sql4="delete from mybatis.major_course where course_id=?";
            Util.update(con,sql4,courseId);
            //删major_course
            String sql5="delete from mybatis.course where id=?";
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
        try(Connection con=dataSource.getConnection()){
            String sql1= """
                    delete from mybatis.section_class where id in(select section_class.id
                    from section_class join mybatis.section s on s.id = section_class.section_id
                    where s.id=?);""";
            Util.update(con,sql1,sectionId);
            String sql2="delete from mybatis.student_section where section_id=?";
            Util.update(con,sql2,sectionId);
            String sql3="delete from mybatis.section where id=?";
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
        try(Connection con=dataSource.getConnection()){
            String sql3="delete from mybatis.section_class where id=?";
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
        try (Connection con=dataSource.getConnection()){
            String sql= """
                        select id,
                               name,
                               credit,
                               class_hour "classHour"
                        from mybatis.course;""";
            return Util.query(Course.class,con,sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    //完成√
    @Override
    public List<CourseSection> getSectionsInSemester(String courseId, int semesterId) {
        try(Connection con=dataSource.getConnection()) {
            String sql= """
                        select id,
                               name,
                               total_capacity "totalCapacity",
                               left_capacity "leftCapacity"
                        from mybatis.section
                        where course_id=? and semester_id=?""";
            ArrayList<CourseSection> res = Util.query(CourseSection.class, con, sql, courseId, semesterId);
            if(res.isEmpty()){
                throw new EntityNotFoundException();
            }
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    //完成√
    @Override
    public Course getCourseBySection(int sectionId) {
        try(Connection con=dataSource.getConnection()) {
            String sql = """
                    select distinct c.id,
                                class_hour "classHour",
                                c.name,credit
                    from mybatis.course c join mybatis.section s on c.id = s.course_id
                    where s.id=?""";
            ArrayList<Course> res = Util.query(Course.class, con, sql, sectionId);
            if (res.isEmpty()) {
                throw new EntityNotFoundException();
            }
            return res.get(0);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    //完成√
    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        try(Connection con=dataSource.getConnection()) {
            String sql = """
                    select id,
                           instructor_id instructor,
                           day_of_week "dayOfWeek",
                           week_list "weekList",
                           class_begin "classBegin",
                           class_end "classEnd",
                           location
                    from mybatis.section_class
                    where id=?;""";
            ArrayList<CourseSectionClass> res = Util.query(CourseSectionClass.class, con, sql, sectionId);
            if (res.isEmpty()) {
                throw new EntityNotFoundException();
            }
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    //完成√
    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try (Connection con=dataSource.getConnection()){
            String sql = """
                        select section_id
                        from mybatis.section_class
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
                    from mybatis.section
                    where id=?""";
            return Util.query(CourseSection.class, con, sql, id.get(0)).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        ArrayList<Student> cs=new ArrayList<>();
        try(Connection con=dataSource.getConnection()) {
            String sql= """
                    select * from
                    (select *
                    from (select s.id
                    from mybatis.course c join mybatis.section s on c.id = s.course_id
                    join mybatis.semester s2 on s2.id = s.semester_id\s
                    where c.id=? and s2.id=?) le join mybatis.student_section on section_id=le.id) mid
                    join mybatis.student stu on mid.student_id=stu.id join mybatis.major m on stu.major_id = m.id
                    join mybatis.department d on d.id = m.department_id;
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
                String firstName = rs.getString(2);
                String lastName = rs.getString(3);
                String name;
                if(firstName.charAt(0) >= 'A' && firstName.charAt(0) <= 'Z')
                    name = firstName + " " + lastName;
                else name = firstName + lastName;
                student.fullName = name;
                student.enrolledDate = rs.getDate(4);
                student.major = major;
                cs.add(student);
            }
            return cs;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
