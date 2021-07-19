京东科技自研联邦学习系统的核心算法包
=============================
系统包结构介绍:
- dispatch package 包包含控制和加密数据聚合等功能
- model package 包含模型训练预测和数据加载等功能
- entity package 是实体定义~
- exception package 是自定义异常
- load package 是文件的加载、初始化和预处理
- math package 是数学工具包，包含向量矩阵等处理
- parameter package 是算法参数设定，

安装和打包步骤：
1. checkout code

2. 编译安装
  - checkout 代码
  - 执行 mvn clean package 编译代码以及生成ProtoBuffers中间代码
  - 在编译完成的jar报上可以执行


3. 部分算法需要grpc服务支持，启动grpc方法如下
  - check out federated-learning-grpc 代码
  - 在每一台client机器上部署 federated-learning-grpc 代码
  - 在每一台client机器上进入src/main/python/algorithm，运行 python active_server.py -P 8891
  - 在每一台client机器上运行 python passive_server.py -P 8890

