package cn.edu.sustech.cs307.benchmark;

import cn.edu.sustech.cs307.config.Config;
import cn.edu.sustech.cs307.factory.ServiceFactory;

/**
 * 配置类，无需改动
 */
public abstract class BasicBenchmark {
    protected ServiceFactory serviceFactory = Config.getServiceFactory();
}