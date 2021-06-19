# 前言

在日常编码中，我们编写的代码文件通常都是以 **.java**为后缀，这里的代码是给应用开发人员阅读并理解，但不被JVM**接受**，所以想要代码运行在JVM上，就必须要转换成JVM**认识**的代码

这样就要进行一步**编译**的过程，把 **.java**为后缀的代码文件编译成 **.class**为后缀的文件，**.class**文件才能被JVM**认识**并运行

而类加载就是把 **.class**字节码文件加载到**内存**，并对文件里面的数据进行**检验、转换解析和初始化**，最终形成可以**被虚拟机直接使用的信息**，这就是虚拟机的类加载机制。

了解java的类加载机制，可以**快速解决运行时的各种加载问题并快速定位其背后的本质原因**，也是解决疑难杂症的利器，那么下面就开始今天的内容：**《硬邦邦的剖析JVM类加载的过程》**


# JVM的启动过程

在讲类加载之前，我们先看一下**JVM的启动过程**，类加载只是**JVM启动过程中的一环**，我们从整个启动的流程上去看类加载处在一个什么样的位置，先留下一个总体的认识，之后再去扣**类加载细节**


## 编写测试代码

先编写一个Math类，其中有一个main方法，使用new关键字创建了Math类的对象，并在main方法中调用对象的compute方法：

```JAVA

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
    }
}
```

## 启动测试代码

使用ide启动，底层其实就是用java命令运行Math类的main函数启动程序，所以使用ide启动和java命令启动，本质是一样的，接下来就看Math类的总体运行过程是怎么样的。

## JVM启动过程源码分析

这一小节开始分析**JVM启动过程的HotSpot源码和类加载的HotSpot源码**，感兴趣的同学可以细看，如果觉得枯燥的或者一下子接受不了的，可以直接跳过，我会把接下来的源码过程，画一张**流程图，方便大家理解**，大家可以根据图也能理解这一整个JVM的启动过程和类加载过程

### 找到启动入口

**jdk/src/java.base/share/native/launcher/main.c**

这是java命令启动入口，在main()函数前打上断点，开始跟踪JVM的启动

```
int main(int argc, char **argv){
    int margc;
    char** margv;
    const jboolean const_javaw = JNI_FALSE;
    margc = argc;
    margv = argv;
    // 程序执行到main函数末尾，调用了JLI_Launch方法
    return JLI_Launch(margc, margv,
                   sizeof(const_jargs) / sizeof(char *), const_jargs,
                   sizeof(const_appclasspath) / sizeof(char *), const_appclasspath,
                   FULL_VERSION,
                   DOT_VERSION,
                   (const_progname != NULL) ? const_progname : *margv,
                   (const_launcher != NULL) ? const_launcher : *margv,
                   (const_jargs != NULL) ? JNI_TRUE : JNI_FALSE,
                   const_cpwildcard, const_javaw, const_ergo_class);
}
```

### 初始化准备

**JLI_Launch方法，在jdk/src/java.base/share/native/libjli/java.c目录下**

```
//由main.c的main()函数调用
int JLI_Launch(int argc, char ** argv,              /* main argc, argc */
        int jargc, const char** jargv,          /* java args */
        ......                             
) {
  // 一些初始化之前准备数据的代码，先不关注，先关注整个的流程
  ......
 // 准备初始化JVM
 return JVMInit(&ifn, threadStackSize, argc, argv, mode, what, ret);
}
```

**JVMInit方法，在jdk/src/java.base/unix/native/libjli/java_md_solinux.c目录下**

```
int JVMInit(InvocationFunctions* ifn, jlong threadStackSize,
        int argc, char **argv,
        int mode, char *what, int ret)
{
    // 先忽略，关注整个的流程
    ......
    //调用 ContinueInNewThread方法
    return ContinueInNewThread(ifn, threadStackSize, argc, argv, mode, what, ret);
}
```

**ContinueInNewThread方法，在jdk/src/java.base/share/native/libjli/java.c目录下**

```
ContinueInNewThread(InvocationFunctions* ifn, jlong threadStackSize,
                    int argc, char **argv,
                    int mode, char *what, int ret)
{
   //设置线程栈大小
   if (threadStackSize == 0) {
      struct JDK1_1InitArgs args1_1;
      memset((void*)&args1_1, 0, sizeof(args1_1));
      args1_1.version = JNI_VERSION_1_1;
      ifn->GetDefaultJavaVMInitArgs(&args1_1);  /* ignore return value */
      if (args1_1.javaStackSize > 0) {
         threadStackSize = args1_1.javaStackSize;
      }
    }
    // 创建一个新线程去创建JVM，调用JavaMain
    { /* Create a new thread to create JVM and invoke main method */
      JavaMainArgs args;
      int rslt;
      args.argc = argc;
      ......
      args.ifn = *ifn;
      // 调用ContinueInNewThread0函数，传递JavaMain函数指针和调用此函数需要的参数args
      rslt = ContinueInNewThread0(JavaMain, threadStackSize, (void*)&args);
}
```

### 寻找main函数并执行

**ContinueInNewThread0方法，在jdk/src/java.base/unix/native/libjli/java_md_solinux.c目录**

```
int ContinueInNewThread0(int (JNICALL *continuation)(void *), jlong stack_size, void * args) {
    int rslt;
    #ifndef __solaris__
    ......
    //创建线程
    if (pthread_create(&tid, &attr, (void *(*)(void*))continuation, (void*)args) == 0) {
      void * tmp;
      pthread_join(tid, &tmp);
      rslt = (int)(intptr_t)tmp;
    } else {
      // 调用JavaMain方法 
      // 方法的第一个参数int (JNICALL continuation)(void )接收的就是JavaMain函数的指针
      // 所以下方continuation方法就是JavaMain函数
      rslt = continuation(args);
    }
    // 忽略后续代码
    ......
    return rslt;
```

**JavaMain方法 在jdk/src/java.base/share/native/libjli/java.c目录**

```
int JNICALL JavaMain(void * _args) {
    JavaMainArgs *args = (JavaMainArgs *)_args;
    ......
    InvocationFunctions ifn = args->ifn;
    start = CounterGet();
    // InitializeJVM 初始化JVM，给JavaVM和JNIEnv对象正确赋值，通过调用InvocationFunctions结构体下的CreateJavaVM()函数指针来实现
    // 该指针在LoadJavaVM()函数中指向libjvm.so动态链接库中JNI_CreateJavaVM()函数
    if (!InitializeJVM(&vm, &env, &ifn)) {
        JLI_ReportErrorMessage(JVM_ERROR1);
        exit(1);
    }
    ......
   //加载Math类
   mainClass = LoadMainClass(env, mode, what); 
   appClass = GetApplicationClass(env);
   //获取Math类的main方法
   mainID = (*env)->GetStaticMethodID(env, mainClass, "main",
                                       "([Ljava/lang/String;)V");
  // 调用main()方法，调用JNIEnv中定义的CallStaticVoidMethod()方法
  // 最终会调用JavaCalls::call()函数执行Math类中的main()方法。
  // JavaCalls:call()函数是个非常重要的方法，后面在讲解方法执行引擎时会详细介绍。
  (*env)->CallStaticVoidMethod(env, mainClass, mainID, mainArgs);
  
  //结束
  LEAVE();
}
```

**在JavaMain函数中，获取调用了main方法，并在main方法调用完成之后，调用LEAVE方法结束，完成了一整个启动->结束的生命周期**

总的流程大致上可以划分成以下几个步骤：

- 准备初始化JVM，主要是准备初始化JVM所需要的一些数据，最后去调用JavaMain函数
- 初始化JVM**在JavaMain函数中**
- **加载main方法所在的类**
- 获取main方法
- 调用main方法**JavaCalls:call()函数**
- 结束**销毁JVM等后续一系列的操作**

我们在上文中也提到，**类加载只是JVM启动过程中的一环**，而上面的步骤中，**加载main方法所在的类**这一步就是类加载，所以就**借着这个步骤**进入我们今天的主题：**深度剖析JVM类加载的过程**

## 类加载过程源码分析

经过上文的分析，我们进入到LoadMainClass方法中

```
//加载Math类
mainClass = LoadMainClass(env, mode, what); 
```

**LoadMainClass方法**

```
static jclass LoadMainClass(JNIEnv *env, int mode, char *name){
     //LancherHelper类
    jclass cls = GetLauncherHelperClass(env);
     //获取LancherHelper类的checkAndLoadMain方法
    NULL_CHECK0(mid = (*env)->GetStaticMethodID(env, cls,"checkAndLoadMain",
                "(ZILjava/lang/String;)Ljava/lang/Class;"));
    NULL_CHECK0(str = NewPlatformString(env, name));
    //使用checkAndLoadMain加载Math类
    NULL_CHECK0(result = (*env)->CallStaticObjectMethod(env, cls, mid,USE_STDERR, mode, str));
    return (jclass)result;
}
```

### 加载LancherHelper类

我们看到会先去加载LancherHelper类，我们看下**GetLauncherHelperClass方法**，发现如果helperClass已经存在就直接返回，如果不存在就调用**FindBootStrapClass方法**

```
jclass GetLauncherHelperClass(JNIEnv *env)
{
    if (helperClass == NULL) {
        NULL_CHECK0(helperClass = FindBootStrapClass(env,
                "sun/launcher/LauncherHelper"));
    }
    return helperClass;
}
```

**FindBootStrapClass方法**

```
jclass FindBootStrapClass(JNIEnv *env, const char* classname)
{
   if (findBootClass == NULL) {
        //获取jvm.cpp中的JVM_FindClassFromBootLoader方法
       findBootClass = (FindClassFromBootLoader_t *)dlsym(RTLD_DEFAULT,
          "JVM_FindClassFromBootLoader");
   }
   //调用JVM_FindClassFromBootLoader方法
   return findBootClass(env, classname); 
}
```

**JVM_FindClassFromBootLoader方法，在hotspot/src/share/vm/prims/jvm.cpp目录下**

```
JVM_ENTRY(jclass, JVM_FindClassFromBootLoader(JNIEnv* env,
                                              const char* name))
  //调用SystemDictionary解析类去加载类
  Klass* k = SystemDictionary::resolve_or_null(h_name, CHECK_NULL);
  return (jclass) JNIHandles::make_local(env, k->java_mirror());
JVM_END
```

**resolve_or_null方法，在hotspot/src/share/vm/classfile/systemDictionary.cpp目录**

```
Klass* SystemDictionary::resolve_or_null(Symbol* class_name, ...) {
   //走了这里
    return resolve_instance_class_or_null(class_name, class_loader, protection_domain, THREAD);
}
```

**resolve_instance_class_or_null方法，在hotspot/src/share/vm/classfile/systemDictionary.cpp目录**

```
Klass* SystemDictionary::resolve_instance_class_or_null(Symbol* name, ...) { 
      // Do actual loading
      k = load_instance_class(name, class_loader, THREAD);
}                                                      
```

**load_instance_class方法，实际调用加载的地方，由ClassLoader去加载类**

```
nstanceKlassHandle SystemDictionary::load_instance_class(Symbol* class_name, Handle class_loader, TRAPS) {
    if (k.is_null()) {
      // Use VM class loader
      k = ClassLoader::load_class(class_name, search_only_bootloader_append, CHECK_(nh));
    }
}
```

**ClassLoader::load_class方法，hotspot/src/share/vm/classfile/classLoader.cpp**

```
instanceKlassHandle ClassLoader::load_class(Symbol* name, bool search_append_only, TRAPS) {
  // 创建字节码文件流
  stream = search_module_entries(_exploded_entries, class_name, file_name, CHECK_NULL);
  // 每个被加载的Java类都对应着一个ClassLoaderData结构，ClassLoaderData内部通过链表维护着ClassLoader和ClassLoader加载的类
  ClassLoaderData* loader_data = ClassLoaderData::the_null_class_loader_data();
  // 解析Java字节码文件流
  instanceKlassHandle result = KlassFactory::create_from_stream(stream, name, ...);
}
```

**最终调用ClassFileParser解析Java字节码文件流**

```
instanceKlassHandle KlassFactory::create_from_stream(ClassFileStream* stream,Symbol*name, ...) {
   //调用类解析
   ClassFileParser parser(stream,name,loader_data,protection_domain,host_klass,cp_patches,
                         ClassFileParser::BROADCAST, // publicity level
                         CHECK_NULL);
  //创建instanceKclass，保存解析结果
  instanceKlassHandle result = parser.create_instance_klass(old_stream != stream, CHECK_NULL);
  return result;
 }
```

### 寻找LancherHelper类的checkAndLoadMain进行Math类的加载

回到最开始的**LoadMainClass方法**，LancherHelper类加载完之后，JVM就会去找获取LancherHelper类的**checkAndLoadMain方法**并执行，进行Math类的加载

**LoadMainClass方法**

```
static jclass LoadMainClass(JNIEnv *env, int mode, char *name){
     //LancherHelper类
    jclass cls = GetLauncherHelperClass(env);
     //获取LancherHelper类的checkAndLoadMain方法
    NULL_CHECK0(mid = (*env)->GetStaticMethodID(env, cls,"checkAndLoadMain",
                "(ZILjava/lang/String;)Ljava/lang/Class;"));
    NULL_CHECK0(str = NewPlatformString(env, name));
    //使用checkAndLoadMain加载Math类
    NULL_CHECK0(result = (*env)->CallStaticObjectMethod(env, cls, mid,USE_STDERR, mode, str));
    return (jclass)result;
}
```

**LancherHelper类的checkAndLoadMain方法 注意：这是Java层面的代码了 这里以JDK 11为例**

```
    @SuppressWarnings("fallthrough")
    public static Class<?> checkAndLoadMain(boolean printToStderr,
                                            int mode,
                                            String what) {
                                            
        // 省略不必要的代码
        ......

        Class<?> mainClass = null;
        switch (mode) {
            //断点显示mode=1,走loadMainClass方法
            case LM_MODULE: case LM_SOURCE:
                mainClass = loadModuleMainClass(what);
                break;
            default:
                mainClass = loadMainClass(mode, what);
                break;
        }
        
        // 省略不必要的代码
        ......
        return mainClass;
    }
```

**loadMainClass方法，这一步获取Java层面的类加载器，并通过Class.forName进行加载**

```
    private static Class<?> loadMainClass(int mode, String what) {
        String cn;
        switch (mode) {
            case LM_CLASS:
                // 使用类加载器加载Hello类，mode=1,what为类名即Hello
                cn = what;
                break;
            case LM_JAR:
                cn = getMainClassFromJar(what);
                break;
            default:
                // should never happen
                throw new InternalError("" + mode + ": Unknown launch mode");
        }

        // load the main class
        cn = cn.replace('/', '.');
        Class<?> mainClass = null;
        // 这一步最后返回的ClassLoader是AppCLassLoader
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        try {
            try {
                // Class.forName将进行安全校验并调用Class.c中的forName0
                mainClass = Class.forName(cn, false, scl);
            } catch (NoClassDefFoundError | ClassNotFoundException cnfe) {
               // 省略不必要的代码
               ......
            }
        } catch (LinkageError le) {
            abort(le, "java.launcher.cls.error6", cn,
                    le.getClass().getName() + ": " + le.getLocalizedMessage());
        }
        return mainClass;
    }
```

**ClassLoader.getSystemClassLoader方法**

```
public static ClassLoader getSystemClassLoader() {
        // 获取类加载器，按初始化等级返回相应的类加载器，在VM.java中定义了各等级的含义：
        // 1. JAVA_LANG_SYSTEM_INITED = 1，lang库初始化结束，
        // 2. MODULE_SYSTEM_INITED = 2模块初始化结束，
        // 3. SYSTEM_LOADER_INITIALIZING = 3 初始化中，
        // 4. SYSTEM_BOOTED= 4 系统完全启动
        // 显然加载Math类时JVM已经初始化完了，所以初始化等级为4 
        // scl为ClassLoader，scl在initSystemClassLoader中被赋值，initSystemClassLoader在HotSpot启动阶段被调用，所以scl不为空，为AppClassLoader
        switch (VM.initLevel()) {
            case 0:
            case 1:
            case 2:
                // the system class loader is the built-in app class loader during startup
                return getBuiltinAppClassLoader();
            case 3:
                String msg = "getSystemClassLoader cannot be called during the system class loader instantiation";
                throw new IllegalStateException(msg);
            default:
                // system fully initialized
                assert VM.isBooted() && scl != null;
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    checkClassLoaderPermission(scl, Reflection.getCallerClass());
                }
                return scl;
        }
    }

// initSystemClassLoader方法
static synchronized ClassLoader initSystemClassLoader() {
        if (VM.initLevel() != 3) {
            throw new InternalError("system class loader cannot be set at initLevel " +
                                    VM.initLevel());
        }

        // detect recursive initialization
        if (scl != null) {
            throw new IllegalStateException("recursive invocation");
        }

        // 调用getBuiltinAppClassLoader方法
        ClassLoader builtinLoader = getBuiltinAppClassLoader();
        // 省略不必要的代码
        ......
}

// getBuiltinAppClassLoader方法
static ClassLoader getBuiltinAppClassLoader() {
        return ClassLoaders.appClassLoader();
}
```

**获取完Java层面的类加载器之后，调用Class.forName(cn, false, scl)方法**

```
public static Class<?> forName(String name, boolean initialize,
                                   ClassLoader loader)
        throws ClassNotFoundException
    {
        Class<?> caller = null;
            // 不必要的代码
            .......
            // 如果传入的类加载器为空，则使用默认的AppClassLoader类加载器
            if (loader == null) {
                ClassLoader ccl = ClassLoader.getClassLoader(caller);
                if (ccl != null) {
                    sm.checkPermission(
                        SecurityConstants.GET_CLASSLOADER_PERMISSION);
                }
            }
        return forName0(name, initialize, loader, caller);
    }
```

### 再次使用SystemDictionary::resolve_or_null进行Math类的加载

**forName0方法，又回到了HotSpot，jdk/src/java.base/share/native/libjava/Class.c目录下**

```
JNIEXPORT jclass JNICALL
Java_java_lang_Class_forName0(JNIEnv *env, jclass this, jstring classname,
                              jboolean initialize, jobject loader, jclass caller){
  cls = JVM_FindClassFromCaller(env, clname, initialize, loader, caller);
}                             
```

**JVM_FindClassFromCaller方法，在hotspot/src/share/vm/prims/jvm.cpp**

```
JVM_ENTRY(jclass, JVM_FindClassFromCaller(JNIEnv* env, const char* name,
                                          jboolean init, jobject loader,
                                          jclass caller)){
jclass result = find_class_from_class_loader(env, h_name, init, h_loader,
                                               h_prot, false, THREAD);
}
```

**find_class_from_class_loader方法**

```
jclass find_class_from_class_loader(JNIEnv* env, Symbol* name, jboolean init,
                                    Handle loader, Handle protection_domain,
                                    jboolean throwError, TRAPS) {
  //加载Math类
  Klass* klass = SystemDictionary::resolve_or_fail(name, loader, protection_domain,throwError != 0, CHECK_NULL);
  return (jclass) JNIHandles::make_local(env, klass_handle->java_mirror());
}
```

**SystemDictionary::resolve_or_fail**

```
Klass* SystemDictionary::resolve_or_fail(Symbol* class_name, bool throw_error, TRAPS)
{
  return resolve_or_fail(class_name, Handle(), Handle(), throw_error, THREAD);
}
```

```
Klass* SystemDictionary::resolve_or_fail(Symbol* class_name, Handle class_loader, Handle protection_domain, bool throw_error, TRAPS) {
  // resolve_or_null方法在上面已经分析过了，直接搜这个方法就可以搜到，这里不重复分析了
  Klass* klass = resolve_or_null(class_name, class_loader, protection_domain, THREAD);
  ......
  return klass;
}
```

**resolve_or_null方法在上面已经分析过了，直接搜这个方法就可以搜到，这里不重复分析了**

### 获取Math类的main方法并调用

```
 //获取Math类的main方法
   mainID = (*env)->GetStaticMethodID(env, mainClass, "main",
                                       "([Ljava/lang/String;)V");
  // 调用main()方法，调用JNIEnv中定义的CallStaticVoidMethod()方法
  // 最终会调用JavaCalls::call()函数执行Math类中的main()方法。
  // JavaCalls:call()函数是个非常重要的方法，后面在讲解方法执行引擎时会详细介绍。
  (*env)->CallStaticVoidMethod(env, mainClass, mainID, mainArgs);
```

好啦，枯燥的源码阅读阶段已经结束，我们把这一整个流程通过画图的形式再加强下大家的印象：

## 类总体运行流程图


![JVM的启动过程](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/96349047cdf5450d93cfbc56d9da2c8e~tplv-k3u1fbpfcp-watermark.image)

 1. 第一步：运行 **java classload.Math.class** 命令，运行字节码文件
 2. 第二步：当运行这个命令的时候，实际上，系统会使用java.exe文件，开始进入main.c文件的main函数**也是程序启动的入口**
 3. 第三步：在创建Java虚拟机的过程中，会创建一个**引导类加载器实例，这部分源码放在下篇文章中，大家先记得是用这个类加载器加载的LauncherHelper类即可**
 4. 第四步：创建完Java虚拟机后，C++代码会去很多调用java虚拟机的启动程序，在启动的程序中会有一个**sun.launcher.LauncherHelper类**，启动**LauncherHelper类**会去创建很多Java层面的类加载器
 5. 第五步：通过Java层面的类加载器，去加载真正的**java字节码文件，例如:Math类**
 6. 第六步：把字节码文件加载完之后，c++代码会直接发起调用**找到Main函数ID，并进行调用**
 7. 第七步：程序运行结束之后，JVM进行销毁**执行LEAVE方法**

# 类加载的具体流程

关于类从 **.class文件**到**被虚拟机直接使用的信息**，总共经历了以下五个步骤：

**加载>>验证>>准备>>解析>>初始化**，如下图所示：

![类加载](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d08e870cf65a4647be5013ecf52e0fe5~tplv-k3u1fbpfcp-zoom-1.image)

下面就开始剖析剖析这五个步骤

## 加载

加载指的就是把class字节码文件**从各个来源**通过类加载器装载进内存，把字节码文件变成字节流，在加载阶段，Java虚拟机需要完成以下三件事情:

1. 通过一个**类的全限定名**来获取定义此类的二进制字节流。
2. 将这个字节流所代表的**静态存储结构**转化为**方法区的运行时数据结构**。
3. 在堆中生成一个代表这个类的**java.lang.Class对象**，作为方法区这个类的访问入口。

这里需要注意一个点：**字节码的来源**

### 字节码来源

《Java虚拟机规范》对这三点要求其实并不是特别具体，留给虚拟机实现与Java应用的灵活度都是相当大的。例如：通过一个类的全限定名来获取定义此类的二进制字节流”这条规则，它并没有指明二进制字节流必须得从某个Class文件中获取，确切地说是根本没有指明要从哪里获取、如何获取。

这样就给了开发人员很大的灵活程度，例如:

- 一般的加载来源包括从本地路径下编译生成的 **.class文件**
- 从ZIP压缩包中读取，这很常见，最终成为日后JAR、EAR、WAR格式的基础
- 从网络中获取，这种场景最典型的应用就是Web Applet。
- 运行时计算生成，这种场景使用得最多的就是动态代理技术，在java.lang.reflect.Proxy中， 就是用了ProxyGenerator.generateProxyClass()来为特定接口生成形式为“*$Proxy”的代理类的二进制字节流
- 由其他文件生成，典型场景是JSP应用，由JSP文件生成对应的Class文件。
- 从数据库中读取，这种场景相对少见些，例如有些中间件服务器(如SAP Netweaver)可以选择把程序安装到数据库中来完成程序代码在集群间的分发。
- 可以从加密文件中获取，这是典型的防Class文件被反编译的保护措施，通过加载时解密Class文 件来保障程序运行逻辑不被窥探。

### 加载结束后

加载阶段结束后，Java虚拟机外部的二进制字节流就按照**虚拟机所设定的格式存储在方法区之中**，方法区中的**数据存储格式完全由虚拟机实现自行定义，《Java虚拟机规范》未规定此区域的具体数据结构。**

类型数据妥善安置在方法区之后，会在Java堆内存中实例化一个java.lang.Class类的对象，这个对象将作为**应用程序访问方法区中的类型数据的外部接口。**

### 加载阶段的注意点

加载阶段与部分验证动作中的部分是交叉进行的，加载阶段尚未完成，验证阶段可能已经开始，但是这两个阶段的**开始时间仍然保持着固定的先后顺序**


## 验证

验证阶段的目的是确保Class文件的字节流中包含的信息符合 **《Java虚
拟机规范》** 的全部约束要求，保证这些信息被当作代码运行后不会危害虚拟机自身的安全。

验证阶段是非常重要的，这个阶段是否严谨，直接决定了**Java虚拟机是否能承受恶意代码的攻击**，从**代码量和耗费的执行性能**的角度上讲，验证阶段的工作量在类加载过程中占了相当大的比重。

从整体上看，验证阶段大致上会完成下面四个阶段的检验动作:**文件格式验证、元数据验证、字节 码验证和符号引用验证**

### 文件格式验证

验证字节流是否符合Class文件格式的规范，并且能被当前版本的虚拟机处理。

举个例子，以Math.class为例，不经过反编译，直接打开，我们可以看到这个文件的开头是**cafe babe**，这个**cafe babe**就说明了这个文件是一个字节码文件，包括后面的**主次版本号等等**，如果把这些信息随意修改，JVM就极可能识别不了，所以说第一步验证，验证的就是字节码的内容符不符合JVM规范

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a1c7db394a0844f695166e279f0671ba~tplv-k3u1fbpfcp-watermark.image)

### 元数据（类元信息）的验证

这个阶段的主要目的是对**类的元数据信息进行校验**，保证不存在与《Java语言规范》定义相悖的元数据信息，这个阶段可能包括的验证点如下:

- 这个类**是否有父类**(除了java.lang.Object之外，所有的类都应当有父类)
- 这个类的父类**是否继承了不允许被继承的类(被final修饰的类)**
- 如果这个类不是抽象类，**是否实现了其父类或接口之中要求实现的所有方法**
- **类中的字段、方法是否与父类产生矛盾(例如覆盖了父类的final字段，或者出现不符合规则的方 法重载，例如方法参数都一致，但返回值类型却不同等)**。

### 字节码的验证

在上个阶段对元数据信息中的数据类型校验完毕以后，这阶段就要对**类的方法体(Class文件中的Code属性)** 进行校验分析，保证被校验类的方法在运行时不会做出危害 虚拟机安全的行为

这个阶段整个验证过程中最复杂的一个阶段，主要目的是**通过数据流分析和控制流分析**，确定程序语义是合法的、符合逻辑的。

- 保证任意时刻操作数栈的数据类型与指令代码序列都能配合工作，例如不会出现类似于**在操作栈放置了一个int类型的数据，使用时却按long类型来加载入本地变量表中**这样的情况。
- 保证任何跳转指令都不会跳转到方法体以外的字节码指令上。
- 保证方法体中的**类型转换总是有效的**

由于**数据流分析和控制流分析的高度复杂性**，Java虚拟机的设计团队为了避免过多的执行时间消耗在字节码验证阶段中，在**JDK 6之后的Javac编译器和Java虚拟机里进行了一项联合优化**，把尽可能多的校验辅助措施挪到Javac编译器里进行。

具体做法就是是给**方法体Code属性的属性表**中新增加了一项名为**StackMapTable的新属性**，这项属性描述了**方法体所有的基本块**(Basic Block，指按照控制流拆分的代码块)开始时**本地变量表和操作栈应有的状态**

在字节码验证期间，JVM就不需要根据程序推导这些状态的合法性，只需要**检查StackMapTable属性中的记录是否合法**。

这样就将字节码验证的类型推导转变为类型检查，从而节省了大量校验时间。

### 符号引用的验证

这个校验行为发生在JVM将**符号引用**转化为**直接引用**的时候，这个转化动作是在**解析阶段中发生**

符号引用验证可以看作是**对类自身以外(常量池中的各种符号引用)的各类信息进行匹配性校验**，通俗来说就是，该类是否缺少或者被禁止**访问它依赖的某些外部 类、方法、字段等资源**

- 符号引用中通过字符串描述的全限定名是否能找到对应的类。
- 在指定类中是否存在符合方法的字段描述符及简单名称所描述的方法和字段。
- 符号引用中的类、字段、方法的可访问性(private、protected、public、package)是否可被当 前类访问。

符号引用验证的主要目的是**确保解析行为能正常执行**，如果无法通过验证，Java虚拟机将会抛出一个java.lang.IncompatibleClassChangeError的子类异常， 如：**java.lang.IllegalAccessError、java.lang.NoSuchFieldError、java.lang.NoSuchMethodError等**



## 准备

准备其实就把类中的静态变量 **(类变量)** 做一个初始值，还是以Math类为例，我们在Math类中新建了两个静态变量，而准备这个步骤，就是把这两个静态变量做一个默认值 **（而不是图中的“666”或者是引用类型）**，int是0，boolean是false依次类推，引用类型的话赋值成null。

```java
public class Math {

    /**
     * 在类加载的准备阶段 赋值成0
     */
    private static int zero = 1;
    /**
     * 在类加载的准备阶段 赋值成null
     */
    private static Math math = new Math();

}
```

关于准备阶段需要注意两个点：

1. 这时候进行内存分配的 仅包括**类变量**，而不包括实例变量，**实例变量**将会在**对象实例化**时随着对象一起分配在Java堆中

2. 那变量zero在准备阶段过后的初始值为0而不是123，因为这时尚未开始执行任何Java方法，而把 zero赋值为1的**putstatic指令是程序被编译后，存放于类构造器方法**之中，所以把zero赋值为1的动作要到类的初始化阶段才会被执行。

### 特殊情况

如果类字段的字段属性表中存在**ConstantValue属性/常量属性**，那在**准备阶段**变量值就会被初始化为ConstantValue属性所指定的初始值，假设上面类变量zero的定义修改为:

```
    /**
     * 在类加载的准备阶段 赋值成0
     */
    private static final int zero = 1;
```


#### 为什么要在准备阶段就把类变量做初值

因为如果是**实例变量**的话，多个实例变量指向**不同的实例变量堆内存**，即实例变量的值只**与对象相关**。而类变量的值与类对象无关，为最后一次修改的值，多个类对象只会共用同一份堆内存，所以基于这个特点，**类变量可以在准备阶段就赋初值**。

## 解析

这一步就是将常量池中的**符号引用替换成直接引用**的过程，这里有两个需要注意的点

1. **什么是符号引用？**
2. **什么是直接引用？**

下面就来讲讲这个**符号引用**和**直接引用**

在JVM中，一个类中的方法名、类名、修饰符、返回值等等都是一系列的符号，而且这些符号都是一个个的常量，存储在常量池中，同时这些个符号、变量、代码块等等在内存中都是由一块块的内存区域来存储，这些内存区域都有对应的内存地址，而这些内存地址就是**直接引用**，而解析这个步骤就是**把"符号"替换成"内存地址"**

### 符号引用

我们以Test.class为例，看下**符号引用**到底是怎么回事

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

我们在Test.class路径下（**执行javap -v 命令**）看看字节码文件

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

每个符号旁都有一个带 **#** 的，这个 **#1、#2**就是一个标识符，在**实例的创建**，**变量的传递**，**方法的调用**，JVM都是用这个标识符来定位，以**new Test**为例：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7c79ec2fe4fd464dbf8fb8dfc3c69ad6~tplv-k3u1fbpfcp-watermark.image)

在main方法中，一开始会去new一个Test类，旁边的注释中，也指明了new的是**class classload/Test**，我们接下来再来看 **#2** 指向了哪个常量

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/687989771ea9434c8816465ec1d5d406~tplv-k3u1fbpfcp-watermark.image)

可以看到#2是一个class，并且又去指向了一个#25，我们再跟踪到#25来看一下


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2b06277ccbf5491d8a7832029182df75~tplv-k3u1fbpfcp-watermark.image)

可以看到 **#25** 是代表着一个类，同时编码是utf8，所以通过常量池中符号的标识符，jvm可以一步步找到创建的到底是啥玩意，方法的调用也是一样，在代码编译完之后，这些**方法名、()、类名等等**，都变成一个个的符号，并且**存放在常量池中**

### 解析和动态连接

截止目前，编译出来的这些符号放到常量池，此时这个常量池是静态的，但是通过加载，放到内存后都有对应的内存地址，那么这个常量池也就会变成运行时常量池 **（注意： JDK1.7 及之后版本的 JVM 已经将运行时常量池从方法区中移了出来，在 Java 堆（Heap）中开辟了一块区域存放运行时常量池。）**

符号引用一部分会在类加载阶段或者第一次使用的时候就被转化为**直接引用**，这种转化被称为**静态解析**。 另外一部分将在每一次运行期间都转化为直接引用，这部分就称为**动态连接**

### 解析的步骤小结

所以在类加载中，解析做的也就是**静态链接**，针对的是**静态方法（例如：main方法）或者其他不变的方法**，因为静态方法等到加载、分配完内存后，内存地址就不会变了，所以，可以在类加载的时候，可以直接**替换成内存地址。**

但是像下图所示，假设我们Test有多个子类，由于多态的存在，像非静态方法，可能有不同的实现，所以**在编译加载的时候是无法知道的**，需要等到真正运行的时候，才能找到具体方法的实现，找到具体的内存地址，并将符号引用替换成直接引用


## 初始化


类的初始化阶段是类加载过程的最后一个步骤，直到初始化阶段，Java虚拟机才真正开始**执行类中编写的Java程序代码**，将主导权移交给应用程序。

初始化阶段就是执行类构造器**clinit方法**的过程。**clinit方法**并不是程序员在Java代码中直接编写的方法，它是**Javac编译器的自动生成物**

clinit方法是由**编译器自动收集类中的所有类变量的赋值动作和静态语句块(static{}块)中的语句合并产生的**，编译器收集的顺序是由语句在源文件中出现的顺序决定的，静态语句块中只能访问到定义在静态语句块之前的变量，定义在它之后的变量，在前面的静态语句块可以赋值，但是不能访问

```java
public class Test { 
    static {
    // 给变量复制可以正常编译通过
    i = 0; 
    // 这句编译器会提示“非法向前引用”
    System.out.print(i);  
  }
    static int i = 1; 
}
```
### 初始化的顺序

clinit方法与**类的构造函数(即实例构造器init方法)不同**，它不需要显式地调用父类构造器，Java虚拟机会保证在子类的clinit方法执行前，父类的clinit方法已经执行完毕。

因此在Java虚拟机中第一个被执行的**clinit方法的类型**肯定是**java.lang.Object**

### 初始化的注意点

clinit方法对于类或接口来说并不是必需的，如果一个类中没有静态语句块，也没有对变量的 赋值操作，那么编译器可以不为这个类生成clinit方法。

接口中不能使用**静态语句块**，但仍然有**变量初始化的赋值操作**，因此接口与类一样都会生成clinit方法。但接口与类不同的是，**执行接口的clinit方法不需要先执行父接口的clinit方法**，因为只有当父接口中定义的变量被使用时，父接口才会被初始化。此外，**接口的实现类**在初始化时也一样不会执行**接口的clinit方法。**

### 初始化的同步机制

JVM必须保证一个类的clinit方法**在多线程环境中被正确地加锁同步**，如果多个线程同时去初始化一个类，那么只会有其中一个线程去执行这个类的clinit方法，其他线程都需要阻塞等待，直到活动线程执行完毕clinit方法。


# 本文总结

好啦，以上就是这篇文章的全部内容了，我们一起来回忆一下：

- 开篇说了为什么要有类加载，明白类加载的作用
- 接下来从HotSpot源码的角度分析了整个类的整体运行过程，**从一个完整的流程中看类加载这块内容处在一个什么样的位置**
- 分析了具体类加载的细节，逐步分析**加载>>验证>>准备>>解析>>初始化**这五步各自做了什么事，负责了哪些功能

# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的非常有用！！！如果想获取**电子书《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）周志明》**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！

