# NDN-LITE理论学习

这部分将会深入去学习ndn-lite相关的内容，对使用ndn-lite打下基础。
ndn-lite的详细介绍请见：https://github.com/named-data-iot/ndn-lite/wiki 。

## 概述

NDN-Lite库是针对物联网（IoT）场景中实现的命名数据网络栈。

### NDN-Lite是什么？

* 设计运行在资源有限的设备之中。
* 一个网络系统框架，具有安全引导（security bootstrapping）、服务发现（service discovery）、访问控制（access control）等模块。
* IoT应用开发人员的开发平台。

### 设计原理

* 一个系统框架
* 兼容不同平台
* 具有最小的内存开销和零动态内存分配
* 在同一个进程中运行应用程序和NDN转发
* 适用于仅支持单线程或进程的IoT平台

### 与现有NDN包的相关性

#### 不同于NFD+NDN-CXX

* 被设计于物联网系统框架：bootstapping、access control等
* 减少部分功能的轻量级转发器：不支持转发提示、RIB管理等
* 轻量级的编码/解码：减少内存开销

#### 不同于NDN-RIOT

* 更新/改善NDN-RIOT代码库
* 设计不仅适用于RIOT，而且可以与所有IOT平台一起使用
* 不支持转发和应用程序的多进程设计

#### 不同于CCN-LITE

* 设计为一个IoT系统框架

### 现阶段发展状况

* 对于高层的支持尚未充分实现。现阶段，开发者仍然需要手动配置Face和interest/Data。
* 可以适用于任何支持C的平台。当前支持的平台有：
  * nRF52板子
  * 树莓派
* 当前缺少的系统模块：
  * 访问控制（已过时），Pub Sub发布订阅

## 体系结构

NDN-LITE库的体系结构独立于操作系统和开发套件。其体系结构如下图所示。

![](./../pic/ndn-lite%20architecture.png)

### 应用程序支持组件

* Bootstapping：获取身份证书和信任锚。
* 服务发现：发现可用的服务和广播自己的服务。
* 访问控制：保护数据的机密性并授予对授权身份。的访问权限。已过时，新版本开发中。
* 模式信任：执行已授权身份的命令。
  
### 网络组件

* 转发器：和应用程序处于同一个进程中。在包处理中没有内存复制。由于大多数物联网设备有RAM限制，故目前不支持内容储存CS。
* NDN包编码：兴趣包和数据包的编码和解码接口。
* Face：抽象的网络Face接口。将通过平台相关调整实现。

### 工具组件

* key storage：密钥存储，用于保存设备的身份证书和信任锚。
* Clock：提供时间支持，尤其是网络超时。有两个需要具体实现的接口：
  * `time()`：得到稳定的时间
  * `delay()`：sleep时间
* 消息队列：保留NDN转发和应用程序的消息。有主循环来执行。
* Fragmentation：碎片，去碎片/重组NDN的wire格式数据包 into/from 字节块以适合链路层MTU。
* 加密支持：默认支持SHA2、AES、ECC、HMAC、RNG模块。

### 适配组件（平台相关）

* Face适配：将抽象的Face扩展到实际的链路层接口以支持不同的平台：IEEE 802.15.4 BLE LoRa
* Clock适配：实现两个接口。（`time（）`和`delay（）`）
* 加密适配：提供不同的加密模块，已实现更强的安全性（硬件 TPM 和 RNG）和高效率（硬件ECC 签名/验证）。可选，如没有加密适配层，则使用NDN-LITE的默认实现方式。

### 技术特点

#### NDN层

* NDN编码和解码，兼容NDN TLV格式0.3
* 抽象Face，可以继承OS/SDK规范。
* DirectFace支持单线程应用程序的应用程序转发器通信。
* DummyFace仅用于测试。
* 碎片：重新使用[ndn-riot](https://github.com/named-data-iot/ndn-riot)碎片头（3个字节的头）
  
```
    0           1           2           3
    0 1 2  3    8         15           23
    +-+-+--+----+----------------------+
    |1|X|MF|Seq#|    Identification    |
    +-+-+--+----+----------------------+
    第一位：标头位，始终为1（表示碎片标头）
    第二位：保留，始终为0
    第三位：MF位，1表示最后一帧
    第4至第8位：序列号（5位，最多编码31个）
    第9至24位：标识（2字节随机数）
```
#### 安全

