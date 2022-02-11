package com.quanquan.dto;

public abstract class User {

    public Integer id;
    /**
     * A user's full name is: first_name || ' ' || last_name, if both first name and last name are alphabetical (English alphabets) or space (' '), otherwise first_name || last_name.
     *
     * For example, if a user has firstName David and last name Lee then the full name is David Lee; if another user has firstName 张 and last name 三, the full name is 张三;
     * if first name 'David Lee' and last name 'Roth' then full name is 'David Lee Roth'.
     */
    public String fullName;
}
