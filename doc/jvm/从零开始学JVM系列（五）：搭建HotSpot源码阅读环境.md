# 前言

这篇文章的原因主要有以下三个方面：

- 第一个方面是因为在前几篇文章中，在讲**类加载器的初始化过程、类加载器的启动时机**这两块内容时穿插了不少的HotSpot源码，肯定会有小伙伴**想自己跟一下HotSpot源码**，所以在这专门写一篇**HotSpot源码阅读环境**的文章给需要的小伙伴

- 其次在之后的文章中会讲**对象的创建过程、垃圾回收算法、GC安全点和安全区域、垃圾回收器**等等一系列的内容中，难免会扯到**HotSpot源码**中去，所以还不如现在就把**源码阅读环境搭建好**，到时候跟着文章的思路一步步的跟下来呢

- 最后一个方面同时也是一个问题：**小伙伴真的对JVM不感兴趣吗？**

那么话不多说，开始准备搭建吧！！！

# 准备工作

## 第一个环节：软件准备

我这里针对的是我自己的环境，您可以根据你自己的环境，去搭建，道理都是一样的。

- OS：Mac系统
- IDE：Clion
- 源码：OpenJDK8

## 第二个环节：环境搭建

### IDE Clion

首先你得安装一个Clion，因为HotSpot虚拟机是C++写的，[链接在这，请点我](https://www.jetbrains.com/clion/download/#section=mac)

#### 测试IDE Clion可用

安装完Clion后，随便新建一个项目，测试一下，确保Clion是正常能使用的

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0e33cc8b14e64f14aabb124a0731f081~tplv-k3u1fbpfcp-watermark.image)


我们点击create之后，发现报下面截图中的错，关键在`missing xcrun at: /Library/Developer/CommandLineTools/usr/bin/xcrun`这行错误代码，百度一下说是缺少**Command Line Tools工具**，那么就下载一下这个工具

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/937e088f9dba464ca74dadbab7dceebe~tplv-k3u1fbpfcp-watermark.image)


##### 安装Command Line Tools

进入苹果的开发者网站：[请点这进入](https://developer.apple.com/download/more/)，进行下载，下载后安装pkg文件即可

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/63f25a1cca264f7bbce44f5b0e69e01b~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c38021b835354db4a4d4b9ec7e8e0f13~tplv-k3u1fbpfcp-watermark.image)


安装完之后，打开Clion，发现原先报错的地方都置灰了，说明已经修复了


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4e7a384498294e1b86b2db44e7a2d578~tplv-k3u1fbpfcp-watermark.image)

运行main函数后，也正常输出了，说明Clion已经正确安装

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/24d11bf4e49d46d38c2ce140acdc602d~tplv-k3u1fbpfcp-watermark.image)


### 下载openjdk源码

IDE准备好了，下一步就需要准备openjdk源码了，由于`官网下载网速太慢，容易文件下不全`，我就直接在网上找到了一个社区维护的openjdk-jdk8u版本

[社区维护的openjdk-jdk8u](https://github.com/AdoptOpenJDK/openjdk-jdk8u)

## 第三个环节：编译源码

下载完最新的openjdk源码之后，就需要对它进行编译

### 准备编译工具

```
// 加速编译
brew install ccache  
// 字体引擎，编译过程中会被依赖到
brew install freetype 
brew install autoconf
// 分布式版本控制
brew install mercurial
```

### 配置BOOT_JDK`很重要，不然编译会报出各种千奇百怪的问题`

编译之前需要配置一个称作BOOT_JDK的东西，其版本要比编译的版本低一级，即编译OpenJDK8，就需要安装JDK7作为环境**OpenJDK7或OracleJDK7均可**


### 安装compiledb`很重要，解决头文件费了将近两天，最后发现安装这玩意就好了，我吐血了`

其实这一步的操作的原因是为了解决在编译openjdk源码的时候很多头文件找不到的问题

安装要求：需要python3+，**brew install python3**

然后需要安装pip，安装指令为：**curl https://bootstrap.pypa.io/get-pip.py | python3**

最后通过**pip安装compiledb**即可，指令为：**pip install compiledb**


### 配置环境变量

```
vi ~/.bash_profile
```

在.bash_profile文件底部插入下面的配置信息：

```
# 设定语言选项，必须设置
export LANG=C
# Mac平台，C编译器不再是GCC，而是clang
export CC=clang
export CXX=clang++
export CXXFLAGS=-stdlib=libc++
# 是否使用clang，如果使用的是GCC编译，该选项应该设置为false
export USE_CLANG=true
# 跳过clang的一些严格的语法检查，不然会将N多的警告作为Error
export COMPILER_WARNINGS_FATAL=false
# 链接时使用的参数
export LFLAGS='-Xlinker -lstdc++'
# 使用64位数据模型
export LP64=1
# 告诉编译平台是64位，不然会按照32位来编译
export ARCH_DATA_MODEL=64
# 允许自动下载依赖
export ALLOW_DOWNLOADS=true
# 并行编译的线程数，编译时长，为了不影响其他工作，可以选择2
export HOTSPOT_BUILD_JOBS=4
export PARALLEL_COMPILE_JOBS=2 #ALT_PARALLEL_COMPILE_JOBS=2
# 是否跳过与先前版本的比较
export SKIP_COMPARE_IMAGES=true
# 是否使用预编译头文件，加快编译速度
export USE_PRECOMPILED_HEADER=true
# 是否使用增量编译
export INCREMENTAL_BUILD=true
# 编译内容
export BUILD_LANGTOOL=true
export BUILD_JAXP=true
export BUILD_JAXWS=true
export BUILD_CORBA=true
export BUILD_HOTSPOT=true
export BUILD_JDK=true
# 编译版本
export SKIP_DEBUG_BUILD=true
export SKIP_FASTDEBUG_BULID=false
export DEBUG_NAME=debug
# 避开javaws和浏览器Java插件之类部分的build
export BUILD_DEPLOY=false
export BUILD_INSTALL=false

# 最后需要干掉这两个环境变量（如果你配置过），不然会发生诡异的事件
unset JAVA_HOME
unset CLASSPATH
```

使环境变量生效：

```
source ~/.bash_profile
```

### 执行配置文件校验命令

进入下载的openjdk目录，执行配置文件校验命令`注意：一定要切换到BOOT_JDK的版本下执行`

```
sh configure --with-freetype-include=/usr/local/include/freetype2 --with-freetype-lib=/usr/local/lib/ --disable-zip-debug-info --disable-debug-symbols --with-debug-level=slowdebug --with-target-bits=64 --with-jvm-variants=server
```

#### 执行配置文件报错案例

1. 执行之后报下图中的错，意思就是**检测不到Xcode的版本**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/df2a800ef08b4903ad645c6851cd8120~tplv-k3u1fbpfcp-watermark.image)

`解决办法`：去苹果的开发者网站：[请点这进入](https://developer.apple.com/download/more/)下载mac系统版本对应的Xcode，下载完Xcode之后记得`执行下面的命令`，不然还是会报上图中的错

```
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

2. `Xcode 4 is required to build JDK 8`的错

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a71d8f09c5ce4f868fdb556cef407a9e~tplv-k3u1fbpfcp-watermark.image)


`解决办法`：找到configure文件并打开`vim common/autoconf/generated-configure.sh`,找到判断版本的地方,将这一段全部注释掉.

```
    #Fail-fast: verify we're building on Xcode 4, we cannot build with Xcode 5 or later
    XCODE_VERSION=`$XCODEBUILD -version | grep '^Xcode ' | sed 's/Xcode //'`XC_VERSION_PARTS=( ${XCODE_VERSION//./ } )
    if test ! "${XC_VERSION_PARTS[0]}" = "4"; then
     as_fn_error $? "Xcode 4 is required to build JDK 8, the version found was  $XCODE_VERSION. Use --with-xcode-path to specify the location of Xcode 4 or make Xcode 4 active by using xcode-select." "$LINENO" 5
    fi
#
```

3. `A gcc compiler is required`的问题

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/42433bfc63b34047840cb8fc040cfa49~tplv-k3u1fbpfcp-watermark.image)

`解决办法`：找到下面这段校验代码并注释掉，**注意有两处，代码是相同的，根据下图中的标志寻找**

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/54123aeeab844206996700bdac6c5bfd~tplv-k3u1fbpfcp-watermark.image)

```
    if test $? -ne 0; then
      { $as_echo "$as_me:${as_lineno-$LINENO}: The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required GCC compiler." >&5
$as_echo "$as_me: The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required GCC compiler." >&6;}
      { $as_echo "$as_me:${as_lineno-$LINENO}: The result from running with --version was: \"$COMPILER_VERSION_TEST\"" >&5
$as_echo "$as_me: The result from running with --version was: \"$COMPILER_VERSION_TEST\"" >&6;}
      as_fn_error $? "GCC compiler is required. Try setting --with-tools-dir." "$LINENO" 5
    fi
```

出现下图的样子，证明校验完成，**注意：要确认Boot JDK的版本**

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7aaaa6b0858e47b390cab8c99510f8b5~tplv-k3u1fbpfcp-watermark.image)

### 开始编译`每一次编译都要耗费很多的时间，所以建议把文中的错误都改了之后，再进行编译`

执行**compiledb make WARNINGS_ARE_ERRORS="" CONF=macosx-x86_64-normal-server-slowdebug all**命令进行编译

如果出错的话，一定要按照下面的步骤重新编译：

1. **compiledb make CONF=macosx-x86_64-normal-server-slowdebug clean**
2. **compiledb make WARNINGS_ARE_ERRORS="" CONF=macosx-x86_64-normal-server-slowdebug all**

```
compiledb make WARNINGS_ARE_ERRORS="" CONF=macosx-x86_64-normal-server-slowdebug all
```

#### 编译的报错案例

1. `fatal error: 'iostream' file not found的问题`：解决方案：设置环境变量

```
NEW_INCLUDE=/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/include/c++/v1
export CFLAGS=-I$NEW_INCLUDE
export CXXFLAGS=-I$NEW_INCLUDE
```

2. `library not found for -lstdc++的问题`：解决方案：按照如下从操作即可。

- 克隆一个工具：`git clone https://github.com/quantum6/xcode-missing-libstdcpp`**（注意：该文件的内容会软连接到Xcode中，所以该目录不能删除）**
- 然后进入该工具执行sh install.sh
- 配置环境变量

```
NEW_LIB=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/lib
export LDFLAGS="-L${NEW_LIB}"
export LIBRARY_PATH=$NEW_LIB:$LIBRARY_PATH
```

3. `symbol(s) not found for architecture x86_64`的问题：解决方案：找到文件`jdk/src/macosx/native/sun/osxapp/ThreadUtilities.m`，然后把`inline void attachCurrentThread(void** env) 改为static inline void attachCurrentThread(void** env) `

```
Undefined symbols for architecture x86_64:
  "_attachCurrentThread", referenced from:
      +[ThreadUtilities getJNIEnv] in ThreadUtilities.o
      +[ThreadUtilities getJNIEnvUncached] in ThreadUtilities.o
ld: symbol(s) not found for architecture x86_64
clang: error: linker command failed with exit code 1 (use -v to see invocation)
```

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/58c35857aed64d0c8386391f85650da2~tplv-k3u1fbpfcp-watermark.image)

4. `clang: error: unknown argument: '-fpch-deps'`的问题，解决方案：编辑`hotspot/make/bsd/makefiles/gcc.make` 注释以下代码

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/52e2b4f482374c1791cf84869a0f0846~tplv-k3u1fbpfcp-watermark.image)

```
ifeq ($(USE_CLANG),)
  ifneq ($(CC_VER_MAJOR), 2)
    DEPFLAGS += -fpch-deps
  endif
endif
```

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f038a0426df54ddbaa4bdf117e6bd75b~tplv-k3u1fbpfcp-watermark.image)

5. `error: invalid argument '-std=gnu++98' not allowed with 'C'`的问题，解决方案:编辑`common/autoconf/generated-configure.sh`,搜索`-std=gnu++98`这个字串并注释

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cecaa7d240e24909ad42fc5c38543ef6~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cbf4d06eff534618b124d9511aaee935~tplv-k3u1fbpfcp-watermark.image)

6. 解决在slowdebug模式下编译之后崩溃的问题：在slowdebug模式下编译完成之后，执行java -version后会有JVM奔溃的错误。找到文件`hotspot/src/share/vm/runtime/perfMemory.cpp`文件。注释掉如下内容：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e5cb0a37dc12486abe0bf28263487eba~tplv-k3u1fbpfcp-watermark.image)

7. `implicit declaration of function 'VerifyFixClassname' is invalid in C99`的问题：解决方案：include头文件或者直接外部声明**最好include头文件，找不到再直接声明**

```
extern jboolean VerifyFixClassname(char* name);
extern jboolean VerifyClassname(char* name, jboolean allowArrayClass);
```

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b0348a91255742e0914f999a297a2861~tplv-k3u1fbpfcp-watermark.image)

8. `implicit declaration of function 'pthread_main_np' is invalid in C99`的问题：和上面一样**最好include头文件，找不到再直接声明**

```
extern jboolean pthread_main_np();
```

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5dafd500bb3a47b5a99bd8c553f9257f~tplv-k3u1fbpfcp-watermark.image)


9. `implicitly declaring library function 'strchr' with type 'char *(const char *, int)'`的问题：和上面一样**最好include头文件，找不到再直接声明**

```
#include <stdio.h>
#include <string.h>
```

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/296b8161f4da484e88d94dba809fc01a~tplv-k3u1fbpfcp-watermark.image)

10. `implicit declaration of function 'time' is invalid in C99`的问题：和上面一样**最好include头文件，找不到再直接声明**

```
#include <time.h>
```
![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8b78584df85a44279025e8f62d57f2be~tplv-k3u1fbpfcp-watermark.image)

11. `implicit declaration of function 'JVM_ActiveProcessorCount' is invalid in C99`的问题：这里直接改成`jint ncpus = 4;`

```
jint ncpus = 4;
```
![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7dc89f6134ce440e8b91ccf1d8aa753f~tplv-k3u1fbpfcp-watermark.image)


## 第四个环节：向Clion中导入源码并配置

如果出现如下图所示的界面，说明已经`编译成功`，在编译后有可能在最后有大量的`No such file or directory的警告。`这个不用担心，下一步就是向Clion中导入编译后的源码

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ffa1eab601564ce5a5418b7ea48af546~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d50956ad9ab64d41a3772e3e32c7692e~tplv-k3u1fbpfcp-watermark.image)


由于使用了**compiledb包装编译OpenJDK源码**。所以编译完成之后我们在源码根目录可以看到多了一个`compile_commands.json文件`。我们的工程导入则依赖这个json文件。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8c9d2c2a66184362af5b76b6c016fe80~tplv-k3u1fbpfcp-watermark.image)


打开Clion，然后点击Open打开上述`compile_commands.json文件`，并在弹出的对话框中选择Open as Project。

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/11fce2eeeafb40b68d0758bd4402dee3~tplv-k3u1fbpfcp-watermark.image)

`等待导入成功后`，进入Clion的`preferences->Build,Execution,Deployment->Custom Build Targets`，在改配置页面点击`Add Target`配置自定义的`Build Targets`。

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/75dafcb6fc6044c1bc3768212f273250~tplv-k3u1fbpfcp-watermark.image)

name自己自行输入，Toolchain选择Default

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/19ebfecdb4c0492e97ae7aa8dde70f17~tplv-k3u1fbpfcp-watermark.image)

新建Tool，一个是make_jdk8，一个是clean_jdk8，**（名字都自行取一个就行）**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3bc81d0e081445ecadcc2820b4ad7c0e~tplv-k3u1fbpfcp-watermark.image)

make_jdk8: 其中arguments填写：`CONF=macosx-x86_64-normal-server-slowdebug，Working directory则添加源码的根目录。`


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9b1ad08f2a2a4467ae1e98a01a33f263~tplv-k3u1fbpfcp-watermark.image)

clean_jdk8: 其中arguments填写：`CONF=macosx-x86_64-normal-server-slowdebug clean，Working directory则添加源码的根目录。`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/74e471671c7846d596706f628919ed11~tplv-k3u1fbpfcp-watermark.image)

然后再Clutom Build Targets完整具体配置，具体配置如下：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7513c650c52f4a879177ffbb702e6adc~tplv-k3u1fbpfcp-watermark.image)


接着在Clion主页面点击`Add Configuration`,新增配置`Run/Debug Configurations。`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1f542ff35c08465ea57f53b1083f0cd3~tplv-k3u1fbpfcp-watermark.image)


## 第五个环节：开始调试

在完成所有配置后，我们就可以开始调试代码了。首先在jni.cpp的create_vm中添加打一个断点。

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/272ca76522a44d1d9499944f4fede24d~tplv-k3u1fbpfcp-watermark.image)

到此debug的调试设置好了，HotSpot源码阅读环境也就搭建完了，希望大家都能成功！！！


# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得**小沙弥**我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取海量Java资源**好用的idea插件、简历模板、设计模式、多线程、架构、编程风格、中间件......**，可以关注微信公众号**Java百科全书**，最后的最后，感谢各位看官的支持！！！



