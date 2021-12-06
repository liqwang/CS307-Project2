package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;
import implement.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class MyStudentService implements StudentService {
    Connection con;

    {
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into student (id,major_id,first_name,last_name,enrolled_date) values (?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setInt(1,userId);
            ps.setInt(2,majorId);
            ps.setString(3,firstName);
            ps.setString(4,lastName);
            ps.setDate(5,enrolledDate);
            ps.executeUpdate();
        }catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    /**
     * CourseSearchEntry本质是Section
     */
    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        String sql= """
                select student_id,
                       semester_id,
                       left_capacity,
                       course_id,
                       c.name||'['||sec.name||']' full_name,
                       first_name,last_name,
                       day_of_week,
                       begin_time,end_time,
                       location,
                       is_pf
                from student_section ss
                     join section sec on ss.section_id=sec.id
                                      and student_id=?
                     join semester sem on sec.semester_id=sem.id
                                       and sem.id=?
                     join section_class sc on sec.id = sc.section_id
                     join instructor i on sc.instructor_id = i.id
                     join course c on sec.course_id = c.id""";
        Stream<Info> infos = Util.query(Info.class, con, sql, studentId, semesterId).stream();
        if(searchCid!=null){
            infos=infos.filter(info -> info.courseId.equals(searchCid));
        }
        if(searchName!=null){
            infos=infos.filter(info -> info.fullName.equals(searchName));
        }
        if(searchInstructor!=null){
            infos=infos.filter(info -> (info.firstName+info.lastName).startsWith(searchInstructor)||
                                       (info.firstName+' '+info.lastName).startsWith(searchInstructor)||
                                        info.firstName.startsWith(searchInstructor)||
                                        info.lastName.startsWith(searchInstructor));
        }
        if(searchDayOfWeek!=null){
            infos=infos.filter(info -> info.dayOfWeek==searchDayOfWeek);
        }
        if(searchClassTime!=null){
            infos=infos.filter(info -> info.beginTime<=searchClassTime &&
                                       info.endTime>=searchClassTime);
        }
        if(searchClassLocations!=null){
            infos=infos.filter(info -> searchClassLocations.contains(info.location));
        }
        //CourseType不筛选了，摆
        if(ignoreFull){
            infos=infos.filter(info -> info.leftCapacity>0);
        }
        if(ignorePassed || ignoreMissingPrerequisites){
            //获取该学生所有pass的课的courseId: passedCids
            sql= """
                    select distinct course_id
                    from student_section
                         join section on section_id = id
                                     and student_id=?
                                     and mark>=60""";
            ArrayList<String> passedCids=Util.query(String.class,con,sql,studentId);
            if(ignorePassed){
                infos=infos.filter(info -> !passedCids.contains(info.courseId));
            }
            if(ignoreMissingPrerequisites){
                //为了避免infos中重复的courseId多次检验
                //1.首先生成不重复的courseId的HashSet: cids
                HashSet<String> cids = new HashSet<>();
                infos.forEach(info -> cids.add(info.courseId));
                //2.筛选满足先修课的cids，生成filCids
                ArrayList<String> filCids = (ArrayList<String>) cids.stream().
                                            filter(cid -> passedPre(passedCids, cid)).
                                            collect(Collectors.toList());
                //3.再用filCids筛选infos
                infos=infos.filter(info -> filCids.contains(info.courseId));
            }
        }
        if(ignoreConflict){

        }
        //最后生成CourseSearchEntry
        ArrayList<CourseSearchEntry> res = new ArrayList<>();
        return null;
    }
    public class Info{
        public int studentId,
                   semesterId,
                   leftCapacity;
        public String courseId,
                      fullName,
                      firstName,lastName;
        public DayOfWeek dayOfWeek;
        public short beginTime,endTime;
        public String location;
        public Course.CourseGrading grading;

        public Info(int studentId, int semesterId, int leftCapacity, String courseId, String fullName, String firstName, String lastName, DayOfWeek dayOfWeek, short beginTime, short endTime, String location, Course.CourseGrading grading) {
            this.studentId = studentId;
            this.semesterId = semesterId;
            this.leftCapacity = leftCapacity;
            this.courseId = courseId;
            this.fullName = fullName;
            this.firstName = firstName;
            this.lastName = lastName;
            this.dayOfWeek = dayOfWeek;
            this.beginTime = beginTime;
            this.endTime = endTime;
            this.location = location;
            this.grading = grading;
        }
    }

    //TODO: 修复rs中columnIndex的bug
    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        //2&3&5.1这三种情况要区分开
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            //1.班级不存在
            String sql1 = "select left_capacity from section where id=?";
            PreparedStatement ps = con.prepareStatement(sql1);
            ps.setInt(1, sectionId);
            ResultSet capacity=ps.executeQuery();
            if(!capacity.next()){return EnrollResult.COURSE_NOT_FOUND;}
            //2.已经选了这个班级
            String sql2="select mark from student_section where section_id=? and student_id=?";
            ps = con.prepareStatement(sql2);
            ps.setInt(1,sectionId);
            ps.setInt(2,studentId);
            if(ps.executeQuery().next()){return EnrollResult.ALREADY_ENROLLED;}
            //3.已经通过了这门课
            String sql3= """
                    select course_id
                    from section join student_section
                         on id=section_id
                         and student_id=?
                         and mark>=60
                         and course_id = (
                             select course_id
                             from section
                             where id=?
                         )""";//找出之前通过的这门课的courseId，如果不为空，则已通过
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
            if(!passedPrerequisitesForCourse(studentId,courseId)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            //5.冲突
            //5.1课程冲突(已经选了这门课)
            String sql5= """
                    select course_id
                    from section join student_section
                         on id=section_id
                         and student_id=?
                         and mark is null
                         and course_id= (
                             select course_id
                             from section
                             where id=?
                         )""";//本学期所选的这门课的courseId，如果不为空，则已选过这门课
            ps = con.prepareStatement(sql5);
            if(ps.executeQuery().next()){return EnrollResult.COURSE_CONFLICT_FOUND;}
            //5.2时间冲突
            //5.2.1获取该学生本学期的所有class中和该sectionId在同一DayOfWeek的classes: sameDayClasses
            String sql6= """
                    select day_of_week,class_begin,class_end,week_list
                    from student_section
                         join section on section_id=section.id
                                      and student_id=?
                                      and semester_id=(
                                              select semester_id
                                              from section
                                              where id=?
                                          )
                         join section_class on section_class.section_id=section.id
                                            and day_of_week in(
                                                    select day_of_week
                                                    from section_class
                                                    where section_id=?
                                                )""";
            ps=con.prepareStatement(sql6);
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            ps.setInt(3,sectionId);
            rs = ps.executeQuery();
            List<CourseSectionClass> sameDayClasses = getClassList(rs);
            //5.2.2获取该sectionId的sectionClass: selectClasses
            String sql7= """
                    select day_of_week,class_begin,class_end,week_list
                    from section_class
                    where section_id=?;""";
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
            if(capacity.getInt(1)==0){
                return EnrollResult.COURSE_IS_FULL;
            }
            //7.选课成功
            //7.1添加一条student_section关系
            String sql8="insert into student_section values (?,?,-1)";//本学期新选的课，mark为-1
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
        //不要修改left_capacity
    }
    private void updateLeftCapacity(Connection con,int sectionId,boolean isAdd) throws SQLException{
        //isAdd为true: left_capacity++
        //isAdd为false: left_capacity--
        String addSql= """
                update section
                set left_capacity=(
                        select left_capacity
                        from section
                        where id=?
                    )+1
                where id=?""";
        String dropSql= """
                update section
                set left_capacity=(
                        select left_capacity
                        from section
                        where id=?
                    )-1
                where id=?""";
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
            //获取所有通过的课的id: passedCids
            String sql1= """
                    select distinct course_id
                    from section
                    where id in(
                        select section_id
                        from student_section
                        where student_id=? and mark>=60
                    )""";
            ArrayList<String> passedCids = Util.query(String.class, con, sql1, studentId);
            return passedPre(passedCids,courseId);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }
    /**
     * 用于复用的辅助判断方法，判断passedCids是否满足了courseId的先修课要求
     */
    public boolean passedPre(ArrayList<String> passedCids,String courseId){
        //1.获取布尔表达式
        //1.1获取先修课String: pre
        String sql="select prerequisite from course where id=?";
        String pre = Util.query(String.class, con, sql, courseId).get(0);
        //1.2提取出pre中的课程id: eg:((MA101A OR MA101B) AND MA103A)
        String[] preCids = pre.split(" (AND|OR) ");// ((MA101A MA101B) MA103A)
        for (int i = 0; i < preCids.length; i++) {
            preCids[i]=preCids[i].replaceAll("[()]","");
        }//去除括号: MA101A MA101B MA103A
        //1.3将pre转为布尔表达式
        pre=pre.replace(" AND ","&").replace(" OR ","|");
        for (String preCid : preCids) {
            pre=pre.replace(preCid,passedCids.contains(preCid)?"T":"F");
        }
        //2.计算布尔表达式pre: ((T|F)&T)
        //2.1用逆波兰算法将pre转为后缀表达式postfix: TF|T&
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
        //2.2用栈计算后缀表达式postfix: TF|T&
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
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try{
            String sql = "select m.id, m.name, m.department_id\n" +
                    "from student join major m on m.id = student.major_id\n" +
                    "where student.id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            int mid = rs.getInt(1);
            String name = rs.getString(2);
            int did = rs.getInt(3);
            String sql2 = "select *\n" +
                    "from department\n" +
                    "where id = ?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1, did);
            ResultSet rs2 = ps2.executeQuery();
            String d_name = rs2.getString(2);
            Department department = new Department(did, d_name);
            Major major = new Major(mid, name, department);
            return major;
        }catch(SQLException throwables){
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
