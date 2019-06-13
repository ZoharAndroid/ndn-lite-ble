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

# 2. 前期准备：开发环境的搭建

这部分内容主要是讲解一些有关开发环境的搭建，包括：JDK/JRE的安装、Android Studio的安装配置、Segger的安装配置等内容。开发环境的搭建都是**基于Windows10操作系统(64位)**上进行的。

这篇操作参考指南的目的就是让自己从零开始，按照操作指南一步一步的走下去以实现相同的效果。如果之前做过Java开发，想必JDK/JRE都已经在系统上安装配置好了，这一步骤就可以省略了。如果之前用Android Studio写过安卓App，那么JDK/JRE安装和Android Studio安装配置都可以跳过不用看了。

### 2.1 JDK和JRE的安装

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

### 2.2 Android Studio的安装配置

JDK环境配置好了之后，就可以进行Android Studio(后面简称AS)的安装了。因为后面要涉及到弄一个相对应的安卓手机的App，而AS又是Google官方推荐的IDE。

1. **下载AS和安装**

可以点击链接进行下载，https://developer.android.google.cn/studio/
或者 http://www.android-studio.org/ （前面的链接因为是google的链接，不可访问可以点击后面的链接下载）。也可以通过百度云下载我上传的AS安装包：https://pan.baidu.com/s/1NDqBK523lE-V7I0uzbJ8qA 提取码：0142。下载完成后，直接双击安装包进行安装即可。至于安装过程如果不用修改目录的话直接<kbd>Next</kbd>即可。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E5%AE%89%E8%A3%85.png?raw=true)

2. **AS运行和配置**

第一次启动AS会出现一些设置项。

第一次启动AS后，如果弹出下面界面，这是让你导入配置文件，这是让之前做过安卓开发人员使用的，可以直接导入之前的配置。而这里是第一次使用，所以没有可用的配置文件可以导入，所以选择`Do not import settings`,然后点击<kbd>OK</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE1.png?raw=true)

接下来如果弹出如下界面，是AS没有查选到可用的SDK目录，让我们设置代理，以便可以进行下载SDK等相关的文件。这里可以暂时不进行设置，直接选择<kbd>Cancel</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE2.png?raw=true)

接下来就会出现一个Welcome界面，直接点击<kbd>Next</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE3.png?raw=true)

然后就会出现一个安装类型设置，也就是选择自定义还是按照标准模式来设置和配置AS。这里我选择自定义`Custom`，因为可以更好地去了解和学习相关的配置。然后点击<kbd>Next</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE4.png?raw=true)

如果选择了标准模式，也没有关系，这些配置都是可以在AS里面进行修改的。

如果选择自定义模式，下面首先就是要进行UI主题设置，这里我习惯了暗黑主题，所以选择了`Darcula`（根据自己的习惯选择）。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE5.png?raw=true)

接下来这张图片就是SDK的安装。可以修改一下SDK安装的位置，如果后面要安装手机模拟器的镜像这对空间占得还是比较大的。然后其他的默认选择，`Android Virtual Device`就是模拟器镜像，可以选择也可以不选择，后面可以再去安装，我这里先不选择。然后<kbd>Next</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE6.png?raw=true)

接下来就是模拟器的设置，默认推荐就可以。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE7.png?raw=true)

最后就直到AS完成这些操作。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE8.png?raw=true)

### 2.3 Segger安装和配置

在开发之前，必须安装一些所需的软件。这些软件包括：连接到开发板的工具(J-Link等)、用于开发应用程序的IDE（Segger Embedded Studio等），以及提供库和示例应用程序的nRF5 SDK。

Segger Embedded Studio（简称:SES）是Nordic公司推荐的IDE，全平台（Windows、Linux和MacOS）都支持。因为后面要涉及到nRF52840这块板子进行开发，所以SES安装也就必不可少。如果之前有用户习惯用Keil去开发嵌入式相关的内容，nRF52840当然也支持用Keil去开发，具体可以去参考这篇文件：[nRF5 Series: Developing on Windows with ARM Keil MDK](https://pan.baidu.com/s/1bRtmcxUn32ZzAhf7NhB84g), 
提取码：rq3p。

1. **下载和安装SES**

点击这个链接：https://www.segger.com/downloads/embedded-studio  ，也可以直接通过百度云去下载我上传的资源：https://pan.baidu.com/s/16KimethzWKN2xYyu1IbnSw ，提取码：rmb9（官网上下载速度比较慢，建议通过百度云去下载）。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/SEEGER%E5%AE%89%E8%A3%85.png?raw=true)

2. **下载nRF5_SDK**

选择对应的nRF5_SDK版本进行下载下载链接为：https://developer.nordicsemi.com/nRF5_SDK/nRF5_SDK_v15.x.x/ ，也可以直接通过百度云去下载我上传的资源，

内容|地址|提取码
|:-:|:-:|:-:|
`15.2`SDK|https://pan.baidu.com/s/15TbJwBNJS9M528NGeitDjQ|49fp|
`15.2`SDK offline文档|https://pan.baidu.com/s/1070Mta5dQWbsTMF6T4P7ig|gr5l|
`15.3`SDK|https://pan.baidu.com/s/1r9FYNFryrPZINsgzSWSHeg|193o|
`15.3`SDK offline文档|https://pan.baidu.com/s/1hiFg50Ml0urB6gPVpMjWaQ|c7kz|

官网下载比较慢，建议用百度云下载，这里建议把`15.2`和`15.3`两个版本都下载下来，以备后用。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/nRF5SDK%E4%B8%8B%E8%BD%BD.png?raw=true)

3. **下载nRF5命令行工具**

nRF5命令行工具用于Nordic Semiconductor的nRF51和nRF52系列SoC的开发、编程和调试。

nRF5命令行工具包括以下组件：

* nrfjprog可执行文件：用于通过Segger J-LINK编程器和调试器进行编程的工具。
* mergehex可执行文件：可以将最多三个.HEX文件合并为一个文件。
* nrfjprog DLL：一个DLL，导出用于编程和控制nRF51和nRF52系列设备的函数，并允许开发人员使用DLL API创建自己的开发工具。
* SEGGER J-Link软件和文档包（仅包含在Windows安装程序中）。

下载链接为：https://www.nordicsemi.com/Software-and-Tools/Development-Tools/nRF5-Command-Line-Tools/Download#infotabs ，也可以通过百度云下载：https://pan.baidu.com/s/1l5dyAdRC3luBxSsU-zyUsA ，提取码：t93p。

安装期间会弹出如下对话框，这是因为SES安装的过程也安装了J-Link，所以会提示，直接点击<kbd>Ok</kbd>。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%AE%89%E8%A3%85%E8%BF%87%E7%A8%8B.png?raw=true)

期间因为需要安装Microsoft Visual C++版本需要重启电脑，建议把一些你需要保存的东西先保存了，然后点击重启。

如果没有更换目录的要求，一直<kbd>Next</kbd>到成功安装即可。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%B7%A5%E5%85%B7%E5%AE%89%E8%A3%853.png?raw=true)

4. **测试是否成功**

通过上面的一些安装操作，接下来就把板子通过数据线与电脑USB口相连，看电脑是否提示J-Link连接了。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E8%BF%9E%E6%8E%A5%E7%94%B5%E8%84%91.png?raw=true)


# 3. 实战体验：一个使用ndn-lite的应用示例

## 3.1 需求介绍

这是一个应用示例，展示了使用ndn-lite在Android手机和nRF52840开发板之间进行**ndn通信、安全登录和信任策略切换**的基本功能。

具体来说，这个应用程序由两部分组成：**Android手机中的用户应用程序**和**nRF52840开发板中的ndn-lite应用程序**。**用户应用程序**是一个通用的Android应用程序，它在可用设备、基本设备信息和turst策略选项等方面提供用户界面。**ndn-lite应用程序**使用ndn-lite来提供基于ndn的通信、安全登录和信任策略切换功能等。

目前，该应用程序使用BLE作为面在Android手机和开发板之间传输数据包。下面开始这两方面的工作。

## 3.2 Andorid应用程序

具体参考这个[NDN-IoT-Android](https://github.com/gujianxiao/NDN-IoT-Android)库。

**要求**：

* 安卓手机（>= 6.0)
* 支持蓝牙5.0以上

1. **下载NDN-IoT-Android库**到自己的电脑中，下载链接为：https://github.com/gujianxiao/NDN-IoT-Android.git 。<br/>
打开git命令行(如果系统中没有安装git工具，可以去Git官网上去下载安装一下，下载地址为：https://git-scm.com/download/win ，也可以直接通过百度云链接下载：https://pan.baidu.com/s/1hkbYz7sJpxxTNbeEXPqOlQ ，提取码: vv7g)，输入`git cloen https://github.com/gujianxiao/NDN-IoT-Android.git`，

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/%E4%B8%8B%E8%BD%BDNDN-IoT-Android.png?raw=true)

2. **用Android Studio去打开这个Project**。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%89%93%E5%BC%80Project.png?raw=true)

这里需要等待一下，因为AS会通过Gradle来构建这个Project。

在Gradle构建工程之后，如果出现AS提示下面这幅图片的的错误：

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%9E%84%E5%BB%BAProject%E9%97%AE%E9%A2%981.png?raw=true)

这个错误是提示当前我还没有下载对应版本的SDK，这里点击最下面的`Install missing SDK package`进行安装即可，接下来就会出现SDK安装的界面，如下图所示，选择`Accpet`之后，直接<kbd>Next</kbd>即可。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%9E%84%E5%BB%BAProject%E9%97%AE%E9%A2%981%E8%A7%A3%E5%86%B31.png?raw=true)

期间，如果遇到了AS提示下面这图片所示的，是要我们更新一下Gradle插件，直接<kbd>Update</kbd>更新就可以了。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/Gradle%E6%8F%92%E4%BB%B6%E6%9B%B4%E6%96%B0.png?raw=true)

经过上面的操作，应该是没有再报什么错误了，接下来就是编译运行这个App。

3. **编译运行App**

**前期准备**

* 上网查询一下如何让手机打开**开发者模式**，每个牌子的手机都是不一样的，这里以小米手机为例，`打开设置 -> 我的设备 -> 全部参数 -> 连续点击数次MIUI版本那栏`就可以打开开发者模式。
* 通过USB连接电脑，并将USB的用途选择`传输文件`。

<p align = "center">
  <img src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/USB%E7%94%A8%E9%80%94.png?raw=true" width = "300px" />
</p>

* 在手机中找到**开发者选项**，打开**USB调试**。具体打开位置以小米手机为例：`打开设置 -> 更多设置 -> 开发者选项 -> USB调试`然后点击确定，之后就会出现一个密钥确认的界面，点击确定，电脑右下角状态栏就会有看到有手机连接了。

<p align="center"> 
<img width="300px" src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E6%89%93%E5%BC%80USB%E8%B0%83%E8%AF%95.png?raw=true"/>
<img  width="300px" src="https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/USB%E6%8C%91%E5%90%88%E9%80%82%E5%AF%86%E9%92%A5.png?raw=true"/>
</p>

## 3.3 nRF52840开发板程序


## 3.4 实现效果