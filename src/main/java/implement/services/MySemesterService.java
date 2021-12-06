package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;
import implement.Util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MySemesterService implements SemesterService {
    Connection con;
    {
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addSemester(String name, Date begin, Date end) {
        String sql="insert into semester(name, begin_time, end_time) values (?,?,?)";
        return Util.addAndGetKey(con, sql, name, begin, end);
    }

    @Override
    public void removeSemester(int semesterId) {
        try{
            // 先删除相关选课记录，再删除学期
            String sql1 = "delete from section where semester_id = ?";
            Util.update(con, sql1, semesterId);
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
        try{
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
