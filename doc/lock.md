## 深入了解Lock同步锁的优化方法

除了在 JVM 层实现的 Synchronized 同步锁的优化方法之外，在 JDK1.5 之后，Java 还提供了 Lock 同步锁。

相对于需要 JVM 隐式获取和释放锁的 Synchronized 同步锁，Lock 同步锁（以下简称 Lock 锁）需要的是显示获取和释放锁，这就为获取和释放锁提供了更多的灵活性。

Lock 锁的基本操作是通过乐观锁来实现的，但由于 Lock 锁也会在阻塞时被挂起，因此它依然属于悲观锁。

两种锁的各自特点：

![github](https://github.com/mituxiaoshami/blog/blob/master/doc/picture/ConcurrentHashMap%E5%88%86%E6%AE%B5%E9%94%81.png "减小锁粒度.jpg")

从性能方面上来说，在并发量不高、竞争不激烈的情况下，Synchronized 同步锁由于具有分级锁的优势，性能上与 Lock 锁差不多；但在高负载、高并发的情况下，Synchronized 同步锁由于竞争激烈会升级到重量级锁，性能则没有 Lock 锁稳定。


### Lock 锁的实现原理

Lock 锁是基于 Java 实现的锁，Lock 是一个接口类，常用的实现类有 ReentrantLock、ReentrantReadWriteLock（RRW），它们都是依赖 AbstractQueuedSynchronizer（AQS）类实现的。

#### AQS的原理解析

[AbstractQueuedSynchronizer原理解析](https://github.com/mituxiaoshami/blog/blob/master/doc/AbstractQueuedSynchronizer.md)

