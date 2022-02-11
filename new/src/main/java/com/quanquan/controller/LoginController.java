package com.quanquan.controller;

import com.quanquan.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {

    @Autowired
    private StudentService studentService;

    @RequestMapping("/user/login")
    public String login(int sid, String password,Model model){
        if(studentService.getPasswordById(sid).equals(password)){
            return "redirect:/main";
        }else{
            model.addAttribute("msg","Wrong Student Id or Password");
            return "index";
        }
    }
}
