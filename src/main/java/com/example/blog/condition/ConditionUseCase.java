package com.example.blog.condition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author 小沙弥
 * @Description
 **/
public class ConditionUseCase {

    /**
     * 显示的创建锁
     **/
    private Lock      lock      = new ReentrantLock();

    /**
     * 通过lock对象进行创建
     **/
    private Condition condition = lock.newCondition();

    public static void main(String[] args)  {

        ConditionUseCase useCase = new ConditionUseCase();

        ExecutorService executorService = Executors.newFixedThreadPool (2);

        executorService.execute(useCase::conditionWait);

        executorService.execute(useCase::conditionSignal);
    }

    private void conditionWait()  {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "拿到锁了");
            System.out.println(Thread.currentThread().getName() + "等待信号");
            // condition在调用方法前获取锁
            condition.await();
            System.out.println(Thread.currentThread().getName() + "拿到信号");
        } catch (Exception e){
            System.out.println("出错啦。。。。。");
        } finally {
            lock.unlock();
        }
    }

    private void conditionSignal() {
        lock.lock();
        try {
            Thread.sleep(5000);
            System.out.println(Thread.currentThread().getName() + "拿到锁了");
            // condition在调用方法前获取锁
            condition.signal();
            System.out.println(Thread.currentThread().getName() + "发出信号");
        } catch (Exception e){
            System.out.println("出错啦。。。。。");
        } finally {
            lock.unlock();
        }
    }

}
