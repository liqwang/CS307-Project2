package implement;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import implement.services.*;

import java.util.List;

public class MyFactory extends ServiceFactory {
    public MyFactory(){
        super();
        registerService(CourseService.class,new MyCourseService());
        registerService(DepartmentService.class,new MyDepartmentService());
        registerService(InstructorService.class,new MyInstructorService());
        registerService(MajorService.class,new MyMajorService());
        registerService(SemesterService.class,new MySemesterService());
        registerService(StudentService.class,new MyStudentService());
        registerService(UserService.class,new MyUserService());
    }
    //TODO: 效率优化：1.Bitmap算法?
    //TODO: 效率优化：2.批量插入?
    //TODO: 效率优化：3.多线程
    //TODO: 系统化排查bug+优化
    //TODO: 并行流，缺点：顺序会被打乱
    @Override
    public List<String> getUIDs() {
        return List.of("12011619","12011941","12012403");
    }
}
