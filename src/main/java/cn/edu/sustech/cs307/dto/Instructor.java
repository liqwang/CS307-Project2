package cn.edu.sustech.cs307.dto;

public class Instructor extends User {
    public Department department;
    public Instructor(int id,String fullName){
        this.id=id;
        this.fullName=fullName;
    }
}
