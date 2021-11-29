package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
            return ps.getGeneratedKeys().getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeDepartment(int departmentId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="delete from department where id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,departmentId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        ArrayList<Department> departments = new ArrayList<>();
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="select * from department";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id = rs.getInt(1);
                String name = rs.getString(2);
                departments.add(new Department(id, name));
            }
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return departments;
    }

    @Override
    public Department getDepartment(int departmentId) {
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()) {
            String sql="select * from department where id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,departmentId);
            ResultSet rs = ps.executeQuery();
            int id=rs.getInt(1);
            String name=rs.getString(2);
            ps.close();
            return new Department(id,name);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
