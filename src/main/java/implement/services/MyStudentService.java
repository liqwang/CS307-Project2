package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;
import implement.Info;
import implement.Util;
import implement.SelectedInfo;

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

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String fullName;
            if(firstName.charAt(0) >= 'A' && firstName.charAt(0) <= 'Z') fullName = firstName + " " + lastName;
            else fullName = firstName + lastName;
            String sql="insert into student (id,major_id,full_name,enrolled_date) values (?,?,?,?)";
            Util.update(con, sql, userId, majorId, fullName, enrolledDate);
        }catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    /**
     * CourseSearchEntry本质是Section
     */
    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            //0.准备所有信息: infos
            String sql= """
                    select left_capacity "leftCapacity",
                           course_id "courseId",
                           c.name||'['||sec.name||']' "courseFullName",
                           full_name "instructorFullName",
                           day_of_week "dayOfWeek",
                           class_begin "classBegin",class_end "classEnd",
                           location,
                           is_pf grading,
                           sec.id "sectionId",
                           instructor_id "instructorId",
                           sc.id "classId",
                           week_list "weekList"
                    from section sec
                            join semester sem on sec.semester_id=sem.id
                                           and sem.id=?
                            join section_class sc on sec.id = sc.section_id
                            join instructor i on sc.instructor_id = i.id
                            join course c on sec.course_id = c.id
                    order by "courseId","courseFullName";""";
            Stream<Info> infos = Util.query(Info.class, con, sql,semesterId).stream();
            //1.筛选searchCid & searchName & ignoreFull(只和section有关)
            if(searchCid!=null && !searchCid.equals("")){
                infos=infos.filter(info -> info.courseId.contains(searchCid));
            }
            if(searchName!=null && !searchName.equals("")){
                infos=infos.filter(info -> info.courseFullName.contains(searchName));
            }
            if(ignoreFull){
                infos=infos.filter(info -> info.leftCapacity>0);
            }
            List<Info> infoList = infos.collect(Collectors.toList());//Stream→List
            //2.处理4个正向筛选条件(和细分的class有关的筛选条件)
            HashSet<Integer> filteredSids = new HashSet<>();
            for (Info info : infoList) {
                if((searchInstructor==null||searchInstructor.equals("")||info.instructorFullName.replace(" ","").contains(searchInstructor.replace(" ","")))&&
                        (searchDayOfWeek==null||info.dayOfWeek == searchDayOfWeek)&&
                        (searchClassTime==null||info.classBegin <= searchClassTime && info.classEnd >= searchClassTime)
                        ){
                    if(searchClassLocations!=null && !searchClassLocations.isEmpty()){
                        for (String eachLocation : searchClassLocations) {
                            if(info.location.contains(eachLocation)){
                                filteredSids.add(info.sectionId);
                                break;
                            }
                        }
                    }else{filteredSids.add(info.sectionId);}
                }
            }
            infoList.removeIf(info -> !filteredSids.contains(info.sectionId));
//            if (searchInstructor != null && !searchInstructor.equals("")) {
//                /*Motivation of filteredSIds:
//                1个section有2个class, 只有一个class被筛除(如筛选instructor时),
//                正确结果应该是该section被筛除，但是只筛选infos无法做到这一点
//                 */
//                HashSet<Integer> filteredSids = new HashSet<>();
//                for (Info info : infoList) {
//                    //偷懒筛选法
//                    if (info.instructorFullName.replace(" ","").contains(searchInstructor)) {
//                        filteredSids.add(info.sectionId);
//                    }
//                }
//                infoList.removeIf(info -> !filteredSids.contains(info.sectionId));
//            }
//            if (searchDayOfWeek != null) {
//                HashSet<Integer> filteredSids = new HashSet<>();
//                for (Info info : infoList) {
//                    if(info.dayOfWeek == searchDayOfWeek){
//                        filteredSids.add(info.sectionId);
//                    }
//                }
//                infoList.removeIf(info -> !filteredSids.contains(info.sectionId));
//            }
//            if (searchClassTime != null) {
//                HashSet<Integer> filteredSids = new HashSet<>();
//                for (Info info : infoList) {
//                    if(info.classBegin <= searchClassTime &&
//                            info.classEnd >= searchClassTime){
//                        filteredSids.add(info.sectionId);
//                    }
//                }
//                infoList.removeIf(info -> !filteredSids.contains(info.sectionId));
//            }
//            if (searchClassLocations != null) {
//                HashSet<Integer> filteredSids = new HashSet<>();
//                for (Info info : infoList) {
//                    for (String eachLocation : searchClassLocations) {
//                        if(info.location.contains(eachLocation)){
//                            filteredSids.add(info.sectionId);
//                            break;
//                        }
//                    }
//                }
//                infoList.removeIf(info -> !filteredSids.contains(info.sectionId));
//            }
            //----------CourseType不筛选了，摆-------------
            //3.筛选ignorePassed & ignoreMissingPrerequisites
            infos=infoList.stream();//List→Stream
            if(ignorePassed || ignoreMissingPrerequisites){
                //3.1.0获取该学生所有pass的课的courseId: passedCids
                sql= """
                        select distinct course_id
                        from student_section
                             join section on section_id = id
                                         and student_id=?
                                         and mark>=60""";
                ArrayList<String> passedCids=Util.querySingle(con,sql,studentId);
                //3.1.1筛选ignorePassed
                if(ignorePassed){
                    infos=infos.filter(info -> !passedCids.contains(info.courseId));
                }
                //3.1.2筛选ignoreMissingPrerequisites
                if(ignoreMissingPrerequisites){
                    //Motivation: 为了避免infos中重复的courseId多次检验，优化效率
                    //3.1.2.1首先生成不重复的courseId的HashSet: cids
                    HashSet<String> cids = new HashSet<>();
                    infoList=infos.collect(Collectors.toList());
                    for (Info info : infoList) {
                        cids.add(info.courseId);
                    }
                    //3.1.2.2筛选满足先修课的cids，生成filCids
                    ArrayList<String> filCids = (ArrayList<String>) cids.stream().
                                                filter(cid -> passedPre(passedCids, cid)).
                                                collect(Collectors.toList());
                    //3.1.2.3再用filCids筛选infos
                    infoList.removeIf(info -> !filCids.contains(info.courseId));
                }
            }
            //4.生成所有CourseSearchEntry: entries
            ArrayList<CourseSearchEntry> entries = new ArrayList<>();
            //4.1聚合生成所有的sectionIds
            ArrayList<Integer> sectionIds = new ArrayList<>();
            for (Info info : infoList) {
                if(!sectionIds.contains(info.sectionId)){
                    sectionIds.add(info.sectionId);
                }
            }
            //4.3生成每个entry对象
            //4.3.0获取该学生本学期已选的课的信息: selectedInfos(被4.3.3使用)
            sql= """
                    select course_id "courseId",
                           c.name||'['||s.name||']' "fullName"
                    from student_section
                         join section s on s.id=section_id
                                       and semester_id=?
                                       and student_id=?
                         join course c on c.id=course_id""";
            ArrayList<SelectedInfo> selectedInfos = Util.query(SelectedInfo.class,con,sql,semesterId,studentId);
            for (Integer sectionId : sectionIds) {
                CourseSearchEntry entry = new CourseSearchEntry();
                entry.sectionClasses=new HashSet<>();
                //hasGenerate标记第一次找到该sectionId，因为course和section只需生成一次
                boolean hasGenerate=false;
                for (Info info : infoList) {
                    if(info.sectionId==sectionId){
                        if(!hasGenerate) {
                            //4.3.1准备course
                            sql = """
                                select id,name,credit,class_hour "classHour",is_pf grading
                                from course
                                where id=?""";
                            entry.course=Util.query(Course.class,con,sql,info.courseId).get(0);
                            //4.3.2准备section
                            sql="""
                                select id,name,total_capacity "totalCapacity",left_capacity "leftCapacity"
                                from section
                                where id=?""";
                            entry.section=Util.query(CourseSection.class,con,sql,sectionId).get(0);
                            //4.3.3准备conflictCourseNames
                            ArrayList<String> conflictCourseNames = new ArrayList<>();
                            //4.3.3.1课程冲突
                            for (SelectedInfo it : selectedInfos) {
                                if(it.courseId.equals(entry.course.id)){
                                    conflictCourseNames.add(it.fullName);
                                    break;
                                }
                            }
                            //4.3.3.2-------------时间冲突，摆---------------
                            entry.conflictCourseNames=conflictCourseNames;
                            hasGenerate=true;
                        }
                        //4.3.4准备sectionClasses
                        CourseSectionClass clazz = new CourseSectionClass();
                        Instructor instructor = new Instructor();
                        instructor.id=info.instructorId;
                        instructor.fullName=info.instructorFullName;
                        clazz.instructor=instructor;
                        clazz.weekList=info.weekList;
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
            //5.筛选ignoreConflict
            Stream<CourseSearchEntry> entryStream = entries.stream();
            if(ignoreConflict){
                entryStream=entryStream.filter(it -> it.conflictCourseNames.isEmpty());
            }
            //6.处理offset和size
            return entryStream.skip(pageIndex*(long)pageSize).limit(pageSize).collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        //2&3&5.1这三种情况要区分开
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()){
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
                    select day_of_week "dayOfWeek",
                           class_begin "classBegin",class_end "classEnd",
                           week_list "weekList"
                    from student_section
                         join section on section_id=section.id
                                      and student_id=?
                                      and semester_id=(
                                              select semester_id
                                              from section
                                              where id=?
                                          )
                         join section_class on section_class.section_id=section.id""";
            ArrayList<CourseSectionClass> classes =
                    Util.query(CourseSectionClass.class,con,sql6,studentId,sectionId);
            //5.2.2获取该sectionId的sectionClass: selectClasses
            String sql7= """
                    select day_of_week "dayOfWeek",
                           class_begin "classBegin",class_end "classEnd",
                           week_list "weekList"
                    from section_class
                    where section_id=?;""";
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
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {String sql="select mark from student_section where student_id=? and section_id=?";
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            ArrayList<Integer> res=Util.querySingle(con,sql,studentId,sectionId);
            if(res.isEmpty()){
                throw new EntityNotFoundException();
            }
            if(res.get(0)!=-1){
                throw new IllegalStateException();
            }
            sql="delete from student_section where student_id=? and section_id=?";
            Util.update(con,sql,studentId,sectionId);
            updateLeftCapacity(con,sectionId,false);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        //不要修改left_capacity
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()){
            int mark;
            String sql1= """
                    select distinct s.id,
                                    c.is_pf grading
                    from section s join public.course c on c.id = s.course_id
                    where s.id=?""";
            PreparedStatement ps1=con.prepareStatement(sql1);
            ps1.setInt(1,sectionId);
            ResultSet rs1=ps1.executeQuery();
            rs1.next();
            boolean isPf=rs1.getBoolean(2);
            if(isPf){
                if(grade==PassOrFailGrade.PASS){
                    mark=-2;
                }else if(grade==PassOrFailGrade.FAIL){
                    mark=-3;
                }else {
                    mark=-1; //空 或 给分成绩与course要求不符，即要求PF，但给的百分制，不计入
                }
            }else{
                if(grade==null){
                    mark=-1;
                }else{
                   mark= grade.when(new Grade.Cases<>() {
                       @Override
                       public Integer match(PassOrFailGrade self) {
                           return -1;
                       }//不匹配

                       @Override
                       public Integer match(HundredMarkGrade self) {
                           return (int) (self.mark);
                       }
                   });
                }
            }
            String sql2= """
                    select * from student_section
                    where section_id=? and student_id=?;""";
            PreparedStatement ps2=con.prepareStatement(sql2);
            ps2.setInt(1,sectionId);
            ps2.setInt(2,studentId);
//            ResultSet rs=ps2.executeQuery();
//            if(rs.wasNull()){
                String sql="insert into student_section(student_id, section_id, mark) values (?,?,?);";
                try{
                    Util.update(con,sql,studentId,sectionId,mark);
                }catch (SQLException e){
                    String sql3="update student_section set mark=?\n" +
                        "where student_section.student_id=? and student_section.section_id=?;";
                    Util.update(con,sql3,mark,studentId,sectionId);
                }
//            }else {
//                String sql3="update student_section set mark=?\n" +
//                        "where student_section.student_id=? and student_section.section_id=?;";
//                Util.update(con,sql3,mark,studentId,sectionId);
//            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    private void updateLeftCapacity(Connection con,int sectionId,boolean isAdd) throws SQLException{
        //isAdd为true: left_capacity--
        //isAdd为false: left_capacity++
        String addSql= """
                update section
                set left_capacity=(
                        select left_capacity
                        from section
                        where id=?
                    )-1
                where id=?""";
        String dropSql= """
                update section
                set left_capacity=(
                        select left_capacity
                        from section
                        where id=?
                    )+1
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
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            int mark;
            String sql1= """
                    select distinct s.id,c.is_pf
                     from section s join public.course c on c.id = s.course_id
                     where s.id=?;""";
            PreparedStatement ps1=con.prepareStatement(sql1);
            ps1.setInt(1,sectionId);
            ResultSet rs1=ps1.executeQuery();
            boolean is_pf=rs1.getBoolean(2);
            if(is_pf){
                if(grade==PassOrFailGrade.PASS){
                    mark=-2;
                }else if(grade==PassOrFailGrade.FAIL){
                    mark=-3;
                }else {
                    mark=-1; //空 或 给分成绩与course要求不符，即要求PF，但给的百分制，不计入
                }
            }else{
                mark= grade.when(new Grade.Cases<>() {
                    @Override
                    public Integer match(PassOrFailGrade self) {
                        return -1;
                    }//不匹配

                    @Override
                    public Integer match(HundredMarkGrade self) {
                        return (int) (self.mark);
                    }
                });
            }
            String sql="update student_section set mark=? where section_id=? and student_id=?;";
            PreparedStatement ps=con.prepareStatement(sql);
            ps.setInt(1,mark);
            ps.setInt(2,sectionId);
            ps.setInt(3,studentId);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        Map<Course,Grade> courseGradeMap=new HashMap<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql= """
                    select student_section.section_id,student_section.mark,course.id,course.name,course.credit,course.class_hour,course.is_pf from(
                           select section_id,student_id,course_id from
                           ((select section_id,mark,student_id from student_section where student_id=?) a
                           join section s on s.id=a.section_id) b
                           where b.semester_id=?) c
                           join student_section on c.student_id=student_section.student_id and c.section_id=student_section.student_id
                           join section s2 on s2.id = student_section.section_id
                           join course on course.id=s2.course_id
                           order by s2.semester_id;""";
            /* c选出了这个学期学过的课，all_section是筛选这个学期学过的课有没有重修过，order by semester_id 为了取最新成绩
                联立course 为了新建course对象
            */
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,studentId);
            ps.setInt(2,semesterId);//TODO: 处理空指针
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                Course course=new Course();
                course.id=rs.getString(3);
                course.name=rs.getString(4);
                course.credit=rs.getInt(5);
                boolean is_pf=rs.getBoolean(6);
                if(is_pf){
                    course.grading= Course.CourseGrading.PASS_OR_FAIL;
                }else {
                    course.grading= Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
                int mark=rs.getInt(2);
                Grade grade;
                if(mark==-1){
                    grade=null;
                }else if(mark==-2){
                    grade= PassOrFailGrade.PASS;
                }else if(mark==-3){
                    grade=PassOrFailGrade.FAIL;
                }else{
                    grade=new HundredMarkGrade((short) mark);
                }
                courseGradeMap.put(course,grade);
            }
            return courseGradeMap;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        //System.out.println(studentId + " " + date);
        CourseTable ct = new CourseTable();
        ct.table=new HashMap<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()){
            String psql = """
                    select *
                    from semester
                    where begin_time <= ? and end_time >= ?
                    """;
            PreparedStatement pps = con.prepareStatement(psql);
            pps.setDate(1,date);
            pps.setDate(2,date);
            ResultSet prs = pps.executeQuery();
            int sid = 0;
            int week = 0;
            while(prs.next()) {
                sid = prs.getInt(1);
                Date ks = prs.getDate(3);
                long days = (date.getTime() - ks.getTime())/(1000*3600*24);
                week = (int) (days / 7 + 1);
            }
            String sql = """
                    select c.name, s.name, sc.instructor_id, i.full_name, sc.class_begin, sc.class_end, location, sc.day_of_week, sc.week_list
                    from student
                             join student_section ss on student.id = ss.student_id
                             join section s on s.id = ss.section_id
                             join course c on c.id = s.course_id
                             join section_class sc on s.id = sc.section_id
                             join instructor i on i.id = sc.instructor_id
                    where student.id = ? and s.semester_id = ?""";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,studentId);
            ps.setInt(2, sid);
            ResultSet rs = ps.executeQuery();
            // 这里分开存了一周内，周一到周日的课程
            ArrayList<Set<CourseTable.CourseTableEntry>> ctSet = new ArrayList<>();
            for(int i=0;i<7;i++){
                //12.12修改，原本用的哈希set，取出来的顺序和放进去的顺序不一样，这里改用LinkedHashset(似乎不需要顺序，遂改回)
                Set<CourseTable.CourseTableEntry> set = new HashSet<>();
                ctSet.add(set);
            }
            while(rs.next()) {
                String wl = rs.getString(9);
                String wk = String.valueOf(week);
                if(!wl.contains(wk)) continue;
                String courseName = rs.getString(1);
                String sectionName = rs.getString(2);
                int instructorId = rs.getInt(3);
                String IFullName = rs.getString(4);
                Instructor ins = new Instructor();
                ins.id = instructorId;
                ins.fullName = IFullName;
                short begin = rs.getShort(5);
                short end = rs.getShort(6);
                String location = rs.getString(7);
                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                entry.courseFullName = String.format("%s[%s]", courseName, sectionName);
                entry.classBegin = begin;
                entry.classEnd = end;
                entry.instructor = ins;
                entry.location = location;
                int weekday = rs.getInt(8);
                ctSet.get(weekday - 1).add(entry);
            }
            for(int i=0;i<7;i++){
                DayOfWeek dow = DayOfWeek.of(i+1);
                ct.table.put(dow, ctSet.get(i));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return ct;
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
            ArrayList<String> passedCids = Util.querySingle(con, sql1, studentId);
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
        try (Connection con=SQLDataSource.getInstance().getSQLConnection()){
            //1.获取布尔表达式
            //1.1获取先修课String: pre
            String sql="select prerequisite from course where id=?";
            String pre = (String)Util.querySingle(con, sql, courseId).get(0);
            if(pre==null){return true;}
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
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()){
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
