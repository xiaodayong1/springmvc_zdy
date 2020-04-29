package com.xiaodayong.framework.springmvc;
import java.lang.reflect.InvocationTargetException;
import	java.lang.reflect.Parameter;
import	java.util.logging.Handler;
import	java.lang.reflect.Method;
import	java.lang.reflect.Field;

import com.xiaodayong.framework.annotation.myAutowired;
import com.xiaodayong.framework.annotation.myRequestMapping;
import com.xiaodayong.framework.annotation.mycontroller;
import com.xiaodayong.framework.annotation.myservice;
import com.xiaodayong.framework.demo.service.IDemoService;
import com.xiaodayong.framework.pojo.Handle;
import org.apache.commons.lang3.StringUtils;

import	java.io.File;
import java.io.InputStream;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {

    //接收properties文件
    private Properties properties=new Properties();

    //缓存扫描到的类的全限定类名
    private List<String> classNames=new ArrayList<>();

    //ioc容器
    private Map<String,Object> ioc=new HashMap<>();

    //handleMapping 存放handle和url的映射关系
    //private Map<String,Method> handleMapping=new HashMap<> ();

    //封装handle对象存放方法参数与方法对象
    List<Handle> handleMapping=new ArrayList<> ();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);
        //2.扫描配置文件相关类与注解
        doscan(properties.getProperty("scanpackage"));
        //3.定义bean对象（基于注解版ioc容器）
        try {
            doInstence();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        //4.加入对象相关依赖
        doAutoWired();
        //5.构造handleMapping映射处理器，实现url与handle之间的映射
        initHandleMapping();
        System.out.println("自定义的mvc初始化完成 666 666 666");
    }

    //构造映射处理器，实现关系映射
    private void initHandleMapping() {
        if(ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> aClass = entry.getValue().getClass();
            if(!aClass.isAnnotationPresent(mycontroller.class)){
                continue;
            }
            myRequestMapping annotation = aClass.getAnnotation(myRequestMapping.class);
            String baseUrl = annotation.value();
            Method[] methods = aClass.getMethods();
            for (Method method : methods){
                if (!method.isAnnotationPresent(myRequestMapping.class)){
                    continue;
                }
                myRequestMapping annotation1 = method.getAnnotation(myRequestMapping.class);
                String methodUrl = annotation1.value();
                String Url=baseUrl + methodUrl;

                Handle handle = new Handle(entry.getValue(), method, Pattern.compile(Url));
                //获取方法参数列表
                Parameter[] parameters = method.getParameters();
                for (int i=0;i<parameters.length; i++){
                    Parameter parameter = parameters [i];
                    //如果参数是这两个的话就获取
                    if(parameter.getType()==HttpServletRequest.class || parameter.getType() == HttpServletResponse.class){
                        handle.getParamIndexMapping().put(parameter.getType().getSimpleName(),i);
                    }else {
                        handle.getParamIndexMapping().put(parameter.getName(),i);
                    }
                }

                handleMapping.add(handle);
            }
        }

    }

    private void doAutoWired() {

        for(Map.Entry<String,Object> entry: ioc.entrySet()) {

            Field[] declaredFields =
                    entry.getValue().getClass().getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field declaredField = declaredFields[i]; //
                if(!declaredField.isAnnotationPresent(myAutowired.class)) {
                    continue;
                }
                myAutowired annotation = declaredField.getAnnotation(myAutowired.class);
                String beanName = annotation.value(); // 需要注⼊的bean的id
                if("".equals(beanName.trim())) {
                            beanName = declaredField.getType().getName();
                }
                declaredField.setAccessible(true);
                try {
                    declaredField.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //初始化bean对象
    private void doInstence() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        if (classNames.size()==0){
            return;
        }
        for(int i=0;i<classNames.size();i++){
            String className = classNames.get(i);
            Class<?> aClass = Class.forName(className);
            //如果是controller的话,将首字母转成小写当成id存入容器
            if(aClass.isAnnotationPresent(mycontroller.class)){
                String simpleName = aClass.getSimpleName();
                String lowSimpleName = lowClassName(simpleName);
                Object o = aClass.newInstance();
                ioc.put(lowSimpleName,o);
            }else if (aClass.isAnnotationPresent(myservice.class)){
                //获取注解value值
                myservice annotation = aClass.getAnnotation(myservice.class);
                String value = annotation.value();
                if(!"".equals(value.trim())){
                    //指定id后放入ioc
                    ioc.put(value,aClass.newInstance());
                }else {
                    String s = lowClassName(aClass.getSimpleName());
                    ioc.put(s,aClass.newInstance());
                }
                //service一般是接口，所以需要将接口也放进ioc容器进行存储方便取出
                Class<?>[] interfaces = aClass.getInterfaces();
                for (int j=0;j<interfaces.length; j++){
                    //将接口的全限定类名放进容器
                    ioc.put(interfaces[j].getName(),aClass.newInstance());
                }
            }else {
                continue;
            }
        }
    }

    //类名首字母改成小写
    public String lowClassName(String str){
        char[] chars = str.toCharArray();
        if (chars[0]>='A'&&chars [0]<='Z'){
            chars [0]+=32;
        }
        return String.valueOf(chars);
    }

    //扫描配置文件类与注解
    private void doscan(String packagePath) {
        //获取扫描的包路径
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath()+packagePath.replaceAll("\\.", "/");
        File file = new File(path);
        //返回文件目录下的所有文件和目录的绝对路径
        File[] files = file.listFiles();
        for(File filepath: files){
            //如果是文件夹的话，再次进行递归遍历
            if (filepath.isDirectory()){
                doscan(packagePath+"."+filepath.getName());
            }else if (filepath.getName().endsWith(".class")){
                String classpath=packagePath + "." + filepath.getName().replaceAll(".class", "");
                classNames.add(classpath);
            }
        }
    }

    //加载配置文件
    private void doLoadConfig(String propertiesPath) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(propertiesPath);
        System.out.println(this.getClass().getName() );
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //执行处理
      /*  String requestURI = req.getRequestURI();
        Method method = handleMapping.get(requestURI);
        method.invoke();*/
      Handle handle=getHandle(req);
      if (handle == null){
          resp.getWriter().write("404啦 臭弟弟");
          return;
      }
      //进行参数绑定
        //获取参数类型数组，长度就是以后的参数长度
      Class<?>[] parameterTypes = handle.getMethod().getParameterTypes();
      //创建参数数组进行参数赋值
        Object[] paramValues=new Object [parameterTypes.length];

        //获取请求参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        // 遍历request中所有参数 （填充除了request，response之外的参数）
        for (Map.Entry < String, String [] > param:parameterMap.entrySet()){
            // name=1&name=2 name [1,2]
            String value = StringUtils.join(param.getValue(), ",");//1,2
            //如果参数与方法中的参数匹配进行赋值
            if(!handle.getParamIndexMapping().containsKey(param.getKey())){
                continue;
            }
            Integer integer = handle.getParamIndexMapping().get(param.getKey());
           // 把前台传递过来的参数值填充到对应的位置去
            paramValues [integer] =value;

        }
        Integer requestIndex = handle.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        paramValues [requestIndex] = req;

        Integer responseIndex = handle.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paramValues [responseIndex]=resp;

        try {
            handle.getMethod().invoke(handle.getController(),paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Handle getHandle(HttpServletRequest req) {
        String uri=req.getRequestURI();
        for (Handle handle:handleMapping){
            Matcher matcher = handle.getPattern().matcher(uri);
            if(!matcher.matches()){
               continue;
            }
            return handle;
        }
        return null;
    }


}
