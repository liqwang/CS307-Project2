package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

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
        //2&3&5.1这三种情况要区分开
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            //1.班级不存在
            String sql1 = "select total_capacity,left_capacity from section where id=?";
            PreparedStatement ps = con.prepareStatement(sql1);
            ps.setInt(1, sectionId);
            ResultSet capacity=ps.executeQuery();
            if(!capacity.next()){return EnrollResult.COURSE_NOT_FOUND;}
            //2.已经选了这个班级
            String sql2="select is_passed from student_section where section_id=? and student_id=?";
            ps = con.prepareStatement(sql2);
            ps.setInt(1,sectionId);
            ps.setInt(2,studentId);
            //新选的课，mark=null, is_passed=null
            //之前学期的课，mark不为null, is_passed为true或false
            if(ps.executeQuery().next()){return EnrollResult.ALREADY_ENROLLED;}
            //3.已经通过了这门课
            String sql3="select course_id\n" +
                        "from section join student_section\n" +
                        "     on id=section_id\n" +
                        "     and student_id=?       \n" +
                        "     and is_passed=true       \n" +
                        "     and course_id = (\n" +
                        "         select course_id\n" +
                        "         from section\n" +
                        "         where id=?\n" +
                        "     )";//找出之前通过的这门课的courseId，如果不为空，则已通过
            ps=con.prepareStatement(sql3);
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            if(ps.executeQuery().next()){return EnrollResult.ALREADY_PASSED;}
            //4.先修课不满足
            String sql4="select course_id from section where id=?";
            ps = con.prepareStatement(sql4);
            ps.setInt(1,sectionId);
            ResultSet rs = ps.executeQuery();
            String courseId=rs.getString(2);
            //这里虽然复用了方法，但是方法中新创建了一个Connection，降低了效率
            if(!passedPrerequisitesForCourse(studentId,courseId)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            //5.冲突
            //5.1课程冲突(已经选了这门课)
            String sql5="select course_id\n" +
                        "from section join student_section\n" +
                        "     on id=section_id\n" +
                        "     and student_id=?\n" +
                        "     and mark is null\n" +
                        "     and course_id= (\n" +
                        "         select course_id\n" +
                        "         from section\n" +
                        "         where id=?\n" +
                        "     )";//本学期所选的这门课的courseId，如果不为空，则已选过这门课
            ps = con.prepareStatement(sql5);
            if(ps.executeQuery().next()){return EnrollResult.COURSE_CONFLICT_FOUND;}
            //5.2时间冲突
            //5.2.1获取该学生本学期的所有class中和该sectionId在同一DayOfWeek的classes: sameDayClasses
            String sql6="select day_of_week,class_begin,class_end,week_list\n" +
                        "from student_section\n" +
                        "     join section on section_id=section.id\n" +
                        "                  and student_id=?\n" +
                        "                  and semester_id=(\n" +
                        "                          select semester_id\n" +
                        "                          from section\n" +
                        "                          where id=?\n" +
                        "                      )\n" +
                        "     join section_class on section_class.section_id=section.id\n" +
                        "                        and day_of_week in(\n" +
                        "                                select day_of_week\n" +
                        "                                from section_class\n" +
                        "                                where section_id=?\n" +
                        "                            )";
            ps=con.prepareStatement(sql6);
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            ps.setInt(3,sectionId);
            rs = ps.executeQuery();
            List<CourseSectionClass> sameDayClasses = getClassList(rs);
            //5.2.2获取该sectionId的sectionClass: selectClasses
            String sql7="select day_of_week,class_begin,class_end,week_list\n" +
                        "from section_class\n" +
                        "where section_id=?;";
            ps=con.prepareStatement(sql7);
            ps.setInt(1,sectionId);
            rs=ps.executeQuery();
            ArrayList<CourseSectionClass> selectClasses = getClassList(rs);
            //5.2.3比较selectClasses和sameDayClasses,判断时间是否冲突(类似哈希思想)
            for (CourseSectionClass selectClass : selectClasses) {
                for (CourseSectionClass sameDayClass : sameDayClasses) {
                    //5.2.3.1先寻找“相同DayOfWeek && 课程时间冲突”的情况
                    if(selectClass.dayOfWeek==sameDayClass.dayOfWeek
                    && selectClass.classBegin>=sameDayClass.classEnd
                    && sameDayClass.classBegin>=selectClass.classEnd){
                        //5.2.3.2再判断weekList是否冲突
                        for (Short week : selectClass.weekList) {
                            if(sameDayClass.weekList.contains(week)){
                                return EnrollResult.COURSE_CONFLICT_FOUND;
                            }
                        }
                    }
                }
            }
            //6.班级满了
            if(capacity.getInt(5)==capacity.getInt(6)){
                return EnrollResult.COURSE_IS_FULL;
            }
            //7.选课成功
            //7.1添加一条student_section关系
            String sql8="insert into student_section values (?,?,null,null)";
            ps=con.prepareStatement(sql8);
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            ps.executeUpdate();
            //7.2表section中的left_capacity++
            updateLeftCapacity(con,sectionId,true);
            ps.close();
            return EnrollResult.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            //8.未知错误
            return EnrollResult.UNKNOWN_ERROR;
        }
    }
    /**
     * 步骤5.2内部的辅助方法
     */
    private ArrayList<CourseSectionClass> getClassList(ResultSet rs) throws SQLException {
        ArrayList<CourseSectionClass> res = new ArrayList<>();
        while(rs.next()){
            DayOfWeek dayOfWeek=DayOfWeek.of(rs.getInt(4));
            short classBegin = rs.getShort(5);
            short classEnd = rs.getShort(6);
            Array array = rs.getArray(8);
            HashSet<Short> weekList = new HashSet<>();
            for (Object o : (Object[]) array.getArray()) {
                if(o instanceof Number){
                    weekList.add((short)o);
                }
            }
            res.add(new CourseSectionClass(
            0,null,dayOfWeek,weekList,null,classBegin,classEnd,null));
        }
        return res;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        //同时要修改表section中的left_capacity,调用updateLeftCapacity()
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        //同时要修改表section中的left_capacity,调用updateLeftCapacity()
    }
    private void updateLeftCapacity(Connection con,int sectionId,boolean isAdd) throws SQLException{
        //isAdd为true: left_capacity++
        //isAdd为false: left_capacity--
        String addSql="update section\n" +
                      "set left_capacity=(\n" +
                      "        select left_capacity\n" +
                      "        from section\n" +
                      "        where id=?\n" +
                      "    )+1\n" +
                      "where id=?";
        String dropSql="update section\n" +
                       "set left_capacity=(\n" +
                       "        select left_capacity\n" +
                       "        from section\n" +
                       "        where id=?\n" +
                       "    )-1\n" +
                       "where id=?";
        PreparedStatement ps;
        if(isAdd){ps=con.prepareStatement(addSql);}
        else{ps=con.prepareStatement(dropSql);}
        ps.setInt(1,sectionId);
        ps.setInt(2,sectionId);
        ps.executeUpdate();
        ps.close();
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
