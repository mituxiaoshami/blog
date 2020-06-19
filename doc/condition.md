## Java并发之Condition

### Condition接口介绍和示例

首先我们需要明白condition对象是依赖于lock对象的，意思就是说condition对象需要通过lock对象进行创建出来(调用Lock对象的newCondition()方法)。condition的使用方式非常的简单。但是需要注意在调用方法前获取锁。

```
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


运行结果：

pool-1-thread-1拿到锁了
pool-1-thread-1等待信号
pool-1-thread-2拿到锁了
pool-1-thread-2发出信号
pool-1-thread-1拿到信号
```

由案例可知：一般都会将Condition对象作为成员变量。当调用await()方法后，当前线程会释放锁并在此等待，而其他线程调用Condition对象的signal()方法，通知当前线程后，当前线程才从await()方法返回，并且在返回前已经获取了锁。

### Condition接口常用方法

condition可以通俗的理解为条件队列。当一个线程在调用了await方法以后，直到线程等待的某个条件为真的时候才会被唤醒。这种方式为线程提供了更加简单的等待/通知模式。Condition必须要配合锁一起使用，因为对共享状态变量的访问发生在多线程环境下。一个Condition的实例必须与一个Lock绑定，因此Condition一般都是作为Lock的内部实现。

1. await() ：造成当前线程在接到信号或被中断之前一直处于等待状态。
2. await(long time, TimeUnit unit) ：造成当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态。
3. awaitNanos(long nanosTimeout) ：造成当前线程在接到信号、被中断或到达指定等待时间之前一直处于等待状态。返回值表示剩余时间，如果在nanosTimesout之前唤醒，那么返回值 = nanosTimeout - 消耗时间，如果返回值 <= 0 ,则可以认定它已经超时了。
4. awaitUninterruptibly() ：造成当前线程在接到信号之前一直处于等待状态。【注意：该方法对中断不敏感】。
5. awaitUntil(Date deadline) ：造成当前线程在接到信号、被中断或到达指定最后期限之前一直处于等待状态。如果没有到指定时间就被通知，则返回true，否则表示到了指定时间，返回false。
6. signal() ：唤醒一个等待线程。该线程从等待方法返回前必须获得与Condition相关的锁。
7. signalAll() ：唤醒所有等待线程。能够从等待方法返回的线程必须获得与Condition相关的锁。

### Condition接口原理简单解析

Condition是AQS的内部类。每个Condition对象都包含一个队列(等待队列)。等待队列是一个FIFO的队列，在队列中的每个节点都包含了一个线程引用，该线程就是在Condition对象上等待的线程，如果一个线程调用了Condition.await()方法，那么该线程将会释放锁、构造成节点加入等待队列并进入等待状态。

