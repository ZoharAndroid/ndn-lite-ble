<h1>NDN-Lite-BLE操作手册</h1>

[TOC]

# 1. 初识ndn-lite

[NDN-Lite](https://github.com/named-data-iot/ndn-lite)库实现了命名数据网络(NDN)栈。该库是用标准C编写的，需要最低版本的C11（ISO/IEC 9899:2011）。（ndn-lite仓库地址：https://github.com/named-data-iot/ndn-lite。）

到目前为止，ndn-lite已经为POSIX平台（Linux，MacOS，Raspberry Pi），RIOT OS和Nordic NRF52840开发套件开发了基于ndn-lite的物联网软件包（准备好平台）。开发人员可以直接开发基于这些软件包的物联网应用程序，而无需担心适应性问题。

当前基于ndn-lite的包有如下几个（具体可以点击进行查看），未来可能添加更多的内容：

* [NDN-Lite Unit Tests over RIOT OS](https://github.com/named-data-iot/ndn-lite-test-over-riot) 
* [NDN IoT Package for Nordic SDK using Segger IDE and Android Phone](https://github.com/named-data-iot/ndn-iot-package-over-nordic-sdk) 
* [NDN IoT Package for Nordic SDK using GCC](https://github.com/named-data-iot/ndn-iot-package-over-nordic-sdk-gcc) 
* [NDN IoT Package for POSIX using CMake](https://github.com/named-data-iot/ndn-iot-package-over-posix) 
* [NDN-Lite Doxygen Documentation](https://zjkmxy.github.io/ndn-lite-docs/index.html)

当前我们主要关注的就是第二个：NDN IoT Package for Nordic SDK using Segger IDE and Android Phone。

# 2. 前期准备

## 2.1 开发环境的搭建

这部分内容主要是讲述一些有关开发环境的搭建，包括：JDK/JRE的安装、Android Studio的安装配置、Segger的安装配置等内容。开发环境的搭建都是**基于Windows10操作系统**上进行的。

### 2.1.1 JDK和JRE的安装


### 2.1.2 Android Studio的安装配置

