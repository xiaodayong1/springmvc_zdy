package com.xiaodayong.framework.demo.service.impl;

import com.xiaodayong.framework.annotation.myservice;
import com.xiaodayong.framework.demo.service.IDemoService;

@myservice("demoService")
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        System.out.println("service 实现类中的name参数：" + name) ;
        return name;
    }
}
