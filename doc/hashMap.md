## 深入浅出HashMap的设计与优化

### 常用的集合底层数据结构

数组：采用一段连续的存储单元来存储数据。对于指定下标的查找，时间复杂度为 O(1)，但在数组中间以及头部插入数据时，需要复制移动后面的元素。

链表：一种在物理存储单元上非连续、非顺序的存储结构，数据元素的逻辑顺序是通过链表中的指针链接次序实现的。

链表由一系列结点（链表中每一个元素）组成，结点可以在运行时动态生成。每个结点都包含“存储数据单元的数据域”和“存储下一个结点地址的指针域”这两个部分。
由于链表不用必须按顺序存储，所以链表在插入的时候可以达到 O(1) 的复杂度，但查找一个结点或者访问特定编号的结点需要 O(n) 的时间。

哈希表：根据关键码值（Key value）直接进行访问的数据结构。通过把关键码值映射到表中一个位置来访问记录，以加快查找的速度。这个映射函数叫做哈希函数，存放记录的数组就叫做哈希表。

树：由 n（n≥1）个有限结点组成的一个具有层次关系的集合，就像是一棵倒挂的树。

### HashMap 的底层实现结构

#### HashMap的基本概念

HashMap是基于哈希表实现的，继承了 AbstractMap 并且实现了 Map 接口。

哈希表将键的 Hash 值映射到内存地址，即根据键获取对应的值，并将其存储到内存地址。也就是说 HashMap 是根据键的 Hash 值来决定对应值的存储位置。通过这种索引方式，HashMap 获取数据的速度会非常快。

例如：键值对（x，“aa”）时，哈希表会通过哈希函数 f(x) 得到"aa"的实现存储位置。

问题：如果再来一个 (y，“bb”)，哈希函数 f(y) 的哈希值跟之前 f(x) 是一样的，这样两个对象的存储地址就冲突了，这种现象就被称为哈希冲突。那么哈希表是怎么解决的呢？方式有很多，比如，开放定址法、再哈希函数法和链地址法。

解决hash冲突的几种场景：ThreadLocal中的解决hash冲突用的线性探测法就是开放定址法的一种，HashMap用的是链地址法

开放定址法：当发生哈希冲突时，如果哈希表未被装满，说明在哈希表中必然还有空位置，那么可以把 key 存放到冲突位置后面的空位置上去。这种方法存在着很多缺点，例如，查找、扩容等，所以不建议作为解决哈希冲突的首选。

再哈希法：产生地址冲突时再计算另一个哈希函数地址，直到冲突不再发生，这种方法不易产生“聚集”，但却增加了计算时间。如果我们不考虑添加元素的时间成本，且对查询元素的要求极高，就可以考虑使用这种算法设计。

HashMap 则是综合考虑了所有因素，采用链地址法解决哈希冲突问题。这种方法是采用了数组（哈希表）+ 链表的数据结构，当发生哈希冲突时，就用一个链表结构存储相同 Hash 值的数据。

#### HashMap的重要属性

```

/**
 * hashMap的默认初始容量(默认16 默认数组长度 2的4次方 )，必须是2的幂次方
 */
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

/**
 * 最大的数组长度(2的30次方)
 */
static final int MAXIMUM_CAPACITY = 1 << 30;

/**
 * 默认的加载因子(0.75 数组长度达到4分之三,需要扩容)
 * 为什么是 0.75 这个值呢？
 * 这是因为对于使用链表法的哈希表来说，查找一个元素的平均时间是 O(1+n)，
 * 这里的 n 指的是遍历链表的长度，因此加载因子越大，
 * 对空间的利用就越充分，这就意味着链表的长度越长，查找效率也就越低。
 * 如果设置的加载因子太小，那么哈希表的数据将过于稀疏，
 * 对空间造成严重浪费。  
 */
static final float DEFAULT_LOAD_FACTOR = 0.75f;

/**
 * 当链表节点长度超过8时,那么就转成红黑树结构(此时数组长度必须要大于等于64)
 */
static final int TREEIFY_THRESHOLD = 8;

/**
 * 当红黑树节点小于6时,又转成链表结构
 */
static final int UNTREEIFY_THRESHOLD = 6;

/**
 * 转红黑树的前提条件(数组长度不小于64 )
 */
static final int MIN_TREEIFY_CAPACITY = 64;


/**
 * 节点数组
 */
transient Node<K,V>[] table;


/**
 * 因为Map的key是不能重复的，所以HashMap中所有的key都统一封装在这个Set中
 */
transient Set<Map.Entry<K,V>> entrySet;


/**
 * HashMap的长度
 */
transient int size;

/**
 * 记录HashMap的修改次数(hashMap不是线程安全的)
 */
transient int modCount;


/**
 * 阈值(达到这个值就需要扩容)
 */
int threshold;

/**
 * 加载因子(默认是0)
 */
final float loadFactor;

```

#### HashMap的初始化

```
/**
 * new HashMap()的时候没有做任何的事，只是把加载因子赋值为0.75
 */
public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; 
    }
```

#### HashMap的put方法

```
/**
 * put方法只是调了一个putVal方法，调之前首先调了一个hash方法
 */
public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
    
/**
 * hash方法是根据key的hashCode算出hash值
 */
static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

/**
 * put值的方法
 */
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        
        // 由于构造方法中并没有对table做初始化操作, 此时table为null,
        // 把table的引用赋值给tab,tab自然也是null
        if ((tab = table) == null || (n = tab.length) == 0)
            // 然后会调用resize()方法,这个resize()方法第一个作用是扩容,另一个作用是初始化
            // 这一步过后，n被赋值为16(默认的数组长度)
            n = (tab = resize()).length;
        // i = (n - 1) & hash] (数组长度-1)&hash值,得到一个数组的下标,tab[i]目前是null
        // 赋值给p之后，p也是null，走到里面，调用newNode方法新建一个Node对象赋值给tab[i]
        if ((p = tab[i = (n - 1) & hash]) == null)
            // 因为新节点没有下一个节点,所以最后一个参数为null
            tab[i] = newNode(hash, key, value, null);
        else {
            // 当相同的数据下标值不为空
            Node<K,V> e; K k;
            // 如果hash值相同,并且(key相同或者key的值不为空但是key的值相同)
            // 那么就把p赋值给e
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            // 现在p上面只有1个节点，所以不是红黑树，所以不走下面
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 这里就是key不相同，但是hash值相同，也就是说，发生hash碰撞了
                // 死循环
                for (int binCount = 0; ; ++binCount) {
                    // 如果p没有下一个节点,也就是遍历到尾节点,那么才会插入一个新的节点(尾插法)
                    if ((e = p.next) == null) {
                        // 那么就new一个节点
                        p.next = newNode(hash, key, value, null);
                        // 如果节点数量大于等于红黑树的节点,那么转换成红黑树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        // 返回
                        break;
                    }
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // e就是冲突前原先的Node 
            if (e != null) { // existing mapping for key
                // e.value就是原先的Node上的value
                V oldValue = e.value;
                // 判断标识为false或者原先值为null，那么覆盖
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                // 空的方法,先不管
                afterNodeAccess(e);
                // 返回原先的值,因为节点数量没有变,所以modCount没有++
                return oldValue;
            }
        }
        // 修改次数+1
        ++modCount;
        // 初始化后size为0,++size为1小于12,所以这里不扩容
        if (++size > threshold)
            resize();
        // 这里是一个空的方法,先不去管 
        afterNodeInsertion(evict);
        // 添加操作没有新的数据,返回给null
        return null;
    }
    
    
/**
 * 第一个作用：扩容
 * 第二个作用：初始化
 * 这里先将初始化，不讲扩容的逻辑,后面会将扩容的逻辑
 */    
final Node<K,V>[] resize() {
        
        // 根据new HashMap()构造方法走到这里时,这个table为null,oldTab自然也为null
        Node<K,V>[] oldTab = table;
        // 三目运算返回0,也就是oldCap为0(旧数组长度为0)
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // int类型,默认初始化为0(旧的使用长度为0)
        int oldThr = threshold;
        // 定义新数组长度和新数组使用长度都为0
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               
            // 由于第一个初始化空的构造函数后,oldCap和oldThr都为0,所以这里赋值新的数组长度和新的使用长度为默认的数组长度和默认的阈值
            // newCap=16,newThr=12
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        // 初始化后这里不为0所以不进这里面
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        // threshold阈值为12,也就是说，达到四分之三的时候扩容
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
            // 初始化新的Node数组,长度为16
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        // 把新的newTab赋值给table,这里以后,table也就不为空
        table = newTab;
        // oldTab为null,不走这里
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        // 返回新的数组
        return newTab;
    }
    
    
    /**
     * 转红黑树的方法
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        // 如果数组为null或者数组长度小于MIN_TREEIFY_CAPACITY也就是64
        // 那么进行扩容，而不是转红黑树
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }
    
    
/**
 * 第一个作用：扩容
 * 第二个作用：初始化
 * 这里讲扩容的逻辑
 */ 
final Node<K,V>[] resize() {
        
        // table不为空
        Node<K,V>[] oldTab = table;
        // oldCap=16(默认情况下)
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // oldThr=12
        int oldThr = threshold;
        int newCap, newThr = 0;
        // 这里不为0
        if (oldCap > 0) {
            // 如果超出最大容量,那么不会扩容,返回最大长度
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 旧的数组容量往左移一位，那么相当于乘以2，赋值给newCap，如果新的数组长度小于最大长度
            // 并且原先的数组长度大于默认的数组长度(16)
            // 那么新的数组使用长度也变成原先的2倍(新的阈值也会变成原先的2倍)
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        // newThr=24也不等于0，所以不走这里面
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        // 新的阈值也变成24(扩容后的阈值变成24)
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        // 新的数组长度也变成32
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            // 把每一个值去覆盖,这里遍历16次
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    // 当前下面只有一个节点
                    if (e.next == null)
                        // 只有一个节点的话那么就重新计算下标，然后替换
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        // 如果说不止有一个节点,并且是红黑树节点,但是因为扩容，所以要根据数组长度重新计算下标，重新计算，位置肯定发生变化
                        // 如果扩容后，原先的红黑树节点小于6个，那么需要转成链表
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

#### HashMap的疑问

1. 为什么要用hash()方法计算hashCode呢，如果我们没有使用hash()方法计算hashCode，而是直接使用对象的hashCode值，会出现什么问题呢？
   
   答：假设要添加两个对象 a 和 b，如果数组长度是 16，这时对象 a 和 b 通过公式 (n - 1) & hash 运算，也就是 (16-1)＆a.hashCode 和 (16-1)＆b.hashCode，15 的二进制为 0000000000000000000000000001111，
假设对象 A 的 hashCode 为 1000010001110001000001111000000，对象 B 的 hashCode 为 0111011100111000101000010100000，
会发现上述与运算结果都是 0。这样的哈希结果就太让人失望了，很明显不是一个好的哈希算法。
   
   但如果我们将 hashCode 值右移 16 位（h >>> 16 代表无符号右移 16 位），也就是取 int 类型的一半，刚好可以将该二进制数对半切开，并且使用位异或运算（如果两个数对应的位置相反，则结果为 1，反之为 0），这样的话，就能避免上面的情况发生。
   这就是 hash() 方法的具体实现方式。
   
   简而言之，就是尽量打乱 hashCode 真正参与运算的低 16 位。

2. 为什么要设计(n - 1) & hash

   答：这里的 n 代表哈希表(数组)的长度，哈希表习惯将长度设置为 2 的 n 次方，这样恰好可以保证 (n - 1) & hash 的计算得到的索引值总是位于 table 数组的索引之内。例如：hash=15，n=16 时，结果为 15；hash=17，n=16 时，结果为 1。
