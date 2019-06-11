<h1>NDN-Lite-BLE操作参考指南</h1>

[TOC]

# 1. 初识ndn-lite

[NDN-Lite](https://github.com/named-data-iot/ndn-lite)库实现了命名数据网络(NDN)栈。该库是用标准C编写的，需要最低版本的C11（ISO/IEC 9899:2011）。ndn-lite仓库地址：https://github.com/named-data-iot/ndn-lite

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

这部分内容主要是讲解一些有关开发环境的搭建，包括：JDK/JRE的安装、Android Studio的安装配置、Segger的安装配置等内容。开发环境的搭建都是**基于Windows10操作系统(64位)**上进行的。

这篇操作参考指南的目的就是让自己从零开始，按照操作指南一步一步的走下去以实现相同的效果。如果之前做过Java开发，想必JDK/JRE都已经在系统上安装配置好了，这一步骤就可以省略了。如果之前用Android Studio写过安卓App，那么JDK/JRE安装和Android Studio安装配置都可以跳过不用看了。

### 2.1.1 JDK和JRE的安装

1 **下载JDK**
  
  JDK版本为8u211，可以直接打开这个链接下载对应版本的JDK(如下图)：https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/JDK%E4%B8%8B%E8%BD%BD.png?raw=true)

  也可以通过百度云下载我已经上传的安装包：https://pan.baidu.com/s/14aMwA8NK3GDm854GIMPYOQ，
  提取码：yt6t 
  
  下载后直接双击选择安装目录即可。

2 **设置环境变量**

* 新建环境变量，变量名称为`JAVA_HOME`，变量值就是你安装JDK的目录地址。如下图所示。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/JDK%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F.jpg?raw=true)

* 在Path中把jdk和jre中的/bin添加到环境变量中。双加打开Path然后点击新建，然后依次输入：`%JAVA_HOME%\bin`和`%JAVA_HOME%\jre\bin`（这里的路径名称要和你安装路径下的文件名称一致）。如下图所示。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/jdk%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F2.png?raw=true)

* 检测安装是否正确。打开终端，输入`javac`和`java -version`看是否输出对应的提示内容和jdk版本信息，如果如下图所示了，说明jdk环境配置成功。如果不成功，可能最大的原因就是系统环境的路径没有设置正确；如果路径确认无误了还是不成功，建议重启或者注销下系统。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/jdk%E7%89%88%E6%9C%AC%E4%BF%A1%E6%81%AF.jpg?raw=true)

### 2.1.2 Android Studio的安装配置

JDK环境配置好了之后，就可以进行Android Studio(后面简称AS)的安装了。因为后面要涉及到弄一个相对应的安卓手机的App，而AS又是Google官方推荐的IDE。

1. **下载AS并进行安装**

可以点击链接进行下载，https://developer.android.google.cn/studio/
或者 http://www.android-studio.org/ （前面的链接因为是google的链接，不可访问可以点击后面的链接下载）。也可以通过百度云下载我上传的AS安装包：https://pan.baidu.com/s/1NDqBK523lE-V7I0uzbJ8qA 提取码：0142。下载完成后，直接双击安装包进行安装即可。至于安装过程如果不用修改目录的话直接Next即可。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E5%AE%89%E8%A3%85.png?raw=true)

2. **
