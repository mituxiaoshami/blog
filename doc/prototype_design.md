## 原型模式与享元模式：提升系统性能的利器

原型模式：在创建多个实例时，对创建过程的性能进行调优

享元模式：少创建实例的方式，来调优系统性能

使用场景：在有些场景下，我们需要重复创建多个实例，例如在循环体中赋值一个对象，此时我们就可以采用原型模式来优化对象的创建过程；而在有些场景下，我们则可以避免重复创建多个实例，在内存中共享对象就好了。

### 原型模式

原型模式是通过给出一个原型对象来指明所创建的对象的类型，然后使用自身实现的克隆接口来复制这个原型对象，该模式就是用这种方式来创建出更多同类型的对象。

使用这种方式创建新的对象的话，就无需再通过 new 实例化来创建对象了。这是因为 Object 类的 clone 方法是一个本地方法，它可以直接操作内存中的二进制流，所以性能相对 new 实例化来说，更佳。

#### 实现原型模式

```

   //实现Cloneable 接口的原型抽象类Prototype 
   class Prototype implements Cloneable {
        //重写clone方法
        public Prototype clone(){
            Prototype prototype = null;
            try{
                prototype = (Prototype)super.clone();
            }catch(CloneNotSupportedException e){
                e.printStackTrace();
            }
            return prototype;
        }
    }
    //实现原型类
    class ConcretePrototype extends Prototype{
        public void show(){
            System.out.println("原型模式实现类");
        }
    }

    public class Client {
        public static void main(String[] args){
            ConcretePrototype cp = new ConcretePrototype();
            for(int i=0; i< 10; i++){
                ConcretePrototype clonecp = (ConcretePrototype)cp.clone();
                clonecp.show();
            }
        }
    }

```

#### 实现原型类的三个必备条件

1. 实现 Cloneable 接口：Cloneable 接口与序列化接口的作用类似，它只是告诉虚拟机可以安全地在实现了这个接口的类上使用 clone 方法。在 JVM 中，只有实现了 Cloneable 接口的类才可以被拷贝，否则会抛出 CloneNotSupportedException 异常。
2. 重写 Object 类中的 clone 方法：在 Java 中，所有类的父类都是 Object 类，而 Object 类中有一个 clone 方法，作用是返回对象的一个拷贝。
3. 在重写的 clone 方法中调用 super.clone()：默认情况下，类不具备复制对象的能力，需要调用 super.clone() 来实现。

#### 对象赋值问题

原型模式的主要特征就是使用 clone 方法复制一个对象。通常，有些人会误以为 Object a=new Object();Object b=a; 这种形式就是一种对象复制的过程，然而这种复制只是对象引用的复制，也就是 a 和 b 对象指向了同一个内存地址，如果 b 修改了，a 的值也就跟着被修改了。

```


class Student {  
    private String name;  
  
    public String getName() {  
        return name;  
    }  
  
    public void setName(String name) {  
        this.name= name;  
    }  
      
}  
public class Test {  
      
    public static void main(String args[]) {  
        Student stu1 = new Student();  
        stu1.setName("test1");  

        Student stu2 = stu1;  
        stu2.setName("test2");  
 
        System.out.println("学生1:" + stu1.getName());  
        System.out.println("学生2:" + stu2.getName());  
    }  
}

如果是复制对象，此时打印的日志应该为：

学生1:test1
学生2:test2

然而，实际上是：

学生1:test2
学生2:test2
```

通过 clone 方法复制的对象才是真正的对象复制，clone 方法赋值的对象完全是一个独立的对象。刚刚讲过了，Object 类的 clone 方法是一个本地方法，它直接操作内存中的二进制流，特别是复制大对象时，性能的差别非常明显。我们可以用 clone 方法再实现一遍以上例子。

```

//学生类实现Cloneable接口
class Student implements Cloneable{  
    private String name;  //姓名
  
    public String getName() {  
        return name;  
    }  
  
    public void setName(String name) {  
        this.name= name;  
    } 
   //重写clone方法
   public Student clone() { 
        Student student = null; 
        try { 
            student = (Student) super.clone(); 
            } catch (CloneNotSupportedException e) { 
            e.printStackTrace(); 
            } 
            return student; 
   } 
      
}  
public class Test {  
      
    public static void main(String args[]) {  
        Student stu1 = new Student();  //创建学生1
        stu1.setName("test1");  

        Student stu2 = stu1.clone();  //通过克隆创建学生2
        stu2.setName("test2");  
 
        System.out.println("学生1:" + stu1.getName());  
        System.out.println("学生2:" + stu2.getName());  
    }  
}

运行结果：

学生1:test1
学生2:test2

```

#### 深拷贝和浅拷贝

在调用 super.clone() 方法之后，首先会检查当前对象所属的类是否支持 clone，也就是看该类是否实现了 Cloneable 接口。

如果支持，则创建当前对象所属类的一个新对象，并对该对象进行初始化，使得新对象的成员变量的值与当前对象的成员变量的值一模一样，但对于其它对象的引用以及 List 等类型的成员属性，则只能复制这些对象的引用了。所以简单调用 super.clone() 这种克隆对象方式，就是一种浅拷贝。

所以，当我们在使用 clone() 方法实现对象的克隆时，就需要注意浅拷贝带来的问题。我们再通过一个例子来看看浅拷贝。

```

//定义学生类
class Student implements Cloneable{  
    private String name; //学生姓名
    private Teacher teacher; //定义老师类
  
    public String getName() {  
        return name;  
    }  
  
    public void setName(String name) {  
        this.name = name;  
    } 

    public Teacher getTeacher() {  
        return teacher;  
    }  
  
    public void setTeacher(Teacher teacher) {  
        this.teacher = teacher;  
    } 
   //重写克隆方法
   public Student clone() { 
        Student student = null; 
        try { 
            student = (Student) super.clone(); 
            } catch (CloneNotSupportedException e) { 
            e.printStackTrace(); 
            } 
            return student; 
   } 
      
}  

//定义老师类
class Teacher implements Cloneable{  
    private String name;  //老师姓名
  
    public String getName() {  
        return name;  
    }  
  
    public void setName(String name) {  
        this.name= name;  
    } 

   //重写克隆方法，堆老师这个类进行克隆
   public Teacher clone() { 
        Teacher teacher= null; 
        try { 
            teacher= (Teacher) super.clone(); 
            } catch (CloneNotSupportedException e) { 
            e.printStackTrace(); 
            } 
            return student; 
   } 
      
}
public class Test {  
      
    public static void main(String args[]) {
        Teacher teacher = new Teacher (); //定义老师1
        teacher.setName("刘老师");
        Student stu1 = new Student();  //定义学生1
        stu1.setName("test1");           
        stu1.setTeacher(teacher);
        
        Student stu2 = stu1.clone(); //定义学生2
        stu2.setName("test2");  
        stu2.getTeacher().setName("王老师");//修改老师
        System.out.println("学生" + stu1.getName + "的老师是:" + stu1.getTeacher().getName);  
        System.out.println("学生" + stu1.getName + "的老师是:" + stu2.getTeacher().getName);  
    }  
}

运行结果：


学生test1的老师是：王老师
学生test2的老师是：王老师

```

观察以上运行结果，我们可以发现：在我们给学生 2 修改老师的时候，学生 1 的老师也跟着被修改了。这就是浅拷贝带来的问题。

我们可以通过深拷贝来解决这种问题，其实深拷贝就是基于浅拷贝来递归实现具体的每个对象，代码如下：

```

   public Student clone() { 
        Student student = null; 
        try { 
            student = (Student) super.clone(); 
            Teacher teacher = this.teacher.clone();//克隆teacher对象
            student.setTeacher(teacher);
            } catch (CloneNotSupportedException e) { 
            e.printStackTrace(); 
            } 
            return student; 
   } 

```

#### 原型模式的适用场景

1. 在一些重复创建对象的场景下，我们就可以使用原型模式来提高对象的创建性能。例如，我在开头提到的，循环体内创建对象时，我们就可以考虑用 clone 的方式来实现。

```

for(int i=0; i<list.size(); i++){
  Student stu = new Student(); 
  ...
}


可以优化为：

Student stu = new Student(); 
for(int i=0; i<list.size(); i++){
 Student stu1 = (Student)stu.clone();
  ...
}
```

2. 除此之外，原型模式在开源框架中的应用也非常广泛。例如 Spring 中，@Service 默认都是单例的。用了私有全局变量，若不想影响下次注入或每次上下文获取 bean，就需要用到原型模式，我们可以通过以下注解来实现，@Scope(“prototype”)。

### 享元模式

通过一个简单的例子来实现一个享元模式：

```

//抽象享元类
interface Flyweight {
    //对外状态对象
    void operation(String name);
    //对内对象
    String getType();
}


//具体享元类
class ConcreteFlyweight implements Flyweight {
    private String type;

    public ConcreteFlyweight(String type) {
        this.type = type;
    }

    @Override
    public void operation(String name) {
        System.out.printf("[类型(内在状态)] - [%s] - [名字(外在状态)] - [%s]\n", type, name);
    }

    @Override
    public String getType() {
        return type;
    }
}



//享元工厂类
class FlyweightFactory {
    private static final Map<String, Flyweight> FLYWEIGHT_MAP = new HashMap<>();//享元池，用来存储享元对象

    public static Flyweight getFlyweight(String type) {
        if (FLYWEIGHT_MAP.containsKey(type)) {//如果在享元池中存在对象，则直接获取
            return FLYWEIGHT_MAP.get(type);
        } else {//在响应池不存在，则新创建对象，并放入到享元池
            ConcreteFlyweight flyweight = new ConcreteFlyweight(type);
            FLYWEIGHT_MAP.put(type, flyweight);
            return flyweight;
        }
    }
}



public class Client {

    public static void main(String[] args) {
        Flyweight fw0 = FlyweightFactory.getFlyweight("a");
        Flyweight fw1 = FlyweightFactory.getFlyweight("b");
        Flyweight fw2 = FlyweightFactory.getFlyweight("a");
        Flyweight fw3 = FlyweightFactory.getFlyweight("b");
        fw1.operation("abc");
        System.out.printf("[结果(对象对比)] - [%s]\n", fw0 == fw2);
        System.out.printf("[结果(内在状态)] - [%s]\n", fw1.getType());
    }
}


输出结果：

[类型(内在状态)] - [b] - [名字(外在状态)] - [abc]
[结果(对象对比)] - [true]
[结果(内在状态)] - [b]
```

观察以上代码运行结果，我们可以发现：如果对象已经存在于享元池中，则不会再创建该对象了，而是共用享元池中内部数据一致的对象。这样就减少了对象的创建，同时也节省了同样内部数据的对象所占用的内存空间。

### 享元模式的使用场景

享元模式在实际开发中的应用也非常广泛。例如 Java 的 String 字符串，在一些字符串常量中，会共享常量池中字符串对象，从而减少重复创建相同值对象，占用内存空间。代码如下：

```

 String s1 = "hello";
 String s2 = "hello";
 System.out.println(s1==s2);//true
 
```

在日常开发中的应用。例如，线程池就是享元模式的一种实现；将商品存储在应用服务的缓存中，那么每当用户获取商品信息时，则不需要每次都从 redis 缓存或者数据库中获取商品信息，并在内存中重复创建商品信息了。

## 原型模式与享元模式的总结

在不得已需要重复创建大量同一对象时，我们可以使用原型模式，通过 clone 方法复制对象，这种方式比用 new 和序列化创建对象的效率要高；在创建对象时，如果我们可以共用对象的内部数据，那么通过享元模式共享相同的内部数据的对象，就可以减少对象的创建，实现系统调优。

## 扩展

1. new一个对象和clone一个对象，性能差在哪里呢？

一个对象通过new创建的过程为：

1. 在内存中开辟一块空间；
2. 在开辟的内存空间中创建对象；
3. 调用对象的构造函数进行初始化对象。

而一个对象通过clone创建的过程为：
1. 根据原对象内存大小开辟一块内存空间；
2. 复制已有对象，克隆对象中所有属性值。

相对new来说，clone少了调用构造函数。如果构造函数中存在大量属性初始化或大对象，则使用clone的复制对象的方式性能会好一些。