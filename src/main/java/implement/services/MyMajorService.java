package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyMajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) { //一如往常
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into major (name, department_id) values (?, ?)";
            PreparedStatement ps=con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,name);
            ps.setInt(2,departmentId);
            ps.executeUpdate();
            return ps.getGeneratedKeys().getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeMajor(int majorId) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            //在删除本专业之前，要先鲨掉所有本专业的学生~
            String sql = "select student.id\n" +
                    "from student left join major m on m.id = student.major_id\n" +
                    "where major_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, majorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int s_id = rs.getInt(1);
                // 想到一种删除学生的思路，就是所有操作在一个sql语句里完成，不知道会不会快一点，但是这里我还是写常规操作吧
                String sql_d_s = "delete\n" +
                        "from student\n" +
                        "where id = ?;";
                PreparedStatement ps_d_s = con.prepareStatement(sql_d_s);
                ps_d_s.setInt(1, s_id);
                ps_d_s.executeUpdate();
                ps_d_s.close();
            }
            ps.executeUpdate();
            ps.close();
        }catch(SQLException throwables){
            throwables.printStackTrace();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        return null;
    }

    @Override
    public Major getMajor(int majorId) {
        return null;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
