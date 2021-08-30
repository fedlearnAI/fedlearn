## fedlearn 京东科技联邦学习系统 
系统包含包含控制端（即前端）、协调段、单点客户端和分布式客户端等

### 1.环境要求
- 最低硬件配置--4核CPU，8G内存，50G硬盘；
- 操作系统--Centos7/ubuntu 16/18/20
- Java环境--JDK1.8
- maven--3.6

### 2.下载代码和编译打包
- clone代码
```shell
git clone http://jcode.cbpmgt.com/git/fedlearn-client.git
```
- package
```shell
mvn clean package 
```

- 打包后是.zip文件，位于 ./assembly/target/fedlearn-all-assembly.zip
  运行时需要把文件解压缩，文件夹内会有多个目录
```text
fedlearn-all
  --conf  配置文件，端口号、相关路径的修改
  --bin   启动、停止脚本
  --lib   相关依赖
  --readme 文档
```

### 3.部署
部署分为两种，单点部署和分布式部署
##### 3.1 协调端部署

##### 3.2 界面部署

##### 3.3单机版客户端部署
到bin目录下运行start.sh 脚本即可启动客户端，默认使用 ./fedlearn-client/conf/client.properties 配置文件
如需使用其他配置文件请在运行 脚本时指定 -c configFileLocation 指定

以/app/目录（对部署目录无特殊要求，用户可根据实际情况完成以下部署工作）为例，具体部署步骤如下：
- a. 安装包下载：获取对应版本安装包至/app/目录； 
- b. 安装包解压缩：通过"sudo chmod 777 /app/"赋予/app/目录的读写权限，使用"unzip fedlearn-client-assembly.zip -d /app"命令把服务包解压到app目录下；
- c. 修改client配置文件：配置文件存放于fedlearn-client的conf目录，执行命令"cd /export/App/fedlearn-client/conf"进入配置文件目录，修改client.properties文件的数据源配置；
- d. 配置服务日志路径，并赋予该路径相应读写权限。此处将日志指向"/app/log/fedlearn-client/",通过"sudo chmod 777 /app/log/"赋予该目录读写权限；
- e. 启动client服务：启动脚本存放于fedlearn-client的bin目录，执行命令"cd /app/fedlearn-client/bin"进入bin目录，根据机器配置修改start.sh中服务JVM内存(因机器不同，JVM会生效两个中的一个)，执行"sh start.sh -c client.properties实际存放路径"启动fedlearn-client。
- f. 查看服务状态：可通过"ps -ef | grep fedlearn-client"查看当前服务是否启动即可。
- g. 停止服务：存放目录同启动脚本，执行命令"cd /app/fedlearn-client/bin"进入bin目录。可通过执行"sh stop.sh"停止当前服务。

- **注：修改完client的配置文件，需重启服务方可生效。**

##### 3.4 分布式客户端部署

##### 3.5 区块链版本部署
与标准HTTP版本相比，区块链实现了 
- 协调端随机选，
- 每次交互均会在链上留存记录
- 使用区块链作为分布式元数据存储工具

### 4. FAQ
- 4.1.