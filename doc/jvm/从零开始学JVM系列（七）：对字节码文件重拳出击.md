# 前言

这篇文章不打算放在任何一个系列里面，纯粹是个人对这方面比较感兴趣才写的，在日常的工作中，也不会用到关于这块的知识，但是，我希望如果有小伙伴和我一样，对字节码感兴趣的，那么这一篇文章希望能帮上你不小的忙


# 字节码之旅

在说字节码之前，我想问大家的是，我们一般都对的是`.java`文件里面的代码，或者说直观一点，看到的是类似下面的代码：


```java
public class User {
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public static void main(String[] args) {
        User user = new User();
        user.setName("Hello World");
        System.out.println(user.getName());
    }

}
```
JVM是不认识这种格式的文件，所以我们就有了**编译**，把`.java`文件**编译**成`.class`文件，而JVM认识的就是`.class`格式的文件,所以我们来看看编译后的User.class文件长什么样子:

**我们用 EditPlus 软件以Hex viewer的格式打开.class文件**


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/744992b7c6ef48069a12f8339eb14bc4~tplv-k3u1fbpfcp-watermark.image)


首先引入我们眼帘的就是下方这一大串东西，这一串东西就是我们这篇文章的**重中之重**，是不是**眼花**了，不要慌，因为后面还有更加**费眼睛**的事，是不是把你带进坑了，哈哈哈，不过在下还是希望各位看官能看完，毕竟写完这篇文章还是很不容易的，那么接下来我们就一点点的**玩弄**下面的这一大坨字节码



![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/761dda9936494b5f99eb4b8295cee7d7~tplv-k3u1fbpfcp-watermark.image)


## 分析字节码之路


在分析上面一大坨字节码之前，我们首先要了解**字节码的进制**和**字节码结构**，我们只有结合这两个基础的知识点，才能**戏弄**字节码


### 字节码基础知识

在JVM规范中，每个字段或者变量都有描述信息，描述信息的主要作用是 数据类型、方法参数列表和返回值类型等

#### 数据类型

基本数据类型和void类型都是用一个大写的字符来表示，对象类型是通过一个大写的L加全类名表示，这么做的好处就是保证JVM能读懂class文件的前提下尽量压缩class文件的大小

##### 基本数据类型

- B对应的就是byte
- C对应的就是char
- D对应的就是double
- F对应的就是float
- I对应的就是int
- J对应的就是long
- S对应的就是short
- Z对应的就是boolean
- V对应的就是void

##### 对象类型

- String对应的就是Ljava/lang/String;(注意有一个分号)

对于数组类型

每一个唯独都是用一个前缀【来表示，比如：int[] 对应的就是[I这样子，String[][]对应的就是[[Ljava.lang.String;

#### 方法参数列表和返回值类型

用描述符来描述方法的参数列表和返回值类型，先方法参数列表，后返回值类型，参数列表按照严格的顺序放在()中，例如：String getUserInfoByIdAndName(int id, String name) 的方法描述符号就写成下面这个样子：

**(I,Ljava/lang/String;) Ljava/lang/String;**


### 字节码进制

字节码（Byte-code）是一种包含执行程序、由一序列 op 代码/数据对组成的二进制文件。字节码是一种中间码，它比机器码更抽象。它经常被看作是包含一个执行程序的二进制文件，更像一个对象模型。字节码被这样叫是因为通常每个 opcode 是一字节长，但是指令码的长度是变化的。每个指令有从 0 到 255（或十六进制的： 00 到FF)的一字节操作码，被参数例如寄存器或内存地址跟随。

这是我在百度百科上找的，说实话，我也看不太懂，但是我知道，字节码文件里面的字符都是十六进制的，这就够了，也就是说，字节码文件里面的2个字符是1个字节，如下图所示：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6ddf09d8f3ab4ac89f2122b5d0b52233~tplv-k3u1fbpfcp-watermark.image)


### 字节码结构

下面这张图是我从网上copy下来的，接下来我们就结合字节码进制和字节码结构来好好**玩弄**我们的字节码

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/af669e82a62b4dea907bd5e36568601a~tplv-k3u1fbpfcp-watermark.image)


### 分析字节码

注意一下哈：这里字节码结构的顺序**从上而下**，就是咱们字节码的顺序，所以我们分析的顺序也是按照这个字节码结构的顺序分析的，所以首先来看第一块内容：**魔数**


#### 魔数

魔数的意思就是一些固定值，占用4个字节，我们从字节码文件的开头找出4个字节，就是下图中的**cafe babe**，没啥特殊的意思，就是简单的一个标识，说明这是一个字节码文件

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a15345a46bb24f5bb79a1f13351c8dad~tplv-k3u1fbpfcp-watermark.image)


#### 次版本号和主版本号

在**魔数**接下来的**2个字节**意思就是这个字节码文件的**次版本号**，再底下**2个字节**是**主版本号**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0e1e43f44e074c5480f90d96ad5d7a0e~tplv-k3u1fbpfcp-watermark.image)

在这个文件中，次版本号是0，主版本号是52（34是16进制，换算成10进制就是52），52就是我们jdk 1.8的版本，依次类推，51就是jdk 1.7的版本，结合主次版本，推断出jdk的版本是1.8.0，我们来验证一下：

我们在cmd中输入命令：java -version，发现版本就是1.8.0（后面的201是更新的号码，不是版本的号）


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/56041efb3a134972950d3b431d419a9c~tplv-k3u1fbpfcp-watermark.image)


#### 常量池容量计数器(常量池中常量的个数)

次版本号和主版本号之后，就来到了常量池容量计数器，说白了就是用来记录常量池中常量的个数，用2个字节来记录


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/92dfc50176474b1d821e9d945d81a92a~tplv-k3u1fbpfcp-watermark.image)


我们把 **00 2F**换算成10进制是47，也就是常量池中常量的个数是47个，我们通过反编译字节码来验证一下：

找到字节码文件对应的路径输入：**javap -v User.class**，之后找到**Constant pool**


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ff30c3a0101649bbbbc4ed924611627b~tplv-k3u1fbpfcp-watermark.image)


Constant pool就是我们的常量池，我们看下它里面的个数：



![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0e3127237696495aa5627af0a3b0edf5~tplv-k3u1fbpfcp-watermark.image)


**发现这里标注的只有46个，和我们的47差一个，其实这一个在Java中有规定，默认第0个是null，所以，咱们会发现，常量池中的常量计数是从1开始的**



#### 常量池表

顾名思义：常量池表就是用来表示咱们的常量池，这一步就是要**盘盘**常量池，这里稍微有点复杂，大家稍微耐心点看

#### 常量池

我们通过**javap -v User.class**反编译字节码文件，截取出完整的常量池

```
Constant pool:
   #1 = Methodref          #10.#31        // java/lang/Object."<init>":()V
   #2 = Fieldref           #3.#32         // test/User.name:Ljava/lang/String;
   #3 = Class              #33            // test/User
   #4 = Methodref          #3.#31         // test/User."<init>":()V
   #5 = String             #34            // Hello World
   #6 = Methodref          #3.#35         // test/User.setName:(Ljava/lang/String;)V
   #7 = Fieldref           #36.#37        // java/lang/System.out:Ljava/io/PrintStream;
   #8 = Methodref          #3.#38         // test/User.getName:()Ljava/lang/String;
   #9 = Methodref          #39.#40        // java/io/PrintStream.println:(Ljava/lang/String;)V
  #10 = Class              #41            // java/lang/Object
  #11 = Utf8               name
  #12 = Utf8               Ljava/lang/String;
  #13 = Utf8               <init>
  #14 = Utf8               ()V
  #15 = Utf8               Code
  #16 = Utf8               LineNumberTable
  #17 = Utf8               LocalVariableTable
  #18 = Utf8               this
  #19 = Utf8               Ltest/User;
  #20 = Utf8               getName
  #21 = Utf8               ()Ljava/lang/String;
  #22 = Utf8               setName
  #23 = Utf8               (Ljava/lang/String;)V
  #24 = Utf8               main
  #25 = Utf8               ([Ljava/lang/String;)V
  #26 = Utf8               args
  #27 = Utf8               [Ljava/lang/String;
  #28 = Utf8               user
  #29 = Utf8               SourceFile
  #30 = Utf8               User.java
  #31 = NameAndType        #13:#14        // "<init>":()V
  #32 = NameAndType        #11:#12        // name:Ljava/lang/String;
  #33 = Utf8               test/User
  #34 = Utf8               Hello World
  #35 = NameAndType        #22:#23        // setName:(Ljava/lang/String;)V
  #36 = Class              #42            // java/lang/System
  #37 = NameAndType        #43:#44        // out:Ljava/io/PrintStream;
  #38 = NameAndType        #20:#21        // getName:()Ljava/lang/String;
  #39 = Class              #45            // java/io/PrintStream
  #40 = NameAndType        #46:#23        // println:(Ljava/lang/String;)V
  #41 = Utf8               java/lang/Object
  #42 = Utf8               java/lang/System
  #43 = Utf8               out
  #44 = Utf8               Ljava/io/PrintStream;
  #45 = Utf8               java/io/PrintStream
  #46 = Utf8               println
```


#### 常量池类型结构


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9953c8507aa849be8114106d7ce2e4e9~tplv-k3u1fbpfcp-watermark.image)


在分析之前，我们先来看上面的一张图，这张图里面截取的是不同的常量类型，也就是说，不同的常量池类型用不同的标识，也意味着有不同的结构，而下面这张图就是完整的常量类型结构，我们通过下面这张表来分析


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9be9529b58bb473eba9d2f3c39ee252e~tplv-k3u1fbpfcp-watermark.image)



##### 常量池分析


###### 例子一

回到我们的字节码，讲完常量池个数之后，就开始讲常量池表，而**0A**就是我们第一个常量项的标记位


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8e44465b37cb4cb68eae56d03876e666~tplv-k3u1fbpfcp-watermark.image)

**0A**换算成10进制就是10，去我们的常量项表中去找tag=10的常量项，发现是下图所示的结构


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e8679e519fa44176a4246d686ab7d0b6~tplv-k3u1fbpfcp-watermark.image)


根据图中所示可知：

- 第一个字节是标志位
- 第二、三个字节是索引位
- 第四、五个字节是名称和返回值


我们首先可以知道，这个常量项占用5个字节(u1+u2+u2)，我们数5个字节：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/587866dfce9c4c7fb300b80303e6b30f~tplv-k3u1fbpfcp-watermark.image)

- 第一个字节是标志位：0A = 10
- 第二、三个字节是索引位：00 0A = 10
- 第四、五个字节是名称和返回值： 00 1F = 31


我们用字节码文件来验证一下：


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f5fde787dfb04427ba0b87818a68a7fd~tplv-k3u1fbpfcp-watermark.image)

发现和我们分析的一致，对应的位置确实是**10**和**31**，表示的是Object类的init方法，返回值是V也就是void类型，匹配上了


###### 例子二

我们接着看第二个例子


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/58ae8baa592f4a0e8636cb37a8d43232~tplv-k3u1fbpfcp-watermark.image)

由于第一个例子占用了5个字节，我们第二个例子就是从它下面一个字节开始是09开始，09换算成10进制就是9，我们去找9对应的常量项


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/14e23a2becbf4e35a5f9fc5c1b9e63a1~tplv-k3u1fbpfcp-watermark.image)


发现这个常量项占用5个字节(u1+u2+u2)，我们数5个字节：


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5aa2c6b92608400693c6f5b951709f42~tplv-k3u1fbpfcp-watermark.image)

根据常量项的图可知：

- 第一个字节是标志位 **09 = 9**
- 第二、三个字节是索引位 **00 03 = 3**
- 第四、五个字节是字段名称和类型 **00 20 = 32**

我们用字节码文件来验证一下：


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/163696eea39f40a7b6c27df0fb649e54~tplv-k3u1fbpfcp-watermark.image)


发现和我们分析的一致，对应的位置确实是**3**和**32**，表示的是字段名是**test/User.name:**，类型是**Ljava/lang/String**，也匹配上了


###### 例子三

紧接着看第三个常量，从字节07开始，07换算成10进制是7，所以去找tag=7的常量项


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3505412de9974920a1fb25749de41b4e~tplv-k3u1fbpfcp-watermark.image)


根据下图所示，我们发现这个常量项只有3个字节，第一个字节表示的是标志位，剩下的字节用来表示索引

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dc44a242ceb443f89f844af61a3ae0b5~tplv-k3u1fbpfcp-watermark.image)


我们按照上面的套路来看下，数三个字节（07 00 21 ），第一个字节是tag=7，剩下的字节**00 21**换算成10进制是33，我们验证一下：


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3058ecd0eb6b4129b56432a12a0dfc9d~tplv-k3u1fbpfcp-watermark.image)

发现也匹配上了，表明它指向的类是test/User


##### 常量池分析步骤


从上面的三个例子中，我们总结了分析字节码的规律：

1. 第一步：先找tag位
2. 第二步：根据tag的值从常量项表中找到对应的常量项结构
3. 第三步：根据常量项的结构，我们找出对应的字节数
4. 第四步：根据字节数，我们换算成下标
5. 第五步：去字节码进行验证

没错，小伙伴们只要按照这个套路，就能看懂关于常量池表部分的字节码，关于相同的我这里就不再举例了，咱们直接看比较特殊的，套路还是一样的套路，就是有些比较**特殊的常量项结构**需要单独拿出来**溜溜**


###### 例子四


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/14d0f4b65b3e449d983e8241035f6c4b~tplv-k3u1fbpfcp-watermark.image)

根据常量项的图可以知道，这个类型第一个字节是标识位，接下来两个字节是长度位，表示占用多少长度的字节，最后一个字节存的是字符串

我们来看下这个类型，在字节码中对应的是下方这一段

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/12742c409048463998baadff13d29cd0~tplv-k3u1fbpfcp-watermark.image)

- 首先01代表的是标志位 01换算成10进制就是1，找到对应的常量项
- 接下来**00 04**是表示字符串占用的字节数，也就是name字符串占用的字节数，**6E 61 6D 65**就是**name**

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7ac0a21fecf84d01a1b286fc6f6b03ab~tplv-k3u1fbpfcp-watermark.image)

- 最后的**01**就是存储长度为4个字节的字符串(name)


###### 例子五

还是这个常量项，我们来看下面这个图（用光标选中的部分）

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1b39b9eca5fa49c780057656cc5a36e1~tplv-k3u1fbpfcp-watermark.image)

同样的套路这里就不在说了，重点说下对应的值，直接看右边是LineNumberTable（行号表，记录字节码和源码行数的一一对应关系），这个在东西在我们的工作中挺常见的，大家在工作中遇到异常的时候，是不是对应源码的行数会告诉你，例如下面这个图所示


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1fd54443ba174583bd494866050c8da3~tplv-k3u1fbpfcp-watermark.image)

这个源代码行号就记录在LineNumberTable（行号表中）


###### 例子六

还是这个常量项，我们来看下面这个图（用光标选中的部分）


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/40430d749bb14a63afebd944dc5b0625~tplv-k3u1fbpfcp-watermark.image)

我们重点还是说下右边的值，发现这些字节码描述的常量项是**LocalVariableTable**，这个就是局部变量表，另外stack就是操作数栈的深度，locals就是局部变量表的深度，从这里也可以看出，**局部变量表的大小、深度，操作数栈的大小、深度**都是在编译的时候就可以确定了


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a3927f1e616647dca1a0376304c7c736~tplv-k3u1fbpfcp-watermark.image)

**问题**

这里其实有一个很奇怪的点，大家发现没有，就是上面截图的那部分其实是调用一个无参的构造方法，按照道理来说，是没有任何局部变量存在的，但是局部变量表的深度却是1

**答案**

因为有**this**关键字存在，这样是为什么可以在实例方法中使用this关键字的原因，在调用构造方法的时候，会把this作为一个隐式的入参传入，放在了局部变量表的第一个位置，所以即使没有入参，this关键字默认就占了一个位置。


#### 常量池表小结

关于常量池表的分析就先告一段落了，**套路**已经教给大家了，感兴趣的小伙伴可以自己一点点的看下来，这里就不再赘述了，关于常量池，我是把这玩意看成一个资源的仓库，下面字节码的操作都是对这个仓库进行引用，从这个仓库里面去找东西，这也是利用了一个**池化**的设计思想，这样可以大大的节省空间



#### 类的访问标志

再次回到字节码结构表，常量池表下面就是类的访问标志，我把类的访问标志图在下方贴了出来，一起来看下，怎么看这部分

![1618148149(1)_338447765.jpg](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/99fb837d687942e2a669feb6321db8e9~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6b210823b0884924b738f0721956e9e1~tplv-k3u1fbpfcp-watermark.image)


首先根据字节码结构表，发现它占用2个字节，我们往下数一下2个字节，发现是**00 21**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bf95c82ccdcb492aa5a57e31323c70b0~tplv-k3u1fbpfcp-watermark.image)

接下来在类的访问标志图中找对应的标志名称，结果发现没找到**00 21**，这里和大家重点说一下，这个**00 21**形成的可能是一种组合，我们找一下对应的组成，发现是**0X0001 和 0X0020**，它们相加就形成了**00 21**，对应的标志组合就是**ACC_PUBLIC和ACC_SUPER**，看下字节码验证一下：


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7a7f6f68046b4c5aa70c6825aa7885c9~tplv-k3u1fbpfcp-watermark.image)

发现和我们的预期一模一样，符合我们的判断


#### 类索引

在类的访问标志下方就是**类索引**，占2个字节，在字节码中找到是**00 03**，它的涵义是索引，所以我们就去常量池表中找索引为3的值，发现指向的就是测试类User的索引


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3d0fdac7e76c43e1a76ae499cd7a38a9~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1da75f9773214a36853c1973a719c29c~tplv-k3u1fbpfcp-watermark.image)


#### 父类索引

父类索引和类索引一样，这里就不再讲述


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/245a004d889b4f3796ebbdbe96c11adc~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/feed2f767025420cbd4cdaee9590ea22~tplv-k3u1fbpfcp-watermark.image)


#### 实现接口计数值

紧接着就到了实现接口计数器，表示一个接口被实现的类的个数，这里占用2个字节，**00 00**就代表着没有类实现，这里有一个点挺重要的，**JDK动态代理的时候，限制了类的实现接口数量要小于65535，这个65535就是从这里来的，1个字节就是8为，2个就是16位，最大值就是2的16次减1，等于65535**


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/48380aedfc3348deb41a819b040b5955~tplv-k3u1fbpfcp-watermark.image)

#### 实现接口结构表

##### 类字段计数值

就是用来计算类的字段个数，在测试类中是1个，字节码中也反映了1个


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/54f809c0deea44cb96aa988b5d11cfae~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/67ee7a42584541b087f800cd3494dd8f~tplv-k3u1fbpfcp-watermark.image)


#### 字段结构表

下方这张图就是字段结构表，我们根据这个字段结构表，开始分析类的字段

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/778dd2b70d0d4cd4947a77a3feb72e42~tplv-k3u1fbpfcp-watermark.image)


首先前两个2字节表示权限修饰符**00 02**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/400fa9bc1897457a87b95aeedd6766ce~tplv-k3u1fbpfcp-watermark.image)

我们找到**00 02**表示私有的，再结合类中的字段发现就是私有的

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b687d32625524988886e1eea8dd639a5~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d7b1b59e52084e15a832da3140ba7891~tplv-k3u1fbpfcp-watermark.image)

接下来2个字节表示字段名称索引，既然是索引，我们就直接在常量池中找到对应索引的常量项，也找到了对应的字段名称

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9ab4020889f44084826519adaa9f5cd6~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/70edab34e87442d7acc2bbe6472abcfb~tplv-k3u1fbpfcp-watermark.image)

接下来2个字节是字段描述索引，和名称类似，直接去常量池找，就是我们name字段的**字段类型是String类型**


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/29555064c6dc42d38f16d3e37eecbd68~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/97f1791655af490687958b983dab73b7~tplv-k3u1fbpfcp-watermark.image)


接下来2个字节是属性表个数，如果一个字段被**volatile等关键字修饰，这里属性表的个数就有值**，在案例中没有关键字修饰，这里的属性表个数就位0

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/28b2cdf0011f45b8b72526aa431cf698~tplv-k3u1fbpfcp-watermark.image)


#### 类方法计数器

讲完了字段结构表，就到了类方法计数器，占用2个字节，就是为了计数方法个数，在案例中是四个**构造方法、main方法、getName、setName**


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c1e85353b4a142d189926d20277787cd~tplv-k3u1fbpfcp-watermark.image)


#### 方法结构表

方法结构表贴在下图了，我们以第一个方法**构造方法**为例：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a8f81327b5d746d499aa2f3d04d74afb~tplv-k3u1fbpfcp-watermark.image)

首先前2个字节是**00 01**，我们根据访问权限表查出是**ACC_PUBLIC**


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d5aaa0d68e1e4c8c84ab2b48fca9618a~tplv-k3u1fbpfcp-watermark.image)

接下来2个字节是方法名称索引，去常量池找对应的常量项，

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/80ecfd7e2f184bc7935f83009df3c7de~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a11f4d869e68495fa6d5e910151c920d~tplv-k3u1fbpfcp-watermark.image)

发现执行的是init方法，接下来2个字节是方法的描述索引，继续去常量池找


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b84dedf03c4d43f8967b979a740d1f71~tplv-k3u1fbpfcp-watermark.image)

就是方法的返回值类型，案例中是void类型

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/490b4b8633a44525839d2bf8b5c4955f~tplv-k3u1fbpfcp-watermark.image)

再接下来就是方法属性表的个数，我们继续来看，还是占2个字节


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/21ed7021f6924e42828881dd01b3ae61~tplv-k3u1fbpfcp-watermark.image)

发现方法的属性表个数不为空，所以属性表个数下面的字节码就是属性表结构，**属性表结构我已经贴在下方了**，我们继续来跟下去

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dfbcd8dd0d994b01a9ea02930acf79f8~tplv-k3u1fbpfcp-watermark.image)

**00 0F**表示属性的名称索引，它指向索引为15的常量项，我们来跟一下

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c3509bba5bd040098ea79a2ba5884074~tplv-k3u1fbpfcp-watermark.image)

发现它指向了索引为code，而这个code，在**用javap编译出来的助记符文件中**有对应的区域

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/efcd35107b944b8ca20f4608f746eb50~tplv-k3u1fbpfcp-watermark.image)

结合属性表结构图和字节码文件就可以把下面这些内容一一解释出来，这里不再赘述

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e6dc375060bb4be0b6f119558ecd32b9~tplv-k3u1fbpfcp-watermark.image)

这里需要重点分析的是**Code_length**，表示字节码的长度，在案例中是**00 05**，我们往下数5个字节就是**Code**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3fa554dbf47c4e2aa931f4c90a0f352e~tplv-k3u1fbpfcp-watermark.image)

**这部分就是我们构造方法的指令**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f37ed5e6f91649cd8729da8785c086ca~tplv-k3u1fbpfcp-watermark.image)

也就是这三行指令，那我们怎么对应的上呢？可以**借助IDEA插件 jclasslib**，或者直接去翻官网文档

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1f47e97cc9a141deb9ce284283a0afef~tplv-k3u1fbpfcp-watermark.image)

发现**aload_0 对应的就是0X2a，也就是我们选中的字节码中第一个字节2A**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e2a51d9b5796401e80f9f36ce782855c~tplv-k3u1fbpfcp-watermark.image)


**invokespecial 对应的就是0Xb7，也就是我们选中的字节码中第一个字节B7**


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a61c6f4d7afd454a8d7f5914cc829e87~tplv-k3u1fbpfcp-watermark.image)

后面紧跟着**00 01**表示**B7指令操作的对象指向常量池中的#1位置**，

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d8d80ae637b4494698fcc6ea8626d9b6~tplv-k3u1fbpfcp-watermark.image)

最后的**B1**就是我们的**return指令**


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/86c1ea36dff54195b44ac4f4854b9db5~tplv-k3u1fbpfcp-watermark.image)

之后的**00 00**代表着异常表的长度，**00 00**表示该方法不抛出异常，所以**exception_info**也没有出现在这个字节码文件中

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/198f4154ddab4784956216b941f97609~tplv-k3u1fbpfcp-watermark.image)

接下来的**00 02**就是方法的属性表的个数，不为空，所以**attribute_info**也就不为空


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0d11e697397340318abe97afe7058381~tplv-k3u1fbpfcp-watermark.image)

**attribute_info**对应的结构体我贴在上面了，主要组成部分是**行号表**和**局部变量表**，**行号表中记录的就是映射对数、指令码的行数和源代码的行号，所以异常能抛出来源码的行号就是取的这里**

局部变量表的结构体我贴在下面了，也是根据结构体的示意图和字节码对照找出相应的含义


![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3d46eaa22e3249739035188abce9922d~tplv-k3u1fbpfcp-watermark.image)

方法的字节码就是根据这种套路来看，其余的三个方法这里就不一一介绍了，大家学完这个套路可以自己去尝试着看方法结构体对应的字节码。


#### class的属性数组长度和class的属性结构表

class的属性数组长度和class的属性结构表，这里就不带着大家跟下去了，套路还是一样的套路，含义可以根据下方的结构表中寻找，这里就不详细展开了

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5186cc60569449e98043905f7bc40ff8~tplv-k3u1fbpfcp-watermark.image)

# 本文总结

好啦，以上就是这篇文章的全部内容，都是围绕一个字节码文件展开的，内容很费眼睛，在日常生活中也接触不到，但是这个对开阔自己的眼界，我感觉还是非常有意思的一个方向，下面来回顾一下本文的重点：

1. **常量池表的查看过程**
2. **方法结构表的查看过程**

大家把重点放在这两块就可以了，其他的字节码部分很容易理解，感兴趣的同学可以多看几遍这两块内容，加深一下印象。

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得**小沙弥**我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取海量Java资源**好用的idea插件、简历模板、设计模式、多线程、架构、编程风格、中间件......**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！
