package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MySemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql="insert into semester(name, begin_time, end_time) values (?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setDate(2, begin);
                ps.setDate(3, end);
            ps.executeUpdate();
            return ps.getGeneratedKeys().getInt(1);
        }catch(SQLException throwables){
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeSemester(int semesterId) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            // 先删除相关选课记录，再删除学期
            String sql1 = "delete from section where semester_id = ?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
                ps1.setInt(1, semesterId);
            ps1.executeUpdate();
            String sql2 ="delete from department where id = ?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
                ps2.setInt(1, semesterId);
            ps2.executeUpdate();
        }catch(SQLException throwables){
            throwables.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        ArrayList<Semester> semesters = new ArrayList<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="select * from semester";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id = rs.getInt(1);
                String name = rs.getString(2);
                Date begin = rs.getDate(3);
                Date end = rs.getDate(4);
                semesters.add(new Semester(id, name, begin, end));
            }
            ps.close();
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        return semesters;
    }

    @Override
    public Semester getSemester(int semesterId) { //可能有bug（关于丢出错误）
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="select * from semester where id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,semesterId);
            ResultSet rs = ps.executeQuery();
            int id = rs.getInt(1);
            String name = rs.getString(2);
            Date begin = rs.getDate(3);
            Date end = rs.getDate(4);
            Semester semester = new Semester(id, name, begin, end);
            ps.close();
            return semester;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
