# 前言

Redis的数据都存放在内存中，如果没有配置持久化，redis重启后数据就全丢失了，那肯定不能让`珍贵的数据`因为这种外力而丢失，所以需要开启redis的`持久化功能`，将数据保存到磁盘上，当redis重启后，可以从磁盘中恢复数据。

这篇文章，从三个方面给大家一个比较直观的Redis持久化

- Redis的`持久化方式`有哪几种
- Redis的`数据恢复`步骤有哪些？
- 企业中Redis的`持久化策略`应该怎么选择？

下面就从`Redis持久化的方式`开始吧！！！！

# Redis持久化的方式

redis提供两种方式进行持久化：

- 一种是RDB持久化（将Reids在内存中的数据库记录定时dump到磁盘上的RDB持久化）
- 另外一种是AOF持久化（将Reids的操作日志以追加的方式写入文件）。

## RDB快照(snapshot)

在默认的情况下，redis将内存数据库快照保存在名为`dump.rdb`的二进制文件中

### 生成dump.rdb文件的条件

#### 自动触发

可以在`redis.conf`文件中设置，让它在`N秒时间内至少有M个改动`这一条件被满足时，自动保存一次，条件策略如下图所示：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/56484083681c41d6b3b12ff3173b77a8~tplv-k3u1fbpfcp-watermark.image)

例如：`save 60 10000`：意味着只要在`60秒内至少有10000个操作`，redis就会自动保存一次

可以添加多条策略，只要符合其中一条，redis就会自动保存一次，`关闭RDB`只需要将所有的`save保存策略`注释掉就可以了

#### 手动执行

进入redis的客户端执行`save命令`或`bgsave命令`可以生成`dump.rdb`文件，每次执行都会将所有redis的内存快照到一个新的rdb文件中，并覆盖原有rdb快照文件

例如：已存的`dump.rdb`文件的创建时间是`20:19`

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7a14dd7ac6974182937bd88eebfd6163~tplv-k3u1fbpfcp-watermark.image)

执行一下`save`命令：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e44762f02d454bbeb76c43d6a7c56a9d~tplv-k3u1fbpfcp-watermark.image)

发现已存的`dump.rdb`文件的创建时间是`当前时间`，说明已经覆盖

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4ff88d075c0c451e8d5c893ea541e3eb~tplv-k3u1fbpfcp-watermark.image)

### dump.rdb的路径

`dump.rdb的生成路径`可以自行设置，在`redis.conf`配置文件设置，位置如下图所示

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ea18ba21fd714831945e2eb97422a7b7~tplv-k3u1fbpfcp-watermark.image)

### dump.rdb的文件名

`dump.rdb的文件名`可以自行设置，在`redis.conf`配置文件设置，位置如下图所示

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f8f29b0f6dc84bf0b56b9814cb878fd5~tplv-k3u1fbpfcp-watermark.image)

### 同步or异步的生成dump.rdb文件

像上文中的`save`命令在生成`dump.rdb`文件的过程是`同步`的，也就是说在生成文件的过程中，会阻塞其他的redis命令执行，如果`redis的内存越大，耗时越长`，所以redis提供了一个`bgsave`的写时复制`(copy-on-write)`机制，在生成快照的同时，依然可以正常处理命令。

Reids主线程fork生成的子进程，`共享主进程的所有内存数据`，子进程写入到文件中。

此时，如果主线程对这些数据也都是`读操作`，那么主线程和子进程互不影响。如果主线程对数据中有`修改操作`，那么这些数据会被复制一份，生成该数据的`副本`。然后，主线程`直接修改副本`，bgsave在的子进程还是基于`原先共享的内存数据`写入到rdb文件中。

自动生成rdb文件后台使用的是`bgsave`方式

#### save和bgsave的对比

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2898c528f7a649c59f8a4af9ad0b0352~tplv-k3u1fbpfcp-watermark.image)

## AOF（append-only file）

`rdb`这种方式有很严重的缺陷：如果Redis宕机了，那么服务器将丢失最近写入，且没有保存到快照中的那些命令。所以Redis增加了另外一种持久化方式：`AOF`，记录操作记录，然后`重放操作`，但是AOF文件只会记录`修改数据的命令`

### 开启AOF

可以在`redis.conf`文件中设置，把`appendonly no`修改成 `appendonly yes`，如下图所示：


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/796e4c66b147403a86492369739f505a~tplv-k3u1fbpfcp-watermark.image)

### AOF生成的文件名

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4f92844ee1b944a592abbcb1f8f5f6b3~tplv-k3u1fbpfcp-watermark.image)

### AOF的策略

AOF支持三种策略：

1. `appendonly always`：每次有新命令追加到AOF文件时就执行一次fsync，非常慢，也非常安全
2. `appendfsync everysec`：每秒执行一次fsync，足够快，并且在故障时只会丢失1s
3. `appendfsync no`：从不fsync，将数据交给操作系统来处理。更快，也更不安全的选择

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b16b74c391ea4b4487a671fe80e930c7~tplv-k3u1fbpfcp-watermark.image)

推荐也是默认的措施是`appendfsync everysec`，兼容速度和安全性

### 测试AOF

- 第一步：在redis中随便输入几个命令

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/df7728d0a09e43d1a6befb3572b2ddbd~tplv-k3u1fbpfcp-watermark.image)

- 第二步：等待1s后观察有没有生产AOF文件

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/00ee6521c57f4d8cbbd9ae998299267f~tplv-k3u1fbpfcp-watermark.image)

- 第三步：使用`cat`命令查看AOF文件，发现`命令已经追加到AOF文件中`

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/65869de3e5414de7a56fc0dc438196cf~tplv-k3u1fbpfcp-watermark.image)

- 第四步：分析AOF文件

Redis的协议规范是 Redis Serialization Protocol (Redis序列化协议)，AOF文件遵循这个协议，我们来举个`set name miludexiaoguangtou`例子来初步分析一下：

- `*3`：aof文件记录每一个命令都是以`*`开始，3表示这个命令有几个参数，例如：`set name miludexiaoguangtou`就是3个参数，`set、name和miludexiaoguangtou`
- `$3`：`$3`意思就是下方的`set`有3个字符
- `$4`：`$4`意思就是下方的`name`有4个字符
- `$18`：`$18`意思就是下方的`miludexiaoguangtou`有18个字符


### AOF的重写

AOF的文件里面可能有太多重复的、没用的指令。所以Redis支持定期根据`内存的最新数据`生产AOF文件，例如：执行了如下几条指令

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/39d0faeb0af9434face117ff9b04ec80~tplv-k3u1fbpfcp-watermark.image)

重写后AOF文件里，后面的`4条incr命令`变成：

```
*3
$3
SET
$2
count
$1
4
```
在`redis.conf`配置文件中还可以对AOF`重写的频率`配置

```
// aof文件自上一次重写后文件大小增长了100%则再次触发
auto-aof-rewrite-percentage 100
// aof文件至少达到64mb才会自动重写，文件太小恢复速度本来就很快，重写的意义不大
auto-aof-rewrite-min-size 64mb
```

AOF文件还支持手动重写，进入`redis客户端`，执行`bgrewriteaof`重写AOF

重写Redis会fork出一个子进程去做 **(与bgsave命令类似)**，不会对正常命令处理有太多影响


### RDB和AOF如何选择


| 命令 | RDB |AOF|
| --- | --- |---|
|  启动优先级| 低  |高|
|  体积| 低  |大|
| 恢复速度 | 快 |慢|
| 数据安全性 | 容易丢数据 |根据策略决定|

生产环境可以都启用，redis启动时如果既有rdb文件，又有aof文件，则优先选择aof文件恢复数据，因为aof一般来说数据更安全一点


## 混合持久化（Redis 4.0）

重启Redis时，很少使用RDB来恢复内存状态，因为会`丢失大量的数据`。通常使用AOF日志重放，但是重放AOF日志性能`相对RDB来说会慢很多`，这样在Redis实例很大的情况下，启动需要花费很长时间

Redis 4.0为了解决这个问题，带来了一个新的持久化方式-`混合持久化`

### 开启持久化配置（必须先开启AOF）

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a9f9609d0efa463db007233051e19d08~tplv-k3u1fbpfcp-watermark.image)

如果开启了混合持久化，`AOF在重写时`，不再是单纯的将内存数据转换为`RESP命令`，写入到AOF文件中，而是将重写`这一刻之前`的内存`做RDB快照处理`，并且将RDB快照内容和`增量的AOF命令`存在一起，都写入新的AOF文件中。

新的AOF文件一开始不叫`appendonly.aof`，等到重写完新的AOF文件才会进行改名，覆盖原有的AOF文件，完成新旧两个AOF文件的替换

所以总体上来看，在Redis启动的时候，先加载RDB格式的内存数据，然后再重放增量AOF日志就可以完全替代之前的AOF全量文件重放，因此重启的效率会大大提高

### 举例：混合持久化

手动执行AOF重写命令，发现已经将`这一刻之前`的内存`做RDB快照处理`，并写入到新的AOF文件中。

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b3a8a9be9433426da0cfa17a84fb8fa5~tplv-k3u1fbpfcp-watermark.image)

写入两个增量的Redis命令后，发现`直接追加在AOF文件最后`

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8733e4ac263743bcb5f1f74ce055c653~tplv-k3u1fbpfcp-watermark.image)

### 混合持久化AOF文件的格式

上面是RDB的二进制格式 **（那一刻的内存压缩数据）**，后面是AOF命令的格式 **(增量的命令)**

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/251affc1476f4d489857b5d045cc416a~tplv-k3u1fbpfcp-watermark.image)

# Redis的数据恢复

截止目前只讲了Redis的持久化，并且会生成RDB和AOF两种格式的文件，但是如何恢复没说，下面就开始根据案例一步步讲如何恢复Redis的数据

- 第一步：先把redis关了，`模拟宕机`的情况

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fd13e1b67fe6402d9b6c2f4b75425241~tplv-k3u1fbpfcp-watermark.image)

- 第二步：把rdb文件和aof文件命名改了，先看下`什么持久化文件都不存在`的情况

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9a92ef5438cd4299a7133eb47b2b09ef~tplv-k3u1fbpfcp-watermark.image)

- 第三步：启动redis，观察里面的数据，发现`什么key都不存在`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/93ab428950a74427b464e69373fbf939~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e6cf85982f374d50852364d1c39314ca~tplv-k3u1fbpfcp-watermark.image)

- 第四步：由于`redis关闭的时候会自动执行一次持久化`，所以我们把`新生产的aof文件删了`

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8c6349b0692b4845919aa3db23b7a9f9~tplv-k3u1fbpfcp-watermark.image)

- 第五步：把原先的aof文件和rdb的`文件命名恢复`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e602984a297e45f880ae6f0a840b4cc0~tplv-k3u1fbpfcp-watermark.image)

- 第六步：重新启动redis，观察数据，发现已经aof文件中的数据`已经加载到redis中`

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/93ab428950a74427b464e69373fbf939~tplv-k3u1fbpfcp-watermark.image)

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2584d3aa520848b19af11ae13503ebda~tplv-k3u1fbpfcp-watermark.image)


# Redis持久化的策略

1. 写crontab定时调度脚本，每小时都copy一份rdb或者aof的备份到一个目录中去，仅仅保留最近48小时的备份
2. 写crontab定时调度脚本，每天都保留一份当日的数据备份到一个目录中去，可以保留最近一个月的备份
3. 每次copy备份的时候，可以把之前太旧的备份记录给删了
4. 每天晚上将当前机器上的备份复制一份到其他机器上，以防机器损坏 **（多地备份）**

# 总结

这篇文章，从三个方面给大家一个比较直观的Redis持久化

- Redis的`持久化方式`有哪几种
- Redis的`数据恢复`步骤有哪些？
- 企业中Redis的`持久化策略`应该怎么选择？

相信现在大家对Redis的持久化都有一个比较清晰的感受，现在赶快行动，去找你们的运维聊聊，看看各自所在的企业中用的是`哪种持久化策略`，为什么要这么设置，有没有更好的方案？

### 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得`小沙弥`我有点东西的话 求点赞👍 求关注❤️ 求分享👥 ，因为这将是我输出更多优质文章的动力，感谢！！！

如果想获取`Redis相关书籍`，可以关注微信公众号`Java百科全书`，`输入Redis，即可获得`

最后感谢各位看官的支持，我们下期再见！