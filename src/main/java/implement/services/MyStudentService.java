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
import java.sql.*;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            //1.班级不存在
            String sql1 = "select total_capacity,left_capacity from section where id=?";
            PreparedStatement ps = con.prepareStatement(sql1);
            ps.setInt(1, sectionId);
            ResultSet capacity=ps.executeQuery();
            if(!capacity.next()){return EnrollResult.COURSE_NOT_FOUND;}
            //2.已经选了这个班级 & 3.已经通过了这门课
            String sql2="select is_passed from student_section where section_id=? and student_id=?";
            ps = con.prepareStatement(sql2);
            ps.setInt(1,sectionId);
            ps.setInt(2,studentId);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
                if(!rs.getBoolean(4)){return EnrollResult.ALREADY_ENROLLED;}
                else {return EnrollResult.ALREADY_PASSED;}
            }
            //4.先修课不满足
            String sql3="select course_id from section where id=?";
            ps = con.prepareStatement(sql3);
            ps.setInt(1,sectionId);
            rs = ps.executeQuery();
            String courseId=rs.getString(2);
            //这里虽然复用了方法，但是方法中新创建了一个Connection，降低了效率
            if(!passedPrerequisitesForCourse(studentId,courseId)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            //5.冲突
            //5.1课程冲突(已经选了这门课)
            String sql4="";
            ps = con.prepareStatement(sql4);
            //5.2时间冲突
            //6.班级满了
            if(capacity.getInt(5)==capacity.getInt(6)){
                return EnrollResult.COURSE_IS_FULL;
            }
            //同时要修改表section中的left_capacity
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            //1.获取所有通过的课的id: passedCids
            String sql1="select course_id\n" +
                        "from section\n" +
                        "where id in(\n" +
                        "    select section_id\n" +
                        "    from student_section\n" +
                        "    where student_id=? and is_passed=true\n" +
                        ")";
            PreparedStatement ps = con.prepareStatement(sql1);
            ps.setInt(1,studentId);
            ResultSet rs = ps.executeQuery();
            HashSet<String> passedCids = new HashSet<>();
            while(rs.next()){
                passedCids.add(rs.getString(2));
            }
            //2.获取布尔表达式
            //2.1获取先修课String: pre
            String sql2="select prerequisite from course where id=?";
            ps = con.prepareStatement(sql2);
            ps.setString(1,courseId);
            String pre = ps.executeQuery().getString(6);
            //2.2提取出pre中的课程id: eg:((MA101A OR MA101B) AND MA103A)
            String[] preCids = pre.split(" (AND|OR) ");// ((MA101A MA101B) MA103A)
            for (int i = 0; i < preCids.length; i++) {
                preCids[i]=preCids[i].replaceAll("[()]","");
            }//去除括号: MA101A MA101B MA103A
            //2.3将pre转为布尔表达式
            pre=pre.replace(" AND ","&").replace(" OR ","|");
            for (String preCid : preCids) {
                pre=pre.replace(preCid,passedCids.contains(preCid)?"T":"F");
            }
            //3.计算布尔表达式pre: ((T|F)&T)
            //3.1用逆波兰算法将pre转为后缀表达式postfix: TF|T&
            Stack<Character> stack = new Stack<>();
            StringBuilder postfix = new StringBuilder();
            for (char c : pre.toCharArray()) {
                switch (c) {
                    case '(' -> stack.push('(');
                    case ')' -> {
                        char top;
                        while ((top = stack.pop()) != '(') {
                            postfix.append(top);
                        }
                    }
                    case '&' -> {
                        if (!stack.isEmpty() && stack.peek() == '&') {
                            postfix.append(stack.pop());
                        }
                        stack.push('&');
                    }
                    case '|' -> {
                        if (!stack.isEmpty() && stack.peek() == '&') {
                            postfix.append(stack.pop());
                        }
                        if (!stack.isEmpty() && stack.peek() == '|') {
                            postfix.append(stack.pop());
                        }
                        stack.push('|');
                    }
                    default -> postfix.append(c);//对应于T,F
                }
            }
            while (!stack.isEmpty()){
                postfix.append(stack.pop());
            }
            //3.2用栈计算后缀表达式postfix: TF|T&
            Stack<Boolean> stack2 = new Stack<>();
            for (char c : postfix.toString().toCharArray()) {
                switch (c){
                    case '|' -> stack2.push(stack2.pop() | stack2.pop());
                    //注意这里不能用||，否则可能只pop一个，出现bug
                    case '&' -> stack2.push(stack2.pop() & stack2.pop());
                    //注意这里不能用&&，否则可能只pop一个，出现bug
                    case 'T' -> stack2.push(true);
                    case 'F' -> stack2.push(false);
                }
            }
            return stack2.pop();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public Major getStudentMajor(int studentId) {
        return null;
    }
}
