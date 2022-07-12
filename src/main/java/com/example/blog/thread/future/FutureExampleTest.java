package com.example.blog.thread.future;

import java.util.concurrent.*;

/**
 * @author 小沙弥
 * @description Future 的演示案例
 * @date 2021/9/8 7:37 下午
 */
public class FutureExampleTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        // 创建线程池，这里只是demo演示
        ExecutorService executorService = Executors.newCachedThreadPool();
        // 直接提交Callable实现类的方式
        Future<String> future = executorService.submit(new FutureCallableExample());
        // 获取结果 设置超时时间
        System.out.println("future:" + future.get(10000, TimeUnit.MILLISECONDS));
        // 提交futureTask的方式
        FutureTask<String> futureTask = new FutureTask<>(new FutureCallableExample());
        executorService.execute(futureTask);
        // 获取结果 设置超时时间
        System.out.println("futureTask:" + futureTask.get(10000, TimeUnit.MILLISECONDS));
    }

}
