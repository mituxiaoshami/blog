## AbstractQueuedSynchronizer原理解析

java.util.concurrent包中很多类都依赖于这个类所提供队列式同步器，比如说常用的ReentranLock，Semaphore和CountDownLatch等。

为了方便理解，以一段使用ReentranLock的代码为例，讲解ReentranLock每个方法中有关AQS的使用。

### ReentranLock示例

ReentranLock的加锁行为和Synchronized类似，都是可重入的锁，但是二者的实现方式确实完全不同的。除此之外，Synchronized的阻塞无法被中断，而ReentrantLock则提供了可中断的阻塞。

下面的代码是ReentranLock的函数，我们就以此为顺序，依次讲解这些函数背后的实现原理。

```
ReentrantLock lock = new ReentrantLock();
lock.lock();
lock.unlock();
```

### 公平锁和非公平锁

ReentrantLock分为公平锁和非公平锁，二者的区别就在获取锁机会是否和排队顺序相关。

我们都知道，如果锁被另一个线程持有，那么申请锁的其他线程会被挂起等待，加入等待队列。理论上，先调用lock函数被挂起等待的线程应该排在等待队列的前端，后调用的就排在后边。如果此时，锁被释放，需要通知等待线程再次尝试获取锁，公平锁会让最先进入队列的线程获得锁。而非公平锁则会唤醒所有线程，让它们再次尝试获取锁，所以可能会导致后来的线程先获得了锁，则就是非公平。

```
/**
 * 创建一个ReentrantLock实例
 * 等价于使用{@code ReentrantLock(false)}.
 */
public ReentrantLock() {
     sync = new NonfairSync();
}

/**
 * 创建一个ReentrantLock实例(公平锁)
 * 如果传入的是true,那么是公平锁,如果传入的是false,那么是非公平锁
 */
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

会发现FairSync和NonfairSync都继承了Sync类，而Sync的父类就是AbstractQueuedSynchronizer(后续简称AQS)。但是AQS的构造函数是空的,并没有任何操作。

#### 公平锁

ReentranLock的lock函数如下所示，直接调用了sync的lock函数。也就是调用了FairSync的lock函数

```
    //ReentranLock
    public void lock() {
        sync.lock();
    }
    //FairSync
    final void lock() {
        //调用了AQS的acquire函数,这是关键函数之一
        acquire(1);
    }
```

接下来就正式开始AQS相关的源码分析了，acquire函数的作用是获取同一时间段内只能被一个线程获取的量，这个量就是抽象化的锁概念。