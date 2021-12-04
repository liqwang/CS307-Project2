package cn.edu.sustech.cs307.dto;

public class Major {
    public int id;
    public String name;
    public Department department;

    public Major(int id, String name, Department department){
        this.id = id;
        this.name = name;
        this.department = department;
    }
}
