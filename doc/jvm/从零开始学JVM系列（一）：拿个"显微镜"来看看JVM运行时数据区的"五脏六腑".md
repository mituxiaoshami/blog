# 前言

这篇文章是系列的第一篇文章，把这篇文章作为**开篇**的目的只有一个：**了解JVM是怎样使用内存的**

对于从事C、C++程序的开发人员来说，在**内存管理领域**，他们既是拥有最高权力的**皇帝**， 既拥有每一个对象的**所有权**，又担负着每一个对象生命从开始 到终结的维护责任。

但是对于Java程序员来说，在**JVM**的帮助下，不需要**管理每一个对象**，看起来由**JVM管理内存**一切都很美好，大大的解放了**生产力**。但是也正是因为Java程序员把控制内存的权力交给了JVM，一旦出现**内存泄漏**和**溢出**这些方面的问题，如果不了解**JVM是怎样使用内存的**，那排查错误、修正问题将会成为一项异常艰难的工作。

所以我们在深入学习JVM的第一步就是必须去**了解JVM是怎样使用内存**，带着这个问题，我们来进入本文的内容：《拿个**显微镜**来看看JVM运行时数据区的**五脏六腑**》


# 总体介绍：JVM运行时数据区

下图就是**JVM运行时数据区的内部结构**，我们发现JVM在使用内存的时候把内存**划分成了不同的模块**，如果把JVM看做是**人**的话，那么这些**模块**就是它的**五脏六腑**，既然身体中的**五脏六腑功能不同**，那么理所当然的，JVM内存中**各个模块**也必然是**各司其职**，所以我们先来看看这些**模块**到底有什么特征


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ae7485e81f544f38a3931ed7ba6af488~tplv-k3u1fbpfcp-watermark.image)

Oracle定义了**JVM规范文档**，规范了JVM的实现方式，文档链接如下：[JVM规范文档](https://docs.oracle.com/javase/specs/index.html)

在文档中，规定了JVM的内存区域的划分，大致上可以划分成五大块内容，**堆**、**栈**、**本地方法栈**、**方法区**、**程序计数器**

其中根据**线程的私有和共享**这个维度来划分可以划分成两块

- 栈、本地方法栈、程序计数器这三块区域是**线程私有**的，每个线程**独一份，互不影响**
- 堆、方法区是线程共有的，**所有线程都是共享的**

那么我根据**日常接触**作为优先级，从**不重要-重要**的顺序来讲讲每个**模块**到底负责的是什么


## 本地方法栈

在说本地方法栈之前，得先和大家聊聊**本地方法**，我们在调用Java类库的时候，经常会发现调用到**native关键字修饰的方法**，特别是在调用**线程类Thread**，而用**native关键字**修饰的就是本地方法

### 为什么要有本地方法？

**native方法出现的契机**主要由以下几个原因：

- 因为java是90年代问世的，在这之前，应用程序都是用c、c++写的，那么java语言问世之后，就需要把原先的程序替换成java版本，就需要一种c、c++和java之间的一种映射关系，就催生了**native方法**

- 像**类似线程调度这种靠近操作系统的代码**，无论从**效率的角度**还是**实现的难度**来看，都是由c、c++语言来实现比较适合，毕竟java毕竟是运行在**JVM**上，所以就要有一种**手段**来通过java代码调用c的函数，这个手段就是**native方法**

关于本地方法其实能往深继续讲，包括**如何映射到c、c++的代码，中间经历了哪些步骤**，但是由于和本篇文章的关联性不是很强，所以决定后续把这些知识点单独抽出来写一篇文章，这里就不再继续展开

### 本地方法栈

说完了本地方法，回过头再来看本地方法栈，其实**本地方法栈**就是为**本地方法**服务的，[JVM规范文档](https://docs.oracle.com/javase/specs/index.html)对本地方法栈中方法**使用的语言、使用方式与数据结构**并没有任何强制规定，不同的JVM可以根据需要自由实现它，甚至有的Java虚拟机直接把本地方法栈和虚拟机栈合二为一。

与JVM栈一样，本地方法栈也是**线程私有**的，在**栈深度溢出**时也会抛出**StackOverflowError**异常。

本地方法栈和**虚拟机栈（详情请看本文后续）** 发挥的作用非常相似，区别只不过是**虚拟机栈为虚拟机执行Java方法服务**，而**本地方法栈则为虚拟机使用到Native方法服务**


## 程序计数器 **Program Counter Register**

说完了**本地方法栈**，我们再来看看**第二个模块：程序计数器**，通过改变记录下来的**字节码指令**来控制程序的分支、循环、跳转、异常处 理、线程恢复等基础功能。

### 为什么要有程序计数器

Java代码编译后的字节码在未经过**JIT 实时编译器**编译前，其执行方式是通过**字节码解释器**进行**解释执行**

简单的工作原理为**解释器读取装载入内存的字节码**，按照顺序读取字节码指令。读取一个指令后，将该指令**翻译**成固定的操作，并根据这些操作进行**分支、循环、跳转、异常处理、线程恢复**等流程。

如果程序只有一个线程，其实并不需要程序计数器。但是在**多线程的环境**下，由于cpu的上下文切换，很容易造成**当前线程的程序执行到一半，就切换到了另一个线程**，当另一个线程用完之后，再尝试执行下面的程序，而这个程序计数器就是用来**记录程序当时执行的位置，当字节码执行引擎执行下一行代码时，就动态的修改程序计数器里面的值，改变当前的代码执行位置**

### 程序计数器的特点

1. **线程隔离性**，每个线程工作时都有属于自己的程序计数器。
2. 执行java方法时，程序计数器是有值的，且记录的是**正在执行的字节码指令的地址**

### 程序计数器的外貌

有下面这么一段测试代码，我们把它**编译成字节码文件**后，用**javap -v**命令解析

```java
public class Test {
    
    public static void main(String[] args) {
        // 测试方法
        say();
    }
    /**
     * 测试方法
     */
    private static void say() {
        System.out.println("Hello World");
    }
}
```
如下图所示：输入**javap -v Test.class**命令，然后回车运行

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6a3a0cb4f68e405fa91d18dc055b6943~tplv-k3u1fbpfcp-watermark.image)

展现在我们眼前的就是Test类的字节码文件，**字节码解释器**就是按照这个文件中的顺序读取**字节码指令**，我**截取了main方法所在的字节码**，具体程序计数器存的可以**简单理解**成下图**用红框框圈起来的数字（真实存储的是字节码指令地址）**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c3233386a7844799a576ef8c8976cfb4~tplv-k3u1fbpfcp-watermark.image)



### 程序计数器和native方法的**爱恨情仇**

大家还记得上文说的**本地方法**吗？对于native方法而言，它的**方法体**并不是由**Java字节码**构成的，自然无法应用上述的**字节码行号**概念。那么当native方法结束后是怎么回到原来的位置的呢？

#### JVM规范中对native方法的规定

**If the method currently being executed by the thread is native, the value of the Java Virtual Machine's pc register is undefined**

翻译：一个线程执行Native方法，程序计数器的值未定义，可不是一定为空，任何值都可以。native方法执行后会退出(栈帧pop)，方法退出返回到被调用的地方继续执行程序。

也就是说，我不管你返回什么，我不记录，我只记录下**调用native之前的java方法的字节码行号**，不管你返回的是什么，我还是**从原来的偏移地址继续往下调用**


## 方法区 **Method Area**

说完了**程序计数器**，我们再来看看**第三个模块：方法区**

### 方法区的作用

方法区用于**存储编译后的class二进制文件**，包含了JVM加载的**类信息、常量、静态变量、即时编译后的代码**等数据，类信息大家可以看做是一个**模板**，JVM根据**每个类的模板，创建类的实例**

其中关于**常量**和**静态变量**，大家在平时接触的也比较多，这里就不再赘述，我们来看看剩下的两块内容：**类信息**和**即时编译后的代码**

### 方法区存储的内容

#### 即时编译后的代码

JVM有一个优化叫做JIT，也就是**即时编译优化**，**Java是解释型语言**，速度肯定是不如C这种编译型，那么很明显的一个可行的优化就是把部分热点字节码也直接编译成可执行的机器码**也就是即时编译器编译后的代码**，这样速度就和编译型的一样了

#### 类信息

类信息大体上可以细分为五个部分：**类型信息、类型的常量池(constant pool)、域(Field)、方法(Method)、类变量(除常量外的所有静态变量)**

#### 类型信息

对每个加载的类的类型，jvm必须在**方法区**中存储以下类型信息：

- 类型的完整有效名

**类型名称**在java类文件和jvm中都以**完整有效名**出现。在java源代码中，完整有效名由**类的所属包名称加一个"."，再加上类名**组成

例如，类Object的所属包为**java.lang**，那它的**完整名称为java.lang.Object**，但在类文件里，所有的"."都被斜杠 **/** 代替，就成为**java/lang/Object**。完整有效名在方法区中的表示**根据不同的实现而不同。**

- 类型直接父类的完整有效名 **(interface或是java.lang.Object，两种情况下都没有父类)**

- 这个类型的修饰符 **(public,abstract, final的某个子集)**

- 这个类型直接接口的一个有序列表 **(实现了哪些接口)**


#### 类型的常量池(constant pool)

jvm为每个已加载的类型都维护了一个常量池。常量池就是这个类型**用到的常量的一个有序集合**，包括**实际的常量，域(成员变量)和方法的符号引用**。常量池中的常量是通过索引访问的。因为常量池存储了一个类型所使用到的**所有类型，域(成员变量)和方法的符号引用**，所以它在java程序的**动态链接**中起了核心的作用。

关于什么是**动态链接**会在下篇 **《类加载》** 中详细介绍，这里只需要知道**方法区实际存了些什么东西就行**

常量池的**外貌：** 在用**javap**反编译出来的字节码文件中，**Constant pool**部分就是常量池

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0af1978bf8d24494a4a2e63d478c81cb~tplv-k3u1fbpfcp-watermark.image)

#### 域(成员变量)

JVM必须在方法区中保存**类型的所有域的相关信息以及域的声明顺序**。

域的相关信息包括: **域名称、 域类型、域修饰符(public, private, protected, static, final, volatile, transient的某个子集)**


#### 方法(Method)

JVM必须保存所有方法的以下信息，同域信息一样包括声明顺序：
- 方法名称
- 方法的返回类型(或void)
- 方法参数的数量和类型(按顺序)
- 方法的修饰符(public, private, protected,static,final,synchronized,native, abstract的一个子集)
- 方法的字节码(bytecodes)
- 操作数栈
- 局部变量表及大小( abstract和native,方法除外)
- 异常表( abstract和native方法除外)每个异常处理的开始位置、结束位置、代码处理在程序计数器中的偏移地址、被捕获的异常类的常量池索引


#### 类变量

就是类的静态变量，它只与类相关，所以称为类变量，类变量**被类的所有实例共享，即使没有类实例时你也可以访问它**。这些变量只与类相关，所以在方法区中，它们成为类数据在逻辑上的一部分。在

jvm使用一个类之前，它必须在方法区中为每个**non-final类变量**分配空间。

全局常量 **(static final)**：常量的处理方法则不同，每个全局常量**在编译的时候**就会被分配了。


### 方法区的特点

- **方法区的访问是线程安全的**，由于所有的线程都共享**方法区**，所以方法区里的**数据访问**必须被设计成线程安全的。例如，假如同时有两个线程都企图访问方法区中的同一个类，而这个类还没有被装入JVM，那么只允许一个线程去装载它，而其它线程必须等待

- 方法区在**JVM启动的时候被创建**，并且它的**实际的物理内存空间中和Java堆区一样都可以是不连续的**

- 方法区的大小，跟堆空间一样，**可以选择固定大小或者可扩展**

- 既然方法区是用来存类的信息，那么方法区的大小决定了系统可以保存多少个类，如果系统定义了太多的类，加载大量的第三方的jar包，Tomcat部署的工程过多，大量动态的生成反射类都有可能**发生OOM，导致方法区溢出，会抛出内存溢出错误**

### 方法区的演变

经历过jdk版本升级的小伙伴可能知道，方法区在抛出方法区溢出错误的时候，**不同版本抛出来的错是不一样的**，比如jdk1.8之前是**java.lang .OutofMemoryError:PermGenspace**，jdk1.8之后是**java.lang.OutOfMemoryError: Metaspace**，这是因为方法区有一个演变的过程

#### 演变过程

**Java虚拟机规范**只是规定了有方法区这么个概念和它的作用**保存类信息、常量、静态变量、即时编译器编译后的代码等数据**，并没有规定如何去实现它。

那么，在不同的JVM上方法区的实现肯定是不同的了。大多数用的JVM都是Sun公司的HotSpot。在HotSpot上使用**永久代**来实现方法区。

因此，我们得到了结论，**永久代是HotSpot的概念，方法区是Java虚拟机规范中的定义**，是一种规范，而永久代是一种实现，**一个是标准一个是实现**。

- jdk1.6及之前：有永久代**permanent generation**，相关信息都存放在永久代上
- jdk1.7：有永久代，但已经逐步**去永久代**，把**字符串常量池、静态变量移除，保存在堆中**
- jdk1.8及之后：无永久代，类型信息、字段、方法、常量保存在本地内存的**元空间**，但字符串常量池、静态变量仍在堆

我们从**jdk1.7-jdk1.8**的过程中发现最后JVM舍弃了永久代，替换成了**元空间**，我们来看看两者有什么区别

##### 永久代和元空间

方法区是JVM的一个规范，而**永久代**和**元空间**是对其具体的实现，在JDK7中是永久代，在JDK8中是元空间。


###### 永久代

永久代是一块连续的内存空间，**它的垃圾回收是跟老年代的垃圾回收绑定的**，两者只要有一个内存满了，**两个区域都会进行垃圾回收**。因为这个原因有的人就把他归入堆中，跟堆中的其他区域一起管理。我们可以通过 **-XX:PermSize**和 **-XX: MaxPermSize**设置永久代的最大值，即使不设置也有默认大小，32位JVM的大小是64M，64位JVM的大小是82M。

###### 元空间

首先元空间与永久代最大的不同在于：**元空间并不在虚拟机之中，而是使用的本地内存**，这就导致了元空间的默认大小是没有限制的**但是会受到本地内存大小的限制**。通过 **-XX:MetaspaceSize**和 **-XX:MaxMetaspaceSize**来设置元空间大小。


#### 为什么要进行这次演变

1. 类的方法的**信息大小难以确定**，因此给永久带的**大小指定**比较困难，太小容易出现永久代溢出，太大则容易导致老年代溢出。

2. 永久带会为GC带来不必要的复杂性，并且回收效率偏低，在永久代中元数据可能会随着每一次Full GC发生而进行移动，而hotspot虚拟机每种类型的垃圾回收器都要**特殊处理永久代中的元数据**，分离出来以后可以简化Full GC。

3. **永久代是hotspot VM的实现特有的**，而别的JVM没有永久代这一说，Oracle可能会将hotspot和这个Jrockit合二为一 **(不同的JVM实现)**，所以元空间来替代可以比较好，方便统一

4. 字符串常量池如果存在于永久代中，容易出现性能问题和内存溢出，频繁的发生Full GC

### 方法区小结

**方法区**就先告一段落了，重点在于**存储的内容和演变的过程**，这块区域和类加载是有很紧密的联系，毕竟存的是类加载的东西，所以在后续讲**类加载**的时候，会继续深入分析**方法区**，目前，我们只需要知道方法区是用来干嘛的，存的是什么内容就可以了，不必过分深究，接下来我们再来看**第四个模块：栈**

## 栈

**栈**这个模块是需要我们好好**挖一挖**的，因为在平时的接触中，**栈**是仅次于最后的**堆**的，意味着这个区域和我们**有着很深的联系**，所以接下来，我们来好好聊聊**栈**

### 栈的特点

- 栈是线程级别，那么意味着每有一个线程就会从**总的栈空间**中，单独划分出一小块内存区域给这个线程，这小块的内存区域就是**线程栈**


### 栈的结构

栈是由一个个的**栈帧**组成，对应**一个方法一个栈帧**，线程每执行一个方法就在它的栈空间中创建一个栈帧，**一个方法对应一块栈帧内存区域**

线程栈和数据结构中的栈是一样的，都是**FILO（first in last out）**，先进后出，**先执行栈顶的栈帧，依次往下**

这其实和我们**程序的执行顺序**保持吻合，程序永远都是**后执行的方法先释放掉内存空间**，栈也是保持一样，**后入栈，先出栈，释放栈帧内存区域**

举个例子：新建一个Math类，如下图所示

```java
public class Math {

    public int compute() {
        int a = 1;
        int b = 2;
        int c = (a + b) * 10;
        return c;
    }
    public static void main(String[] args) {
        Math math = new Math();
        math.compute();
        System.out.println("Hello World");
    }
}
```

对应main线程的栈就应该是下图这样子

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6915fbafae1c48ff9913dfe2d10495a5~tplv-k3u1fbpfcp-watermark.image)

既然栈是由一个个栈帧组成的，我们来深挖一下栈帧是什么样的一个结构


#### 栈帧

##### 栈帧的作用

栈帧是用于支持JVM进行**方法调用和方法执行的数据结构**

栈帧存储了**方法的局部变量表、操作数栈、动态连接和方法返回地址等信息**。每一个方法从调用至执行完成的过程，都对应着**一个栈帧在虚拟机栈里从入栈到出栈**的过程。结构如下图所示：

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ab369f8e6d2e41578b7a8137ff548f33~tplv-k3u1fbpfcp-watermark.image)

##### 局部变量表

局部变量表**Local Variable Table**就是一组变量值存储空间，用于存放**方法参数和方法内部定义的局部变量**。容量以**变量槽 Variable Slot**为最小单位 **(其中64位长度的long和 double类型的数据会占用两个变量槽，其余的数据类型只占用一个）**。在Java程序**编译为Class文件时**,就在方法的Code属性中的**max_locals数据项中**确定了该方法所需分配的局部变量表的**最大Slot数量**，局部变量表的结构如下图所示：


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4104769a90d94366bc4017fdd48fbccd~tplv-k3u1fbpfcp-watermark.image)

我们举个例子来看下局部变量表在字节码的**表现形式**:

```java
public class Test {

    public static void main(String[] args) {
        // 测试静态方法
        say1();
        // 测试实例方法
        Test test = new Test();
        test.say2();
    }
    /**
     * 测试实例方法
     */
    private void say2() {
        System.out.println("Hello World------2");
    }
    /**
     * 测试静态方法
     */
    private static void say1() {
        System.out.println("Hello World------1");
    }
}
```

既然局部变量表是栈帧的组成部分，所以我们使用**javap -v Test.class**观察下方法相关的字节码，**以say2方法为例**：

```
  public com.project.mall.Test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 3: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       5     0  this   Lcom/project/mall/Test;
```

其中LocalVariableTable就是say2方法的**局部变量表**，它就是由一个个的Slot组成，同时**如果是实例方法的话，第0个slot就留给this关键字了**，我们再来看下静态方法main方法：


```
 public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=2, args_size=1
         0: invokestatic  #2                  // Method say1:()V
         3: new           #3                  // class com/project/mall/Test
         6: dup
         7: invokespecial #4                  // Method "<init>":()V
        10: astore_1
        11: aload_1
        12: invokevirtual #5                  // Method say2:()V
        15: return
      LineNumberTable:
        line 7: 0
        line 9: 3
        line 10: 11
        line 11: 15
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      16     0  args   [Ljava/lang/String;
           11       5     1  test   Lcom/project/mall/Test;
    MethodParameters:
      Name                           Flags
      args
```

发现main方法对应栈帧中的局部变量表第一个slot不是this关键字，变相的验证了java的知识点：静态方法中不能用this关键字

**为什么静态方法中不能用this关键字？**

因为this是个引用，哪个对象调用方法就引用哪个对象。 而静态方法有可能不是被对象调用的，this无从引用。类方法是属于类本身的 所有对象共享 this表示当前实例的引用 静态方法中不能引用非静态实例成员。

###### 局部变量表小结

一句话总结，局部变量表就是用来存储**一个方法的参数和方法内部定义的局部变量**

##### 操作数栈

操作数栈的作用就是记录在方法运行过程中产生的数，既然是栈，那么也是**FILO**的结构，**先进后出**，先执行栈顶的数

我们通过一个字节码文件为例来看看**局部变量表和操作数栈：**


```java
public class Math {
    public int compute() {
        int a = 1;
        int b = 2;
        int c = (a + b) * 10;
        return c;
    }
    public static void main(String[] args) {
        Math math = new Math();
        math.compute();
        System.out.println("Hello World");
    }
}
```

Math类编译后的字节码文件后用**javap指令**，生成可读性好的字节码文件，在对应字节码的包路径下执行 **javap -c Math.class > Math.txt** ，把字节码文件生成到相同目录下的Math.txt文件

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f72a99bda77c46b381d02dd3924b7b24~tplv-k3u1fbpfcp-watermark.image)


##### 局部变量表和操作数栈

我们先把目光放在compute方法，看下这个方法的**局部变量表和操作数栈：**

```
  public int compute();
    Code:
       0: iconst_1
       1: istore_1
       2: iconst_2
       3: istore_2
       4: iload_1
       5: iload_2
       6: iadd
       7: bipush        10
       9: imul
      10: istore_3
      11: iload_3
      12: ireturn
```

我们以compute方法为例，结合字节码表**网上可以百度查下**来分析下这个方法的执行流程：
- 第一行代码iconst_1：将int型常量1推送至**操作数栈**的栈顶（也就是**压入栈顶**）
- 第二行代码istore_1：将操作数栈顶int类型的数值存入**下标为1**的局部变量表中，**因为下标为0的给this关键字了**
- 第三行代码iconst_2：将int型常量2推送至**操作数栈**栈顶
- 第四行代码istore_2：将操作数栈顶int型数值存入**下标为2**的局部变量表中
- 第五行代码iload_1：将第二个int型本地变量推送至栈顶，也就是**加载局部变量表中下标为1的int类型的变量，加载到操作数栈的栈顶**
- 第六行代码iload_2：将第三个int型本地变量推送至栈顶，也就是**加载局部变量表中下标为2的int类型的变量，加载到操作数栈的栈顶**
- 第七行代码iadd：**将栈顶两int型数值相加并将结果压入栈顶**
- 第八行代码bipush：将单字节的常量值(-128~127)推送至栈顶，后面跟着10，意思就是**把10压入到操作数栈**
- 第九行代码imul：将栈顶两int型数值相乘并**将结果压入栈顶**
- 第十行代码istore_3：将栈顶int型数值存入第四个本地变量，**赋值给c**
- 第十一行代码iload_3：将第四个int型本地变量推送至栈顶，**推送到操作数栈的栈顶**
- 第十二行代码ireturn：从当前方法**返回int**

##### 局部变量表和操作数栈小结

经过上面这么一个字节码流程分析，我们可以发现：

1. 方法内部的变量都是存放在局部变量表中，**特别提醒：如果是对象类型，那么局部变量表中存放的是堆给对象分配的内存地址，也就是指针，而不是对象直接存在局部变量表中**，以类似数组的结构进行存储，通过下标的方式进行搜索

2. 操作数栈：就是**存放方法运行过程中产生的一些临时数据，目的是为了计算**，以栈的数据结构进行存储。

#### 动态链接

在讲**动态链接**之前，得先和大家聊聊什么是**符号引用**

##### 符号引用

在JVM中，一个类的**方法名、类名、修饰符、返回值等等都是一系列的符号，而且这些符号都是一个个的常量，存储在常量池中**，同时这些个**符号被加载到内存后**，都有对应的内存地址，而这些**内存地址就是直接引用**

###### 符号引用的例子

我们还是以下面这段代码为例，先将编译后的文件用**javap -v Test.class**命令进行反编译

```java
public class Test {

    public static void main(String[] args) {
        Test test = new Test();
        test.say();
    }
    private void say() {
        System.out.println("Hello World");
    }

}
```

**javap指令后的反编译文件**


```
public class classload.Test
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #8.#24         // java/lang/Object."<init>":()V
   #2 = Class              #25            // classload/Test
   #3 = Methodref          #2.#24         // classload/Test."<init>":()V
   #4 = Methodref          #2.#26         // classload/Test.say:()V
   #5 = Fieldref           #27.#28        // java/lang/System.out:Ljava/io/PrintStream;
   #6 = String             #29            // Hello World
   #7 = Methodref          #30.#31        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #8 = Class              #32            // java/lang/Object
   #9 = Utf8               <init>
  #10 = Utf8               ()V
  #11 = Utf8               Code
  #12 = Utf8               LineNumberTable
  #13 = Utf8               LocalVariableTable
  #14 = Utf8               this
  #15 = Utf8               Lclassload/Test;
  #16 = Utf8               main
  #17 = Utf8               ([Ljava/lang/String;)V
  #18 = Utf8               args
  #19 = Utf8               [Ljava/lang/String;
  #20 = Utf8               test
  #21 = Utf8               say
  #22 = Utf8               SourceFile
  #23 = Utf8               Test.java
  #24 = NameAndType        #9:#10         // "<init>":()V
  #25 = Utf8               classload/Test
  #26 = NameAndType        #21:#10        // say:()V
  #27 = Class              #33            // java/lang/System
  #28 = NameAndType        #34:#35        // out:Ljava/io/PrintStream;
  #29 = Utf8               Hello World
  #30 = Class              #36            // java/io/PrintStream
  #31 = NameAndType        #37:#38        // println:(Ljava/lang/String;)V
  #32 = Utf8               java/lang/Object
  #33 = Utf8               java/lang/System
  #34 = Utf8               out
  #35 = Utf8               Ljava/io/PrintStream;
  #36 = Utf8               java/io/PrintStream
  #37 = Utf8               println
  #38 = Utf8               (Ljava/lang/String;)V
{
  public classload.Test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 3: 0
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0       5     0  this   Lclassload/Test;

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=2, args_size=1
         0: new           #2                  // class classload/Test
         3: dup
         4: invokespecial #3                  // Method "<init>":()V
         7: astore_1
         8: aload_1
         9: invokespecial #4                  // Method say:()V
        12: return
      LineNumberTable:
        line 7: 0
        line 8: 8
        line 9: 12
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
            0      13     0  args   [Ljava/lang/String;
            8       5     1  test   Lclassload/Test;
}
SourceFile: "Test.java"
```

**Constant pool就是我们的常量池，常量池中存放的就是各种各样的符号**


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e2ced5e791724918a57a4c962247c538~tplv-k3u1fbpfcp-watermark.image)

每个符号旁都有一个带 **#** 的，这个 **#1、#2**就是一个**标识符或者说定位符**，在实例的创建，变量的传递，方法的调用，JVM都是用这个标识符来定位


##### 符号引用的调用过程

我们以**new Test方法**为例，来看下**符号引用**是怎么定位的：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7c79ec2fe4fd464dbf8fb8dfc3c69ad6~tplv-k3u1fbpfcp-watermark.image)

在main()方法中，一开始会去new一个Test类，旁边的注释中，也指明了**new的是class classload/Test**，我们接下来再来看#2指向了啥

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/687989771ea9434c8816465ec1d5d406~tplv-k3u1fbpfcp-watermark.image)

可以看到#2是一个class，并且又去指向了一个#25，我们再跟踪到#25来看一下

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2b06277ccbf5491d8a7832029182df75~tplv-k3u1fbpfcp-watermark.image)

可以看到#25是代表着一个类，同时编码是utf8，最终定位到了new的是Test类，所以通过常量池中符号的**标识符**，jvm可以一步步找到创建的到底是什么类，方法的调用也是一样

##### 符号引用和动态链接

Java代码在编译完之后，这些**方法名、()、类名等等，都变成一个个的符号**，并且存放在常量池中，符号引用一部分会在**类加载阶段或者第一次使用的时候**就被转化为直接引用，这种转化被称为**静态解析**。 另外一部分将在**每一次运行期间都转化为直接引用，这部分就称为动态连接**


#### 方法出口

顾名思义，方法出口就是方法执行完成后，需要返回的一些信息

当一个方法开始执行后，只有两种方式退出这个方法。

- 第一种方式：**执行引擎遇到方法返回的字节码指令，如return指令**，这种退出方法的方式称为**正常调用完成**

- 第二种方式：**在方法执行的过程中遇到了异常**，这种退出方法的方式称为**异常调用完成**。一个方法使用异常完成出口的方式退出，是不会**给它的上层调用者提供任何返回值**。

一般来说，方法正常退出时，**方法的PC计数器的值就可以作为返回地址**，栈帧中很可能会保存这个计数器值。而方法异常退出时，返回地址是要通过**异常处理器表**来确定的，栈帧中就一般不会保存这部分信息。

## 堆

截止到目前为止，我们知道了以下几点

- 本地方法的信息存在了哪里-----**方法区**
- 类的信息存在了哪里-----**方法区**
- **方法的参数和局部变量和程序运行过程中产生的数据**存在了哪里-----**栈**

我们还剩下最后一块，也是最重要的一块内容，**堆-----用来存放对象的区域**，接下来就盘盘**堆**

### 堆的作用

Java堆是**垃圾收集器管理的内存区域**，因此一些资料中它也被称作GC堆**Garbage Collected Heap**，是用来**存放对象**的**实际内存区域**

### 堆的结构

从回收内存的角度看，由于现代垃圾收集器大部分都是基于**分代收集理论**设计的，大致上分为**年轻代和老年代**，其中年轻代又分为**Eden区和Survivor 0区和Survivor 1区（Survivor0和Survivor1也叫From和To区）**

但是需要**注意**的是，从JDK 11开始引入的ZGC开始出现不采用**分代设计**的新垃圾收集器，但是目前主流还是采用**分代设计**的垃圾收集器，所以这里还是以分代为例，所以**堆的结构**如下图所示，默认情况下，**老年代占总的堆内存的2/3，年轻代占1/3**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0c27576445cc479989696782667cf31a~tplv-k3u1fbpfcp-watermark.image)

接下来我们就从年轻代入手，来看看各个区域到底负责是什么内容

#### 年轻代

在整个年轻代中又可以细分为**Eden区和survivor区**，survivor区又分为survivor0和survivor1

Eden区和Survivor0区和Survivor1区，他们之间的配比是**8：1：1（默认情况下）**，这三个区域之前的关系请看下面**年轻代的GC回收流程（Minor GC/Young GC）**

##### 年轻代GC回收过程（Minor GC/Young GC）

我们在项目中new出来的/通过反射调用出来的对象通常情况下都是存放在Eden区，**注意：这种情况不是绝对的，我们先举通常情况为例**

- 当一个应用程序24小时不间断的运行，不断会有新的对象产生，迟早会把eden区给塞满

- 当Eden区被对象塞满后就会发生GC，这个GC是**Young GC/Minor GC**, **字节码执行引擎**后台会开启一个**垃圾回收线程**，专门用来执行**Minor GC**，回收年轻代的无用对象

- 在eden区中**筛选出来有用的对象**，把有用的对象复制到**survivor0/survivor1，第一次随机分配，假设分配到survivor0**，剩下**eden区的对象都是垃圾对象**，直接干掉

- 程序继续执行，eden区的对象又放不下了，又触发Minor GC，这次它**不仅会回收eden区，还会回收survivor0区的垃圾对象**，把eden区和survivor0区的有用对象一起放到**survivor1区**，eden区和survivor0区**剩余的垃圾对象**直接干掉

- 程序继续执行，eden区的对象又放不下了，又会触发Minor GC，这次它不仅会回收eden区，还会回收survivor1区的垃圾对象，把**eden区和survivor1区的引用对象一起放到survivor0区**，eden区和survivor1区**剩余的垃圾对象直接干掉**

- 依次往复循环，直到超过了一定的次数，对象就会放到老年代区，老年代放不下就要执行Full GC了，关于Full GC我们以后的文章再细讲，这里先知道年轻代中**Eden区和Survivor0区和Survivor1区的关系是什么样子的就可以了**


# 本文总结

好啦，以上就是这篇文章的全部内容了，我们再来回顾一下文章的大致脉络

1. 第一部分：开篇讲了**为什么**要去了解JVM运行时数据区，JVM运行时数据区**大致上被划分成了哪几个模块**

2. 第二部分：讲述了JVM运行时数据区中**各个模块存放的数据是什么，各个模块的作用是什么，在JVM中承担了什么样的一个角色**

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取**电子书《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）周志明》**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！


