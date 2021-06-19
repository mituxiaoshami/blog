# 前言

在上一篇文章中，讲了三种不同的**垃圾收集算法**以及各自的优缺点，也分析了垃圾收集算法中的关键点**怎么判断一个对象是无用对象**，同时对于**各种不同的引用**做了不同的分析，那么从这一篇开始讲讲各种不同的**垃圾收集器**对于垃圾收集算法的实现

# 垃圾收集器

目前市面上有五花八门的垃圾收集器了，大致上有10种左右，如下图所示：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/04476f9327774e018d7edd0f5c7a792c~tplv-k3u1fbpfcp-watermark.image)

根据作用的区域不同可以这样分：

1. 作用于**年轻代**的有Serial、ParNew、Parallel
2. 作用于**老年代**的有CMS、Serial Old、Parallel Old
3. 至于G1和G1后面的垃圾收集器就是**混合回收**

虽然有那么多种的垃圾回收器，但是直到目前为止，还是没有最好的垃圾收集器出现，**更加没有万能的垃圾收集器，要不然也不会出现那么多垃圾收集器了**，我们需要做的就是**根据具体的场景，选择合适的垃圾收集器**，那么就首先来看看**老古董 Serial 收集器**。

## Serial收集器

Serial收集器是最基本、历史最悠久的垃圾收集器了，大家从这个名字种就可以看出来，这是一个**串行收集器**，也就是**单线程收集器**

### Serial Old收集器

Serial Old是Serial收集器的老年代版本，它同样是一个单线程收集器，使用**标记-整理算法**。它可能有两种用途: 一种是在JDK 5以及之前的版本中与Parallel Scavenge收集器搭配使用，另外一种就是作为**CMS收集器发生失败时的后备预案**，在并发收集**发生Concurrent Mode Failure时**使用。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/495eac5cb2f549c7aec1f0e2835994c5~tplv-k3u1fbpfcp-watermark.image)

### Serial的特点

- **只会使用一条垃圾收集线程去完成垃圾收集工作**
- 在进行垃圾收集工作的时候必须暂停其他所有的工作线程**stop the world**，直到它回收结束。
- 新生代/年轻代采用**标记-复制**算法，老年代采用**标记-整理**算法。



### Serial的缺点

最大的缺点就是不良的用户体验，由于**stop the world**机制的存在，在GC线程进行回收这项工作是由虚拟机在后台自动发起和自动完成的，在用户**不可知、不可控的情况下**把用户的正常工作的线程全部停掉，这对很多应用来说都是不能接受的。

不妨试想一下，要是你的电脑每运行一个小时就会暂停响应五分钟，你会有什么样的心情?

### Serial的优点

Serial不只有缺点，它有着优于其他收集器的地方，那就是**简单而高效(与其他收集器的单线程相比)**，对于内存资源受限的环境，它是所有收集器里额外内存消耗最小的;

对于**单核处理器或处理器核心数较少的环境**来说，Serial收集器由于没有线程交互的开销，专心做垃圾收集自然可以获得最高的单线程收集效率。

### JVM设置参数

- `-XX:+UseSerialGC(年轻代) `
- `-XX:+UseSerialOldGC(老年代)`


## ParNew收集器

由于**stop the world**机制的存在造成不良的用户体验，所以要缩短**stop the world**的时间，缩短时间最直接的方向就是在GC的时候由**单线程**改成**多线程**，所以就来看看它的**进化版-ParNew收集器**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1056ada0a03349218b9966aec24c2942~tplv-k3u1fbpfcp-watermark.image)

从上面这张图中我们就可以发现，ParNew收集器在新生代**采用多线程并行收集**去完成垃圾收集工作，但是**老年代还是单线程**

随着**CPU核心数量**的增加，ParNew的存在还是很有好处的。它**默认开启的收集线程数与处理器核心数量相同**，可以使用`-XX:ParallelGCThreads`参数来限制垃圾收集的线程数。


### Parnew收集器的特点

- **新生代采用多线程并行收集去完成垃圾收集工作**
- 在进行垃圾收集工作的时候必须暂停其他所有的工作线程**stop the world**，直到它回收结束。
- 新生代/年轻代采用**标记-复制**算法，老年代采用**标记-整理**算法

ParNew收集器除了**支持多线程并行收集**之外，其他与Serial收集器相比并没有太多创新之处

### JVM设置参数

`-XX:+UseParNewGC`

## Parallel Scavenge收集器

Parallel Scavenge的诸多特性从表面上看`和ParNew非常相似`，但是它的关注点和其他的垃圾收集器不同，**它的关注点在于吞吐量(高效的利用CPU)**，而**CMS等垃圾收集器**的关注点更多的是用户线程的停顿时间 **(提高用户体验)**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5b88c645d4f04c39a01496ad5a8c9c00~tplv-k3u1fbpfcp-watermark.image)

### Parallel Old收集器

Parallel Old是Parallel Scavenge收集器的老年代版本，支持多线程并发收集，基于**标记-整理**算法实现。直到Parallel Old收集器出现后，**吞吐量优先**收集器终于有了合适的搭配组合，在**注重吞吐量**或者**处理器资源较为稀缺的场合**，都可以优先考虑Parallel Scavenge+Parallel Old收集器这个组合，**JDK 8默认就是用的这种组合**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a957e32d64a04ef7b2f442bc241ed9fd~tplv-k3u1fbpfcp-watermark.image)

### Parallel Scavenge收集器的特点

- 新生代/年轻代采用**标记-复制**算法，老年代采用**标记-整理**算法
- 控制最大垃圾收集停顿时间的`-XX:MaxGCPauseMillis参数`
- 直接设置吞吐量大小:`-XX:GCTimeRatio参数`
- 自适应的调节策略：`-XX:+UseAdaptiveSizePolicy参数`

`-XX:MaxGCPauseMillis`：允许的值是一个大于0的毫秒数，收集器将尽力保证内存回收花费的时间不超过用户设定值。需要注意的是**垃圾收集停顿时间缩短是以牺牲吞吐量和新生代空间为代价换取的**，系统**减少了STW的时间**，意味着导致**垃圾收集发生得更频繁**，原来10秒收集一次、每次停顿100毫秒，现在变成5秒收集一次、每次停顿70毫秒。**停顿时间的确在下降，但吞吐量也降下来了**

`-XX:GCTimeRat io`：允许的值是一个大于0小于100的整数，也就是**垃圾收集时间占总时间的比率，相当于吞吐量的倒数**。譬如把此参数设置为19，那允许的最大垃圾收集时间就占总时间的5% (即1/(1+19))，默认值为99，即允许最大1%(即1/(1+99))的垃圾收集时间。

`-XX:+UseAdaptiveSizePolicy`：当设置之后，就不需要指定**新生代的大小(-Xmn)、Eden与Survivor区的比例`(-XX:SurvivorRatio)`、晋升老年代对象大小`(-XX:PretenureSizeThreshold)`等参数了，虚拟机会根据当前系统的运行情况收集性能监控信息，动态调整这些参数以提供最合适的停顿时间或者最大的吞吐量。


### JDK 8 下的Parallel Scavenge收集器

打开**终端**，输入**java -XX:+PrintCommandLineFlags -version**后，按回车：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/afdd8a06b03141e2852a23e114595847~tplv-k3u1fbpfcp-watermark.image)

发现使用的是 **-XX:+UseParallelGC**，但是从JDK7u4开始，就对 **-XX:+UseParallelGC**默认的**老年代收集器**进行了改进，改进使得HotSpot VM在选择使用 **-XX:+UseParallelGC** 时，会默认开启 **-XX:+UseParallelOldGC**，也就是说默认的老年代收集器是Parallel Old。

JDK8中默认的选择是 **-XX:+UseParallelGC**，是Parallel Scavenge+Parallel Old组合。

**文档出处**

1.[openjdk对于parallel_gc的设置](http://hg.openjdk.java.net/jdk8u/jdk8u/hotspot/rev/24cae3e4cbaa)

2.[openjdk对于parallel_gc的优化](https://bugs.openjdk.java.net/browse/JDK-6679764)

### JVM设置参数

- `-XX:+UseParallelGC(年轻代) `
- `-XX:+UseParallelOldGC(老年代)`

## CMS垃圾收集器


CMS（Concurrent Mark Sweep）收集器是一种**以获取最短回收停顿时间**为目标的收集器。它非常符合在注重用户体验的应用上使用。它是HotSpot虚拟机第一款真正意义上的并发收集器，它第一次实现了让**垃圾收集线程与用户线程(基本上)同时工作。**

从名字的**Mark Sweep**可以推断出，它是**标记-清除**算法的实现，它的运作过程相比于上面的几种垃圾收集器更加复杂，整个过程可以分为以下四个步骤：

- 初始标记：暂停所有的其他线程**stop the world**，并记下GC Roots直接引用的对象，速度很快。

- 并发标记：并发标记阶段就是**从GC Roots的直接关联对象开始遍历整个对象链的过程**，这个过程很长，但是`不需要停顿用户线程`，**可以与垃圾收集线程一起并发运行**。因为用户程序继续执行，可能会有导致已经标记过的对象状态发生变化。

- 重新标记：重新标记阶段就是为了**修正并发标记期间因为用户线程继续运行而导致标记产生变动**的那一部分对象的标记记录。这个阶段的停顿时间一般会**比初始标记阶段的时间稍长，远远比并发标记阶段时间短。主要用到三色标记里的增量更新算法(见下面详解）做重新标记。**

- 并发清理：开启用户线程，同时GC线程开始对未标记的区域做清扫。这个阶段如果有新增对象会被标记为**黑色**，不做任何处理**关于黑色，见下方三色标记算法详解**

- 并发重置：**重置本次GC过程中的标记数据。**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6439e26b22084824892995075fa5bc6f~tplv-k3u1fbpfcp-watermark.image)

### 流程解析

1. **初始标记**中，记录的是GC Roots的**直接引用对象**，**注意是直接引用对象，而不是引用链上的所有对象，比如 User user = new User()，只会记录new出来的user对象，关于user对象依赖的对象是不会记录下来的**，同时在初始标记这一步的时候**stop the world**是非常有必要的，因为如果用户线程一直在执行，不断的会有新的GC Roots产生，那么初始标记就做不完了。

2. **并发标记**中，没有执行**stop the world**，GC线程和用户线程并发执行

3. **重新标记**中，进行**stop the world**机制，但是这一步耗费的时间肯定要比第二步**并发标记**短的多，因为它不用去找对象引用链，关于它的实现**三色标记**，会在下方详细的介绍

4. **并发清理**中，清除**没有打上标记的对象**，但是这一步也没有执行**stop the world**机制，由于GC垃圾收集线程和用户线程并行的执行，所以在这一步执行过程中，有可能会产生新的对象，CMS就会把这些增量的对象直接打标成**黑色**，这个**黑色**也是放在待会的**三色标记**中详细介绍

### 没有`stop the world`带来的第一个问题

CMS垃圾收集器让这`并发标记、并发清理`这两步没有执行**stop the world**机制，特别是在并发标记完成后，由于用户程序的执行，很多对象的状态其实是发生变化的，**原本有GC Roots引用的对象，现在没有GC Roots引用了/原本是垃圾对象，后面又复活了，又不是垃圾对象了**，对象引用状态的改变，对JVM来说是很不可控的一种行为，同时还会引出第二个问题

### 没有`stop the world`带来的第二个问题

在**并发标记**和**并发清理**阶段，用户线程是还在继续运行的，自然有**新的垃圾对象不断产生**，但这一部分垃圾对象是出现在标记过程结束以后，CMS无法在当次收集中处理掉它们，只好留待下一次垃圾收集 时再清理掉。这一部分垃圾就称为**浮动垃圾**。

当浮动垃圾过多，又**发生了CMS的GC**，但上一次GC还没结束，这时候就会出现**并发失败（Concurrent Mode Failure)**，这时候JVM启动后备预案:临时启用Serial Old收集器来重新进行老年代的垃圾收集，Serial Old收集器执行的时候是会**STW**的，这样停顿时间就很长了。

所以参数`-XX:CMSInitiatingOccupancyFraction`设置得太高将会很容易导致`大量的并发失败`产生，性能反而降低，用户应在生产环境中根据实际应用情况来权衡设置。


既然会产生上述这样的问题，为什么CMS还要这样实现呢？
其实这一切其都为了**用户体验**，因为如果**堆内存大**，那么一旦发生**stop the world**，用户**停顿的时间是很久的**，对用户线程很不友好

### Parallel和CMS之间的`相爱相杀`

对比发现，Parallel在GC的时候发生了**stop the world**，但是CMS把整一个GC收集过程拆分成了五步，目的就是为了减少**stop the world**的时间，经过经验统计，花费在**并发标记找对象引用链路**这一步花费的时间，占整个GC时间的80%，所以这一步CMS就不执行**stop the world**机制，把**stop the world**拆分进其他两个相对来说耗时没那么长的步骤中，这样从用户的体验来说就是**一顿一顿**的，而不是**直接拉闸**，大大的提高了**用户体验**，特别是在**堆内存**很大的情况

**但有一点很重要**： **这并不是说CMS比Parallel收集器要好**，既然CMS收集器在GC过程中，还让用户线程和GC收集线程并发执行，那么势必会增加GC收集时间 **(CPU资源被用户线程分去)**，Parallel收集器由于**stop the world**机制，GC收集线程能大大占用CPU的时间片，所以Parallel收集器GC时间比CMS收集器GC的时间短，变相的可以说：**CMS是牺牲了GC收集时间来获取用户体验**

而Parallel收集器的特色也正是由于**stop the world**，反而大大提高了吞吐量，在**堆内存**比较小的场景下**一般小于4G**，反而使用这种收集器比较合适，能尽快清理出堆内存空间，这也是JDK 8 默认使用Parallel收集器的原因之一

所以还是介绍垃圾收集器时的一句话：没有最好的垃圾收集器，**更加没有万能的垃圾收集器**，我们需要做的就是**根据具体的场景，选择合适的垃圾收集器**

### CMS的优缺点

- 优点：

1. **并发收集**
2. **低停顿**

- 缺点：

1. 对CPU的资源敏感，会和服务器抢资源，也就是说**GC的时间会长**
2. 无法处理**浮动垃圾，就是在并发标记和并发清理阶段产生的垃圾对象，只能等下一次GC来收集**
3. 既然它使用的是**标记-清除**算法，那么就会导致收集结束后，会产生大量的**内存随便**，不利于后续对象的存储，当然，可以使用 **-XX:+UseCMSCompactAtFullCollection**可以让JVM在执行完**标记-清除**后，再做整理。
4. 执行过程中的不确定性，会存在上一次垃圾回收还没执行完，然后垃圾回收又被触发的情况，**特别是在耗时相对比较长的并发标记和并发清理阶段**，一边回收，一边程序继续执行，也许没执行完又发生了Full GC，也就是**concurrent model failure**，此时会进入**stop the world**，用**Serial old垃圾收集器来回收**


### CMS的核心参数设置

1. 启用CMS：`-XX:+UseConcMarkSweepGC`

2. 设置并发的GC线程数：`-XX:ConcGCThreads`

3. FullGC之后做压缩整理，**用来减少内存碎片**：`-XX:+UseCMSCompactAtFullCollection`

4. 设置多少次Full GC之后压缩整理一次，**默认是0，说明每次FullGC之后都会压缩**：`-XX:CMSFullGCsBeforeCompaction`

5. 当老年代使用达到该比例时会触发Full GC**默认是92，注意：这是百分比，这里留8%的空间也正是为了避免上面说的concurrent model failure，如果你的系统大对象比较多，建议把这个参数调小一点**：`-XX:CMSInitiatingOccupancyFraction`

6. 只使用设定的阈值 **-XX:CMSInitiatingOccupancyFraction设置**，如果不指定，那么JVM仅在第一次使用设定值，后续会自动调整，**比如，触发了concurrent model failure，JVM就会把上述的比例值调小，如果不触发concurrent model failure，那么JVM会把这个值调大**：`-XX:+UseCMSInitiatingOccupancyOnly`

7. **在GC前启动一次Minor GC，减少老年代对年轻代的引用，降低标记阶段时的开销**，，一般CMS的GC耗时80%都在并发标记阶段**在并发标记阶段，很有可能老年代的对象会依赖年轻代的对象，也就是所说的跨代引用，如果并发标记的时候还要去跨代的寻找引用，而且这个放在新生代的对象很有可能是垃圾对象了，那么其实没必要再去标记它，所以在GC之前启动一次Minor GC**：`-XX:+CMSScavengeBeforeRemark`

8. 表示**在初始标记的时候多线程执行，缩短STW**：`-XX:+CMSParallelInitialMarkEnabled`

9. 在**重新标记的时候多线程执行，缩短STW**：`-XX:+CMSParallelRemarkEnabled`


### CMS参数设置实战例子

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/87b9b2f1dc5946628ceec7540883128d~tplv-k3u1fbpfcp-watermark.image)


#### 内存空间设置

假设现在存在一台8G内存的服务器，一般是分配4G内存给**JVM**，正常的JVM参数如下：

```
java -Xms3072M -Xmx3072M -Xss1M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:SurvivorRatio = 8
```

**-XX:SurvivorRatio = 8**，这个参数就是让我们新生代中**eden区和survivor1区和survivor2区的内存大小比例是8：1：1**，如果 **-XX:SurvivorRatio = 4 就是 4：1：1**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4867541753ef4b41981b62361134582f~tplv-k3u1fbpfcp-watermark.image)

##### 运行过程

- 在实际运行的时候，**前13秒产生的对象都能放到eden区中，但是在第14秒执行下单程序的时候，JVM发现eden区已经塞满了，所以stw(stop the world)，进行MinorGC**
- 这个时候第14秒产生的对象还没有出栈，所以对象还在堆中无法被回收，所以第14秒中**产生的60M对象**会尝试放到survivor区，分代年龄设为1，同时，这一批60M对象已经超出了survivor区50%的内存空间，所以survivor区分代年龄大于等于1的对象都会挪到老年代中**此时已经Minor GC已经执行完成**

根据运行过程可知，由于**动态对象年龄判断机制**的存在，按照这样分配内存会导致Full GC频繁，所以我们需要更新下我们的JVM参数，增大Survivor区域

```
java -Xms3072M -Xmx3072M -Xmn2048M Xss1M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:SurvivorRatio = 8
```


这样就降低了因为**对象动态年龄判断机制**而导致的**对象频繁进入老年代的问题**，`其实很多优化无非是让短期存活的对象尽量都留在survivor区里，不要进入老年代`，这样Minor GC的时候，这些对象都会被回收，不会进入到老年代去等待Full GC。


#### 对象动态年龄设置

对于对象年龄应该在多少的时候进入老年代，要根据真实的业务场景做判断，以上面的案例来看：

在本例中，MinorGC的间隔时间是15S左右，大多数的对象在`几秒之内`就会变成垃圾对象，那么完全可以把动态年龄的值改小，比如改成3，**假设大多数对象在5s后变成垃圾对象，那么15/5=3**，那么意味着对象要经过3次Minor GC才会进入到老年代，整个的时间也接近1分钟了**3次Minor GC的间隔时间+MinorGC的执行时间+移动到老年代花费的时间**，如果对象这么长时间还没有被回收，那么完全可以认为这些对象是会存活比较久的对象，可以移动到老年代，而不是一直占据Survivor区域的内存。

#### 大对象设置

对于多大的对象直接进入老年代，这个一般可以结合自己系统看下有没有什么大对象生成，预估下大对象的大小，一般来说设置成**1M （经验所得）** 差不多了，这些对象一般都是系统初始化分配的缓存对象**比如大的缓存List，Map等**

#### 最终结果

```
java -Xms3072M -Xmx3072M -Xmn2048M Xss1M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:SurvivorRatio = 8 -XX:MaxTenuringThreshold=3 -XX:PretenureSizeThreshold=1M
```

#### CMS参数设置

内存空间分配完了，就到了设置垃圾收集器的参数，对于JDK8 默认的垃圾收集器是 **-XX:+UseParallelGC(年轻代)和-XX:+UseParallelOldGC(老年代)**，如果内存较大**超过4个G，也是经验值**，系统停顿时间比较长，能明显的感觉到，我们就可以使用 **ParNew + CMS(-XX:+UseParNewGC -XX:+UseConcMarkSweepGC)** 来优化

##### 大对象设置

我们可以进入到老年代的对象本身开始进行分析，什么样的对象经过3次MinorGC还没有被回收，无非就是一些**缓存对象、Spring容器里的对象，线程池对象**等等，这些对象加起来可能还不到100M，根据这种情况下就去分析**缓存对象、Spring容器里的对象，线程池对象**等等有多大即可

还有一种情况：比如秒杀业务，一瞬间可能超过我们的预期了，每秒可能要处理500-600单，那么每秒生成的对象很有可能超过60M，再加上系统压力激增，一个订单的处理时间可能会被拉长，这些单位时间内产生的对象会变大，所以关于秒杀的场景，很有可能原本预估的60M变成100多M。

##### 设置CMS压缩整理频率

假设每隔5-6分钟出现一次案例中的情况，那么大概1个小时就有可能因为老年代满了触发一次Full GC，Full GC的触发条件还包括了**老年代空间分配担保机制**，针对秒杀的业务场景，历代Minor GC的`均值`是很小的，所以几乎不会在Minor GC触发之前由于老年代空间分配担保失败而产生Full GC，半个小时到1个小时发生一次Full GC是完全能接受的，因为`秒杀业务可能就持续个10-20分钟`，后续可能就是几个小时或者几天执行一次Full GC

#### 关于CMS的碎片整理

因为都是半个小时-1个小时才做一次Full GC，是可以每次执行完就执行一次碎片整理的，但是如果秒杀的时间比较长，我们可以把碎片整理调整到`多次Full GC之后`，这里就以`3次`为例了

#### 设置结果


```
java -Xms3072M -Xmx3072M -Xmn2048M Xss1M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:SurvivorRatio = 8 -XX:MaxTenuringThreshold=3 -XX:PretenureSizeThreshold=1M -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=92 -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=3
```

# 支撑低停顿的利器-三色标记

CMS垃圾收集器是以**低停顿**闻名的，根据CMS的运行过程来看，它的低停顿是在**并发标记**、**并发收集**阶段，做成并行，而没有**STW**，但是这样做也会带来一个问题：

- 由于用户程序的执行，对象的状态很容易发生变化，**原本有GC Roots引用的对象，现在没有GC Roots引用了/原本是垃圾对象，后面又复活了，又不是垃圾对象了**，引用状态的改变，对JVM来说是很不可控的一种行为

为了解决这个问题，JVM引入了**三色标记**这个解决方案

## 三色标记

在**并发标记**的过程中，因为标记期间**应用线程还在继续跑**，对象间的引用很可能发生变化，大致上可以划分成**多标**和**漏标**这两种场景

- **多标:** 在并发标记阶段，把一个GC Roots引用链上的对象已经标记了，但是用户线程没有停止，当方法结束的时候，这个对象链上可能都是垃圾对象**被称为浮动垃圾**，这个就是**多标**

**多标**的情况还好，就是多了些**浮动垃圾**，最多就等到下次GC的时候，这些垃圾对象还是会被回收的。**另外：针对并发标记开始后产生的新对象，通常做法是全部标记成黑色，本轮不会清除。这部分对象期间也可能变成垃圾对象，这也算浮动垃圾的一部分**

- **漏标** 在并发标记阶段，原先已经**被扫描过的对象重新有了新的引用**，导致无法被扫描

JVM引入**三色标记算法**来解决这个问题，它通过可达性分析算法找出**GC Roots**引用的对象链，按照**是否访问过**这个条件标记成以下三种颜色：

1. **黑色:** 表示对象已经被垃圾收集器访问过，且这个对象的所有引用都已经扫描过。**黑色**的对象代表已经扫描，它是安全存活的，如果有其他对象引用指向了这个对象，那么无需重新扫描一遍。**黑色对象不可能直接（不经过灰色对象）指向某个白色对象**

2. **灰色:** 表示对象已经被垃圾收集器访问过，但这个对象上至少存在一个引用还没有被扫描过

3. **白色:** 表示对象尚未被垃圾收集器访问过。显然在可达性分析刚刚开始的阶段，所有的对象都是**白色**的，如果**在分析结束的阶段，仍然是白色的对象**，即代表**不可达**。

## 三色标记过程

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/84092b34ab474c9b918a237eae52f6c8~tplv-k3u1fbpfcp-watermark.image)

`流程图`如上图所示，结合一个例子再来详细看下这个**三色标记:**

```java
package jvm;

public class ThreeColorRemark {
    public static void main(String[] args) {
        Test_A testA = new Test_A();
        // 开始做并发标记
        // 读
        Test_D testD = testA.testB.testD;
        // 写
        testA.testB.testD = null;
        // 写
        testA.testD = testD;
    }
}

class Test_A {
    Test_B testB = new Test_B();
    Test_D testD = null;
}
class Test_B {
    Test_C testC = new Test_C();
    Test_D testD = new Test_D();
}
class Test_C {
}
class Test_D {
}

```

1. 首先开始**初始标记**，因为是GC Roots的引用，所以testA直接标记成**黑色**

2. 进入**并发标记**阶段，根据可达性分析算法找对象的引用链，在扫描testB的时候，发现他依赖了testC对象和testD对象，这时候我们开始去扫描**testC**对象，发现它存在并且没有依赖，所以我们把**testC对象标记成黑色**

3. 这时候我们还没有开始扫描**testD**对象，所以它还是白色，同时由于**testB对象**已经被垃圾收集器访问过，但它依赖的**testD对象**还没有被访问过，所以把testB对象标记成**灰色**

4. **并发收集的时候**，如果对象还是被标记成**白色**，说明它是垃圾对象，可以被回收

## 三色标记如何解决漏标场景

由于在**并发标记**阶段，用户线程没有停止，导致对象状态发生变化，其中就有漏标的场景，举个例子，在上面的main方法中有这么几行代码：

```
// 开始做并发标记
// 读
Test_D testD = testA.testB.testD;
// 写
testA.testB.testD = null;
// 写
testA.testD = testD;
```

testA对象在初始化的时候，**成员变量testD是null**，**初始标记阶段**，把testA对象标记成**黑色**。在程序运行的过程中，我们把testB对象中依赖的testD引用置为null，把**testA对象**中的testD引用设置为原先**testB对象中的testD引用指向的对象**，也就是下图的过程

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b87c7baa22aa4ad780e7e83576a7c429~tplv-k3u1fbpfcp-watermark.image)

但是testA对象`已经标记过了`，也就是说无法被再次扫描，那么它依赖的testD指向的对象就无法被标记到，**同时它也不是GC Roots指向的对象**，所以就出现了漏标的场景，等**并发标记**结束的时候，testD必然还是白色，被判定成是垃圾对象，但是**testD对象**不是一个垃圾对象，所以就出现了严重的垃圾回收BUG

### 解决办法

Wilson于1994年在理论上证明了，当且仅当以下两个条件同时满足时，会产生**漏标的问题**，即原本应该是黑色的对象被误标为白色:

- 插入一条或多条**从黑色对象到白色对象的新引用**，案例中就是把**testA对象**中的testD引用设置为原先**testB对象中的testD引用指向的对象**

- 删除全部**从灰色对象到该白色对象的直接或间接引用**，案例中就是把**testB对象中依赖的testD引用置为null**

漏标会导致被引用的对象当成是**垃圾对象**被误删除，大体上有两种解决方案：

1. 增量更新**Incremental Update**
2. 原始快照**Snapshot At The Begining, STAB**

增量更新针对的是`新增`，原始快照针对的是`删除`

`增量更新：` 当黑色对象插入新的指向**白色对象**的引用关系的时候，就将这个新插入的引用记录下来，等**并发标记**结束之后，等到**重新标记阶段，会stop the world**再将这些记录过的引用关系中的**黑色对象**为根，重新扫描一次

`原始快照：` 当灰色对象要删除指向白色对象的引用关系时，就将这个要删除的**引用关系**记录下来，在**并发标记**结束之后，等到**重新标记阶段，会stop the world**再讲这些记录过的引用关系中的**灰色对象**为根，重新扫描一次，这样就能扫描到**白色对象**，将这些白色对象标记成**黑色对象**，**目的就是让这种对象在本次GC中存货下来，等待下一轮GC的时候重新扫描，这个对象也有可能是浮动垃圾**

无论是**插入**和**删除**，JVM的记录操作都是通过**写屏障**实现的，**STAB**是写前屏障，**增量更新**是写后屏障，举个伪代码的例子：

#### 写屏障`注意:这是伪代码`

假设给某个对象的成员变量赋值时底层代码长这样

```
void field_store(oop* field, oop new_value) {
   // 赋值操作
   *field = new_value;
}
```

`写屏障`就是在赋值的操作前后，增加一些处理，**和AOP类似**

```
void field_store(oop* field, oop new_value) {
   // 赋值之前操作
   pre_write((oop*)field);
   // 赋值操作
   *field = new_value;
   // 赋值之后操作
   after_write(new_value);
}
```

##### 写屏障实现SATB

当对象testB的成员变量发生变化，比如引用消失**testA.testB.testD = null**，可以利用写屏障，将testB对象**原来的引用testD记录下来**

```
void pre_write(oop* field) {
   // 获取旧值
   oop old_value = (oop*) field;
   // 将旧值记录下来
   remark_set.add(old_value);
}
```

##### 写屏障实现增量更新

当对象testA的成员变量发生变化，比如引用新增**testA.testD = testD**，可以利用写屏障，将A对象新的成员变量**testD对象**记录下来

```
void after_write(oop new_value) {
   // 将新值记录下来
   remark_map.add(new_value);
}
```

### 三色标记算法的总结

现代追踪式**可达性算法**的垃圾收集器，几乎都借鉴了三色标记的算法，尽管实现方式可能不同，比如：**白色/黑色集合一般都不会出现，但是有其他体现颜色的地方，灰色集合可以通过栈、队列等方式体现，遍历方式可以是广度/深度遍历等**

对于读写屏障，以Java HotSpot VM为例，其在并发标记时漏标的处理：

- CMS: `写屏障+增量更新`
- G1，Shenandoah：`写屏障+SATB`
- ZGC：`读屏障`

读写屏障还有其他功能，比如写屏障可以用于记录**跨代/区引用**的变化，读屏障可以用于支持**移动对象的并发执行等等**，功能之外，还有性能的考虑，每款垃圾回收器都有自己的想法。


# 本文总结

好啦，以上就是这篇文章的全部内容，围绕着各种垃圾收集器展开的，重点分析了`CMS`，在下一篇文章分析的`G1、ZGC`都是在`CMS`的基础上演变而来的，所以我们得把CMS的机制弄明白，才能去看之后的`G1和ZGC`，在这篇文章中，我们也经历了下面的几个步骤

1. 第一阶段：介绍了各种垃圾收集器以及它们的**特点和优缺点**
2. 第二阶段：把**CMS**单独拎出来详细的介绍机制和参数调优配置
3. 第三阶段：聊聊支撑CMS低停顿的**三色标记算法**

其实还有很重要的两个**垃圾收集器没有去讲**，就是**G1、ZGC**，一方面是公司用的还是JDK 8，另一方面也有篇幅的关系，如果放在这里的话，信息量一下子就太大了，所以我打算放在下篇文章中，去分析**G1和ZGC**，可以期待一下呦！！！

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得**小沙弥**我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取海量Java资源**好用的idea插件、简历模板、设计模式、多线程、架构、编程风格、中间件......**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！
