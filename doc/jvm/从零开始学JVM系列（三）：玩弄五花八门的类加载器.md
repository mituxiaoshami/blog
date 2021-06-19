# 前言

在上一篇中，从源码的角度分析了**JVM的启动过程**和**类加载过程**，从细节上也分析一个完整的类加载经历了哪几个步骤，以及各个步骤的细节，但是小伙伴们知道**类加载器**是从什么时候**开始初始化的吗？类加载器加载类的方式是什么？类加载器的种类有哪些？各自的加载路径是什么？**

带着这些问题，我用一篇文章的篇幅从以下五个环节全面的介绍类加载器：

- **类加载器初始化的时机**
- **类加载器加载类的方式**
- **介绍各种五花八门的类加载器**
- **各种类加载器加载文件的路径**
- **从源码的角度分析类加载器的初始化过程**


# 第一个环节：类加载器初始化的时机

在上文中，我们有解析到JVM虚拟机启动，需要`使用类加载器加载main方法所在的类`，那么就可以判定在这之前类加载器就已经初始化完成，那我们就往前再退一步，看看在`初始化JVM的时候`有没有初始化类加载器：

## 初始化JVM

**JavaMain方法 在jdk/src/java.base/share/native/libjli/java.c目录**

```
int JNICALL JavaMain(void * _args) {
    // 无用代码先忽略
    ......
    // InitializeJVM 初始化JVM，给JavaVM和JNIEnv对象正确赋值，通过调用InvocationFunctions结构体下的CreateJavaVM()函数指针来实现
    // 该指针在LoadJavaVM()函数中指向libjvm.so动态链接库中JNI_CreateJavaVM()函数
    if (!InitializeJVM(&vm, &env, &ifn)) {
        JLI_ReportErrorMessage(JVM_ERROR1);
        exit(1);
    }
    // 无用代码，先忽略
    ......
}
```

**InitializeJVM方法**

```
static jboolean
InitializeJVM(JavaVM **pvm, JNIEnv **penv, InvocationFunctions *ifn)
{
    // 无用代码，先忽略
    ......
    r = ifn->CreateJavaVM(pvm, (void **)penv, &args); //通过ifn的函数指针 调用CreateJavaVM函数初始化JavaVM 和 JNIEnv
    JLI_MemFree(options);
    return r == JNI_OK;
}
```

**CreateJavaVM方法**

```
_JNI_IMPORT_OR_EXPORT_ jint JNICALL JNI_CreateJavaVM(JavaVM **vm, void **penv, void *args) {
 
  //通过Atomic::xchg方法修改全局volatile变量vm_created为1，该变量默认为0，如果返回1则说明JVM已经创建完成或者创建中，返回JNI_EEXIST错误码，如果返回0则说明JVM未创建
  if (Atomic::xchg(1, &vm_created) == 1) {
    return JNI_EEXIST;   // already created, or create attempt in progress
  }
  //通过Atomic::xchg方法修改全局volatile变量safe_to_recreate_vm为0，该变量默认为1，如果返回0则说明JVM已经在重新创建了，返回JNI_ERR错误码，如果返回1则说明JVM未创建
  if (Atomic::xchg(0, &safe_to_recreate_vm) == 0) {
    return JNI_ERR;
  }
 
  assert(vm_created == 1, "vm_created is true during the creation");
  
  bool can_try_again = true;
  //完成JVM的初始化，如果初始化过程中出现不可恢复的异常则can_try_again会被置为false
  result = Threads::create_vm((JavaVMInitArgs*) args, &can_try_again);
  // 无用代码，先忽略
  ......
}
```

**Threads::create_vm方法**

注意：这是**初始化JVM最重要的一个方法，非常复杂**，这里先省略部分代码，**只关注类加载的部分**，至于初始化JVM的HotSpot源码分析以后会单独开一个系列

```
jint Threads::create_vm(JavaVMInitArgs* args, bool* canTryAgain) {
 
  // 无用代码，先不关注
  ......
  
  // 最终系统初始化，包括安全管理器和系统类加载器
  call_initPhase3(CHECK_JNI_ERR);
  // 完成SystemClassLoader的加载
  SystemDictionary::compute_java_system_loader(THREAD);
  if (HAS_PENDING_EXCEPTION) {
    vm_exit_during_initialization(Handle(THREAD, PENDING_EXCEPTION));
  }
  
  // 无用代码，先不关注
  .......
}
```

**SystemDictionary::compute_java_system_loader方法**

`call_initPhase3方法`初始化的是系统类加载器，Java层面的在`compute_java_system_loader方法`后就初始化完成了，

```
void SystemDictionary::compute_java_system_loader(TRAPS) {
  KlassHandle  system_klass(THREAD, WK_KLASS(ClassLoader_klass));
  JavaValue    result(T_OBJECT);
  // 调用java.lang.ClassLoader类的getSystemClassLoader()方法
  JavaCalls::call_static(&result, // 调用Java静态方法的返回值存储在result中
                         KlassHandle(THREAD, WK_KLASS(ClassLoader_klass)), // 调用的目标类为java.lang.ClassLoader
                         vmSymbols::getSystemClassLoader_name(), // 调用目标类中的目标方法为getSystemClassLoader
                         vmSymbols::void_classloader_signature(), // 调用目标方法的方法签名
                         CHECK);
  // 获取调用getSystemClassLoader()方法的结果并保存到_java_system_loader属性中
  _java_system_loader = (oop)result.get_jobject();  // 初始化属性为系统类加载器/应用类加载器/AppClassLoader
}
```
通过**JavaClass::call_static方法**调用`java.lang.ClassLoader类的getSystemClassLoader方法`

**JavaClass::call_static方法**非常重要，它是`HotSpot调用Java静态方法的API`


**java.lang.ClassLoader类的getSystemClassLoader方法 注意：这是Java层面的代码**

```
@CallerSensitive
    public static ClassLoader getSystemClassLoader() {
        initSystemClassLoader();
        if (scl == null) {
            return null;
        }
        // 无关紧要的代码，先不关注
        ......
        return scl;
    }
```

**initSystemClassLoader方法**

```
    private static synchronized void initSystemClassLoader() {
        if (!sclSet) {
            if (scl != null)
                throw new IllegalStateException("recursive invocation");
            // 开始进行Java层面类加载器的初始化
            sun.misc.Launcher l = sun.misc.Launcher.getLauncher();
            // 无关紧要的代码，先不关注
            ......
        }
    }
```

我们重点来看下`sun.misc.Launcher l = sun.misc.Launcher.getLauncher()`这行代码，这行代码就是执行Java层面类加载器的入口

**sun.misc.Launcher.getLauncher方法**

发现sun.misc.Launcher.getLauncher直接返回`launcher`变量，这个变量又在Launcher类加载的最后一步`初始化阶段`已经创建好实例，所以我们去`Launcher类的无参构造函数`中去看看

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2917c26786dc4144b96c1d5b7f86a68d~tplv-k3u1fbpfcp-watermark.image)

**new Launcher方法**

```
    public Launcher() {
        Launcher.ExtClassLoader var1;
        try {
            // 初始化ExtClassLoader类加载器
            var1 = Launcher.ExtClassLoader.getExtClassLoader();
        } catch (IOException var10) {
            throw new InternalError("Could not create extension class loader", var10);
        }

        try {
            // 初始化AppClassLoader类加载器，并把loader变量指向成AppClassLoader类加载器
            this.loader = Launcher.AppClassLoader.getAppClassLoader(var1);
        } catch (IOException var9) {
            throw new InternalError("Could not create application class loader", var9);
        }
        
        // 无关紧要的代码，先不关注
        ......

    }
```

至此，我们类加载器的初始化动作就已经全部完成，总体上可以划分成如下两块：

1. JVM层面：会创建一个系统的类加载器，就是下面要说的`Bootstrap ClassLoader`
2. Java层面：会创建两个应用层面的类加载器`ExtClassLoader`和`AppClassLoader`

我们的`main方法所在的类`就是用`初始化出来的AppClassLoader类加载器`加载的，目前，我们只需要知道存在着五花八门的类加载器就可以了，至于为什么会出现这么多类加载器，我会在下一篇文章中，借着`双亲委派机制`说明原因


# 第二个环节：类加载器加载类的方式

在第一个环节，我们看到了各种`不同的类加载器`以及它们`初始化的时机`，加载器的作用就是用来加载类的，在咱们平时的项目中，经常会用到各种第三方库`包括各种框架、工具类、sdk等等`，想象一下，如果在项目启动的时候，把这些类一股脑加载到JVM内存中，这不得把JVM给`挤爆了`，可能直接抛出OOM异常，所以JVM一定是用了一种`特殊的方式`避免这个问题，这个环节就是讲这个`特殊的方式`

## 加载类的方式：懒加载

**懒**体现在：JVM不会在项目启动的时候，就把`程序涉及到的类全部一股脑加载`，等到程序需要用到哪个类，就加载哪个类，就是`按需加载`，这样会大大节省了JVM内存空间，或者`说细一点是大大节省了方法区的空间`

## 证明JVM的加载是懒加载

```java

public class LazyLoading {
    static {
        System.out.println("*********** loading TestLazyLoading class ***********");
    }
    public static void main(String[] args) {
        new TestA();
        System.out.println("*************** loading test ************");
        TestB testB = null;
    }
}
// 测试用的A类
class TestA {
    static {
        System.out.println("********* load TestA **********");
    }
    public TestA() {
        System.out.println("*********** initial A **************");
    }
}
// 测试用的B类
class TestB {
    static {
        System.out.println("********* load TestB **********");
    }
    public TestB() {
        System.out.println("*********** initial B **************");
    }
}
```

我们分析一下加载步骤：

- 第一步：根据上篇文章中的`HotSpot启动过程`可知，HotSpot启动后首先会加载main方法所在的`TestLazyLoading类`，类加载的最后一步是**初始化**，其中就会**执行静态代码块**，所以首先会输出`loading TestLazyLoading class`

- 第二步：加载完`TestLazyLoading类`，就会执行`main方法`，会去`new TestA()`，所以肯定会加载TestA这么一个类，同样最后一步`初始化`中会去执行`静态代码快`，输出`load TestA`，接着再执行构造方法，输出`initial A`

- 第三步：为了方便观察打印结果，我们直接输出**loading test**

- 第四步：定义了`TestB类`，但是我们给它赋值的是空值，说明`没用到TestB类`，自然也不会打印`load TestB`和`initial B`


来看一下输出结果，也符合我们的分析，证明了类加载的方式是一个**懒加载的方式**

```
Connected to the target VM, address: '127.0.0.1:62820', transport: 'socket'
*********** loading TestLazyLoading class ***********
********* load TestA **********
*********** initial A **************
*************** loading test ************
Disconnected from the target VM, address: '127.0.0.1:62820', transport: 'socket'
```


# 第三个环节：介绍各种五花八门的类加载器

## 类加载器

类的加载是通过**类加载器**来实现的，在Java的世界中，**不同路径下的类使用了不同的类加载器**进行加载，Java自带的类加载器包括**引导类加载器**、**扩展类加载器**、**应用程序类加载器**

 - 引导类加载器：由JVM虚拟机实现，负责加载支撑JVM运行的位于JRE的lib目录下的核心类库，比如rt.jar、charsets.jar（字符集）等等

 - 扩展类加载器`ExtClassLoader`：负责加载**支撑JVM运行**的位于JRE的lib目录下的ext扩展目录中的JAR类包

 - 应用程序类加载器`AppClassLoader`：负责**加载classPath路径下的类包**，主要加载的就是你自己写的类

 - 自定义加载器：负责加载**用户自定义路径下的类包**

写一个测试类来看看我们的各种类加载器长什么样子：

```java
public class JDKClassLoader {
    public static void main(String[] args) {
        System.out.println(String.class.getClassLoader());
        System.out.println(com.sun.crypto.provider.DESKeyFactory.class.getClassLoader());
        System.out.println(JDKClassLoader.class.getClassLoader());
    }
}
```

- String类在Java中是**属于核心类**
- AESKeyGenerator这个类是在**JRE的lib目录下的ext扩展目录**中随便找的一个类
- JDKClassLoader是**应用程序的类**

```
Connected to the target VM, address: '127.0.0.1:63102', transport: 'socket'
null
sun.misc.Launcher$ExtClassLoader@1b0375b3
sun.misc.Launcher$AppClassLoader@18b4aac2
Disconnected from the target VM, address: '127.0.0.1:63102', transport: 'socket'
```

- **第二行和第三行**输出内容的不同证明了**不同路径下的类加载器不同**
- 无论是**ExtClassLoader**还是**AppClassLoader**其实前缀都是**sun.misc.Launcher**这个类，进入Launcher源码中可以看到`ExtClassLoader`和`AppClassLoader`都是**Launcher类的内部类**
- java的**核心类库**都是通过`引导类加载器`加载，但是这个类加载器是用C++语言写的，生成的自然是`C++的对象，而不是java对象`，C++生成的对象，Java自然获取不到，返回的就是null



# 第四个环节：各种类加载器加载文件的路径

```java
public class JDKClassLoader {
    public static void main(String[] args) {
        System.out.println("bootstrapLoader加载以下文件：");
        URL[] urls = Launcher.getBootstrapClassPath().getURLs();
        for (URL url : urls) {
            System.out.println(url);
        }
        System.out.println();
        System.out.println("extClassLoader加载以下文件：");
        System.out.println(System.getProperty("java.ext.dirs"));

        System.out.println();
        System.out.println("appClassLoader加载以下文件：");
        System.out.println(System.getProperty("java.class.path"));
    }
}
```

运行一下看下输出结果：

```
bootstrapLoader加载以下文件：
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/resources.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/rt.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/sunrsasign.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jsse.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jce.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/charsets.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jfr.jar
file:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/classes

extClassLoader加载以下文件：
/Users/zhouxinze/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java

appClassLoader加载以下文件：
/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/lib/tools.jar:/Users/zhouxinze/IdeaProjects/java_basic/out/production/java_basic:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar:/Users/zhouxinze/Library/Caches/JetBrains/IntelliJIdea2020.3/captureAgent/debugger-agent.jar
```

分析输出结果：

- `bootStrapLoader`读取的是jre目录下最核心的包
- `extClassLoader`读取的是lib\ext目录下的文件
- `appClassLoader`读取的是应用程序java文件的编译输出路径


# 第五个环节：类加载器的初始化过程

我们在第一个环节中分析过，JVM通过**JavaClass::call_static方法**调用`java.lang.ClassLoader类的getSystemClassLoader方法`初始化各种类加载器，**Launcher类的实例在初始化过程中就会创建各种不同的java层面的类加载器**，我们直接看源码：


`java.lang.ClassLoader类的getSystemClassLoader方法`

```
@CallerSensitive
    public static ClassLoader getSystemClassLoader() {
        // 初始化类加载器
        initSystemClassLoader();
        // 先忽略，不关注
        ......
    }
```

`java.lang.ClassLoader类的initSystemClassLoader方法`

```
    private static synchronized void initSystemClassLoader() {
        if (!sclSet) {
            if (scl != null)
                throw new IllegalStateException("recursive invocation");
            // Launcher类的实例在初始化过程中就会创建各种不同的java层面的类加载器
            sun.misc.Launcher l = sun.misc.Launcher.getLauncher();
            // 先忽略 不关注
            ......
    }
```

`Launcher类的getLauncher方法`

```

// 发现在Launcher类加载的最后一步初始化的时候就已经创建
private static Launcher launcher = new Launcher();

public static Launcher getLauncher() {
        return launcher;
}
```

`Launcher类的new Launcher方法`

```
    public Launcher() {
        Launcher.ExtClassLoader var1;
        try {
            // 创建ExtClassLoader类加载器实例
            var1 = Launcher.ExtClassLoader.getExtClassLoader();
        } catch (IOException var10) {
            throw new InternalError("Could not create extension class loader", var10);
        }

        try {
            // 创建AppClassLoader类加载器实例
            this.loader = Launcher.AppClassLoader.getAppClassLoader(var1);
        } catch (IOException var9) {
            throw new InternalError("Could not create application class loader", var9);
        }
        // 先忽略，不关注
        ......

    }
```

1. 在无参构造函数中，通过`Launcher.ExtClassLoader.getExtClassLoader方法`来创建ExtClassLoader类加载器实例

2. 通过`Launcher.AppClassLoader.getAppClassLoader(ExtClassLoader extClassLoader)方法`来创建AppClassLoader类加载器实例

## 初始化ExtClassLoader类加载器

`Launcher.ExtClassLoader.getExtClassLoader方法`

```
public static Launcher.ExtClassLoader getExtClassLoader() throws IOException {
            if (instance == null) {
                Class var0 = Launcher.ExtClassLoader.class;
                // 加了类级别的锁，保证同步
                synchronized(Launcher.ExtClassLoader.class) {
                    if (instance == null) {
                        instance = createExtClassLoader();
                    }
                }
            }

            return instance;
}
```

- 实际上我们发现，创建ExtClassLoader实例就是**new ExtClassLoader**，当然前面还有各种安全控制检查**AccessController.doPrivileged**这里不再展开

```
private static Launcher.ExtClassLoader createExtClassLoader() throws IOException {
            try {
                // 做一些安全校验
                return (Launcher.ExtClassLoader)AccessController.doPrivileged(new PrivilegedExceptionAction<Launcher.ExtClassLoader>() {
                    public Launcher.ExtClassLoader run() throws IOException {
                        // 先忽略，不关注
                        ......
                        // 创建ExtClassLoader类加载器
                        return new Launcher.ExtClassLoader(var1);
                    }
                });
            } catch (PrivilegedActionException var1) {
                throw (IOException)var1.getException();
            }
        }
```

## 初始化AppClassLoader类加载器

`Launcher类的getAppClassLoader方法`

```
public static ClassLoader getAppClassLoader(final ClassLoader var0) throws IOException {
            final String var1 = System.getProperty("java.class.path");
            final File[] var2 = var1 == null ? new File[0] : Launcher.getClassPath(var1);
            return (ClassLoader)AccessController.doPrivileged(new PrivilegedAction<Launcher.AppClassLoader>() {
                public Launcher.AppClassLoader run() {
                    URL[] var1x = var1 == null ? new URL[0] : Launcher.pathToURLs(var2);
                    return new Launcher.AppClassLoader(var1x, var0);
                }
            });
        }
```

- 创建AppClassLoader实例就是**new AppClassLoader**，同样的，也有各种安全控制检查
- 调用**Launcher.AppClassLoader.getAppClassLoader(ExtClassLoader extClassLoader)** 的时候我们把扩展类加载器实例传进去了，我们跟随着这个参数来看看，发现它最终进入到了`ClassLoader类的构造方法中`

```
// The parent class loader for delegation
// Note: VM hardcoded the offset of this field, thus all new fields
// must be added *after* it.
private final ClassLoader parent;
    
private ClassLoader(Void unused, ClassLoader parent) {
        this.parent = parent;
        if (ParallelLoaders.isRegistered(this.getClass())) {
            parallelLockMap = new ConcurrentHashMap<>();
            package2certs = new ConcurrentHashMap<>();
            domains =
                Collections.synchronizedSet(new HashSet<ProtectionDomain>());
            assertionLock = new Object();
        } else {
            // no finer-grained lock; lock on the classloader instance
            parallelLockMap = null;
            package2certs = new Hashtable<>();
            domains = new HashSet<>();
            assertionLock = this;
        }
    }
```

- 传进去的**ExtClassLoader extClassLoader**赋值给了这个**parent**属性，这个**parent**属性在**ClassLoader类**，所有的类加载器都继承ClassLoader类，也就是说，所有的类加载器都有**parent属性**，同时也说明了AppClassLoader类的加载器有一个父类的类加载器属性是**ExtClassLoader**

### 衍生出来的问题

既然所有的类加载器都有一个`parent属性`，那么ExtClassLoader应该也有呀，所以我们来看看，`ExtClassLoader的parent属性`是什么？


我们追踪到ExtClassLoader的构造函数，发现他传入的父类类加载器是null，**这是因为引导类的类加载器是用C++写的，无法通过Java层面来获取到，所以这里就传了null**
![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9b6e72bd52f34aa089e3d2e5c8fdbe2b~tplv-k3u1fbpfcp-zoom-1.image)


# 花絮

还记得上面那个打印各个类加载器的读取文件路径的例子吗？

细心的小伙伴肯定会注意到，应用程序类加载器**AppClassLoader**打印的路径既有引导类加载器**bootStrapLoader**负责的读取路径，也有扩展类加载器**extClassLoader**负责的读取路径，也就是说它打印了不属于它加载的文件的路径？

这就涉及到了类加载中的**双亲委派机制**，这里这个**知识点**比较独立，所以在本篇中就不在讲述**双亲委派机制**，在下篇文章中，会好好讲讲这个**双亲委派机制**，这里大家先忍耐一下。

# 本文总结

好啦，以上就是这篇文章的全部内容了，我们一起来回忆一下：

- 第一步：从HotSpot源码的角度解析了**类加载器初始化的时机**
- 第二步：证明**类加载器加载类的方式**是懒加载
- 第三步：介绍了**各种五花八门的类加载器**，并证明不同路径下的类由不同的类加载器加载
- 第四步：打印了**各种类加载器加载文件的路径**
- 第五步：**从JDK源码的角度分析类加载器的初始化过程**，以及每个类都有的parent属性

关于类加载器的介绍就先到这，其实还剩下最后一个也是**最重要的知识点**：类加载器加载类的**双亲委派机制**，我把它单独放到下篇文章中，这里就先卖个关子


# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的非常有用！！！如果想获取**电子书《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）周志明》**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！
