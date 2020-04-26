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

### 锁升级优化

为了提升性能，JDK1.6 引入了偏向锁、轻量级锁、重量级锁概念，来减少锁竞争带来的上下文切换，而正是新增的 Java 对象头实现了锁升级功能。

当 Java 对象被 Synchronized 关键字修饰成为同步锁后，围绕这个锁的一系列升级操作都将和 Java 对象头有关。

#### Java 对象头

在 JDK1.6 JVM 中，对象实例在堆内存中被分为了三个部分：对象头、实例数据和对齐填充。其中 Java 对象头由 Mark Word、指向类的指针以及数组长度三部分组成。

Mark Word 记录了对象和锁有关的信息。Mark Word 在 64 位 JVM 中的长度是 64bit，我们可以一起看下 64 位 JVM 的存储结构是怎么样的。如下图所示：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "Java对象头.jpg")

锁升级功能主要依赖于 Mark Word 中的锁标志位和释放偏向锁标志位，Synchronized 同步锁就是从偏向锁开始的，随着竞争越来越激烈，偏向锁升级到轻量级锁，最终升级到重量级锁。

#### 锁优化步骤

偏向锁->轻量级锁->重量级锁

#### 偏向锁

##### 偏向锁应用场景

偏向锁主要用来优化同一线程多次申请同一个锁的竞争。

在某些情况下，大部分时间是同一个线程竞争锁资源，例如，在创建一个线程并在线程中执行循环监听的场景下，或单线程操作一个线程安全集合时，同一线程每次都需要获取和释放锁，每次操作都会发生用户态与内核态的切换。

##### 偏向锁工作原理

当一个线程再次访问这个同步代码或方法时，该线程只需去对象头的 Mark Word 中去判断一下是否有偏向锁指向它的ID(线程ID)，无需再进入 Monitor 去竞争对象了。

当对象被当做同步锁并有一个线程抢到了锁时，锁标志位还是 01，“是否偏向锁”标志位设置为 1，并且记录抢到锁的线程 ID，表示进入偏向锁状态。

一旦出现其它线程竞争锁资源时，偏向锁就会被撤销。偏向锁的撤销需要等待全局安全点，暂停持有该锁的线程，同时检查该线程是否还在执行该方法，如果是，则升级锁，反之则被其它线程抢占。

###### 全局安全点

1、从线程的角度，安全点是代码执行中的一些特殊位置，当线程执行到这些特殊的位置，如果此时在GC，那么在这个地方线程会暂停，直到GC结束。

2、GC的时候要挂起所有活动的线程，因此线程挂起，会选择在到达安全点的时候挂起。

3、安全点这个特殊的位置保存了线程上下文的全部信息。说白了，在进入安全点的时候打印日志信息能看出线程此刻都在干嘛。

##### 偏向锁获取和撤销流程(红色部分)：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "Java对象头.jpg")


因此，在高并发场景下，当大量线程同时竞争同一个锁资源时，偏向锁就会被撤销，发生 stop the word 后， 开启偏向锁无疑会带来更大的性能开销，这时我们可以通过添加 JVM 参数关闭偏向锁来调优系统性能，示例代码如下：

```
-XX:-UseBiasedLocking //关闭偏向锁（默认打开）
```
或

```
-XX:+UseHeavyMonitors  //设置重量级锁
```

#### 轻量级锁

当有另外一个线程竞争获取这个锁时，由于该锁已经是偏向锁，当发现对象头 Mark Word 中的线程 ID 不是自己的线程 ID，就会进行 CAS 操作获取锁，如果获取成功，直接替换 Mark Word 中的线程 ID 为自己的 ID，该锁会保持偏向锁状态；如果获取锁失败，代表当前锁有一定的竞争，偏向锁将升级为轻量级锁。

轻量级锁适用于线程交替执行同步块的场景，绝大部分的锁在整个同步周期内都不存在长时间的竞争。

##### 轻量级锁操作流程(红色部分)：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "Java对象头.jpg")

#### 自旋锁与重量级锁

轻量级锁 CAS 抢锁失败，线程将会被挂起进入阻塞状态。如果正在持有锁的线程在很短的时间内释放资源，那么进入阻塞状态的线程无疑又要申请锁资源。

JVM 提供了一种自旋锁，可以通过自旋方式不断尝试获取锁，从而避免线程被挂起阻塞。这是基于大多数情况下，线程持有锁的时间都不会太长，毕竟线程被挂起阻塞可能会得不偿失。

从 JDK1.7 开始，自旋锁默认启用，自旋次数由 JVM 设置决定，不建议设置的重试次数过多，因为 CAS 重试操作意味着长时间地占用 CPU。

自旋锁重试之后如果抢锁依然失败，同步锁就会升级至重量级锁，锁标志位改为 10。在这个状态下，未抢到锁的线程都会进入 Monitor，之后会被阻塞在 _WaitSet 队列中。

##### 重量级锁操作流程(红色部分)：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "Java对象头.jpg")


在锁竞争不激烈且锁占用时间非常短的场景下，自旋锁可以提高系统性能。一旦锁竞争激烈或锁占用的时间过长，自旋锁将会导致大量的线程一直处于 CAS 重试状态，占用 CPU 资源，反而会增加系统性能开销。所以自旋锁和重量级锁的使用都要结合实际场景。

在高负载、高并发的场景下，我们可以通过设置 JVM 参数来关闭自旋锁，优化系统性能，示例代码如下：

```
-XX:-UseSpinning //参数关闭自旋锁优化(默认打开) 
-XX:PreBlockSpin //参数修改默认的自旋次数。JDK1.7后，去掉此参数，由jvm控制
```

### 动态编译实现锁消除 / 锁粗化

除了锁升级优化，Java 还使用了编译器对锁进行优化。JIT 编译器在动态编译同步块的时候，借助了一种被称为逃逸分析的技术，来判断同步块使用的锁对象是否只能够被一个线程访问，而没有被发布到其它线程。

确认是的话，那么 JIT 编译器在编译这个同步块的时候不会生成 synchronized 所表示的锁的申请与释放的机器码，即消除了锁的使用。在 Java7 之后的版本就不需要手动配置了，该操作可以自动实现。

锁粗化同理，就是在 JIT 编译器动态编译时，如果发现几个相邻的同步块使用的是同一个锁实例，那么 JIT 编译器将会把这几个同步块合并为一个大的同步块，从而避免一个线程“反复申请、释放同一个锁“所带来的性能开销。

### 减小锁粒度

除了锁内部优化和编译器优化之外，我们还可以通过代码层来实现锁优化，减小锁粒度就是一种惯用的方法。

当锁对象是一个数组或队列时，集中竞争一个对象的话会非常激烈，锁也会升级为重量级锁。我们可以考虑将一个数组和队列对象拆成多个小对象，来降低锁竞争，提升并行度。

最经典的减小锁粒度的案例就是 JDK1.8 之前实现的 ConcurrentHashMap 版本。我们知道，HashTable 是基于一个数组 + 链表实现的，所以在并发读写操作集合时，存在激烈的锁资源竞争，也因此性能会存在瓶颈。而 ConcurrentHashMap 就很很巧妙地使用了分段锁 Segment 来降低锁资源竞争，如下图所示：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/%E8%8E%B7%E5%8F%96%E5%AF%B9%E8%B1%A1%E7%9B%91%E8%A7%86%E5%99%A8%E6%B5%81%E7%A8%8B.jpg "Java对象头.jpg")

## 总结

JVM 在 JDK1.6 中引入了偏向锁机制来优化 Synchronized，当一个线程获取锁时，首先对象锁将成为一个偏向锁，这样做是为了优化同一线程重复获取导致的用户态与内核态的切换问题；其次如果有多个线程竞争锁资源，锁将会升级为轻量级锁，它适用于在短时间内持有锁，且分锁有交替切换的场景；轻量级锁还使用了自旋锁来避免线程用户态与内核态的频繁切换，大大地提高了系统性能；但如果锁竞争太激烈了，那么同步锁将会升级为重量级锁。

减少锁竞争，是优化 Synchronized 同步锁的关键。我们应该尽量使 Synchronized 同步锁处于轻量级锁或偏向锁，这样才能提高 Synchronized 同步锁的性能；通过减小锁粒度来降低锁竞争也是一种最常用的优化方法；另外我们还可以通过减少锁的持有时间来提高 Synchronized 同步锁在自旋时获取锁资源的成功率，避免 Synchronized 同步锁升级为重量级锁。


Synchronized锁升级步骤

1. 偏向锁:JDK6中引入的一项锁优化,它的目的是消除数据在无竞争情况下的同步原语，进一步提高程序的运行性能 ,
2. 偏向锁会偏向于第一个获得它的线程，如果在接下来的执行过程中，该锁没有被其他的线程获取，则持有偏向锁的线程将永远不需要同步。大多数情况下，锁不仅不存在多线程竞争，而且总是由同一线程多次获得，为了让线程获得锁的代价更低而引入了偏向锁
3. 当锁对象第一次被线程获取的时候，线程使用CAS操作把这个锁的线程ID记录再对象Mark Word之中，同时置偏向标志位1。以后该线程在进入和退出同步块时不需要进行CAS操作来加锁和解锁，只需要简单地测试一下对象头的Mark Word里是否存储着指向当前线程的偏向锁。如果测试成功，表示线程已经获得了锁。
4. 如果线程使用CAS操作时失败则表示该锁对象上存在竞争并且这个时候另外一个线程获得偏向锁的所有权。当到达全局安全点（safepoint，这个时间点上没有正在执行的字节码）时获得偏向锁的线程被挂起，膨胀为轻量级锁（涉及Monitor Record，Lock Record相关操作，这里不展开），同时被撤销偏向锁的线程继续往下执行同步代码。
5. 当有另外一个线程去尝试获取这个锁时，偏向模式就宣告结束
6. 线程在执行同步块之前，JVM会先在当前线程的栈帧中创建用于存储锁记录(Lock Record)的空间，并将对象头中的Mard Word复制到锁记录中，官方称为Displaced Mark Word。然后线程尝试使用CAS将对象头中的Mark Word替换为指向锁记录的指针。如果成功，当前线程获得锁，如果失败，表示其他线程竞争锁，当前线程便尝试使用自旋来获取锁。如果自旋失败则锁会膨胀成重量级锁。如果自旋成功则依然处于轻量级锁的状态
7. 轻量级锁的解锁过程也是通过CAS操作来进行的，如果对象的Mark Word仍然指向线程的锁记录，那就用CAS操作把对象当前的Mark Word和线程中赋值的Displaced Mark Word替换回来，如果替换成功，整个同步过程就完成了，如果替换失败，就说明有其他线程尝试过获取该锁，那就要在释放锁的同时，唤醒被挂起的线程
8. 轻量级锁提升程序同步性能的依据是：对于绝大部分的锁，在整个同步周期内都是不存在竞争的（区别于偏向锁）。这是一个经验数据。如果没有竞争，轻量级锁使用CAS操作避免了使用互斥量的开销，但如果存在锁竞争，除了互斥量的开销外，还额外发生了CAS操作，因此在有竞争的情况下，轻量级锁比传统的重量级锁更慢
简单概括为:
1. 检测Mark Word里面是不是当前线程ID,如果是,表示当前线程处于偏向锁
2. 如果不是,则使用CAS将当前线程ID替换到Mark Word,如果成功则表示当前线程获得偏向锁,设置偏向标志位1
3. 如果失败,则说明发生了竞争,撤销偏向锁,升级为轻量级锁
4. 当前线程使用CAS将对象头的mark Word锁标记位替换为锁记录指针,如果成功,当前线程获得锁
5. 如果失败,表示其他线程竞争锁,当前线程尝试通过自旋获取锁 for(;;)
6. 如果自旋成功则依然处于轻量级状态
7. 如果自旋失败,升级为重量级锁
   - 锁指针:在当前线程的栈帧中划出一块空间,作为该锁的锁记录,并且将锁对象的标记字段复制到改锁记录中!