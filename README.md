# Blog

### Java基础

* [深入浅出HashMap的设计与优化](https://github.com/mituxiaoshami/blog/blob/master/doc/hashMap.md)

### 设计模式

* [单例模式：如何创建单一对象优化系统性能](https://github.com/mituxiaoshami/blog/blob/master/doc/singleton_design.md)
* [原型模式与享元模式：提升系统性能的利器](https://github.com/mituxiaoshami/blog/blob/master/doc/prototype_design.md)
* [如何使用设计模式优化并发编程](https://github.com/mituxiaoshami/blog/blob/master/doc/thread_design.md)
* [生产者消费者模式：电商库存设计优化](https://github.com/mituxiaoshami/blog/blob/master/doc/producer_design.md)

### 多线程

* [Java并发之Condition](https://github.com/mituxiaoshami/blog/blob/master/doc/condition.md)


### Spring

* [Spring介绍和环境搭建](https://github.com/mituxiaoshami/blog/blob/master/doc/spring/spring_1.md)

### 性能调优

#### 多线程性能调优

* [深入了解Synchronized同步锁的优化方法](https://github.com/mituxiaoshami/blog/blob/master/doc/synchronized.md)
* [AbstractQueuedSynchronizer原理解析](https://github.com/mituxiaoshami/blog/blob/master/doc/AbstractQueuedSynchronizer.md)
* [深入了解Lock同步锁的优化方法](https://github.com/mituxiaoshami/blog/blob/master/doc/lock.md)

### DDD学习


#### MAC下访问github速度慢或者无法访问的解决办法


打开终端，编辑hosts文件

```
sudo vim /etc/hosts
```

添加以下内容

```
# Github

151.101.185.194 github.global.ssl.fastly.net
192.30.253.112 github.com 
151.101.112.133 assets-cdn.github.com 
151.101.184.133 assets-cdn.github.com 
185.199.108.153 documentcloud.github.com 
192.30.253.118 gist.github.com
185.199.108.153 help.github.com 
192.30.253.120 nodeload.github.com 
151.101.112.133 raw.github.com 
23.21.63.56 status.github.com 
192.30.253.1668 training.github.com 
192.30.253.112 www.github.com 
151.101.13.194 github.global.ssl.fastly.net 
151.101.12.133 avatars0.githubusercontent.com 
151.101.112.133 avatars1.githubusercontent.com
```

刷新dns

```
dscacheutil -flushcache
```

这样做之后发现：无法访问github

首先测试ping github.com。

```
PING github.com (192.30.253.112): 56 data bytes
Request timeout for icmp_seq 0
Request timeout for icmp_seq 1
Request timeout for icmp_seq 2
Request timeout for icmp_seq 3
```

发现hosts里面的192.30.253.112 ip地址已经无法ping通。

通过查看 https://github.com.ipaddress.com ，发现github.com地址已经变成了140.82.114.3。

通过ping 140.82.114.3，发现可以成功连通。而且能看到丢包率。

```
PING 140.82.114.3 (140.82.114.3): 56 data bytes
64 bytes from 140.82.114.3: icmp_seq=0 ttl=48 time=345.266 ms
Request timeout for icmp_seq 1
64 bytes from 140.82.114.3: icmp_seq=2 ttl=48 time=292.797 ms
64 bytes from 140.82.114.3: icmp_seq=3 ttl=48 time=305.325 ms
64 bytes from 140.82.114.3: icmp_seq=4 ttl=48 time=329.859 ms
```

所以又继续编辑hosts文件，将github.com改成

```
140.82.114.3 github.com 
```

最后测试

浏览器访问 https://github.com 就能正常了。

#### github无法加载图片的解决办法

当前有效ip查询办法：

登录 https://www.ipaddress.com/ 网站，搜索 raw.githubusercontent.com
在搜索结果中找到想要的ip即可。

编辑hosts文件,在hosts文件后追加如下内容：

```
199.232.28.133 raw.githubusercontent.com 
```

修改完成后，刷新github页面即可，图片加载问题就成功解决了~



