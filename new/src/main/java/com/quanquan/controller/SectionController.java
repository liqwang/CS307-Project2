package com.quanquan.controller;

import com.quanquan.dto.CourseSection;
import com.quanquan.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class SectionController {

    @Autowired
    CourseService courseService;

    @RequestMapping("/sections")
    public String sectionList(Model model){
        List<CourseSection> sections = courseService.getAllSections();
        model.addAttribute("sections",sections);
        return "list";
    }
}
