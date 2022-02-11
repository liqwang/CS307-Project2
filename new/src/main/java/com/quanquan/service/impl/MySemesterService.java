package com.quanquan.service.impl;

import com.quanquan.dto.Semester;
import com.quanquan.exception.EntityNotFoundException;
import com.quanquan.service.SemesterService;
import com.quanquan.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Service
public class MySemesterService implements SemesterService {

    @Autowired
    DataSource dataSource;

    @Override
    public int addSemester(String name, Date begin, Date end) {
        try (Connection con=dataSource.getConnection()){
            String sql="insert into mybatis.semester(name, begin_time, end_time) values (?,?,?)";
            return Util.addAndGetKey(con, sql, name, begin, end);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
    }

    @Override
    public void removeSemester(int semesterId) {
        try(Connection con=dataSource.getConnection()){
            // 先删除相关选课记录，再删除学期
            String sql1 = "delete from mybatis.section where semester_id = ?";
            Util.update(con, sql1, semesterId);
            String sql2 ="delete from mybatis.department where id = ?";
            PreparedStatement ps2 = con.prepareStatement(sql2);
                ps2.setInt(1, semesterId);
            ps2.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        ArrayList<Semester> semesters = new ArrayList<>();
        try(Connection con=dataSource.getConnection()){
            String sql="select * from mybatis.semester";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                int id = rs.getInt(1);
                String name = rs.getString(2);
                Date begin = rs.getDate(3);
                Date end = rs.getDate(4);
                Semester sem = new Semester();
                sem.id=id;
                sem.name=name;
                sem.begin=begin;
                sem.end=end;
                semesters.add(sem);
            }
            return semesters;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Override
    public Semester getSemester(int semesterId){
        try(Connection con=dataSource.getConnection()) {
            String sql="select * from mybatis.semester where id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,semesterId);
            ResultSet rs = ps.executeQuery();
            int id = rs.getInt(1);
            String name = rs.getString(2);
            Date begin = rs.getDate(3);
            Date end = rs.getDate(4);
            Semester sem = new Semester();
            sem.id=id;
            sem.name=name;
            sem.begin=begin;
            sem.end=end;
            ps.close();
            return sem;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
