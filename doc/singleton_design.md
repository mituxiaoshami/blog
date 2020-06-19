## 单例模式：如何创建单一对象优化系统性能


### 什么是单例模式？

单例模式的核心在于可以保证一个类创建一个实例，并提供一个访问它的全局访问入口

### 单例模式的作用

在一个系统中，一个类经常会被使用在不同的地方，通过单例模式，我们可以避免多次创建多个实例，从而节约系统资源。

### 单例模式的三个基本要点

1. 类只能有一个实例
2. 必须自行创建这个实例
3. 必须自行向整个系统提供这个实例

例子：

```

//饿汉模式
public final class Singleton {
    
    //自行创建实例
    private static Singleton instance=new Singleton();
    
    //构造函数
    private Singleton(){}
    
    //通过该函数向整个系统提供实例
    public static Singleton getInstance(){
        return instance;
    }
}

```

### 饿汉模式

在上面的例子中，可以发现，使用了 static 修饰了成员变量 instance，所以该变量会在类初始化的过程中被收集进类构造器即"clinit"方法中。

在多线程场景下，JVM 会保证只有一个线程能执行该类的"clinit"方法，其它线程将会被阻塞等待。

等到唯一的一次"clinit"方法执行完成，其它线程将不会再执行 方法，转而执行自己的代码。

也就是说：static 修饰了成员变量 instance，在多线程的情况下能保证只实例化一次。

#### 饿汉模式的单例

这种方式实现的单例模式，在类初始化阶段就已经在堆内存中开辟了一块内存，用于存放实例化对象，所以也称为饿汉模式。

#### 饿汉模式的优点

可以保证多线程情况下实例的唯一性，而且 getInstance 直接返回唯一实例，性能非常高。


#### 饿汉模式的缺陷

然而，在类成员变量比较多，或变量比较大的情况下，这种模式可能会在没有使用类对象的情况下，一直占用堆内存。试想下，如果一个第三方开源框架中的类都是基于饿汉模式实现的单例，这将会初始化所有单例类，无疑是灾难性的。

### 懒汉模式

懒汉模式就是为了避免直接加载类对象时提前创建对象的一种单例设计模式。该模式使用懒加载方式，只有当系统使用到类对象时，才会将实例加载到堆内存中。

例子：

```

//懒汉模式
public final class Singleton {

    //不实例化
    private static Singleton instance= null;
    
    //构造函数
    private Singleton(){}
    
    //通过该函数向整个系统提供实例
    public static Singleton getInstance(){
    
        //当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){
        
            //实例化对象
            instance = new Singleton();
            
        }
        
        //返回已存在的对象
        return instance;
    }
}

```

#### 懒汉模式的线程不安全性

虽然避免直接加载类对象时提前创建对象，但是如果运行在多线程下，就会出现实例化多个类对象的情况

模拟场景：当线程 A 进入到 if 判断条件后，开始实例化对象，此时 instance 依然为 null；又有线程 B 进入到 if 判断条件中，之后也会通过条件判断，进入到方法里面创建一个实例对象。

##### 第一种解决办法：synchronized

```

//懒汉模式 + synchronized同步锁
public final class Singleton {
 
    //不实例化
    private static Singleton instance= null;
    
    //构造函数
    private Singleton(){}
    
    //加同步锁，通过该函数向整个系统提供实例
    public static synchronized Singleton getInstance(){
        
        //当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){
            
            //实例化对象
            instance = new Singleton();
        }
        
        //返回已存在的对象
        return instance;
    }
}

```

缺陷：同步锁会增加锁竞争，带来系统性能开销，从而导致系统性能下降，因此这种方式也会降低单例模式的性能。还有，每次请求获取类对象时，都会通过 getInstance() 方法获取，除了第一次为 null，其它每次请求基本都是不为 null 的。

在没有加同步锁之前，是因为 if 判断条件为 null 时，才导致创建了多个实例。基于以上两点，我们可以考虑将同步锁放在 if 条件里面，这样就可以减少同步锁资源竞争。


###### synchronized同步的第一次优化

```

//懒汉模式 + synchronized同步锁
public final class Singleton {

    //不实例化
    private static Singleton instance= null;
    
    //构造函数
    private Singleton(){}
    
    //加同步锁，通过该函数向整个系统提供实例
    public static Singleton getInstance(){
    
        //当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){
          synchronized (Singleton.class){
              //实例化对象
              instance = new Singleton();
          } 
        }        
        //返回已存在的对象
        return instance;
    }
}

```

缺陷：这种方式依然会创建多个实例。因为当多个线程进入到 if 判断条件里，虽然有同步锁，但是进入到判断条件里面的线程依然会依次获取到锁创建对象，然后再释放同步锁。
所以我们还需要在同步锁里面再加一个判断条件：

```

//懒汉模式 + synchronized同步锁 + double-check
public final class Singleton {
    
    //不实例化
    private static Singleton instance= null;
    
    //构造函数
    private Singleton(){}
    
    //加同步锁，通过该函数向整个系统提供实例
    public static Singleton getInstance(){
    
        //第一次判断，当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){   
          //同步锁
          synchronized (Singleton.class){
             //第二次判断
             if(null == instance){
                //实例化对象
                instance = new Singleton();
             }
          } 
        }
        //返回已存在的对象
        return instance;
    }
}
```

以上这种方式，通常被称为 Double-Check，它可以大大提高支持多线程的懒汉模式的运行性能。
但是这种方式也不能保证万无一失，这和Happens-Before 规则有关

#### Happens-Before的简单介绍

编译器为了尽可能地减少寄存器的读取、存储次数，会充分复用寄存器的存储值，比如以下代码，如果没有进行重排序优化，正常的执行顺序是步骤 1/2/3，而在编译期间进行了重排序优化之后，执行的步骤有可能就变成了步骤 1/3/2，这样就能减少一次寄存器的存取次数。

```
//步骤1：加载a变量的内存地址到寄存器中，加载1到寄存器中，CPU通过mov指令把1写入到寄存器指定的内存中
int a = 1;

//步骤2 加载b变量的内存地址到寄存器中，加载2到寄存器中，CPU通过mov指令把2写入到寄存器指定的内存中
int b = 2;

//步骤3 重新加载a变量的内存地址到寄存器中，加载1到寄存器中，CPU通过mov指令把1写入到寄存器指定的内存中
a = a + 1;

```

在 JMM 中，重排序是十分重要的一环，特别是在并发编程中。如果 JVM 可以对它们进行任意排序以提高程序性能，也可能会给并发编程带来一系列的问题。

例如：Double-Check 的单例问题，假设类中有其它的属性也需要实例化，这个时候，除了要实例化单例类本身，还需要对其它属性也进行实例化：

```

//懒汉模式 + synchronized同步锁 + double-check
public final class Singleton {
    //不实例化
    private static Singleton instance= null;
    //list属性
    public List<String> list = null;
    //构造函数
    private Singleton(){
      list = new ArrayList<String>();
    }
    //加同步锁，通过该函数向整个系统提供实例
    public static Singleton getInstance(){
        //第一次判断，当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){
          //同步锁
          synchronized (Singleton.class){
             //第二次判断
             if(null == instance){
                //实例化对象
                instance = new Singleton();
             }
          } 
        }
        //返回已存在的对象
        return instance;
    }
}

```

在执行 instance = new Singleton(); 代码时，正常情况下，实例过程这样的：

1. 给 Singleton 分配内存；
2. 调用 Singleton 的构造函数来初始化成员变量；
3. 将 Singleton 对象指向分配的内存空间（执行完这步 singleton 就为非 null 了）。

如果虚拟机发生了重排序优化，这个时候步骤 3 可能发生在步骤 2 之前：

如果初始化线程刚好完成步骤 3，而步骤 2 没有进行时，则刚好有另一个线程到了第一次判断，这个时候判断为非 null，并返回对象使用，这个时候实际没有完成其它属性的构造，因此使用这个属性就很可能会导致异常。

在这里，Synchronized 只能保证可见性、原子性，无法保证执行的顺序。

这个时候，就体现出 Happens-Before 规则的重要性了。通过字面意思，你可能会误以为是前一个操作发生在后一个操作之前。然而真正的意思是，前一个操作的结果可以被后续的操作获取。这条规则规范了编译器对程序的重排序优化。

#### 通过使用volatile关键字防止指令重排

volatile 关键字可以保证线程间变量的可见性，简单地说就是当线程 A 对变量 X 进行修改后，在线程 A 后面执行的其它线程就能看到变量 X 的变动。除此之外，volatile 在 JDK1.5 之后还有一个作用就是阻止局部重排序的发生，也就是说，volatile 变量的操作指令都不会被重排序。

#### 懒汉模式的最终设计结果

```

//懒汉模式 + synchronized同步锁 + double-check
public final class Singleton {

    //不实例化
    private volatile static Singleton instance= null;
    
    //list属性
    public List<String> list = null;
    
    //构造函数
    private Singleton(){
      list = new ArrayList<String>();
    }
    //加同步锁，通过该函数向整个系统提供实例
    public static Singleton getInstance(){
        //第一次判断，当instance为null时，则实例化对象，否则直接返回对象
        if(null == instance){
          //同步锁
          synchronized (Singleton.class){
             //第二次判断
             if(null == instance){
                //实例化对象
                instance = new Singleton();
             }
          } 
        }
        //返回已存在的对象
        return instance;
    }
}

```

### 内部类实现

以上这种同步锁 +Double-Check 的实现方式相对来说，复杂且加了同步锁，那有没有稍微简单一点儿的可以实现线程安全的懒加载方式呢？

在饿汉模式中，我们使用了 static 修饰了成员变量 instance，所以该变量会在类初始化的过程中被收集进类构造器即"clinit"方法中。在多线程场景下，JVM 会保证只有一个线程能执行该类的"clinit"方法，其它线程将会被阻塞等待。这种方式可以保证内存的可见性、顺序性以及原子性。

我们可以利用这个特性在 Singleton 类中创建一个内部类来实现成员变量的初始化，则可以避免多线程下重复创建对象的情况发生。这种方式，只有在第一次调用 getInstance() 方法时，才会加载 InnerSingleton 类，而只有在加载 InnerSingleton 类之后，才会实例化创建对象。具体实现如下：

```

//懒汉模式 内部类实现
public final class Singleton {

  // list属性
  public List<String> list = null;

  //构造函数
  private Singleton() {
    list = new ArrayList<String>();
  }

  // 内部类实现
  public static class InnerSingleton {
    //自行创建实例
    private static Singleton instance=new Singleton();
  }

  // 返回内部类中的静态变量
  public static Singleton getInstance() {
    return InnerSingleton.instance;
  }
}

```

### 枚举实现

```
public class SinletonExample5 {
    private static SinletonExample5 instance = null;

    // 私有构造函数
    private SinletonExample5(){
    }

    public static SinletonExample5 getInstance(){
        return Sinleton.SINLETON.getInstance();
    }

    private enum Sinleton{
        SINLETON;

        private SinletonExample5 singleton;

        // JVM保证这个方法只调用一次
        Sinleton(){
            singleton = new SinletonExample5();
        }

        public SinletonExample5 getInstance(){
            return singleton;
        }
    }
}

```

## 总结

单例的实现方式其实有很多，但总结起来就两种：饿汉模式和懒汉模式，我们可以根据自己的需求来做选择。

如果我们在程序启动后，一定会加载到类，那么用饿汉模式实现的单例简单又实用；如果我们是写一些工具类，则优先考虑使用懒汉模式，因为很多项目可能会引用到 jar 包，但未必会使用到这个工具类，懒汉模式实现的单例可以避免提前被加载到内存中，占用系统资源。

