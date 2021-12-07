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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class MyStudentService implements StudentService {
    public Connection con;

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
        //0.准备所有信息: infos
        String sql= """
                select left_capacity leftCapacity,
                       course_id courseId,
                       c.name||'['||sec.name||']' fullName,
                       first_name firstName,last_name lastName,
                       day_of_week dayOfWeek,
                       class_begin classBegin,class_end classEnd,
                       location,
                       is_pf grading,
                       sec.id sectionId,
                       instructor_id instructorId,
                       sc.id classId,
                       week_list weekList
                from student_section ss
                     join section sec on ss.section_id=sec.id
                                      and student_id=?
                     join semester sem on sec.semester_id=sem.id
                                       and sem.id=?
                     join section_class sc on sec.id = sc.section_id
                     join instructor i on sc.instructor_id = i.id
                     join course c on sec.course_id = c.id""";
        Stream<Info> infos = Util.query(Info.class, con, sql, studentId, semesterId).stream();
        //1.筛选searchCid & searchName & ignoreFull(只和section有关)
        if(searchCid!=null){
            infos=infos.filter(info -> info.courseId.equals(searchCid));
        }
        if(searchName!=null){
            infos=infos.filter(info -> info.fullName.equals(searchName));
        }
        if(ignoreFull){
            infos=infos.filter(info -> info.leftCapacity>0);
        }
        //TODO: 2.处理4个正向筛选条件(和细分的class有关的筛选条件)
        if(!(searchInstructor==null && searchDayOfWeek==null && searchClassTime==null && searchClassLocations==null)){
            /*Motivation of filteredSecIds:
            1个section有2个class, 只有一个class被筛除(如筛选instructor时),
            正确结果应该是该section被筛除，但是只筛选infos无法做到这一点
             */
            HashSet<Integer> filteredSecIds = new HashSet<>();
            //2.1层层生成filteredSecIds
            if (searchInstructor != null) {
                /*infos.
                infos = infos.forEach(info -> (info.firstName + info.lastName).startsWith(searchInstructor) ||
                                    (info.firstName + ' ' + info.lastName).startsWith(searchInstructor) ||
                                    info.firstName.startsWith(searchInstructor) ||
                                    info.lastName.startsWith(searchInstructor));*/
            }
            if (searchDayOfWeek != null) {
                infos = infos.filter(info -> info.dayOfWeek == searchDayOfWeek);
            }
            if (searchClassTime != null) {
                infos = infos.filter(info -> info.classBegin <= searchClassTime &&
                        info.classEnd >= searchClassTime);
            }
            if (searchClassLocations != null) {
                infos = infos.filter(info -> searchClassLocations.contains(info.location));
            }
            //2.2利用filteredSecIds筛选infos
        }
        //----------CourseType不筛选了，摆-------------
        //3.筛选ignorePassed & ignoreMissingPrerequisites
        if(ignorePassed || ignoreMissingPrerequisites){
            //3.1.0获取该学生所有pass的课的courseId: passedCids
            sql= """
                    select distinct course_id
                    from student_section
                         join section on section_id = id
                                     and student_id=?
                                     and mark>=60""";
            ArrayList<String> passedCids=Util.query(String.class,con,sql,studentId);
            //3.1.1筛选ignorePassed
            if(ignorePassed){
                infos=infos.filter(info -> !passedCids.contains(info.courseId));
            }
            //3.1.2筛选ignoreMissingPrerequisites
            if(ignoreMissingPrerequisites){
                //Motivation: 为了避免infos中重复的courseId多次检验，优化效率
                //3.1.2.1首先生成不重复的courseId的HashSet: cids
                HashSet<String> cids = new HashSet<>();
                infos.forEach(info -> cids.add(info.courseId));
                //3.1.2.2筛选满足先修课的cids，生成filCids
                ArrayList<String> filCids = (ArrayList<String>) cids.stream().
                                            filter(cid -> passedPre(passedCids, cid)).
                                            collect(Collectors.toList());
                //3.1.2.3再用filCids筛选infos
                infos=infos.filter(info -> filCids.contains(info.courseId));
            }
        }
        //4.生成所有CourseSearchEntry: entries
        ArrayList<CourseSearchEntry> entries = new ArrayList<>();
        //4.1聚合生成所有的sectionIds
        HashSet<Integer> sectionIds = new HashSet<>();
        infos.forEach(info -> sectionIds.add(info.sectionId));
        //4.2将流infos转为List
        List<Info> information = infos.collect(Collectors.toList());
        //4.3生成每个entry对象
        for (Integer sectionId : sectionIds) {
            CourseSearchEntry entry = new CourseSearchEntry();
            //hasGenerate标记第一次找到该sectionId，因为course和section只需生成一次
            boolean hasGenerate=false;
            for (Info info : information) {
                if(info.sectionId==sectionId){
                    if(!hasGenerate) {
                        //4.3.1准备course
                        sql = """
                            select id,name,credit,class_hour classHour,is_pf grading
                            from course
                            where id=?""";
                        entry.course=Util.query(Course.class,con,sql,info.courseId).get(0);
                        //4.3.2准备section
                        sql="""
                            select id,name,total_capacity totalCapacity,left_capacity leftCapacity
                            from section
                            where id=?""";
                        entry.section=Util.query(CourseSection.class,con,sql,sectionId).get(0);
                        //TODO: 4.3.3准备conflictCourseNames
                        //4.3.3.1课程冲突
                        //4.3.3.2时间冲突
                        hasGenerate=true;
                    }
                    //4.3.4准备sectionClasses
                    CourseSectionClass clazz = new CourseSectionClass();
                    //4.3.4.1准备instructor
                    Instructor instructor = new Instructor();
                    if ((Pattern.compile("[a-zA-Z]").matcher(info.firstName).find() || info.firstName.equals(" ")) &&
                            (Pattern.compile("[a-zA-Z]").matcher(info.lastName).find() || info.lastName.equals(" "))) {
                        instructor.fullName = info.firstName + " " + info.lastName;
                    } else {instructor.fullName = info.firstName + info.lastName;}
                    instructor.id=info.instructorId;
                    clazz.instructor=instructor;
                    //4.3.4.2准备weekList
                    try {
                        clazz.weekList= new HashSet<>(List.of((Short[])info.weekList.getArray()));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    //4.3.4.3准备其余属性
                    clazz.id=info.classId;
                    clazz.dayOfWeek=info.dayOfWeek;
                    clazz.classBegin=info.classBegin;
                    clazz.classEnd=info.classEnd;
                    clazz.location=info.location;
                    entry.sectionClasses.add(clazz);
                }
            }
            entries.add(entry);
        }
        if(ignoreConflict){
            //1.课程冲突
            //1.1获取该学生本学期已选的courseId: selectedCids
            sql= """
                    select course_id
                    from student_section
                         join section on id=section_id
                                     and semester_id=?
                                     and student_id=?""";
            ArrayList<String> selectedCids = Util.querySingle(con, sql, semesterId, studentId);
            infos=infos.filter(info -> !selectedCids.contains(info.courseId));
            //2.时间冲突
        }
        return entries;
    }

    /**
     * searchCourse()的内部类
     */
    public class Info{
        public int leftCapacity;
        public String courseId,
                fullName,
                firstName,lastName;
        public DayOfWeek dayOfWeek;
        public short classBegin,classEnd;
        public String location;
        public Course.CourseGrading grading;
        public int sectionId,
                   instructorId,
                   classId;
        public Array weekList;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        //2&3&5.1这三种情况要区分开
        try{
            //1.班级不存在
            String sql1 = "select left_capacity from section where id=?";
            ArrayList<Integer> capacity = Util.querySingle(con, sql1, sectionId);
            if(capacity.isEmpty()){
                return EnrollResult.COURSE_NOT_FOUND;
            }
            //2.已经选了这个班级
            String sql2="select mark from student_section where section_id=? and student_id=?";
            if(!Util.querySingle(con, sql2, sectionId, studentId).isEmpty()){
                return EnrollResult.ALREADY_ENROLLED;
            }
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
            if(!Util.querySingle(con, sql3, studentId, sectionId).isEmpty()){
                return EnrollResult.ALREADY_PASSED;
            }
            //4.先修课不满足
            String sql4="select course_id from section where id=?";
            String courseId=(String)Util.querySingle(con,sql4,sectionId).get(0);
            if(!passedPrerequisitesForCourse(studentId,courseId)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            //5.冲突
            //5.1课程冲突(已经选了这门课)
            //TODO: 这里可能会有bug, “本学期”的判断条件可能是学期与该section的学期相同，而不是mark=-1
            String sql5= """
                    select course_id
                    from section join student_section
                         on id=section_id
                         and student_id=?
                         and mark=-1
                         and course_id= (
                             select course_id
                             from section
                             where id=?
                         )""";//本学期所选的这门课的courseId，如果不为空，则已选过这门课
            if(!Util.querySingle(con,sql5,studentId,sectionId).isEmpty()){
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }
            //5.2时间冲突
            //5.2.1获取该学生本学期的所有class的classes: classes
            String sql6= """
                    select day_of_week dayOfWeek,
                           class_begin classBegin,class_end classEnd,
                           week_list weekList
                    from student_section
                         join section on section_id=section.id
                                      and student_id=?
                                      and semester_id=(
                                              select semester_id
                                              from section
                                              where id=?
                                          )
                         join section_class on section_class.section_id=section.id""";
            //TODO: query中，Array→Set<Short>有没有bug?
            ArrayList<CourseSectionClass> classes =
                    Util.query(CourseSectionClass.class,con,sql6,studentId,sectionId);
            //5.2.2获取该sectionId的sectionClass: selectClasses
            String sql7= """
                    select day_of_week dayOfWeek,
                           class_begin classBegin,class_end classEnd,
                           week_list weekList
                    from section_class
                    where section_id=?;""";
            //TODO: query中，Array→Set<Short>有没有bug?
            ArrayList<CourseSectionClass> selectClasses =
                    Util.query(CourseSectionClass.class,con,sql7,sectionId);
            //5.2.3比较selectClasses和classes,判断时间是否冲突(类似哈希思想)
            for (CourseSectionClass selectClass : selectClasses) {
                for (CourseSectionClass clazz : classes) {
                    //5.2.3.1先寻找“相同DayOfWeek && 课程时间冲突”的情况
                    if(selectClass.dayOfWeek==clazz.dayOfWeek
                    && selectClass.classBegin>=clazz.classEnd
                    && clazz.classBegin>=selectClass.classEnd){
                        //5.2.3.2再判断weekList是否冲突
                        for (Short week : selectClass.weekList) {
                            if(clazz.weekList.contains(week)){
                                return EnrollResult.COURSE_CONFLICT_FOUND;
                            }
                        }
                    }
                }
            }
            //6.班级满了
            if(capacity.get(0)==0){
                return EnrollResult.COURSE_IS_FULL;
            }
            //7.选课成功
            //7.1添加一条student_section关系
            String sql8="insert into student_section values (?,?,-1)";//本学期新选的课，mark为-1
            Util.update(con,sql8,studentId,sectionId);
            //7.2表section中的left_capacity++
            updateLeftCapacity(con,sectionId,true);
            return EnrollResult.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            //8.未知错误
            return EnrollResult.UNKNOWN_ERROR;
        }
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        //同时要修改表section中的left_capacity,调用updateLeftCapacity()
        String sql="delete from student_section where student_id=? and section_id=?;";
        if(Util.update(con,sql,studentId,sectionId)==1){
            try {
                updateLeftCapacity(con,sectionId,false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        //不要修改left_capacity
        String sql="insert into student_section(student_id, section_id, mark) values (?,?,?);";
        Util.update(con,sql,studentId,sectionId,grade);
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
            String sql = """
                    select m.id, m.name, m.department_id
                    from student join major m on m.id = student.major_id
                    where student.id = ?""";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            int mid = rs.getInt(1);
            String name = rs.getString(2);
            int did = rs.getInt(3);
            String sql2 = """
                    select *
                    from department
                    where id = ?""";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1, did);
            ResultSet rs2 = ps2.executeQuery();
            String d_name = rs2.getString(2);
            Department dep = new Department();
            dep.id=did;
            dep.name=d_name;
            Major maj = new Major();
            maj.id=mid;
            maj.name=name;
            maj.department=dep;
            return maj;
        }catch(SQLException throwables){
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
