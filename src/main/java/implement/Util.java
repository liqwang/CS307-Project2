package implement;

import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
            return ps.getGeneratedKeys().getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }
}
