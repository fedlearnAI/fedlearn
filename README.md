## fedlearn 京东科技联邦学习系统 
系统包含包含控制端（即前端）、协调端、单点客户端和分布式客户端等
### 1.代码结构
代码分为多个模块
- assembly 整体代码打包模块，无实际功能
- client 单机版客户端
- common 公共包，实体和工具定义
- coordinator 协调端，负责协调多个参与方数据交互
- core 核心算法
- frontend 业务逻辑提交和前端页面
- manager 分布式客户端管理单元
- worker 分布式客户端计算单元

### 2.环境要求
- 最低硬件配置--4核CPU，8G内存，50G硬盘；
- 操作系统--Centos7/8,ubuntu 16/18/20
- Java环境--JDK1.8
- maven--3.6

### 3.下载代码和编译打包
- clone代码
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

### 4.部署
系统组件包含协调端、控制端、客户端三部分，其中客户端部署分为两种，单点客户端和分布式客户端。
##### 4.1 协调端部署
- 数据初始化
  服务端依赖数据库保存持久化数据，所以需要创建数据库和初始化表结构。我们现有python项目支持数据库和表的初始化工作；
  依照元数据存储方式的不同，目前支持mysql和sqlite两种方式，其中sqlite为系统安装包自带。
- 修改配置，根据实际情况配置元数据和日志等目录。
-命令启动
```bash
cd ./fedlearn-all
bash bin/start-coordinator.sh -c ./conf/coordinator.properties
```

##### 4.2 界面部署
- 修改配置
- 命令启动
```bash
cd ./fedlearn-all
bash bin/start-frontend.sh -c ./conf/application.yml
```

##### 4.3单机版客户端部署
-修改配置
根据实际情况修改conf/client.properties文件
-命令启动
```bash
cd ./fedlearn-all
bash bin/start-client.sh -c ./conf/client.properties
```

##### 4.4 分布式客户端部署
分布式客户端包括manager和worker两部分，
```bash
cd ./fedlearn-all
bash bin/start-worker.sh -c ./conf/worker.properties
```

##### 4.5 区块链版本部署
区块链系统依赖京东的区块链jdchain，请先安装jdchain，参考 https://github.com/blockchain-jd-com
- 修改各项配置文件中的区块链项目为
```text
jdchain.available=true
```
并根据实际情况修改配置文件中的区块链地址
- 参照标准版模式启动区块链系统，

### 5. FAQ
- 有任何其他问题请联系 fedlearn-support@jd.com
