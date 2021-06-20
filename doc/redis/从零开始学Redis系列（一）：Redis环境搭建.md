# 前言

Redis在企业级的项目中用的非常频繁，最让大家印象深刻的就是它**丰富的数据结构**和**优秀的性能**，在**缓存**、**分布式锁**、**分布式session**、**控制接口幂等等**应用层面上看，也无不彰显着Redis的强大，所以对于Java开发人员来说，Redis应当放在你的必备技能列表中

这个系列我是打算从**初学者**的角度一点点的走完Redis这条路，**从安装->数据结构的了解->demo案例->底层实现原理->企业应用实践，这个思路来分析**，希望带给大家一个比较清晰的Redis系列

所以就开始今天的目的，也希望这个系列开个好头：**Redis环境搭建**

# 环境准备

## Redis服务器

为了省事，我就直接在阿里云买了个最便宜的服务器，目前里面什么内容都没有

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5941369de86e4e3896dad7ac922aeaa2~tplv-k3u1fbpfcp-watermark.image)

## 系统以及终端

- 系统：macOS
- 终端：iTerm


# 下载安装Redis

## 连接服务器

环境准备好了，第一步就是`连接到Redis所在的服务器`，我这里用的是`iTerm`

- 运行以下命令，进行远程连接，就是下图中的IP地址

```
ssh root@<Linux服务器的公网IP>
```

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/838b0d20d5e048ac811d8d4bc84b34f1~tplv-k3u1fbpfcp-watermark.image)

- 输入yes，然后按Enter键。首次连接时，系统因无法确认远程服务器的真实性，只能提供服务器的公钥指纹，并向您问询是否继续连接。输入yes，表示您信任该服务器

- 输入Linux服务器的登录密码，然后按Enter键。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a976541dd868493bb8bcefbc28132772~tplv-k3u1fbpfcp-watermark.image)

- 出现如下图，说明已经已经连上了Redis服务器

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f3dcb7436fd3441c90269ec94b41dc33~tplv-k3u1fbpfcp-watermark.image)


## 下载安装Redis

### 安装gcc

`yum install gcc`

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aebbe884850c4d4ebd250461d3cad2cc~tplv-k3u1fbpfcp-watermark.image)


### 下载解压并编译redis

```
$ wget https://download.redis.io/releases/redis-6.2.4.tar.gz
$ tar xzf redis-6.2.4.tar.gz
$ cd redis-6.2.4
$ make
```

- 下载Redis

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e8dfc18a482d46d7ad96fa7752907dbd~tplv-k3u1fbpfcp-watermark.image)

- 解压Redis

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/986838c96bd84c07aa99b50c24d6d80d~tplv-k3u1fbpfcp-watermark.image)

- 进入到redis目录后编译Redis

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a0a724bd3b484844a46561bba5d22708~tplv-k3u1fbpfcp-watermark.image)


### 修改配置文件成后台启动

还是在Redis目录下

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c38f569494184469b34dce6fd31d5c29~tplv-k3u1fbpfcp-watermark.image)

通过vim工具修改`daemonize`改为`yes`

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2e8b7c0d955c4db8980ec3696f48552a~tplv-k3u1fbpfcp-watermark.image)


# 启动Redis

通过`src/redis-server redis.conf`命令启动Redis

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/fdb692dcb294421ab8a97d174f676ec0~tplv-k3u1fbpfcp-watermark.image)


通过`ps -ef | grep redis`命令验证是否启动成功

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1e8b949733504193ae9cc49c61ccbfd8~tplv-k3u1fbpfcp-watermark.image)

出现上图就是证明redis启动成功，端口是**6379**


# 测试Redis

通过`src/redis-cli`启动Redis客户端

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7ea9cda0f20c448ca0aab0a506d1565d~tplv-k3u1fbpfcp-watermark.image)

测试String的案例：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/567c2945afad45aaae76919fd8417413~tplv-k3u1fbpfcp-watermark.image)

`set String`和`get String`都没问题，证明Redis测试成功


# 退出Redis客户端

通过`quit`命令退出客户端

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/207da7c1eabe4213b7ef56126fb885b5~tplv-k3u1fbpfcp-watermark.image)

# 退出Redis服务

- `pkill redis-server`
- `kill` 进程号
- `src/redis-cli shutdown`

我演示一下`pkill redis-server`，其他的小伙伴们自己试一下

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2353f54016c5482d83ff800119e21012~tplv-k3u1fbpfcp-watermark.image)

通过`ps -ef | grep redis`命令发现Redis服务已经关闭了

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8d4fb7eb8842479987f19d731f411c17~tplv-k3u1fbpfcp-watermark.image)


# 絮叨

最后，如果感到文章有哪里困惑的，请第一时间留下评论，如果各位看官觉得`小沙弥`我有点东西的话 求点赞👍 求关注❤️ 求分享👥 对我来说真的 非常有用！！！如果想获取`Redis相关书籍`，可以关注微信公众号`Java百科全书`，`输入Redis，即可获得`，最后的最后，感谢各位看官的支持！！！




















