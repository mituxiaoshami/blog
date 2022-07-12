package com.example.blog.thread.completableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author 小沙弥
 * @description CompletableFuture 测试类
 * @date 2021/9/9 5:41 下午
 */
public class CompletableFutureExampleTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 创建异步执行任务
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread() + " start job1, time->"+System.currentTimeMillis());
            return "cf";
        });

        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread() + " start job2, time->"+System.currentTimeMillis());
            return "cf2";
        });

        //allof 等待所有任务执行完成才执行cf3，如果有一个任务异常终止，则cf3.get时会抛出异常，都是正常执行，cf3.get返回null
        CompletableFuture<Void> cf3 = CompletableFuture.allOf(cf, cf2);
        //等待子任务执行完成
        System.out.println("cf3 run result->"+cf3.get());

        //anyOf 是只有一个任务执行完成，无论是正常执行或者执行异常，都会执行cf3，cf3.get的结果就是已执行完成的任务的执行结果
        CompletableFuture<Object> cf4 = CompletableFuture.anyOf(cf, cf2);
        //等待子任务执行完成
        System.out.println("cf4 run result->"+cf4.get());
    }
}
