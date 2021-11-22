package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyDepartmentService implements DepartmentService {

    @Override
    public int addDepartment(String name) {
        try(Connection con= SQLDataSource.getInstance().getSQLConnection()) {
            String sql="insert into department (name) values (?)";
            PreparedStatement ps=con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1,name);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.getInt(2);//自增生成的key被放置于第2列
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeDepartment(int departmentId) {

    }

    @Override
    public List<Department> getAllDepartments() {
        return null;
    }

    @Override
    public Department getDepartment(int departmentId) {
        return null;
    }
}
