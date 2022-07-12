package com.example.blog.thread.future;

import java.util.concurrent.Callable;

/**
 * @author 小沙弥
 * @description Callable测试类
 * @date 2021/9/8 7:41 下午
 */
public class FutureCallableExample implements Callable<String> {

    @Override
    public String call() throws Exception {
        System.out.println("============== 实现callable接口 ==============");
        return "callable";
    }

}
