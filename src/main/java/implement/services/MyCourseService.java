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

    @Override
    public void removeCourse(String courseId) {
        try{
            String sql1="delete from course where id=?";
            if(Util.update(con,sql1,courseId)==0){
                throw new EntityNotFoundException();
            }

            String sql4="delete from major_course where course_id=?";
            Util.update(con,sql4,courseId);

            String sql2="delete from section where course_id=?";
            PreparedStatement ps2 = con.prepareStatement(sql2,PreparedStatement.RETURN_GENERATED_KEYS);
            ps2.setString(1,courseId);
            ps2.executeUpdate();
            ResultSet rs=ps2.getGeneratedKeys();
            //TODO: bug:一个course有多个section
            int sectionId=rs.getInt(1);

            String sql5="delete from student_section where section_id=?";
            Util.update(con,sql5,sectionId);

            String sql6="delete from semester where id=?";
            Util.update(con,sql6,sectionId);

            String sql3="delete from section_class where section_id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3,PreparedStatement.RETURN_GENERATED_KEYS);
            ps3.setInt(1,sectionId);
            ps3.executeUpdate();
            ResultSet rs2=ps3.getGeneratedKeys();
            int sectionClassId=rs2.getInt(1);

            String sql7="delete from instructor where id=?";
            Util.update(con,sql7,sectionClassId);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try{
            String sql2="delete from section where id=?";
            if(Util.update(con,sql2,sectionId)==0){
                throw new EntityNotFoundException();
            }

            String sql5="delete from student_section where section_id=?";
            Util.update(con,sql5,sectionId);

            String sql6="delete from semester where id=?";
            Util.update(con,sql6,sectionId);

            String sql3="delete from section_class where section_id=?";
            PreparedStatement ps3 = con.prepareStatement(sql3,PreparedStatement.RETURN_GENERATED_KEYS);
            ps3.setInt(1,sectionId);
            ps3.executeUpdate();
            ResultSet rs2=ps3.getGeneratedKeys();
            //TODO: bug:一个section有多个sectionClass

            int sectionClassId=rs2.getInt(1);
            String sql7="delete from instructor where id=?";
            Util.update(con,sql7,sectionClassId);
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
        ArrayList<Course> list = Util.query(Course.class, con, sql, sectionId);
        if (list.isEmpty()) {
            throw new EntityNotFoundException();
        }
        return list.get(0);
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        ArrayList<CourseSectionClass> cs=new ArrayList<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            Util.query()
            String sql= """
                    select * from section_class
                    join instructor i on section_class.instructor_id = i.id
                    join section s on section_class.section_id = s.id
                    where section_id=?;""";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,sectionId);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                int id=rs.getInt(1);
                String fullName=rs.getString(9)+" "+rs.getString(10);
                Instructor instructor=new Instructor();
                instructor.id=id;
                instructor.fullName=fullName;
                DayOfWeek dayOfWeek=DayOfWeek.of(rs.getInt(4));
                Short[] arr= (Short[]) rs.getArray(8).getArray();
                Set<Short> weekList=new HashSet<>(Arrays.asList(arr));//不确定
                CourseSection section=new CourseSection();

                section.id=rs.getInt(1);
                section.name=rs.getString(13);
                section.totalCapacity=rs.getInt(14);
                section.leftCapacity=rs.getInt(15);
                short classBegin, classEnd;
                classBegin=rs.getShort(5);
                classEnd=rs.getShort(6);
                String location=rs.getString(7);

                CourseSectionClass courseSectionClass=new CourseSectionClass();
                courseSectionClass.id=id;
                courseSectionClass.instructor=instructor;
                courseSectionClass.dayOfWeek=dayOfWeek;
                courseSectionClass.weekList=weekList;
                courseSectionClass.classBegin=classBegin;
                courseSectionClass.classEnd=classEnd;
                courseSectionClass.location=location;
                cs.add(courseSectionClass);
            }
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
        return cs;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql= """
                    select distinct section.id,course_id,semester_id,name,total_capacity,left_capacity
                    from section join public.section_class sc on section.id = sc.section_id
                    where sc.id=?;""";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,classId);
            ResultSet rs = ps.executeQuery();
            ps.close();
            CourseSection courseSection=new CourseSection();
            courseSection.id=rs.getInt(1);
            courseSection.name=rs.getString(4);
            courseSection.totalCapacity=rs.getInt(5);
            courseSection.leftCapacity=rs.getInt(6);
            return courseSection;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
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
