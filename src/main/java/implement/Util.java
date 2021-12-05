package implement;

import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Util {
    /**
     * 通用的增、删、改操作
     */
    public static void update(Connection con,String sql,Object... param) throws SQLException{
        PreparedStatement ps=null;
        try {
            ps = con.prepareStatement(sql);
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i + 1, param[i]);
            }
        } catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        ps.executeUpdate();
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
     * 通用的查询操作，注意：参数顺序要按照构造器的参数顺序
     */
    public static <T> List<T> query(Class<T> clazz,Connection con,String sql,Object... param){
        try(PreparedStatement ps=con.prepareStatement(sql)){
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i+1,param[i]);
            }
            ResultSet rs;
            try {
                rs = ps.executeQuery();
            }catch (SQLException e){
                e.printStackTrace();
                throw new EntityNotFoundException();
            }
            int col = rs.getMetaData().getColumnCount();
            Constructor<T> constr = (Constructor<T>)clazz.getDeclaredConstructors()[0];
            List<T> list = new ArrayList<>();
            while(rs.next()){
                ArrayList<Object> consParams = new ArrayList<>();
                for (int i = 0; i < col; i++) {
                    consParams.add(rs.getObject(i+1));
                }
                list.add(constr.newInstance(consParams));
            }
            rs.close();
            return list;
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException|SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
