package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.DepartmentService;
import implement.Util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyDepartmentService implements DepartmentService {
    Connection con;
    {
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addDepartment(String name) {
        String sql="insert into department (name) values (?)";
        return Util.addAndGetKey(con,sql,name);
    }

    @Override
    public void removeDepartment(int departmentId) {
        String sql="delete from department where id=?";
        try {
            Util.update(con,sql,departmentId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
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
            throw new EntityNotFoundException();
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
