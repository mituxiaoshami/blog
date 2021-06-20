# 前言

相信大家在日常工作中多多少少有用过Redis，得益于Redis丰富的数据结构，使它在企业的项目中应用越来越广泛，充当的`角色`越来越重要。

随着`redis在企业中的应用被越挖越深`，但是也有越来越多的开发人员由于没有经历过`复杂/特定的业务`，无法对redis的命令进行深入了解，甚至可能只了解到`对象缓存`为止

所以这篇文章的目的就是带着大家走完以下两个里程碑：

- 了解`redis的常用命令`
- 如何在企业级项目中`重拳出击`

---

# 常用命令

## String

### 常用操作

- `set key value`：存入字符串键值对
- `mset key value [value ...]`：批量存入字符串键值对
- `setnx key value`：存入一个不存在的字符串键值对
- `get key`：获取一个字符串键值
- `mget key [key ...]`：批量获取字符串键值
- `del key [key ...]`：删除一个键
- `expire key seconds`：设置一个键的过期时间（秒）

### 原子加减

- `incr key`：将key中存储的数字值+1
- `decr key`：将key中存储的数字值-1
- `incrby key increment`：将key中所存储的值加上increment
- `decrby key decrement`：将key中所存储的值减去decrement

## Hash

### 常用操作

- `hset key field value`：存储一个哈希表key的值
- `hsetnx key field value`：存储一个不存在的哈希表key的键值
- `hmset key field value [field value ...]`：在一个哈希表key中存储多个键值对
- `hget key field`：获取哈希表key对应的field键值
- `hmget key field [field ...]`：批量获取哈希表key中多个field键值
- `hdel key field [field ...]`：删除哈希表key中的field键值
- `hlen key`：返回哈希表key中field的数量
- `hgetall key`：返回哈希表key中所有的键值

### 原子加减

- `hincrby key field increment`：为哈希表key中field键的值加上增量increment

## List

- `lpush key value/[value ...]`：将一个或多个值value插入到key列表的表头 **（最左边）**
- `rpush key value/[value ...]`：将一个或多个值value插入到key列表的表尾 **（最右边）**
- `lpop key`：移除并返回key列表的头元素
- `rpop key`：移除并返回key列表的尾元素
- `lrange key start stop`：返回列表key中指定区间内的元素，区间以偏移量start和stop指定
- `blpop key [key ...] timeout`：假如在**指定时间内**没有任何元素被弹出，则返回一个 nil 和**等待时长**，如果timeout=0，就一直阻塞等待。反之，返回一个含有两个元素的列表，第一个元素是被弹出元素所属的key，第二个元素是从key列表表头弹出一个元素
- `brpop key [key ...] timeout`：假如在**指定时间内**没有任何元素被弹出，则返回一个 nil 和**等待时长**，如果timeout=0，就一直阻塞等待。反之，返回一个含有两个元素的列表，第一个元素是被弹出元素所属的key，第二个元素是从key列表表尾弹出一个元素

## Set

### 常用操作

- `sadd key value`：往集合key中存入元素，元素存在则忽略
- `srem key value`：往集合key中删除元素
- `smembers key`：获取集合key中所有元素
- `srandmember key [count]`：从集合key中选出count个元素，元素不从key中删除
- `spop key [count]`：从集合key中选出count个元素，元素从key中删除

### 运算操作

- `sinter key [key ...]`：交集运算
- `sinterstore destination key [key ...]`：交集运算，将结果存入新集合destination中
- `sunion key [key ...]`：并集运算
- `sunionstore destination key [key ...]`：并集运算，将结果存入新集合destination中
- `sdiff key [key ...]`：差集运算
- `sdiffstore destination key [key ...]`：差集运算，将结果存入新集合destination中

## ZSet-有序集合

### 常用操作

- `zadd key score member [[score member]...]`：往有序集合中加入带分值的元素
- `zrem key member [member ...]`：从有序集合中删除元素
- `zscore key member`：返回有序集合key中元素member的分值
- `zincrby key increment member`：将有序集合中元素member的分值加上increment
- `zcard key`：返回有序集合key中元素的个数
- `zrange key start stop [withscores]`：正序获取有序集合key从start下标到stop下标的元素
- `zrevrange key start stop [withscores]`：倒序获取有序集合key从start下标到stop下标的元素

### 集合操作

- `zunionstore destkey numkeys key [key ...]` ：并集计算
- `zinterstore destkey numkeys key [key ...]` ：交集计算


## 其他命令

- `keys`：全量遍历，用来列出所有`满足特定正则字符串规则`的key，当redis数据量比较大时，性能比较差，要避免使用

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d15193e5b6f740e7b89fb624121b8e54~tplv-k3u1fbpfcp-watermark.image)

- `scan`：渐进式遍历，`scan cursor [MATCH pattern] [COUNT count]`

scan 参数提供了三个参数:

- 第一个：cursor：游标，如果要遍历全量数据，一开始的游标一定要是0
- 第二个：match：正则匹配
- 第三个：count：一次遍历的key的数量`参考值，底层遍历的数量不一定`，结果数量有可能不符合

---

# 应用场景

## String

### 单值缓存

- `set key value`：设置单值缓存
- `get key`：获取单值缓存

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0a0ecdea1ebf4ee08054c67544880e1d~tplv-k3u1fbpfcp-watermark.image)

### 对象缓存

- `set student value(json格式数据)`：设置**完整对象**缓存
- `MSET student:name aoteman student:age 18`：设置**对象部分数据**缓存，其实就是批量设置key，一个key就是一个对象中的一个字段
- `MGET student:name student:age`：获取**对象部分数据**缓存，就是批量获取key

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fda679f7b14643c28fb58c0f7b5f38bc~tplv-k3u1fbpfcp-watermark.image)

- 第一种方式：`set student value`，比较简单，但是更新相对来说复杂
- 第二种方式：`MSET student:name aoteman student:age 18`，比较灵活，如果遇到频繁修改某个对象的部分数据，可以使用这个命令，**比如：只需要修改商品活动价格，没必要把整个商品对象取出来转换成对象更新后，再设置回去**，但是这个命令会**创建大量的key**，也是一个弊端

### 简单版分布式锁

- `setnx lock true`：设置分布式锁，返回0代表设置失败，返回1代表设置成功

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f18f71fb68cf4dcda91c728bd79d9450~tplv-k3u1fbpfcp-watermark.image)

- `del lock`：删除/释放锁

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a4b17a954e8d4725a0e98f613b10ef47~tplv-k3u1fbpfcp-watermark.image)

- `set lock true ex 10 nx`：设置分布式锁的**超时时间**，防止程序意外终止导致死锁


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1c8c76c6e2df4ab39973187ceb8880e4~tplv-k3u1fbpfcp-watermark.image)


### 计数器

- `incr key`：计数器+1
- `get key`：获取计数器值

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/63e0167485a54b4a8a6c20f09f3e23be~tplv-k3u1fbpfcp-watermark.image)

应用场景：像`文章的阅读量、点赞、赞赏量等等需要计数的显示等等`，设置key为`article:{read/like/...}:count:{id}`这种格式即可，只要能确定**key的唯一性**，如果把这三个属性放到DB，**数据库的压力就太大了，显然不合适**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d8bef29bda1f4f5d83385666df0165f1~tplv-k3u1fbpfcp-watermark.image)

### 分布式系统全局序列号

- `incrby key value`：`incrby student 1000`：直接往key为student的值往上加1000

应用场景：`分布式系统全局序列号`，每次key值上加value，拿到返回值后，**在本机中去分配，在本机的内存中去把id+1**，如果用`incr`命令的话，每次生成都去请求redis，redis的压力会非常大

用这种方式**生成的全局序列号是不连续的**，如果有多台机器的话，如果要连续就需要其他解决方案




### 分布式session

spring + redis实现`session共享`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/38863cb940d746d3bcd5d884afbc90cf~tplv-k3u1fbpfcp-watermark.image)

## Hash

### 对象缓存

- `hmset user {userId}:name aoteman {userId}:age 18`：设置**对象**缓存
- `hmget user {userId}:name {userId}:age`：获取对象缓存

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/31fa391c21534b5697771a1ec7c38796~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/039adddc22dc464fb84cce2ebe2fc22a~tplv-k3u1fbpfcp-watermark.image)

这个应用场景有一个弊端，就是**数据量的问题**，假设user表有几千万条记录，外面就通过一个**user的key来获取**，特别是在检索时会非常耗时，而且redis是单线程模型，会非常影响吞吐量，这种key也被称之为**big key**

### 电商中的购物车模型

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d5e6506fa034449dace90a010360038c~tplv-k3u1fbpfcp-watermark.image)

- 添加商品：`hset cart:{用户id} {商品id} {商品个数}`

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c8629bbe75b34e1ba4c27d3d44980376~tplv-k3u1fbpfcp-watermark.image)


- 购物车商品数量增加：`hincrby cart:{用户id} {商品id} {商品个数}`

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/db03174bd21d42ef95807b89e1bab28d~tplv-k3u1fbpfcp-watermark.image)

- 查询购物车商品数量：`hlen cart:{用户id}`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b9375a18bbda41dabc498dd7ede5dc31~tplv-k3u1fbpfcp-watermark.image)

- 删除购物车某一个商品：`hdel cart:{用户id} {商品id}`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ed03a912e7074f25840fe59f0a4065e1~tplv-k3u1fbpfcp-watermark.image)

- 获取购物车所有商品：`hgetall cart:{用户id}`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8b68c51ce99e4a968e8b8ea73c49520c~tplv-k3u1fbpfcp-watermark.image)

### Hash的优缺点

#### 优点

1. 同类数据归类整合存储，方便数据管理，比如上面**一个user的key**可以管理所有的用户
2. 同样大小的一个对象，使用Hash比String`操作消耗内存更小`
3. 同样大小的一个对象，使用Hash比String`更节省空间`

#### 缺点

1. **Hash的过期功能不能使用在内层的field上，只能使用在外层的key上，也就是一旦key过期，key底下的数据全都生效**

2. **Redis集群架构下不适合大规模使用**，假设user的key底下有几千万的数据量，那么根据hash路由下来，这`一个key只可能分配给一台机器`，也就是几千万的数据都在一台redis上，访问也都是访问在这台redis上，发生了**数据倾斜**的场景，可以通过**分段存储**解决这种case


![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c4d3251594f2481ea5f7f6b4dec2be13~tplv-k3u1fbpfcp-watermark.image)

## List

### 数据结构

在分布式/多台机器环境下，想用`统一的数据结构`，那么redis会是一种很好的解决方案

- Stack（栈）：`lpush + lpop` = 先进后出（FILO）

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5d3c64e78d94404e9c06954e32c137a2~tplv-k3u1fbpfcp-watermark.image)

- Queue（队列）：`lpush + rpop` = 先进先出（FIFO）

因为**dijia**已经被移除并返回了，所以现在最右端是**aisi**

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/32d61b9480654db5b647e55364f3300e~tplv-k3u1fbpfcp-watermark.image)

- Blocking MQ（阻塞队列）：`lpush + brpop`，和消息队列中的消费者类似

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7bd0b73369d2445fb796983e56773282~tplv-k3u1fbpfcp-watermark.image)


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6d6410c3489e4844a545770824d178a2~tplv-k3u1fbpfcp-watermark.image)


### 推送微博和微信公众号等消息流

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8c761b3de90d4aadad766833a739b27d~tplv-k3u1fbpfcp-watermark.image)

### 推送消息流案例

微博ABAB关注了A、B这两个微博/公众号消息同理：

- A发微博消息，消息ID为100：`lpush msg:{微博ABAB的ID} {文章ID}`
- B发微博消息，文章ID为101：`lpush msg:{微博ABAB的ID} {文章ID}`
- 微博ABAB登录查看最新的微博消息：`lrange msg:{微博ABAB的ID} 0 4`：获取最新的5条消息/最左边就是最新的消息

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f649bcfb05ac41a480cb21a82e3cfc2c~tplv-k3u1fbpfcp-watermark.image)

微博的消息流推送方式就是给**所有关注它的粉丝的消息列表**里面push文章，粉丝按照顺序拿消息

但这种方式有一个致命的缺点：**微博粉丝数量过大**，假设一个公众号粉丝过千万，那么每一次发布文章，都需要推送几千万个列表，势必会造成性能瓶颈，所以就需要另外的一种方式：`pull`的方式

A发微博把消息放到一个专门属于`A的消息队列`，粉丝账号登录的时候**主动去拉取**这个队列，pull之后**存放在本地**，如下图所示：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a78feb5b59a54738a10cae24515440b9~tplv-k3u1fbpfcp-watermark.image)

## Set

### 各种抽奖场景

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e29e8b90f41440149cb4f1b0c4774cd6~tplv-k3u1fbpfcp-watermark.image)

- `sadd key {userId}`:点击参与抽奖加入集合
- `smembers key`：查看参与抽奖的所有用户
- `srandmember key [count]/spop key [count]`：抽取count名中奖者

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/85bfa65fc45542bb902fad4a1a478e02~tplv-k3u1fbpfcp-watermark.image)

中奖数量只需要改变一下`count`大小即可，比如：一等奖1名，二等奖2名，三等奖5名，使用`spop key [count]`命令让中奖的人从集合中删除


### 微信、微博、抖音点赞`(收藏、标签同理)`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2474db05507747e49a032effcf652b52~tplv-k3u1fbpfcp-watermark.image)

- 点赞：`sadd like:{文章ID} {用户ID}`
- 取消点赞：`srem like:{文章ID} {用户ID}`
- 检查用户是否点过赞：`sismember like:{文章ID} {用户ID}`：**抖音的点赞爱心高亮**就可以通过这个命令实现
- 获取点赞的用户列表：`smembers like:{消息ID}`
- 获取点赞的用户数：`scard like:{消息ID}`


### 集合操作

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/442054149a744cd0bc85da5eab25983d~tplv-k3u1fbpfcp-watermark.image)

- `sinter setA setB setC` -> {C}：**交集**
- `sunion setA setB setC` -> {A,B,C,D,E}：**并集**
- `sdiff setA setB setC` -> {A}：**以setA集合为基准，和setB、setC集合的并集做差集**


### 集合操作实现`社交关注模型`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e4f4be75db6b42e399a10ca4cb3984c8~tplv-k3u1fbpfcp-watermark.image)

#### 社交关注模型案例

- 泰罗奥特曼关注的其他奥特曼：tailuo:Set -> {aisi, dijia}

- 艾斯奥特曼关注的其他奥特曼：aisi:Set -> {tailuo, dijia, leiou, gaiya}

- 迪迦高特曼关注的其他奥特曼：dijia:Set -> {aisi, saiwen, zuofei}

第一种场景：泰罗和艾斯`共同关注的奥特曼`：`sinter tailuo:Set aisi:Set -> {dijia}`

第二种场景：泰罗`可能认识的奥特曼`/泰罗关注的奥特曼也关注的奥特曼：`sdiff aisi:Set tailuo:Set -> {leiou, gaiya}`


### 集合操作实现`筛选`

把商品的特点做成筛选条件，**一个筛选条件是一个Set集合**，最后取交集，可以实现筛选

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2d2c6e9022f24b63bf514ffb886aedfc~tplv-k3u1fbpfcp-watermark.image)

- `sadd brand:huawei 商品ID`
- `sadd brand:xiaomi 商品ID`
- `sadd phoneType:picture 商品ID`
- `sadd rom:512 商品ID`
- ......

sinter brand:huawei phoneType:picture rom:512 .... -> {商品ID集合}

用redis做这样的商品搜索显然不太合适，这个案例希望告诉大家，`Set数据结构，针对操作集合的场景有天然的优势`

## ZSet

### 排行榜：`热搜排行、音乐排行、新闻排行等等都可以用ZSET实现`

- 点击新闻：`zincrby new:{日期} 1 {消息ID}`
- 展示当日排行前十：`zrevrange new:{日期} 0 9 withscores`
- 七日搜索榜单计算：`zunionstore new:{日期开始-结束} 7 new:{日期} new:{日期+1} new:{日期+2}...`
- 展示七日榜单前十：`zrevrange new:{日期开始-结束} 0 9 withscores`

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/419b43d427dc4c449066278ecd5c73a7~tplv-k3u1fbpfcp-watermark.image)


## SCAN

### Redis分页

假设现在有这么多的key，如下图所示：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fb3ed6cf6e994e03bf9096ed213b47fe~tplv-k3u1fbpfcp-watermark.image)

我们把count设置成3，发现没有匹配到，因为`count是一次遍历的key的数量`，也就是从上面总的key中选3个key来遍历匹配，如果没有匹配到，就返回`empty array`，返回的2就是

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a0f64ecfb56c40e19f84e25fea380725~tplv-k3u1fbpfcp-watermark.image)

第一次遍历时，`cursor为0`，然后将返回结果中第一个整数作为`下一次遍历的cursor`。一直遍历到`cursor为0为止`，算是所有的key扫描完了

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b53553097f384367a3601726e8582dc9~tplv-k3u1fbpfcp-watermark.image)

这个就是redis的`分页`，但是这个分页不像mysql那样能做到`精确匹配`

#### 高能预警

`scan`虽然能做到分页，但是并非完美无瑕，如果在scan的过程中如果有键的变化（**增加、删除、修改**），那么遍历效果可能会碰到如下问题，这些问题都是需要我们在开发的时候需要考虑的

1. `新增的键可能没有遍历到`
2. `遍历出了重复的键`

因为Redis的存储结构是以`k-v`的形式存储的，类似于HashMap，key通过Hash函数定位到一个`下标或者说是桶位`，如果发生hash冲突/碰撞，又会以`类似链表`的方式存储，这个下标就是`scan的游标`

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d8faef62bec840a293721837f107ea10~tplv-k3u1fbpfcp-watermark.image)

- 如果在scan的过程中增加键，同时**键通过hash算出的桶位**已经被扫描过，那么是不会再次扫描的，就会发生新增的键可能没有遍历到

- 如果在scan的过程中增加键，导致发生**扩容**的场景，扩容之后会有**rehash机制**，那么**原先下标就会发生变化**，就会发生遍历出了重复的键



---

# 总结

通过本篇文章对`不同数据结构中的Redis常见命令`，`企业级的常见场景`以及`各自注意事项`的介绍，应该已经了解到下面几块内容：

- 不同的数据结构对应有哪些`常见的命令`
- 常见的Redis命令有哪些`企业级的应用场景`

Redis的场景肯定不仅仅如此，这里只是将我`能想到的场景`写在上面了，最终还是需要大家`自己去琢磨，透过现象看本质`，想想在自己负责的领域里，有哪些业务可以用Redis`丰富的数据结构`优化的，欢迎`底下评论区探讨`，顺便让我也`开开眼界`

---

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得`小沙弥`我有点东西的话 求点赞👍 求关注❤️ 求分享👥 ，因为这将是我输出更多优质文章的动力，感谢！！！

如果想获取`Redis相关书籍`，可以关注微信公众号`Java百科全书`，`输入Redis，即可获得`

最后感谢各位看官的支持，我们下期再见！