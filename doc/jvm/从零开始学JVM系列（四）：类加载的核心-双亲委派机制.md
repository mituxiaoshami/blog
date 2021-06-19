# 前言

在上篇文章中，从以下几个角度展开了对类加载器全面的介绍，**类加载器初始化的时机、类加载器加载类的方式、介绍各种五花八门的类加载器、各种类加载器加载文件的路径、类加载器的初始化过程**，看到这的小伙伴应该对类加载器有一个大体的了解，那么我在这问两个问题：

1. 为什么**不同的类加载器的读取类路径不同**
2. 每个类加载器的parent属性到底有什么，为什么要**设立这种父子关系**

其实这些问题的解释都可以通过一个机制来回答：就是**双亲委派机制**，它是类加载器加载类的一个**特点**，这个机制是由**Java层面的代码实现的**，所以这篇文章，我们就专门**盘盘**这个机制

# 双亲委派机制

## 双亲委派机制流程图

双亲委派机制总结起来就是一句话：**父加载器加载失败就有子类加载器自己加载**，但是这句话肯定很多小伙伴看到都会一脸懵逼，所以画个**流程图**来讲下双亲委派机制的流程：

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a2bf8867072d409cb332dc619b6bf5eb~tplv-k3u1fbpfcp-watermark.image)

从流程图上来看`应用类加载器`加载类首先会委托给`扩展类加载器`，当扩展类加载器加载类的时候会委托给`引导类加载器`(先不考虑自定义类加载器)

当`引导类加载器`加载失败的时候会交给子类`扩展类加载器`再此尝试加载，当扩展类加载器加载失败的时候会交给子类`应用类加载器`进行加载

从整体上来，这是一个`自下而上再而下`的这么一个过程

## 双亲委派机制源码分析

这么**干巴巴**的说，相信还有不少的小伙伴会懵逼，所以再结合源码来看看这个**双亲委派机制**，我们以TestJDKClassLoader类为例：

```java
public class TestJDKClassLoader {
    public static void main(String[] args) {
        system.out.println("类加载.......");
    }
}
```

首先，根据**上篇文章可知**，下图中的出现的**loader就是我们在初始化Launcher的时候赋值的AppClassLoader**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b01ca0ac3d664a03ab0a6bdea2085bbd~tplv-k3u1fbpfcp-zoom-1.image)

我们在平常调用类加载器加载类的时候通常调用的是ClassLoader里面的loadClass方法，所以我们就从`sun.misc.Launcher.AppClassLoader#loadClass`作为入口，好好看看具体的实现

```
        public Class<?> loadClass(String var1, boolean var2) throws ClassNotFoundException {
            // 无关代码，先不关注
            ......
            if (this.ucp.knownToNotExist(var1)) {
                // 无关代码，先不关注
                ......
                } else {
                    throw new ClassNotFoundException(var1);
                }
            } else {
                // 最后调用了父类的loadClass方法
                return super.loadClass(var1, var2);
            }
        }
```

`父类的loadClass方法`

```
// classLoader的loadClass方法，里面实现了双亲委派机制
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // 检查当前类加载器是否已经加载了该类
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    // 如果当前加载器的父加载器不为空则委托父类类加载器进行加载
                    if (parent != null) {
                        c = parent.loadClass(name, false);
                    } else {
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    long t1 = System.nanoTime();
                    // 都会调用URLClassLoader的findClass方法在加载器的类路径里查找并加载该类
                    c = findClass(name);
                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
```

双亲委派模式就是在这个loadClass方法中实现的，我们就一步步抽丝剥茧的来看这个方法

首先调用**本地方法findLoadedClass**，如果已经加载就返回该类，如果没有返回返回null，紧接着就在findLoadedClass外部判断，如果**不为null说明已经加载过**，就直接返回，第一次进来肯定没有加载过，所以这里**返回的一定是null**

```
    protected final Class<?> findLoadedClass(String name) {
        if (!checkName(name))
            return null;
        return findLoadedClass0(name);
    }
    private native final Class<?> findLoadedClass0(String name);
```

返回是null，说明这个类没有被加载过，那么就判断**当前类加载器的父加载器是否为空**，如果父加载器不为空，就让父加载器进行加载

```
// 如果当前加载器的父加载器不为空则委托父类类加载器进行加载
if (parent != null) {
   c = parent.loadClass(name, false);
}
```

**当前是AppClassLoader，父加载器是ExtClassLoader，所以这里肯定不为空，那么调用父加载器的loadClass方法**，`相当于是AppClassLoader委托给ExtClassLoader进行加载`

进入到父类加载器的**loadClass方法**之后，还是一样的**套路**，但是ExtClassLoader的父属性parent是null，所以进入到**findBootstrapClassOrNull方法**中，findBootstrapClassOrNull这个方法就**相当于委托给BootStrapLoader类加载器**


```
    // 如果当前加载器的父加载器不为空则委托父类类加载器进行加载
    if (parent != null) {
       c = parent.loadClass(name, false);
    } else {
       c = findBootstrapClassOrNull(name);
    }
    // 判断BootstrapClassLoader是否加载了该类，如果没有加载，直接返回null
    private Class<?> findBootstrapClassOrNull(String name)
    {
        if (!checkName(name)) return null;

        return findBootstrapClass(name);
    }
    // 如果没有找到，那么直接返回null
    private native Class<?> findBootstrapClass(String name);
```

`BootStrapLoader类加载器`开始加载，它会去对应的加载路径中去寻找，但是`TestJDKClassLoader类`是在我们的应用程序中，jdk的核心包里当然没有，所以必然会失败，返回null之后，还是在`ExtClassLoader类的loadClass方法中`，**相当于ExtClassLoader类加载器接受了BootStrapLoader委托**，紧接着进入到下面的这段代码中

```
                if (c == null) {
                    long t1 = System.nanoTime();
                    c = findClass(name);
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
```

ExtClassLoader类加载器接受BootStrapLoader委托，从lib\ext路径中去寻找**findClass方法就是寻找的实现**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a1d069c74a3142d7b9c61f7ac6c3abcc~tplv-k3u1fbpfcp-zoom-1.image)

整个findClass最核心的部分就是用红框框框起来的部分，**首先它会拼出类的路径（把"."替换成"/"，再后缀加上class），再根据这个类路径，从当前类加载器负责的类路径下开始寻找，如果寻找到就执行defineClass方法，如果没有就直接返回null**

显而易见，ExtClassLoader不会加载我们应用程序的`TestJDKClassLoader类`，最后返回的还是null

但是`c = parent.loadClass(name, false)`这行代码是一个嵌套方法，ExtClassLoader最后返回null后，会返回到AppClassLoader类**相当于是AppClassLoader类加载器接受了ExtClassLoader委托**，执行下面这段代码

```
                if (c == null) {
                    long t1 = System.nanoTime();
                    c = findClass(name);
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
```

AppClassLoader类加载器接受ExtClassLoader委托，从对应路径中去寻找**findClass方法就是寻找的实现**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a1d069c74a3142d7b9c61f7ac6c3abcc~tplv-k3u1fbpfcp-zoom-1.image)

最后执行`defineClass这个方法`，这个方法很重要，做的事情就是**类的加载过程**，**加载->验证->准备->解析->初始化**这五步，**我们在这里不继续跟进去了（如果跟进去要翻HotSpot 源码了，这一章中我们先讲双亲委派机制）**


**至此，就是我们完成的一个双亲委派机制的流程，从AppClassLoader开始委托给ExtClassLoader再委托给BootStrapLoader开始进行加载，BootStrapLoader如果加载不到再委托给ExtClassLoader，ExtClassLoader加载不到再委托给AppClassLoader，这样一个自下而上再而下的这么一个过程，再次对照下流程图，也符合**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a2bf8867072d409cb332dc619b6bf5eb~tplv-k3u1fbpfcp-watermark.image)


## 双亲委派机制流程带来的思考

### 思考一

**为什么类的加载器是自下而上进行委托，而不是由上到下或者其他顺序呢？** 因为如果直接从上到下开始加载只需要走一遍，如果从下到上再到下，就重复了一遍，那么这一遍到底有没有必要呢？

**其实这样保证了所有的类的加载流程都保持统一，都是从AppClassLoader类加载器开始进行加载**

### 思考二

**那JVM为什么要从AppClassLoader开始进行加载呢？为什么不直接返回BootStrapLoader类加载器？这样的话就避免了一次向上委托的流程了吗**

在我们的日常应用中，90%以上的类都是在classPath的目录下，虽然在第一次加载的时候，会重复一轮委托，但是如果有第二次、第三次、第四次等等重复加载一个类的时候（手动加载的场景），AppClassLoader判断如果已经加载的话就会直接返回，避免向上委托

如果从BootStrapLoader类加载器开始判断，那么无论重复几次，都需要由上往下走一遍，虽然第一次是快了，但是之后的每一次实际上都慢了

## 双亲委派机制的优缺点

说完了**双亲委派机制流程带来的思考**，那么就来看看这个机制有哪些优缺点吧

### 优点

1. **沙箱安全机制**：**不同的类由不同的类加载器进行加载，保护Java核心的类不被随意的修改**

2. **避免类的重复加载**：当父加载器已经加载了该类时，没有必要子类的classLoader再去加载一遍，**保证被加载的类的唯一性**


沙箱安全机制的场景：在应用程序下新建java.lang包，在包下面新建String类，在String类中运行main方法

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/931601e97f4e43319bc67f4a5a9c88bf~tplv-k3u1fbpfcp-zoom-1.image)

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c4ab040e94b1475db31dd9ed770a2ab4~tplv-k3u1fbpfcp-zoom-1.image)

分析输出结果：

**因为在双亲委派机制中，向上委托到BootStrapLoader类加载器的时候，发现java.lang.String在它扫描的路径下，所以它会去加载（只认文件名），但是它加载的不是我们自定义的String类，加载的是rt包下的String类，而java自带的String类是没有main方法的，这里就会报错，很好的保护了Java核心的类库不被随意的修改**

### 缺点

**双亲委派这种类的加载模式也不是适用于所有的场景**

举个例子：双亲委派的一个很鲜明的特点就是**相同路径下的类只会被加载一次**，但是在Tomcat中，如果一个Tomcat想部署多个应用，而这多个应用恰巧依赖了不同小版本之间的Spring，比如Spring4.1x、Spring4.2x，这两个微小版本的Spring肯定会有相同路径的类，但是如果使用双亲委派机制的类加载器，这两个相同路径下的类只会被加载一个，其中一个应用正好运用到了另外一个类的某些特性，所以必然会导致应用无法正常执行，所以**双亲委派机制在Tomcat场景中肯定不适用，就必须规避这种双亲委派机制**



## 自定义类加载器

既然要规避这种双亲委派机制，用**现有的类加载器**肯定是不行的，所以首先就是要**自定义类加载器**

自定义加载器只需要继承java.lang.ClassLoader类，这个类里面有两个核心的方法

- 一个是**loadClass方法**，这个方法中实现了我们的**双亲委派机制**
- 还有一个是**findClass方法**，默认是空实现，这个方法的定义就是根据路径找到我们的类并进行加载，打破双亲委派机制只需要**重写loadClass方法**就可以了

### 编写自定义类加载器

我们先只重写findClass方法，来说明为什么一定要重写loadClass方法

```java
public class MyClassLoaderTest {
    // 自定义类加载器一般都需要继承一个ClassLoader（有很多方法可以复用）
    static class MyClassLoader extends ClassLoader {
        // 自定义类加载器加载类的路径
        private String classpath;
        public MyClassLoader(String classpath) {
            this.classpath = classpath;
        }
        /**
         * 自定义读取文件方法
         * @param name 文件路径名
         * @return 读取的二进制数据
         * @throws Exception 一场
         */
        private  byte[] loadByte(String name) throws Exception {
            name = name.replaceAll("\\.", "/");
            FileInputStream fileInputStream = new FileInputStream(classpath + "/" + name + ".class");
            int len = fileInputStream.available();
            byte[] data = new byte[len];
            fileInputStream.read(data);
            fileInputStream.close();
            return data;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                // 自定义类的读取方法
                byte[] data = loadByte(name);
                // defineClass 方法只是类的加载，用原生的即可
                return defineClass(name, data, 0, data.length);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ClassNotFoundException();
            }
        }
    }
 
    public static void main(String[] args) throws Exception {
        // 初始化自定义类加载器，会先初始化父类ClassLoader，其中会把自定义类加载器的父加载器设置为应用程序类加载器AppClassLoader
        MyClassLoader classLoader = new MyClassLoader("D:/test");
        // D盘创建 test/classLoader/TestJDKClassLoader 将TestJDKClassLoader.class丢入该目录
        Class clazz = classLoader.loadClass("classLoader.TestJDKClassLoader");
        Object obj = clazz.newInstance();
        Method method = clazz.getDeclaredMethod("sout", null);
        method.invoke(obj, null);
        System.out.println(clazz.getClassLoader().getClass().getName());

    }
}
```

我们看下输出结果：

```
I can fly
sun.misc.Launcher$AppClassLoader
```

我们发现虽然编写了自定义的类加载器，但是TestJDKClassLoader类为什么是被**AppClassLoader类加载器加载的**，我们带着这个疑问来继续看

### 自定义类加载器的父类加载器

在上篇文章中有说道：每个类都有一个parent属性，那么**自定义类加载器的parent属性**存储的是什么呢？来看下源码：

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/67dbc918fe304364a7366e00f501467c~tplv-k3u1fbpfcp-zoom-1.image)

我们发现在初始化自定义类加载器的时候，由于它**继承ClassLoader**，所以会先去调用**父类的构造函数**，在父类的构造函数中，默认的塞了一个**系统类加载器**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bee90f90e4b34944a470387773cf7fa8~tplv-k3u1fbpfcp-zoom-1.image)

跟到**getSystemClassLoader方法**中，发现它调用的还是**Launcher.getClassLoader**，而这个方法返回的就是**AppClassLoader**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5012bb9233b04a9398ae21aa5b8a9e41~tplv-k3u1fbpfcp-zoom-1.image)

也就是说，**自定义类加载器的parent属性就是上面的AppClassLoader类加载器**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/06140514b9c548d3bfd96c5dde05dbb0~tplv-k3u1fbpfcp-zoom-1.image)

这一顿分析其实解释了上面的问题：

- 自定义的类加载器默认情况下的**父类是AppClassLoader**，由于没有重写loadClass方法，也就是说**没有打破双亲委派机制**，所以还是执行了**双亲委派模式**
- 在项目中没有把`TestJDKClassLoader.java`这个类给删除掉，AppClassLoader还是能在项目路径下读到**编译后的TestJDKClassLoader.class文件**

那么我们删除项目里的TestJDKClassLoader.java类，并且在指定的目录：**D:/test/classLoader下创建**TestJDKClassLoader.class文件，再次输出结果，发现就变成自定义类加载器

```
I can fly
classLoader.MyClassLoaderTest$MyClassLoader
```

注意：**虽然输出了自定义类加载器，但还是走了双亲委派模式，只是父类AppClassLoader再它的路径下找不到TestJDKClassLoader.class文件而已**，为了打破双亲委派模式，我们继续往下看：

### 打破双亲委派机制

为了打破双亲委派机制，还是需要重写loadClass方法，在loadClass方法中不委托给父类尝试着进行加载，直接在当前的类加载器进行加载，所以我们重写下loadClass

```java
public class MyClassLoaderTest {

    // 自定义类加载器一般都需要继承一个ClassLoader（有很多方法可以复用）
    static class MyClassLoader extends ClassLoader {
        private String classpath;
        public MyClassLoader(String classpath) {
            this.classpath = classpath;
        }
        /**
         * 自定义读取文件方法
         * @param name 文件路径名
         * @return 读取的二进制数据
         * @throws Exception 一场
         */
        private  byte[] loadByte(String name) throws Exception {
            name = name.replaceAll("\\.", "/");
            FileInputStream fileInputStream = new FileInputStream(classpath + "/" + name + ".class");
            int len = fileInputStream.available();
            byte[] data = new byte[len];
            fileInputStream.read(data);
            fileInputStream.close();
            return data;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    // 发现如果当前类加载器没有加载过，那么就去加载
                    c = findClass(name);
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                // 自定义类的读取方法
                byte[] data = loadByte(name);
                // defineClass 方法只是类的加载，用原生的即可
                return defineClass(name, data, 0, data.length);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ClassNotFoundException();
            }
        }
    }
    public static void main(String[] args) throws Exception {
        // 初始化自定义类加载器，会先初始化父类ClassLoader，其中会把自定义类加载器的父加载器设置为应用程序类加载器AppClassLoader
        MyClassLoader classLoader = new MyClassLoader("D:/test");
        // D盘创建 test/classLoader/TestJDKClassLoader 将TestJDKClassLoader.class丢入该目录
        Class clazz = classLoader.loadClass("classLoader.TestJDKClassLoader");
        Object obj = clazz.newInstance();
        Method method = clazz.getDeclaredMethod("sout", null);
        method.setAccessible(true);
        method.invoke(obj, null);
        System.out.println(clazz.getClassLoader().getClass().getName());

    }
}
```

运行应用程序后发现报了下图中的错

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b26052ad0ae24b6f8c33b6fb9667a086~tplv-k3u1fbpfcp-zoom-1.image)

- 一方面：因为在java中，默认情况下每个类都会继承**Object类**，但是**Object.class**文件是不存在**test/classLoader**目录下

- 另一方面：Java也不会允许核心的包用自定义的类加载器加载**要是不信可以自己把Object.class拖出来放到对应目录下，会报安全错误，这里就不贴图了**

#### 过滤特殊的类

为了能让Object类加载，我们还得在loadClass做一个简单的过滤，保证特定的类 **(类似Object)** 还是走双亲委派机制，自己应用程序的类就打破双亲委派机制：

```
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    // 如果是指定目录下的文件，那么就打破双亲委派机制
                    if (name.startsWith("classLoader")) {
                        c = findClass(name);
                    } else {
                        // 否则就走双亲委派机制
                        c = this.getParent().loadClass(name);
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }
```

我们运行一下看看：

```
I can fly
classLoader.MyClassLoaderTest$MyClassLoader
```

可以看到即使在项目中存在了classLoader.MyClassLoaderTest类，还是使用了自己的类加载器加载，没用父类的AppClassLoader类加载器，证明已经打破了双亲委派机制


### 打破双亲委派机制的场景

打破双亲委派机制的场景有不少，典型的就是Tomcat

 1. 一个Tomcat可能部署多个应用，不同的应用可能依赖的同一个第三方类库的不同版本（会造成很多大量的文件路径相同的类），这种情况下就不能通过双亲委派机制去加载，要保证每个应用的类库是独立的，相互隔离

 2. web容器要支持jsp修改，jsp文件最终也需要编译成class文件才能在虚拟机中运行，但程序运行后修改jsp是一件高频的事情，web容器需要支持jsp修改后无需重启

 3. ......**还有很多场景，需要大家自己了解**

# 本文总结

好啦，以上就是这篇文章的全部内容了，大致上可以划分成以下几块内容

1. 结合源码来分析**双亲委派机制流程**
2. 说说**双亲委派机制带来的思考**，以及它的优缺点
3. 如何**自定义类加载器**，需要注意哪些点
4. **如何打破双亲委派机制**

看到这，我相信各位看官都能回答篇头的两个问题：

- **为什么不同的类加载器的读取类路径不同？让不同的类由不同的类加载器进行加载，可以保护Java核心的类不被随意的修改**

- 每个类加载器的parent属性到底有什么，**为什么要设立这种父子关系？设立父子关系的目的还是实现双亲委派机制，具体可以看它的优点**


关于类加载也就告一段落了，但是这不是类加载的**终点**，后面会出**Tomcat系列的文章**，其中有一块就是**自定义类加载器在Tomcat中的应用**，这里就不适合详细展开，小伙伴可以期待一下

类加载的目的是把**类的字节码文件加载到JVM运行时数据区变成可以直接用的类元信息**，但是JVM运行时数据区存的不仅仅是**类元信息**对吗？所以下篇文章就来分析一个对象是怎么进入到JVM运行时数据区，到底存在了哪里？

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的非常有用！！！如果想获取**电子书《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）周志明》**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！