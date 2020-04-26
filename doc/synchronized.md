## 深入了解Synchronized同步锁的优化方法

在并发编程中，多个线程访问同一个共享资源时，我们必须考虑如何维护数据的原子性。

在JDK1.5之前，Java是依靠 Synchronized 关键字实现锁功能来做到这点的。Synchronized 是 JVM 实现的一种内置锁，锁的获取和释放是由 JVM 隐式实现。

### Synchronized同步锁被称作重量级锁的原因

Synchronized 是基于底层操作系统的 Mutex Lock 实现的，每次获取和释放锁操作都会带来用户态和内核态的切换，从而增加系统性能开销。因此，在锁竞争激烈的情况下，Synchronized 同步锁在性能上就表现得非常糟糕，因此它也常被大家称为重量级锁。

特别是在单个线程重复申请锁的情况下，JDK1.5 版本的 Synchronized 锁性能要比 Lock 的性能差很多。

### Synchronized 同步锁使用

一、对象锁

包括方法锁（默认锁对象为this,当前实例对象）和同步代码块锁（自己指定锁对象）

1.代码块形式：手动指定锁定对象，可以是this，也可以是自定义的锁

```
public class SynchronizedObjectLock implements Runnable {

    static SynchronizedObjectLock instence = new SynchronizedObjectLock();

    @Override
    public void run() {
    
        // 同步代码块形式——锁为this,锁住的是当前SynchronizedObjectLock对象
        // 而只有一个SynchronizedObjectLock对象，2个线程是通过SynchronizedObjectLock转换为Thread创建出来的
        // 所以两个线程使用的锁是一样的,线程1必须要等到线程0释放了该锁后，才能执行
        synchronized (this) {
            System.out.println("我是线程" + Thread.currentThread().getName());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "结束");
        }
        
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(instence);
        Thread t2 = new Thread(instence);
        t1.start();
        t2.start();
    }
}

输出结果：

　　我是线程Thread-0
　　Thread-0结束
　　我是线程Thread-1
　　Thread-1结束

```
2.方法锁形式：synchronized修饰普通方法，锁对象默认为this

```
public class SynchronizedObjectLock implements Runnable {
    static SynchronizedObjectLock instence = new SynchronizedObjectLock();

    @Override
    public void run() {
        method();
    }

    public synchronized void method() {
        System.out.println("我是线程" + Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + "结束");
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(instence);
        Thread t2 = new Thread(instence);
        t1.start();
        t2.start();
    }
}

输出结果：

　　我是线程Thread-0
　　Thread-0结束
　　我是线程Thread-1
　　Thread-1结束

```

二、类锁

指synchronize修饰静态的方法或指定锁对象为Class对象

1.synchronize修饰静态方法

```
public class SynchronizedObjectLock implements Runnable {
    static SynchronizedObjectLock instence1 = new SynchronizedObjectLock();
    static SynchronizedObjectLock instence2 = new SynchronizedObjectLock();

    @Override
    public void run() {
        method();
    }

    // synchronized用在静态方法上，默认的锁就是当前所在的Class类，所以无论是哪个线程访问它，需要的锁都只有一把
    public static synchronized void method() {
        System.out.println("我是线程" + Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + "结束");
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(instence1);
        Thread t2 = new Thread(instence2);
        t1.start();
        t2.start();
    }
}

输出结果：

　　我是线程Thread-0
　　Thread-0结束
　　我是线程Thread-1
　　Thread-1结束

```

2.synchronized指定锁对象为Class对象

```
public class SynchronizedObjectLock implements Runnable {
    static SynchronizedObjectLock instence1 = new SynchronizedObjectLock();
    static SynchronizedObjectLock instence2 = new SynchronizedObjectLock();

    @Override
    public void run() {
        // 所有线程需要的锁都是同一把
        synchronized(SynchronizedObjectLock.class){
            System.out.println("我是线程" + Thread.currentThread().getName());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "结束");
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(instence1);
        Thread t2 = new Thread(instence2);
        t1.start();
        t2.start();
    }
}

输出结果：
我是线程Thread-0
Thread-0结束
我是线程Thread-1
Thread-1结束

```

### Synchronized 同步锁实现原理

对象锁例子：

```

// 关键字在实例方法上，锁为当前实例
  public synchronized void method1() {
      // code
  }
  
  // 关键字在代码块上，锁为括号里面的对象
  public void method2() {
      Object o = new Object();
      synchronized (o) {
          // code
      }
  }

```

通过反编译看下具体字节码的实现

```

javac -encoding UTF-8 SyncTest.java  //先运行编译class文件命令

javap -v SyncTest.class //再通过javap打印出字节文件

```

输出的字节码

```
   public synchronized void method1();
    descriptor: ()V
    flags: ACC_PUBLIC, ACC_SYNCHRONIZED // ACC_SYNCHRONIZED 标志
    Code:
      stack=0, locals=1, args_size=1
         0: return
      LineNumberTable:
        line 8: 0


```

同步方法的字节码，你会发现：当 Synchronized 修饰同步方法时，出现了一个 ACC_SYNCHRONIZED 标志。

JVM 使用了 ACC_SYNCHRONIZED 访问标志来区分一个方法是否是同步方法。当方法调用时，调用指令将会检查该方法是否被设置 ACC_SYNCHRONIZED 访问标志。如果设置了该标志，执行线程将先持有 Monitor 对象，然后再执行方法。在该方法运行期间，其它线程将无法获取到该 Mointor 对象，当方法执行完成后，再释放该 Monitor 对象。

```

  public void method2();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=4, args_size=1
         0: new           #2                  
         3: dup
         4: invokespecial #1                  
         7: astore_1
         8: aload_1
         9: dup
        10: astore_2
        11: monitorenter //monitorenter 指令
        12: aload_2
        13: monitorexit  //monitorexit  指令
        14: goto          22
        17: astore_3
        18: aload_2
        19: monitorexit
        20: aload_3
        21: athrow
        22: return
      Exception table:
         from    to  target type
            12    14    17   any
            17    20    17   any
      LineNumberTable:
        line 18: 0
        line 19: 8
        line 21: 12
        line 22: 22
      StackMapTable: number_of_entries = 2
        frame_type = 255 /* full_frame */
          offset_delta = 17
          locals = [ class com/demo/io/SyncTest, class java/lang/Object, class java/lang/Object ]
          stack = [ class java/lang/Throwable ]
        frame_type = 250 /* chop */
          offset_delta = 4

```

在修饰同步代码块时，是由 monitorenter 和 monitorexit 指令来实现同步的。进入 monitorenter 指令后，线程将持有 Monitor 对象，退出 monitorenter 指令后，线程将释放该 Monitor 对象。

#### JAVA中的同步实现原理

JVM 中的同步是基于进入和退出管程（Monitor）对象实现的。每个对象实例都会有一个 Monitor，Monitor 可以和对象一起创建、销毁。Monitor 是由 ObjectMonitor 实现，而 ObjectMonitor 是由 C++ 的 ObjectMonitor.hpp 文件实现，如下所示：

```

ObjectMonitor() {
   _header = NULL;
   _count = 0; //记录个数
   _waiters = 0,
   _recursions = 0;
   _object = NULL;
   _owner = NULL;
   _WaitSet = NULL; //处于wait状态的线程，会被加入到_WaitSet
   _WaitSetLock = 0 ;
   _Responsible = NULL ;
   _succ = NULL ;
   _cxq = NULL ;
   FreeNext = NULL ;
   _EntryList = NULL ; //处于等待锁block状态的线程，会被加入到该列表
   _SpinFreq = 0 ;
   _SpinClock = 0 ;
   OwnerIsThread = 0 ;
}

```

当多个线程一起访问某个对象监视器的时候，对象监视器会将这些请求存储在不同的容器中。

1、  Contention List：竞争队列，所有请求锁的线程首先被放在这个竞争队列中

2、  Entry List：Contention List中那些有资格成为候选资源的线程被移动到Entry List中

3、  Wait Set：哪些调用wait方法被阻塞的线程被放置在这里

4、  OnDeck：任意时刻，最多只有一个线程正在竞争锁资源，该线程被成为OnDeck

5、  Owner：当前已经获取到所资源的线程被称为Owner

6、  !Owner：当前释放锁的线程

下图展示了他们之前的关系

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E7%BB%84%E4%BB%B6%E5%85%B3%E7%B3%BB.jpg "对象监视器组件关系.jpg")

当多个线程同时访问一段同步代码时，多个线程会先被存放在 ContentionList 和 _EntryList 集合中，处于 block 状态的线程，都会被加入到该列表。接下来当线程获取到对象的 Monitor 时，Monitor 是依靠底层操作系统的 Mutex Lock 来实现互斥的，线程申请 Mutex 成功，则持有该 Mutex，其它线程将无法获取到该 Mutex，竞争失败的线程会再次进入 ContentionList 被挂起。

如果线程调用 wait() 方法，就会释放当前持有的 Mutex，并且该线程会进入 WaitSet 集合中，等待下一次被唤醒。如果当前线程顺利执行完方法，也将释放 Mutex。

下图展示了他们之前的流程

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "获取对象监视器流程.jpg")