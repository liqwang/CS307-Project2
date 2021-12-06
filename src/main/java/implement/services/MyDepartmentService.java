package implement.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.DepartmentService;
import implement.Util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
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
        if(Util.update(con,sql,departmentId)==0){
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        String sql="select * from department";
        return Util.query(Department.class,con,sql);
    }

    @Override
    public Department getDepartment(int departmentId) {
        String sql="select * from department where id=?";
        ArrayList<Department> res=Util.query(Department.class,con,sql,departmentId);
        if(res.isEmpty()){
            throw new EntityNotFoundException();
        }else {return res.get(0);}
    }
}
