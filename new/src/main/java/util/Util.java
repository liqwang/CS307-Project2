package util;

import com.quanquan.dto.*;
import com.quanquan.exception.IntegrityViolationException;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Util {
    /**
     * 通用的增、删、改操作，返回受影响的行数
     */
    public static int update(Connection con,String sql,Object... param) throws SQLException{
        PreparedStatement ps;
        try {
            ps = con.prepareStatement(sql);
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i + 1, param[i]);
            }
        } catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
        return ps.executeUpdate();
    }

    /**
     * 通用的add操作，返回自动生成的主键
     */
    public static int addAndGetKey(Connection con,String sql,Object... param){
        try (PreparedStatement ps=con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS)){
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i+1,param[i]);
            }
            try {
                ps.executeUpdate();
            }catch (SQLException e){
                e.printStackTrace();
                throw new IntegrityViolationException();
            }
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return ps.getGeneratedKeys().getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }

    /**
     * 通用的查询具体类的方法，**别名必须与属性名一致**
     */
    public static <T> ArrayList<T> query(Class<T> clazz,Connection con,String sql,Object... param){
        try(PreparedStatement ps=con.prepareStatement(sql)){
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i+1,param[i]);
            }
            ResultSet rs=ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int col = rsmd.getColumnCount();
            ArrayList<T> list = new ArrayList<>();
            while(rs.next()){
                T t = clazz.newInstance();
                for (int i = 0; i < col; i++) {
                    Object val;
                    String otherName = rsmd.getColumnLabel(i+1);
                    switch (otherName) {
                        case "weekList" -> val = new HashSet<>(List.of((Short[]) rs.getArray(i + 1).getArray()));
                        case "dayOfWeek" -> val = DayOfWeek.of(rs.getInt(i + 1));
                        case "grading" -> val = (rs.getBoolean(i + 1) ?
                                Course.CourseGrading.PASS_OR_FAIL :
                                Course.CourseGrading.HUNDRED_MARK_SCORE);
                        case "classBegin", "classEnd" -> val = rs.getShort(i + 1);
                        case "instructor" -> {
                            sql = """
                                    select id,
                                           full_name "fullName"
                                    from instructor
                                    where id=?""";
                            val = query(Instructor.class, con, sql, rs.getInt(i + 1)).get(0);
                        }
                        case "student" -> {
                            sql = """
                                    select id,
                                           full_name "fullName",
                                           enrolled_date "enrolledDate",
                                           major_id "majorId"
                                    from student
                                    where id=?;""";
                            val = query(Student.class, con, sql, rs.getInt(i + 1)).get(0);
                        }
                        case "major" -> {
                            sql = """
                                    select id,
                                           name,
                                           department_id "departmentId"
                                    from major
                                    where id=?;""";
                            val = query(Major.class, con, sql, rs.getInt(i + 1)).get(0);
                        }
                        case "department" -> {
                            sql = """
                                    select *
                                    from department
                                    where id=?;""";
                            val = query(Department.class, con, sql, rs.getInt(i + 1)).get(0);
                        }
                        default -> val = rs.getObject(i + 1);
                    }
                    String fieldName = rsmd.getColumnLabel(i+1);
                    Field field = clazz.getField(fieldName);
                    field.set(t,val);
                }
                list.add(t);
            }
            return list;
        } catch (NoSuchFieldException|InstantiationException|IllegalAccessException|SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * 通用的查询单一属性的方法,如String, int, DayOfWeek
     * <p>不需要修改列的别名
     */
    public static <T> ArrayList<T> querySingle(Connection con,String sql,Object... param){
        try(PreparedStatement ps=con.prepareStatement(sql)){
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i+1,param[i]);
            }
            ResultSet rs=ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            ArrayList<T> list = new ArrayList<>();
            while(rs.next()) {
                Object val;
                if (rsmd.getColumnName(1).equals("day_of_week")) {
                    val = DayOfWeek.of(rs.getInt(1));
                } else {
                    val = rs.getObject(1);
                }
                list.add((T)val);
            }
            return list;
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
