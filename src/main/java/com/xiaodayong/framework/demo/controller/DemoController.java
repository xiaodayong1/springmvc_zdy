package com.xiaodayong.framework.demo.controller;

import com.xiaodayong.framework.annotation.myAutowired;
import com.xiaodayong.framework.annotation.myRequestMapping;
import com.xiaodayong.framework.annotation.mycontroller;
import com.xiaodayong.framework.demo.service.IDemoService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@mycontroller
@myRequestMapping("/demo")
public class DemoController {


    @myAutowired
    private IDemoService demoService;


    /**
     * URL: /demo/query?name=lisi
     * @param request
     * @param response
     * @param name
     * @return
     */
    @myRequestMapping("/query")
    public String query(HttpServletRequest request, HttpServletResponse response,String name) {
        return demoService.get(name);
    }
}
