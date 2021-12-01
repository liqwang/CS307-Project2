package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@ParametersAreNonnullByDefault
public class MySemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql="insert into semester(name, begin_time, end_time) values (?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
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

    }

    @Override
    public List<Semester> getAllSemesters() {
        return null;
    }

    @Override
    public Semester getSemester(int semesterId) {
        return null;
    }
}
