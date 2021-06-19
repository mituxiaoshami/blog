# 前言

通过前几篇的文章分析下来，相信小伙伴们对**类加载**以及和它紧紧绑在一起的**方法区**有了一个清晰的认识，**方法区**咱们就先告一段落，我们把目标盯准到下一个存储区域：**堆**

这个区域相信大家多多少少都有听说过，重要性就不言而喻了，所以从这篇文章开始，我就通过对象创建的**一个心路历程**，来看看**对象是如何被创建又是如何进入到堆的**

# 对象的创建

## 流程图

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e3e4938d81b047b6975a76fc3cbdcf1f~tplv-k3u1fbpfcp-watermark.image)

一个对象的流程图就如上图所示，大致上可以分为以下五步：

1. 类检查机制
2. 分配内存
3. 初始化
4. 设置对象头
5. 执行init方法

我们先根据流程图**留一个大概的印象**，后面我会带着大家走一遍HotSpot源码加强印象，接下来，就从**类检查机制**开始，一个步骤接一个步骤分析下去

## 类检查机制

虚拟机遇到一条**new指令**时，首先将去检查这个指令的参数是否能在`常量池`中定位到一个**类的符号引用**，并且检查这个符号引用代表的类**是否已经被加载、解析和初始化过，如果没有，那必须先执行相应的类加载过程**

new指令对应到语言层面上分多种情况：`new关键词、对象克隆、对象序列化等`

### 例子

我们以下面这段代码为例：

```
public static void main(String[] args) {
        Math math = new Math();
        math.compute();
        System.out.println("Hello World");
}
```

**通过javap指令编译后输出看下**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a536e622f5c048e1a819f7d212d12d36~tplv-k3u1fbpfcp-watermark.image)

**#2对应的符号引用**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7101ea50b21043bf924770d2a42b69df~tplv-k3u1fbpfcp-watermark.image)

其中在main方法对应的字节码头一行就是new指令，旁边的#2就是用来对应静态常量池中对应的符号引用**根据常量类型可以看出这是一个Class，根据注释可以看出#2对应的是Math类**

接下来的检查也就是检查**Math类**是否已经被**加载、解析和初始化过**，如果没有，那必须先执行相应的类加载过程，**类检查机制**完成之后，下一步就到了**分配内存**阶段。

## 分配内存

接下来就到了分配内存的阶段，JVM为新生对象**分配内存**，对象**所需内存的大小**在类加载完成后便可完全确定，为对象分配空间的任务等同于**把一块确定大小的内存从Java堆中划分出来**

关于这块内容，我从以下**三块内容**来讲：

1. `对象内存分配`的方式
2. 分配内存时带来的`并发问题`
3. 对象`存储的地方`是在哪

### 第一块内容：`对象内存分配`的方式

我们知道，**堆是一整块的内存空间**，但是一个对象只需要占到其中极其微小的一块，但是JVM是怎么`在一整块内存中划分出一小块内存`给这个对象，这个问题其实就和**和一块蛋糕怎么分有点类似**

目前Java主流的划分内存的方式有两种，根据**堆中内存空间是不是规整的**，可以把**分配方式**分为以下两种方式：

- **指针碰撞-假设Java堆中内存是绝对规整**
- **空闲列表-假设Java堆中内存并不是规整**

#### 第一种方式：指针碰撞

Java堆中内存是规整的，用过的内存都放在一边，空闲内存放在另外一边，中间放着一根指针作为分界线，那么当一个新对象需要分配内存时，只要**将指针向空闲的空间移动，移动的大小就是和对象的大小相同**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/12846772e17443e9a03600467cde24d5~tplv-k3u1fbpfcp-watermark.image)

如上图所示，用过的内存和空闲的内存**用一根指针作为分界线**，当一个新的对象需要分配内存的时候，指针就往空闲的区域移动，**移动的大小就是新对象的大小**

#### 第二种方式：空闲列表

Java堆中的内存并不是规整的，**已使用的内存和空闲的内存相互交错**，那就没有办法简单的进行简单碰撞了**因为指针移动的区域很有可能已经被使用了**

这时候，针对这种场景，虚拟机就必须维护一个列表，记录上哪些内存块是可用的，**在分配的时候从列表中找到一块足够大的空间划分给对象实例，并更新列表上的记录**，这个列表就是**空闲列表**

#### 如何选择内存分配方式

选择哪种分配方式由**Java堆是否规整**决定，而Java堆是否规整又由所采用的**垃圾收集器的收集算法是否带有空间整理的能力决定**。因此当使用`Serial、ParNew`等带**整理过程的收集器**时，采用的分配算法是指针碰撞，简单又高效，而当使用CMS这种基于**清除算法的收集器**时，理论上就只能采用较为复杂的空闲列表来分配内存

关于这段话，我相信大家


### 第二块内容：分配内存时带来的`并发问题`

在本系列第一篇文章中就提到过，堆是所有线程共享的，那么在多线程的环境下，肯定会涉及到**并发问题**，JVM正在给对象A分配内存**线程A**，指针还没来得及修改，对象B又同时使用了原来的指针来分配内存的情况**线程B**，也就是**不同的对象在不同的线程中需要申请使用同一块内存空间**

面对这种情况，JVM提供了两种解决方案，第一种是**CAS**，第二种是**本地线程分配缓冲（TLAB）**

#### 第一种解决方案：CAS（compare and swap）

虚拟机采用CAS配上失败重试的方式保证更新操作的原子性

以指针碰撞的内存分配方式举个例子，**只是模拟CAS的想法，并不是具体实现**：

- 第一步：线程A过来了，需要给对象A分配内存，线程A拿到当前指针指向的内存地址
- 第二步：线程B也过来了，需要给对象B分配内存，线程B拿到相同的内存地址
- 第三步：之后线程A来更新，发现当前指针指向的内存地址和原先拿到的内存地址相同，那么更新这块内存，把当前指针指向的内存地址改变
- 第四步：之后线程B来更新，发现当前指针指向的内存地址和原先拿到的内存地址不相同，那么就进行失败重试的步骤

#### 第二种解决方案：本地线程分配缓冲（Thread Local Allocation Buffer, TLAB）

把内存分配的动作按照线程划分在不同的空间之中进行，即每个线程在Java堆中预先分配一小块内存**每个线程独有的一块内存空间，默认是eden区的%1，但如果预先分配的内存中放不下了，还是放到eden区，走CAS的逻辑**

**注意：TLAB只是在分配对象时的操作属于线程私有，分配的对象对于其他线程仍是可读的。**

优点：这样的好处是在分配内存时，无需对一整块内存进行加锁，是一种**以空间换时间**的一种方式

##### 设置TLAB

- `-XX:+/-UseTLAB`参数来设定虚拟机是否使用TLAB，**默认开启**
- `-XXTLABSize`指定TLAB大小


### 第三块内容：对象`存储的地方`是在哪

下面这张流程图很清晰的讲述了对象分配的心路历程？我们从一个个**节点进行分析，或者说是一个个的判断来进行分析**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8364323327304633bb33ef496573c2da~tplv-k3u1fbpfcp-watermark.image)

### 第一个节点：栈内分配？

在Java中并不是所有对象一上来就**直接往堆中去分配**，因为一旦这样做会发现**GC特别特别频繁**，特别是**在循环次数多的方法中创建对象**

针对这种场景，JVM利用方法结束栈帧出栈，释放栈帧空间的特性，把**符合条件**的对象分配在栈上

**既然要符合条件，那么这个条件有以下两点：**

1. 首先这个对象**不能占用很多的空间**，总共栈帧就那么点大，超出了肯定不行
2. 这个对象要满足**对象逃逸分析**的要求

#### 对象逃逸分析

关于第一点，我们相信大家都能很快理解，那么下面就来说说这个**对象逃逸分析**

##### 什么是对象逃逸分析

分析对象动态的**作用域**，当一个对象**在方法中被定义后**，如果它被外部方法引用，说明对象动态的**作用域**不仅仅是这个方法，那么就说它已经**逃逸**

##### 对象逃逸分析的例子

```java
public class ObjectEscapeTest {
    public User tes1() {
        User user = new User();
        user.setAge(1);
        user.setName("test1");
        //  ....... 后续操作
        return user;
    }
    public void test2() {
        User user = new User();
        user.setAge(2);
        user.setName("test2");
        // ....... 后续操作
    }
}
public class User {
    private int age;
    private String name;
    // 省略get、set方法
}
```

- 首先来看test1方法，在这个方法中我们new了User类的对象，在方法的最后，把这个对象给返回，给调用这个方法的地方使用，针对这样的user对象，我们就说这个对象逃逸了它动态的作用域，**它原本的作用域是这个方法，但是由于最后返回了这个对象，所以这个对象的作用域已经不局限于这个方法，已经"逃逸"**

- 再来看test2方法，在这个方法中我们new了User类的对象，但是最后并没有把这个对象当作返回值返回，也就是说，这个**对象的作用域一直在这个方法中**，所以这个对象**并没有"逃逸"**

JVM通过**逃逸分析确定该对象不会被外部访问**，**如果不会逃逸**可以将对象在**栈上分配内存**，这样该对象所占用的内存空间就可以随着栈帧的出栈而释放，减轻了GC时候的压力

##### 设置逃逸分析

JVM可以通过开启逃逸分析参数 **-XX:+DoEscapeAnalysis** 来优化对象内存位置分配，使其通过**标量替换**优先在栈上进行分配，**JDK7之后，默认开启逃逸分析**，如果要关闭，使用参数 **-XX:-DoEscapeAnalysis**

这里又引出一个新的概念：**标量替换**，接着就来看下这个**标量替换**是什么操作

#### 标量替换

首先通过逃逸分析确定对象不会被外部访问，并且**对象可以进一步分解**时，**JVM不会创建该对象**，而是将该对象成员变量分解成**若干个被这个方法使用的成员变量所代替**，这些代替的成员变量**在栈帧或寄存器上分配空间**，这样**就不会因为没有一大块连续的内存空间导致对象内存不够分配**

##### 标量替换的作用

主要还是考虑到内存，我们知道一个栈帧里面**已经有局部变量表、操作数栈等一些固定的内存区域**，所以剩下的内存空间**很有可能不是一大块连续的，而是一小块，一小块的内存碎片**，如果要把对象分配在栈中，那么就要考虑到怎么放进去。

所以JVM想到了一个办法，如果这个对象的成员变量有A、B、C三个，在这个方法中只用到了A、B两个，那么它不会去创建这个对象，而是会**把这个方法用到的A、B两个成员变量给存储，存储在栈帧或者寄存器上**，然后需要标识表明这些成员变量是属于这个对象的。

##### 标量和聚合量

**标量就是不可进一步分割的量，Java基本数据类型就是标量**，标量的对立就是聚合量，表明可以进一步可以分割的量，而这种量称之为聚合量，**对象就是可以被进一步分解的聚合量**


##### 怎么设置标量替换

首先要明确一点，**标量替换是要在开启逃逸分析之后，才有效果，如果没有开启逃逸分析，那么也不会开启标量替换**

开启标量替换参数：`-XX:+EliminateAllocations`,**JDK 7之后默认开启**


#### 逃逸分析和标量案例分析

```java
public class AllotOnStack {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            alloc();
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
    private static void alloc() {
        User user = new User();
        user.setAge(3);
        user.setName("alloc");

    }
}
```

案例分析：在main方法中，**循环了一亿次，调用alloc方法**，在alloc方法中，我们**创建了User类对象**，但是User类对象并没有逃逸出这个方法的作用域，因为循环了一亿次，**相当于创建了一亿个User类对象**

##### 验证第一点：不是所有的对象一创建就直接存入堆中

**调整JVM参数（减小内存并且打印GC日志）运行一下：-Xmx15m -Xms15m -XX:+PrintGC**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6ce6180ea04e46478ff5dec7d9284341~tplv-k3u1fbpfcp-watermark.image)

**如果所有的对象都创建在了堆上，那么就一定会发生大量的GC，但是我们发现压根就没有发生GC，所以证明了不是所有的对象一创建就直接存入堆中**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1bcf72e3930b4b63b5225534e82a2736~tplv-k3u1fbpfcp-watermark.image)

##### 验证第二点：不开启逃逸分析，但开启标量替换

调整JVM参数运行一下：**-Xmx15m -Xms15m -XX:-DoEscapeAnalysis -XX:+PrintGC -XX:+EliminateAllocations**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/84ea484d0f664b5e835a2e2f4fc34322~tplv-k3u1fbpfcp-watermark.image)

这里我截取了部分，**发现发生了大量的GC**，说明单开启**标量替换**没啥用，**标量替换**生效的前提是必须开启**逃逸分析**

##### 验证第三点：开启逃逸分析，但不开启标量替换

调整JVM参数运行一下：**-Xmx15m -Xms15m -XX:+DoEscapeAnalysis -XX:+PrintGC -XX:-EliminateAllocations**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/07219ab2e32943ac959586c4ebb3a93f~tplv-k3u1fbpfcp-watermark.image)

这里我也截取了部分，**发现发生了大量的GC**，说明**只开启逃逸分析还达不到很大的优化有效，还必须开启标量替换，才能做到优化的效果**

### 第二个节点：大对象？

当栈上没有分配成功，就去堆中分配内存了，去堆中分配内存的路上，遇到了第二个岔入口判断，**是否是大对象**

根据**对象创建的流程图**，判断如果是大对象，那么**直接进入老年代**，如果不是大对象，那么就进行**本地线程分配缓冲（TLAB）**

TLAB的做法是首先在eden区划分一块内存给你这个线程使用，所以这个**无论是否是通过TLAB分配内存，最后对象到的都是eden区**


#### 创建大对象直接进入老年代案例

JVM默认有这个参数：**-XX:+UseAdaptiveSizePolicy 默认开启，会导致这个8：1：1比例自动变化**，如果不想这个比例有变化，可以使用 **-XX:-UseAdaptiveSizePolicy**


我们来看一下创建后，堆中各个部分的内存占用情况

```java
/**
 * 添加JVM运行参数：-XX:+PrintGCDetails 打印详细GC日志信息
 */
public class GCTest {
    public static void main(String[] args) {
        byte[] allocation = new byte[29500 * 1024];
    }
}
```

**添加-XX:+PrintGCDetails参数后运行一下**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a8c59c068dd04fb1ba7eb6e3c523462c~tplv-k3u1fbpfcp-watermark.image)

**分析一下输出结果**

1. 在main方法中创建了一个大约29500K的对象，可以肯定的是，这个对象`在栈帧空间中放不下`，所以是存放在了堆中
2. 整个年轻代占用了38400K的空间，其中eden区占用了33280K的空间，**from（survivor1）** 占用了5120K空间，**to（survivor2）** 占用了5120K空间，大约是**8：1：1**的比例
3. **eden区的使用率已经达到了100%**，from（survivor1）和 to（survivor2）区使用率为0


很显然，**eden区已经被放满了，我们再放一个大对象**，看看会发生什么样的结果

```java
public class GCTest {
    public static void main(String[] args) {
        byte[] allocation = new byte[29500 * 1024];
        byte[] allocation1 = new byte[8000 * 1024];
    }
}
```

**继续运行，看一下输出结果**

```
Heap
 PSYoungGen      total 38400K, used 9125K [0x00000000d5b80000, 0x00000000da680000, 0x0000000100000000)
  eden space 33280K, 25% used [0x00000000d5b80000,0x00000000d63a34b8,0x00000000d7c00000)
  from space 5120K, 15% used [0x00000000d7c00000,0x00000000d7cc6030,0x00000000d8100000)
  to   space 5120K, 0% used [0x00000000da180000,0x00000000da180000,0x00000000da680000)
 ParOldGen       total 87552K, used 29508K [0x0000000081200000, 0x0000000086780000, 0x00000000d5b80000)
  object space 87552K, 33% used [0x0000000081200000,0x0000000082ed1010,0x0000000086780000)
 Metaspace       used 3237K, capacity 4496K, committed 4864K, reserved 1056768K
  class space    used 352K, capacity 388K, committed 512K, reserved 1048576K
```

**分析一下输出结果，可以得出以下分析结果**

- 第一步：eden区放不下了的时候触发`Minor GC`，所以它会把29500K的对象往`survivor区`放
- 第二步：由于survivor区只有5120K，肯定放不下，这时候就会放到老年代，所以会发现`老年代中使用率已经33%`，剩下的 **from（survivor1）** 有一些`JVM内部的对象`，这些对象还不用放到老年代中，所以我们看到**from（survivor1）** 也有15%的使用率，同时新创建的对象放到了eden区，eden区也有25%的使用率


我们**再创建4个对象**，运行一下看一下

```java
public class GCTest {
    public static void main(String[] args) {
        byte[] allocation =  new byte[29500 * 1024];
        byte[] allocation1 = new byte[8000 * 1024];
        byte[] allocation2 = new byte[1000 * 1024];
        byte[] allocation3 = new byte[1000 * 1024];
        byte[] allocation4 = new byte[1000 * 1024];
        byte[] allocation5 = new byte[1000 * 1024];
    }
}
```

输出结果：

```
Heap
 PSYoungGen      total 38400K, used 13786K [0x00000000d5b80000, 0x00000000da680000, 0x0000000100000000)
  eden space 33280K, 38% used [0x00000000d5b80000,0x00000000d682ca10,0x00000000d7c00000)
  from space 5120K, 15% used [0x00000000d7c00000,0x00000000d7cca020,0x00000000d8100000)
  to   space 5120K, 0% used [0x00000000da180000,0x00000000da180000,0x00000000da680000)
 ParOldGen       total 87552K, used 29508K [0x0000000081200000, 0x0000000086780000, 0x00000000d5b80000)
  object space 87552K, 33% used [0x0000000081200000,0x0000000082ed1010,0x0000000086780000)
 Metaspace       used 3239K, capacity 4496K, committed 4864K, reserved 1056768K
  class space    used 352K, capacity 388K, committed 512K, reserved 1048576K
```

分析一下输出结果：

发现**新创建的对象还是不断的在往eden区放，使用率从原先的25%上升到了38%**，**from区** 和老年代的使用率保持不变，说明没有发生GC，eden区的内存空间够用

#### 怎么判断是不是大对象

根据案例可以得出结论，**如果不设置的话，超过eden区剩余内存大小的对象就是大对象**

#### 怎么设置大对象的大小配置

JVM参数：**-XX:PretenureSizeThreshold**可以设置大对象的大小

如果对象超过设置大小会直接进入老年代，不会进入年轻代，**注意：这个参数只在Serial和ParNew两个收集器下有效**

#### 设置大对象大小例子

```java
public class GCTest {
    public static void main(String[] args) {
        byte[] allocation1 = new byte[8000 * 1024];
    }
}
```

用`-XX:+PrintGCDetails -XX:PretenureSizeThreshold=1000 -XX:UseSerialGC`这个参数，发现8000K的对象直接进入到了老年代，证明参数有效

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7e921e4047eb4cf68c3a95e5065d3a0c~tplv-k3u1fbpfcp-watermark.image)

#### 大对象直接进入老年代的优点

为了避免为**大对象分配内存时的复制操作而降低效率**，因为年轻代的GC用的**标记复制**算法，如果大对象存放在年轻代，会极大的耗费**复制时间**，而Minor GC又是比较频繁发生的GC，**要保证吞吐量**，所以大对象比较适合直接进入老年代

### 第三个节点：对象从年轻代进入老年代

如果一个对象不是大对象，那么就必须得从**年轻代**进入到**老年代**，一个对象从年轻代进入到老年代有很多种方式，分别为**分代年龄判断**、**动态年龄判断**和**老年代空间分配担保机制**，我们就从最普遍的分代年龄判断来看下：

#### 第一种方式:长期存活的对象将进入老年代

JVM采用了**分代收集**的思想来管理内存，那么内存回收就必须能识别**哪些对象应该放在新生代，哪些对象应该放在老年代**。为了做到这一点，JVM给每一个对象都设置了一个**分代年龄**的标识。

如果对象在eden区出生并且**经过第一次Minor GC后存活下来并且被survivor容纳**的话，将被移动到survivor区，并且这个**对象的分代年龄设为1**

对象在survivor区**每熬过一次Minor GC，分代年龄都+1**，当它的年龄达到一定程度**默认是15，CMS收集器为6岁，不同的垃圾回收器会稍微有点不同**，这个对象就会进入到**老年代**中

##### 设置分代年龄阈值

**-XX:MaxTenuringThreshold**来设置

#### 第二种方式:对象动态年龄判断

**当前放对象的survivor区域里**，一批对象的总大小大于**这块survivor区域内存大小的50%（可以配置）**，那么此时**大于等于**这批对象**分代年龄最大值的对象**，就可以直接进入老年代

举个例子：有一批对象 **（分代年龄1+年龄2+年龄n）总和超过了survivor区的50%**，此时就会把**分代年龄n以上**的对象都放入老年代

##### 实际场景例子

根据流量高峰期，调用最频繁的接口进行评估，预估出大约每秒产生60M的对象，过程如下图：

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/69f8f115ae0843bfbccd31c646d1c062~tplv-k3u1fbpfcp-watermark.image)

假设物理机内存为8G，我们**根据物理机的具体大小**，给具体的堆中各个区域分配内存空间，按照下方JVM参数分配内存空间

```
java -Xms3072M -Xmx3072M -Xss1M -XX:MetaspaceSize=512M -XX:MaxMetaspaceSize=512M -jar xx.jar
```
![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/38d15799fbda4562916792a7b1a69c77~tplv-k3u1fbpfcp-watermark.image)

- 实际运行的时候，前13秒产生的对象都能放到eden区中，但是在第14秒执行下单程序的时候，JVM发现eden区已经塞满了，所以**stw(stop the world)，进行MinorGC**
- 假设这个时候第14秒产生的对象还没有退出方法进行出栈，所以还无法被`Minor GC回收`，所以第14秒中产生的60M对象会被放到survivor区，分代年龄设为1，但这一批60M对象已经超出了`survivor区50%的内存空间`，survivor区分代年龄大于等于1的对象都会挪到老年代中

注意：survivor区的对象挪到老年代的时候`Minor GC已经执行完成`

##### 案例总结

在案例中，每14秒都会往老年代中放60M的对象，那几分钟，老年代的内存空间就会被放满，几分钟就会执行一次**full GC**

这种因为**无效的垃圾对象频繁的产生full GC肯定不行**，那怎么来优化？

**问题的症状在于survivor区内存空间太小，导致在对象动态年龄判断机制下直接放入老年代，所以最直接的方法就是扩大新生代的内存空间**

我们按照下方JVM参数分配内存空间，增加新生代的空间，这样按照8：1：1的比例，survivor就是200多M，60M就不会直接放进老年代，**经过一次MinorGC之后，这些垃圾对象就会被回收掉**，不会进入老年代中，这样就大大减少Full GC

```
java -Xms3072M -Xmx3072M -Xmn2048M -Xss1M -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -jar xx.jar
```

##### 设置动态年龄判断大小

**-XX:TargetSurvivorRatio**可以设置动态年龄判断大小

##### 对象动态年龄判断机制的目的

希望那些**可能是长期存活的对象**，尽早的进入老年代。

注意：**对象动态年龄判断机制一般是在Minor GC之后触发的**

#### 第三种方式:老年代空间分配担保机制

年轻代**每次Minor GC之前**，JVM都会计算下**老年代剩余可用空间**，如果可用空间小于年轻代里现有的所有对象大小之和，就会看一个 `-XX:-HandlePromotionFailure JDK 1.8默认设置了`的参数是否设置，这个参数就是一个担保参数，担保一下如果小于历史平均值，就不会发生Full GC

如果有这个参数，就会看看**老年代的可用内存大小，是否大于之前每一次Minor GC后进入老年代的对象的平均大小**

- 如果小于或者参数没有设置，那么就会触发一次Full GC，对老年代和新生代一起回收一次，如果回收完还没有足够空间存放新的对象，那么就会发生**OOM**

- 如果大于之前设置的参数，就会**执行Minor GC**，当然，如果Minor GC之后剩余存活的需要挪动到老年代的对象大小还是大于老年代可用空间，那么也会触发Full GC，Full GC完了之后如果还是没有空间放Minor GC之后的存活对象，也会发生**OOM**

完整的流程图就如下图所示：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/315cd250032d4b4397f91fdd482b18b5~tplv-k3u1fbpfcp-watermark.image)


##### 老年代空间分配担保机制设计的目的：

每一次MinorGC之前，判断如果大概率要发生Full GC，那么就直接执行**Full GC**，就不进行MinorGC**避免流程图中第一个MinorGC**，虽然Full GC完了之后还会执行一次Minor GC，但是这次Minor GC的压力明显会小很多

## 初始化

内存分配完成之后，虚拟机需要**将分配到的内存空间都初始化为零值，不包括对象头**，如果使用TLAB，这一工作过程也可以**提前至TLAB分配时进行**

这一步的作用是保证了**对象的实例字段在Java代码中可以不赋初始值就直接使用，程序能访问到这些字段的数据类型所对应的零值**

```java
public class Student {
   public int age = 100;
}
```

在初始化这一步，就把变量age设置成0，**基本类型设置成默认值，引用类型设置成null**


## 设置对象头

在HotSpot虚拟机中，**对象在内存中可以存储的布局可以分为三个区域：对象头（Object Header）、实例数据（Instance Data）和对齐填充（Padding）**，如下图所示

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d6aca382ccb64c7797e7a2388ceae5dc~tplv-k3u1fbpfcp-watermark.image)

初始化零值之后，JVM要对对象进行必要的设置，例如：这个对象是哪个类的实例，如果才能找到类的元数据信息、对象的hash码、对象的GC分代年龄等信息。这些信息都存放在对象的对象头中，接下来就好好聊聊这个**对象头**

### 说说对象头

**对象可以细分为三个区域：Mark Word、类型指针、数组长度**


1. 第一块区域是**Mark Word**，用于存储对象自身的运行时数据 ，**hash码（hashCode）、GC分代年龄，锁状态标志，线程持有的锁，偏向线程ID、偏向时间戳等等**

2. 第二块区域是**类型指针**，**即指向它的类元数据的指针**，虚拟机通过这个类型指针来确定这个对象是哪个类的实例。

3. 第三块区域是**数组长度**，如果对象是数组类型，那么对象头中还有一个**数组长度，占用4个字节**

#### 第一块区域：Mark Word（以32位为例）

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3379ad23d3314386ad1eb0de320ca259~tplv-k3u1fbpfcp-watermark.image)

记录了不同锁状态下自身的运行时数据：比如我们刚new出来是无锁状态，那么在32位操作系统中，**无锁状态下的对象前25位记录的是对象的hashCode，中间的4位记录的是对象的分代年龄，之后1位记录是否偏向锁，最后2位记录的是锁标志位**

##### 锁状态和锁标志位

关于**锁状态**和**锁标志位**我想在**多线程系列的文章中**详细讲述，先把注意力放在**分代年龄**上

##### 分代年龄

分代年龄在对象头中只能占4位，4位换算成十进制是2的4次方减1=15，所以分代年龄最大只能是15

对象锁的状态从**无锁->偏向锁->轻量级锁**的过程中，**分代年龄丢失了**，这是什么情况呢，连分代年龄都没有了，还怎么进行GC呢？

[其实分代年龄不是消失，而是被拷贝到其它对象里面，详情请点这](https://blog.csdn.net/qq_35124535/article/details/70312553)

#### 第二块区域：Klass Pointer类型指针

讲完了Mark Word，再来看第二块区域：**类型指针**，在讲JVM在类加载的时候说过JVM会把**类的字节码加载到方法区并且以类元信息的形式存储在方法区**，而这个**类型指针**就是用来指向类元信息

##### 实际场景例子

我们以下面这个Math类为例，在main方法中创建一个Math类的对象

```
public static void main(String[] args) {
    Math math = new Math();
    math.compute();
    System.out.println("Hello World");
}
```

根据下方的图所示，math对象的对象头中有指向**方法区中Math类元信息的类型指针**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d03f1c63bd334032a406525dc1b61ad6~tplv-k3u1fbpfcp-watermark.image)

学过反射的小伙伴肯定知道下图中的mathClass对象，那么这个**类对象**和**类元信息**有什么区别呢？

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/577015fb610a4fc4bbed9eaeecd61daf~tplv-k3u1fbpfcp-watermark.image)

这个**类对象是存放在堆**的，因为类中一些静态变量、方法等类元信息都是存放在方法区的，为了让开发人员获取到方法区里面的这些类信息，JVM提供了这么一个**类对象，方便获取类信息，JVM自己内部用的还是对象头里面的类型指针**

#### 数组长度

如果对象是**数组类型**，那么对象头中还有一个**数组长度，占用4个字节**

### 查看对象头

讲完了对象头概念上的组成部分，接下来就通过一个实际的例子，来个对象头长什么样

1. 我们首先在项目中导入下面这个包

```
<!-- https://mvnrepository.com/artifact/org.openjdk.jol/jol-core -->
        <dependency>
            <groupId>org.openjdk.jol</groupId>
            <artifactId>jol-core</artifactId>
            <version>0.9</version>
        </dependency>
```


2. 写上测试代码，查看对象头

```java
public class ObjectTest {
    public static void main(String[] args) {
        ClassLayout classLayout = ClassLayout.parseInstance(new Object());
        System.out.println(classLayout.toPrintable());

        System.out.println();
        ClassLayout classLayout1 = ClassLayout.parseInstance(new int[]{});
        System.out.println(classLayout1.toPrintable());

        System.out.println();
        ClassLayout classLayout2 = ClassLayout.parseInstance(new A());
        System.out.println(classLayout2.toPrintable());
    }
    public static class A {
        int id;
        String name;
        byte b;
        Object object;
    }
}
```

3. 执行main方法，查看输出结果，**我的电脑是64位**

```
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 20 (11100101 00000001 00000000 00100000) (536871397)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total


[I object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           6d 01 00 20 (01101101 00000001 00000000 00100000) (536871277)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
     16     0    int [I.<elements>                             N/A
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total


com.test.ObjectTest$A object internals:
 OFFSET  SIZE               TYPE DESCRIPTION                               VALUE
      0     4                    (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4                    (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4                    (object header)                           6a ce 00 20 (01101010 11001110 00000000 00100000) (536923754)
     12     4                int A.id                                      0
     16     1               byte A.b                                       0
     17     3                    (alignment/padding gap)                  
     20     4   java.lang.String A.name                                    null
     24     4   java.lang.Object A.object                                  null
     28     4                    (loss due to the next object alignment)
Instance size: 32 bytes
Space losses: 3 bytes internal + 4 bytes external = 7 bytes total
```

测试例子：打印了三个对象的对象头，分别是**Object，int类型的数组和拥有成员变量的A**，分别来看下各自的对象头：

#### Object对象

```
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 20 (11100101 00000001 00000000 00100000) (536871397)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```

- OFFSET：起始偏移位置
- SIZE：偏移量，**单位是字节**
- DESCRIPTION：描述，**object header就是对象头**
- Value：值


根据上文介绍，**在64位的机器中，Mark Word在整个对象头中占8个字节**，前2行就是**Mark Word**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f79491c4da1e405eb6bf043d50e79b89~tplv-k3u1fbpfcp-watermark.image)

紧接着就是类型指针Klass Pointer，在64位机器中，分配**4个字节**，所以第三行代表着**类型指针**，但是这里有一个重要的点，Object类型的指针是8个字节，为什么这里显示的是4个字节，其实这个现象就是JVM很重要的一个优化点**指针压缩**，这块内容在下面会和大家详细展开

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4525e7908bc34124a790feaf6135ac61~tplv-k3u1fbpfcp-watermark.image)

Object对象不是数组类型，所以没有数组长度，但是最后一行代表着什么呢，我们重点看最后一个单词alignment，意思是**补齐**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2f6f7f121dcd4ecd8738e2e8d4c97704~tplv-k3u1fbpfcp-watermark.image)

还记得这张对象头的图嘛，对象头中最后有一块内容就是对齐填充，目的是为了保证对象是8个字节的整数倍，这是因为经过大量的验证，**当对象头的大小是8的整数倍的时候，对象的寻址和存取效率是最高的，所以为了补齐对象头，最后JVM自动补了4个字节大小**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8e610a00aa5e48f4ab78ad5600cfd098~tplv-k3u1fbpfcp-watermark.image)

**最后总计16个字节，符合8的倍数**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6413fc855945431d961c1c7c1a811d80~tplv-k3u1fbpfcp-watermark.image)


#### int数组对象

```
[I object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           6d 01 00 20 (01101101 00000001 00000000 00100000) (536871277)
     12     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
     16     0    int [I.<elements>                             N/A
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total
```

前三行和Object对象保持相同，**前8个字节是Mark word，下面4个字节是类型指针，但是由于是数组类型，所以需要分配4个字节给数组长度**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6a1855a49a894299b231166c22adb352~tplv-k3u1fbpfcp-watermark.image)

加起来总共是16个字节，符合8的倍数，自然也就不用对象补齐

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4501d1bdf9654bb393dfbd3cd67794a4~tplv-k3u1fbpfcp-watermark.image)

#### A对象

```
com.test.ObjectTest$A object internals:
 OFFSET  SIZE               TYPE DESCRIPTION                               VALUE
      0     4                    (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4                    (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4                    (object header)                           6a ce 00 20 (01101010 11001110 00000000 00100000) (536923754)
     12     4                int A.id                                      0
     16     1               byte A.b                                       0
     17     3                    (alignment/padding gap)                  
     20     4   java.lang.String A.name                                    null
     24     4   java.lang.Object A.object                                  null
     28     4                    (loss due to the next object alignment)
Instance size: 32 bytes
Space losses: 3 bytes internal + 4 bytes external = 7 bytes total
```

在A对象中，前三排还是我们**熟悉的Mark Word和类型指针**，但是从第四行开始，就是属于我们**实例数据的部分**了

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5f859371475946278930b3d44954ec57~tplv-k3u1fbpfcp-watermark.image)

- 首先int类型变量id占4个字节，byte类型变量b占1个字节，这里需要注意的是，变量内部还搞了自动补齐，目的还是一样的，为了对象的寻址和存取

- 接下来的String类型占4个字节，Object类型是一个引用类型，按照道理来说占8个字节，这里为什么是4个字节呢**还是牵扯到了指针压缩这块内容，马上就会介绍指针压缩**，总共上面所有的加起来是28个字节，不符合8的倍数，所以最后补齐4个字节，总共是32个字节

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8fd02668e3bb453bac4dc5d230ff5662~tplv-k3u1fbpfcp-watermark.image)

### 指针压缩

我们在查看对象头的时候，发现String类型指针的大小应该是**8个字节**，结果打印出来是**4个字节**，Object类型指针是8个字节的，最后也变成了4个字节，这就涉及到了**指针压缩**，从jdk1.6 update14开始，在64位操作系统中，JVM支持指针压缩

##### JVM配置指针压缩参数

- 启用压缩指针: **-XX:+UseCompressedOops**
- 禁止指针压缩: **-XX:-UseCompressedOops，压缩所有指针，类型指针和对象指针**
- 只压缩对象头里的类型指针：**-XX:+UseCompressedClassPointers**

##### 禁用指针压缩案例

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b6d4e8ccb1fb42449af8a44030ac9099~tplv-k3u1fbpfcp-watermark.image)

禁用指针压缩后再来看下Object对象的对象头：

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e86b4293dd364235830affe9f57af418~tplv-k3u1fbpfcp-watermark.image)

- 原先类型指针只有4个字节，也就是一行，现在有两行，变成8个字节
- 原先String类型的变量是4个字节，现在变成8个字节，Object类型的变量原先是4个字节，现在也是8个字节，说明**禁用了指针压缩**

##### 指针压缩的目的

1. 在**64位的操作系统**上，HotSpot使用32位指针（实际存储用64位），内存使用会多出1.5倍左右，使用**较大指针在主内存和缓存之间移动数据，占用较大带宽，同时GC也会承受较大压力**，为了减少64位平台下**内存的消耗**，启用指针压缩功能
2. 在jvm中可以通过对对象指针的存入堆内存时**压缩编码**，取出到cpu寄存器后解码方式进行优化，使得JVM可以只用32位地址就可以支持更多的内存配置 **（堆内存要小于等于32G）**
3. 堆内存小于4G时，不需要启用指针压缩，jvm会自动去除高32位地址，即使用**低虚拟地址空间**
4. 堆内存大于32G时，指针压缩会失效，会强制使用64位（8字节）来对java对象寻址，这就会出现第一点的问题，所以堆内存最好不要大于**32G**

**关于第一点，这里要解释一下：**

学过计算机原理的同学都知道，**在32位的操作系统中，内存最大是2的32次方，也就是4G**，那么对应的64位，理论上最大是2的64次方，这计算下来是一个以T位单位的内存空间，**但是我们市面上普遍的都是8G，16G的偏多，也就是说在一般情况下，最大只用到了64位中的第33位（2的33次方是8G)，或者第35位（2的35次方是16G）**，那么如果是64位的操作系统，**一个对象指针是35位的，剩下的29位就很有可能放不下一个对象了**，所以基于此，JVM采用了指针压缩的方式，对超过32位的对象指针的存入堆内存时压缩编码，压缩到32位以内，取出到cpu寄存器后解码方式进行复原，因为在寄存器上运行的还是要35位的地址，这样做了之后，**64位就可以放下两个对象指针了，大大的节省了内存空间**

## 执行init方法

执行完上面的步骤，从虚拟机的视角来看，一个新的对象已经产生了。但是从Java程序的视角看来，对象创建才刚刚开始——**构造函数**，即Class文件中的**init方法还没有执行**，所有的字段都为默认的零值。

一般来说由**new指令后面是否跟随invokespecial指令决定**，Java编译器会在遇到new关键字的地方同时生成**这两条字节码指令，new和invokespecial**，但如果直接通过其他方式产生的则不一定

**init方法中会按照程序员的意愿对对象进行初始化，这样一个真正可用的对象才算完全被构造出来**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4969a42821714abba50e010a30c29196~tplv-k3u1fbpfcp-watermark.image)

# 通过HotSpot源码中看Java对象创建的流程

通过源码再来复习一下**对象创建这一整套流程**，以下面的代码作为例子

```java
public class Math {
    public static void main(String[] args) {
        Math math = new Math();
        math.say();
    }
    private void say() {
        System.out.println("Hello World!");
    }
}
```

用`javap -v Math.class`指令反编译字节码看下结构，直接看`main方法所在的字节码`，把不必要的代码都省略，结果如下图所示：

```
  public static void main(java.lang.String[]);
    Code:
      stack=2, locals=2, args_size=1
         0: new           #2                  // class com/project/mall/jvm/Math
         3: dup
         4: invokespecial #3                  // Method "<init>":()V
         7: astore_1
         8: aload_1
         9: invokevirtual #4                  // Method say:()V
        12: return

```

HotSpot使用new指令来创建Math对象，下面详细介绍一下HotSpot对new指令的处理，**在上篇文章中讲述了HotSpot源码阅读环境的搭建，感兴趣的可以看下**

Java字节码是解释执行的，如果当前是解释执行，执行new指令其实会去执行`/hotspot/src/cpu/x86/vm/templateTable_x86_64.cpp`文件中的`TemplateTable::_new`方法生成的一段机器码，**就是找对应的方法模板执行**，我们一步步分析一下这个模板


## TemplateTable::_new

```

void TemplateTable::_new() {
  //栈顶缓存验证
  transition(vtos, atos);
  // 调用InterpreterMacroAssembler::get_unsigned_2_byte_index_at_bcp()方法加载new指令后的操作数，对于如上实例来说，这个值就是常量池的下标索引2
  __ get_unsigned_2_byte_index_at_bcp(rdx, 1);
  Label slow_case;
  Label done;
  Label initialize_header;
  Label initialize_object;
  Label allocate_shared;
  // 调用get_cpool_and_tags()方法获取常量池首地址放入rcx寄存器，获取常量池中元素类型数组_tags首地址，放入rax中
  __ get_cpool_and_tags(rsi, rax);
  // 确保class已在常量池中
  const int tags_offset = Array<u1>::base_offset_in_bytes();
  __ cmpb(Address(rax, rdx, Address::times_1, tags_offset),
          JVM_CONSTANT_Class);
  __ jcc(Assembler::notEqual, slow_case);

  // 获取创建对象所属类地址，放入rcx寄存器中，即类的运行时数据结构InstanceKlass，并将其入栈，这个InstanceKlass就是上文说的类型指针
  __ movptr(rsi, Address(rsi, rdx,
            Address::times_8, sizeof(ConstantPool)));

  //  判断类是否已经被初始化过，没有初始化过的话直接跳往slow_close进行慢速分配，
  //  如果对象所属类已经被初始化过，则会进入快速分配
  __ cmpb(Address(rsi,
                  InstanceKlass::init_state_offset()),
          InstanceKlass::fully_initialized);
  __ jcc(Assembler::notEqual, slow_case);

  // 此时rcx寄存器中存放的是类InstanceKlass的内存地址，利用偏移获取类对象大小并存入rdx寄存器
  __ movl(rdx,
          Address(rsi,
                  Klass::layout_helper_offset()));
  __ testl(rdx, Klass::_lh_instance_slow_path_bit);
  __ jcc(Assembler::notZero, slow_case);

  const bool allow_shared_alloc =
    Universe::heap()->supports_inline_contig_alloc() && !CMSIncrementalMode;
    
  // 当计算出了创建对象的大小后就可以执行内存分配了，默认UseTLAB的值为true，也就是使用TLAB
  if (UseTLAB) {
    // 获取TLAB区剩余空间首地址，放入%rax中
    __ movptr(rax, Address(r15_thread, in_bytes(JavaThread::tlab_top_offset())));
    
    // %rdx保存对象大小，根据TLAB空闲区首地址可计算出对象分配后的尾地址，然后放入%rbx中
    __ lea(rbx, Address(rax, rdx, Address::times_1));
    
    // 将%rbx中对象尾地址与TLAB空闲区尾地址进行比较
    __ cmpptr(rbx, Address(r15_thread, in_bytes(JavaThread::tlab_end_offset())));
    
    // 如果%rbx大小TLAB空闲区结束地址，表明TLAB区空闲区大小不足以分配该对象，
    // 在allow_shared_alloc（允许在Eden区分配）情况下，跳转到allocate_shared，否则跳转到slow_case处
    __ jcc(Assembler::above, allow_shared_alloc ? allocate_shared : slow_case);
    
    // 执行到这里，说明TLAB区有足够的空间分配对象
    // 对象分配后，更新TLAB空闲区首地址为分配对象后的尾地址
    __ movptr(Address(r15_thread, in_bytes(JavaThread::tlab_top_offset())), rbx);
    
    // 如果TLAB区默认会对回收的空闲区清零，那么就不需要在为对象变量进行清零操作了，
    // 直接跳往对象头初始化处运行
    if (ZeroTLAB) {
      // the fields have been already cleared
      __ jmp(initialize_header);
    } else {
      // initialize both the header and fields
      __ jmp(initialize_object);
    }
  }

  // 如果在TLAB区分配失败，会直接在Eden区进行分配
  if (allow_shared_alloc) {
  
    // TLAB区分配失败会跳到这里
    __ bind(allocate_shared);

    // 获取Eden区剩余空间的首地址和结束地址
    ExternalAddress top((address)Universe::heap()->top_addr());
    ExternalAddress end((address)Universe::heap()->end_addr());

    const Register RtopAddr = rscratch1;
    const Register RendAddr = rscratch2;

    __ lea(RtopAddr, top);
    __ lea(RendAddr, end);
    
    // 将Eden空闲区首地址放入rax寄存器中
    __ movptr(rax, Address(RtopAddr, 0));

    Label retry;
    __ bind(retry);
    
    // 计算对象尾地址，与空闲区尾地址进行比较，内存不足则跳往慢速分配。
    __ lea(rbx, Address(rax, rdx, Address::times_1));
    __ cmpptr(rbx, Address(RendAddr, 0));
    __ jcc(Assembler::above, slow_case);

    // rax: object begin rax此时记录了对象分配的内存首地址
    // rbx: object end rbx此时记录了对象分配的内存尾地址
    // rdx: instance size in bytes rdx记录了对象大小
    if (os::is_MP()) {
      __ lock();
    }
    // 利用CAS操作，更新Eden空闲区首地址为对象尾地址，因为Eden区是线程共用的，所以需要加锁。
    __ cmpxchgptr(rbx, Address(RtopAddr, 0));

    __ jcc(Assembler::notEqual, retry);

    __ incr_allocated_bytes(r15_thread, rdx, 0);
  }
  
  // 对象所需内存已经分配好后，就会进行对象的初始化了，先初始化对象实例数据
  if (UseTLAB || Universe::heap()->supports_inline_contig_alloc()) {
    // The object is initialized before the header.  If the object size is
    // zero, go directly to the header initialization.
    __ bind(initialize_object);
    
    // 如果rdx和sizeof(oopDesc)大小一样，即对象所需大小和对象头大小一样，
    // 则表明对象真正的实例数据内存为0，不需要进行对象实例数据的初始化，
    // 直接跳往对象头初始化处即可。Hotspot中虽然对象头在内存中排在对象实例数据前，
    // 但是会先初始化对象实例数据，再初始化对象头。
    __ decrementl(rdx, sizeof(oopDesc));
    __ jcc(Assembler::zero, initialize_header);

    // Initialize object fields
    // 执行异或，使得rcx为0，为之后给对象变量赋零值做准备
    __ xorl(rcx, rcx); // use zero reg to clear memory (shorter code)
    __ shrl(rdx, LogBytesPerLong);  // divide by oopSize to simplify the loop
    {
      // 此处以rdx（对象大小）递减，按字节进行循环遍历对内存，初始化对象实例内存为零值
      // rax中保存的是对象的首地址
      Label loop;
      __ bind(loop);
      __ movq(Address(rax, rdx, Address::times_8,
                      sizeof(oopDesc) - oopSize),
              rcx);
      __ decrementl(rdx);
      __ jcc(Assembler::notZero, loop);
    }

    // initialize object header only.
    // 对象实例数据初始化好后，开始初始化对象头（就是初始化oop中的mark和metadata属性的初始化）
    __ bind(initialize_header);
    // 是否使用偏向锁，大多时一个对象只会被同一个线程访问，所以在对象头中记录获取锁的线程id，
    // 下次线程获取锁时就不需要加锁了。
    if (UseBiasedLocking) {
       // 将类的偏向锁相关数据移动到对象头部
      // rax中保存的是对象的首地址
      __ movptr(rscratch1, Address(rsi, Klass::prototype_header_offset()));
      __ movptr(Address(rax, oopDesc::mark_offset_in_bytes()), rscratch1);
    } else {
      __ movptr(Address(rax, oopDesc::mark_offset_in_bytes()),
               (intptr_t) markOopDesc::prototype()); // header (address 0x1)
    }
    // 此时rcx保存了InstanceKlass，rax保存了对象首地址，此处保存对象所属的类数据InstanceKlass放入对象头中，
    // 对象oop中的_metadata属性存储对象所属的类InstanceKlass的指针
    __ xorl(rcx, rcx); // use zero reg to clear memory (shorter code)
    __ store_klass_gap(rax, rcx);  // zero klass gap for compressed oops
    __ store_klass(rax, rsi);      // store klass last

    {
      SkipIfEqual skip(_masm, &DTraceAllocProbes, false);
      // Trigger dtrace event for fastpath
      __ push(atos); // save the return value
      __ call_VM_leaf(
           CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_object_alloc), rax);
      __ pop(atos); // restore the return value

    }
    __ jmp(done);
  }

  // 如果无法在TLAB和Eden区分配，那么进行下面的步骤

  // 慢速分配，如果类没有被初始化过，会跳到此处执行
  __ bind(slow_case);
  // 获取常量池首地址，存入rarg1寄存器
  __ get_constant_pool(c_rarg1);
  // 获取new指令后操作数，即类在常量池中的索引，存入rarg2寄存器
  __ get_unsigned_2_byte_index_at_bcp(c_rarg2, 1);
  // 调用InterpreterRuntime::_new()函数进行对象内存分配
  call_VM(rax, CAST_FROM_FN_PTR(address, InterpreterRuntime::_new), c_rarg1, c_rarg2);
  __ verify_oop(rax);

  // continue
  __ bind(done);
}
```

## InterpreterRuntime::_new

如果**无法在TLAB和Eden区分配，那么会调用InterpreterRuntime::_new()函数进行分配**

```
IRT_ENTRY(void, InterpreterRuntime::_new(JavaThread* thread, ConstantPool* pool, int index))
  Klass* k_oop = pool->klass_at(index, CHECK);
  instanceKlassHandle klass (THREAD, k_oop);

  // Make sure we are not instantiating an abstract klass
  klass->check_valid_for_instantiation(true, CHECK);

  // Make sure klass is initialized
  klass->initialize(CHECK);

  // 进行类的加载和对象分配，并将分配的对象地址返回
  oop obj = klass->allocate_instance(CHECK);
  thread->set_vm_result(obj);
IRT_END


// 进行类的加载和对象分配，并将分配的对象地址返回
instanceOop instanceKlass::allocate_instance(TRAPS) {
  assert(!oop_is_instanceMirror(), "wrong allocation path");
  //是否重写finalize()方法
  bool has_finalizer_flag = has_finalizer(); // Query before possible GC
  //分配的对象的大小
  int size = size_helper();  // Query before forming handle.
  KlassHandle h_k(THREAD, as_klassOop());
  instanceOop i;
  //分配对象
  i = (instanceOop)CollectedHeap::obj_allocate(h_k, size, CHECK_NULL);
  if (has_finalizer_flag && !RegisterFinalizersAtInit) {
    i = register_finalizer(i, CHECK_NULL);
  }
  return i;
}
```

上面的代码片段就是**new指令**完整的流程，根据源码分析下来和一开始的流程图也对应上了，但是缺少了最后一步**类加载的初始化步骤**，这是因为这个步骤是**invokespecial指令**应该做的事，这里就不再继续展开了，感兴趣的同学可以自己研究一下


# 本文总结

到此，一个对象创建走过的路就走完啦，也是我们这篇文章的全部内容了，核心都是围绕着对象的创建流程展开的，我们再来回顾一下：

1. 在**类检查机制**阶段会检查这个符号引用代表的类**是否已经被加载、解析和初始化过，如果没有，那必须先执行相应的类加载过程**
2. 在**分配内存**阶段涉及到**逃逸分析**、**标量替换**、**大对象**、**动态年龄判断机制**、**老年担保机制**等等比较重要的知识点，这一步也是最复杂的步骤
3. 在**初始化**阶段给内存空间赋零值
4. 在**设置对象头**阶段涉及到了**指针压缩**，**自动补齐**等知识点
5. 在**init方法**阶段，执行类的构造方法

对象的`出生`是讲完了，从下一篇开始，就开始分析对象的`回收`，就开始介绍`垃圾回收算法、垃圾收集器`等等相关的知识点

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得**小沙弥**我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取海量Java资源**好用的idea插件、简历模板、设计模式、多线程、架构、编程风格、中间件......**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！