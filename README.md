<h1>NDN-Lite-BLE操作参考指南</h1>
  
<!-- TOC -->

- [1. 初识ndn-lite](#1-%E5%88%9D%E8%AF%86ndn-lite)
- [2. 前期准备：开发环境的搭建](#2-%E5%89%8D%E6%9C%9F%E5%87%86%E5%A4%87%E5%BC%80%E5%8F%91%E7%8E%AF%E5%A2%83%E7%9A%84%E6%90%AD%E5%BB%BA)
  - [2.1. JDK和JRE的安装](#21-JDK%E5%92%8CJRE%E7%9A%84%E5%AE%89%E8%A3%85)
    - [2.1.1. 下载JDK](#211-%E4%B8%8B%E8%BD%BDJDK)
    - [2.1.2. 设置环境变量](#212-%E8%AE%BE%E7%BD%AE%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F)
  - [2.2. Android Studio的安装配置](#22-Android-Studio%E7%9A%84%E5%AE%89%E8%A3%85%E9%85%8D%E7%BD%AE)
    - [2.2.1. 下载AS和安装](#221-%E4%B8%8B%E8%BD%BDAS%E5%92%8C%E5%AE%89%E8%A3%85)
    - [2.2.2. AS运行和配置](#222-AS%E8%BF%90%E8%A1%8C%E5%92%8C%E9%85%8D%E7%BD%AE)
  - [2.3. Segger安装和配置](#23-Segger%E5%AE%89%E8%A3%85%E5%92%8C%E9%85%8D%E7%BD%AE)
    - [2.3.1. 下载和安装SES](#231-%E4%B8%8B%E8%BD%BD%E5%92%8C%E5%AE%89%E8%A3%85SES)
    - [2.3.2. 下载nRF5_SDK](#232-%E4%B8%8B%E8%BD%BDnRF5SDK)
    - [2.3.3. 下载nRF5命令行工具](#233-%E4%B8%8B%E8%BD%BDnRF5%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%B7%A5%E5%85%B7)
    - [2.3.4. 测试是否成功](#234-%E6%B5%8B%E8%AF%95%E6%98%AF%E5%90%A6%E6%88%90%E5%8A%9F)
  - [2.4. 初识nRF52840板子](#24-%E5%88%9D%E8%AF%86nRF52840%E6%9D%BF%E5%AD%90)
- [3. 实战体验：一个使用ndn-lite的应用示例](#3-%E5%AE%9E%E6%88%98%E4%BD%93%E9%AA%8C%E4%B8%80%E4%B8%AA%E4%BD%BF%E7%94%A8ndn-lite%E7%9A%84%E5%BA%94%E7%94%A8%E7%A4%BA%E4%BE%8B)
  - [3.1. 需求介绍](#31-%E9%9C%80%E6%B1%82%E4%BB%8B%E7%BB%8D)
  - [3.2. Andorid应用程序](#32-Andorid%E5%BA%94%E7%94%A8%E7%A8%8B%E5%BA%8F)
    - [3.2.1. 下载NDN-IoT-Android库](#321-%E4%B8%8B%E8%BD%BDNDN-IoT-Android%E5%BA%93)
    - [3.2.2. 用Android Studio去打开这个Project](#322-%E7%94%A8Android-Studio%E5%8E%BB%E6%89%93%E5%BC%80%E8%BF%99%E4%B8%AAProject)
    - [3.2.3. 编译运行App](#323-%E7%BC%96%E8%AF%91%E8%BF%90%E8%A1%8CApp)
  - [3.3. nRF52840开发板程序](#33-nRF52840%E5%BC%80%E5%8F%91%E6%9D%BF%E7%A8%8B%E5%BA%8F)
    - [3.3.1. 下载nRFProject到本地](#331-%E4%B8%8B%E8%BD%BDnRFProject%E5%88%B0%E6%9C%AC%E5%9C%B0)
    - [3.3.2. 修改nRFProject中的SDK和ndn-lite路径](#332-%E4%BF%AE%E6%94%B9nRFProject%E4%B8%AD%E7%9A%84SDK%E5%92%8Cndn-lite%E8%B7%AF%E5%BE%84)
    - [3.3.3. Build编译nRF52Project](#333-Build%E7%BC%96%E8%AF%91nRF52Project)
      - [3.3.3.1. 找不到micro_ecc_lib_nrf52.a文件？](#3331-%E6%89%BE%E4%B8%8D%E5%88%B0microecclibnrf52a%E6%96%87%E4%BB%B6)
  - [3.4. 实现效果](#34-%E5%AE%9E%E7%8E%B0%E6%95%88%E6%9E%9C)
    - [3.4.1. 将nRFProject分别烧录到nRF52840板](#341-%E5%B0%86nRFProject%E5%88%86%E5%88%AB%E7%83%A7%E5%BD%95%E5%88%B0nRF52840%E6%9D%BF)
      - [3.4.1.1. 烧录第一块板子](#3411-%E7%83%A7%E5%BD%95%E7%AC%AC%E4%B8%80%E5%9D%97%E6%9D%BF%E5%AD%90)
      - [3.4.1.2. 烧录第二块板子](#3412-%E7%83%A7%E5%BD%95%E7%AC%AC%E4%BA%8C%E5%9D%97%E6%9D%BF%E5%AD%90)
    - [3.4.2. 显示效果](#342-%E6%98%BE%E7%A4%BA%E6%95%88%E6%9E%9C)
- [4. nRF52上的BLE Mesh](#4-nRF52%E4%B8%8A%E7%9A%84BLE-Mesh)
  - [4.1. 相关慨念](#41-%E7%9B%B8%E5%85%B3%E6%85%A8%E5%BF%B5)
    - [4.1.1. BLE Mesh相关慨念](#411-BLE-Mesh%E7%9B%B8%E5%85%B3%E6%85%A8%E5%BF%B5)
    - [4.1.2. nRF52 for Mesh体系结构](#412-nRF52-for-Mesh%E4%BD%93%E7%B3%BB%E7%BB%93%E6%9E%84)
  - [4.2. 实例操作：nRF52上运行一个BLE Mesh的例子](#42-%E5%AE%9E%E4%BE%8B%E6%93%8D%E4%BD%9CnRF52%E4%B8%8A%E8%BF%90%E8%A1%8C%E4%B8%80%E4%B8%AABLE-Mesh%E7%9A%84%E4%BE%8B%E5%AD%90)
- [5. ndn-lite学习与使用](#5-ndn-lite%E5%AD%A6%E4%B9%A0%E4%B8%8E%E4%BD%BF%E7%94%A8)
  - [5.1. ndn-lite体系结构](#51-ndn-lite%E4%BD%93%E7%B3%BB%E7%BB%93%E6%9E%84)
  - [5.2. ndn-lite库的代码结构](#52-ndn-lite%E5%BA%93%E7%9A%84%E4%BB%A3%E7%A0%81%E7%BB%93%E6%9E%84)

<!-- /TOC -->
  
# 1. 初识ndn-lite
  
[NDN-Lite](https://github.com/named-data-iot/ndn-lite )库实现了命名数据网络(NDN)栈。该库是用标准C编写的，需要最低版本的C11（ISO/IEC 9899:2011）。ndn-lite仓库地址：https://github.com/named-data-iot/ndn-lite
  
到目前为止，ndn-lite已经为POSIX平台（Linux，MacOS，Raspberry Pi），RIOT OS和Nordic NRF52840开发套件开发了基于ndn-lite的物联网软件包（准备好平台）。开发人员可以直接开发基于这些软件包的物联网应用程序，而无需担心适应性问题。
  
当前基于ndn-lite的包有如下几个（具体可以点击进行查看），未来可能添加更多的内容：
  
* [NDN-Lite Unit Tests over RIOT OS](https://github.com/named-data-iot/ndn-lite-test-over-riot ) 
* [NDN IoT Package for Nordic SDK using Segger IDE and Android Phone](https://github.com/named-data-iot/ndn-iot-package-over-nordic-sdk ) 
* [NDN IoT Package for Nordic SDK using GCC](https://github.com/named-data-iot/ndn-iot-package-over-nordic-sdk-gcc ) 
* [NDN IoT Package for POSIX using CMake](https://github.com/named-data-iot/ndn-iot-package-over-posix ) 
* [NDN-Lite Doxygen Documentation](https://zjkmxy.github.io/ndn-lite-docs/index.html )
  
当前我们主要关注的就是第二个：NDN IoT Package for Nordic SDK using Segger IDE and Android Phone。
  
# 2. 前期准备：开发环境的搭建

这部分内容主要是讲解一些有关开发环境的搭建，包括：JDK/JRE的安装、Android Studio的安装配置、Segger的安装配置等内容。开发环境的搭建都是**基于Windows10操作系统(64位)**上进行的。
  
这篇操作参考指南的目的就是让自己从零开始，按照操作指南一步一步的走下去以实现相同的效果。如果之前做过Java开发，想必JDK/JRE都已经在系统上安装配置好了，这一步骤就可以省略了。如果之前用Android Studio写过安卓App，那么JDK/JRE安装和Android Studio安装配置都可以跳过不用看了。
  
## 2.1. JDK和JRE的安装
  
### 2.1.1. 下载JDK
  
JDK版本为8u211，可以直接打开这个链接下载对应版本的JDK(如下图)：https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/JDK%E4%B8%8B%E8%BD%BD.png?raw=true )
  
也可以通过百度云下载我已经上传的安装包：https://pan.baidu.com/s/14aMwA8NK3GDm854GIMPYOQ ,提取码：yt6t。
  
下载后直接双击选择安装目录即可。
  
### 2.1.2. 设置环境变量
  
* 新建环境变量，变量名称为`JAVA_HOME`，变量值就是你安装JDK的目录地址。如下图所示。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/JDK%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F.jpg?raw=true )
  
* 在Path中把jdk和jre中的/bin添加到环境变量中。双加打开Path然后点击新建，然后依次输入：`%JAVA_HOME%\bin`和`%JAVA_HOME%\jre\bin`（这里的路径名称要和你安装路径下的文件名称一致）。如下图所示。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/jdk%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F2.png?raw=true )
  
* 检测安装是否正确。打开终端，输入`javac`和`java -version`看是否输出对应的提示内容和jdk版本信息，如果如下图所示了，说明jdk环境配置成功。如果不成功，可能最大的原因就是系统环境的路径没有设置正确；如果路径确认无误了还是不成功，建议重启或者注销下系统。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/jdk%E7%89%88%E6%9C%AC%E4%BF%A1%E6%81%AF.jpg?raw=true )
  
## 2.2. Android Studio的安装配置
 
JDK环境配置好了之后，就可以进行Android Studio(后面简称AS)的安装了。因为后面要涉及到弄一个相对应的安卓手机的App，而AS又是Google官方推荐的IDE。
  
### 2.2.1. 下载AS和安装

可以点击链接进行下载，https://developer.android.google.cn/studio/
或者 http://www.android-studio.org/ （前面的链接因为是google的链接，不可访问可以点击后面的链接下载）。也可以通过百度云下载我上传的AS安装包：https://pan.baidu.com/s/1NDqBK523lE-V7I0uzbJ8qA 提取码：0142。下载完成后，直接双击安装包进行安装即可。至于安装过程如果不用修改目录的话直接<kbd>Next</kbd>即可。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E5%AE%89%E8%A3%85.png?raw=true )
  
### 2.2.2. AS运行和配置

第一次启动AS会出现一些设置项。
  
第一次启动AS后，如果弹出下面界面，这是让你导入配置文件，这是让之前做过安卓开发人员使用的，可以直接导入之前的配置。而这里是第一次使用，所以没有可用的配置文件可以导入，所以选择`Do not import settings`,然后点击<kbd>OK</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE1.png?raw=true )
  
接下来如果弹出如下界面，是AS没有查选到可用的SDK目录，让我们设置代理，以便可以进行下载SDK等相关的文件。这里可以暂时不进行设置，直接选择<kbd>Cancel</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE2.png?raw=true )
  
接下来就会出现一个Welcome界面，直接点击<kbd>Next</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE3.png?raw=true )
  
然后就会出现一个安装类型设置，也就是选择自定义还是按照标准模式来设置和配置AS。这里我选择自定义`Custom`，因为可以更好地去了解和学习相关的配置。然后点击<kbd>Next</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE4.png?raw=true )
  
如果选择了标准模式，也没有关系，这些配置都是可以在AS里面进行修改的。
  
如果选择自定义模式，下面首先就是要进行UI主题设置，这里我习惯了暗黑主题，所以选择了`Darcula`（根据自己的习惯选择）。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE5.png?raw=true )
  
接下来这张图片就是SDK的安装。可以修改一下SDK安装的位置，如果后面要安装手机模拟器的镜像这对空间占得还是比较大的。然后其他的默认选择，`Android Virtual Device`就是模拟器镜像，可以选择也可以不选择，后面可以再去安装，我这里先不选择。然后<kbd>Next</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE6.png?raw=true )
  
接下来就是模拟器的设置，默认推荐就可以。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE7.png?raw=true )
  
最后就直到AS完成这些操作。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-11/AS%E9%85%8D%E7%BD%AE8.png?raw=true )
  
## 2.3. Segger安装和配置

在开发之前，必须安装一些所需的软件。这些软件包括：连接到开发板的工具(J-Link等)、用于开发应用程序的IDE（Segger Embedded Studio等），以及提供库和示例应用程序的nRF5 SDK。
  
Segger Embedded Studio（简称:SES）是Nordic公司推荐的IDE，全平台（Windows、Linux和MacOS）都支持。因为后面要涉及到nRF52840这块板子进行开发，所以SES安装也就必不可少。如果之前有用户习惯用Keil去开发嵌入式相关的内容，nRF52840当然也支持用Keil去开发，具体可以去参考这篇文件：[nRF5 Series: Developing on Windows with ARM Keil MDK](https://pan.baidu.com/s/1bRtmcxUn32ZzAhf7NhB84g ), 
提取码：rq3p。
  
### 2.3.1. 下载和安装SES

点击这个链接：https://www.segger.com/downloads/embedded-studio  ，也可以直接通过百度云去下载我上传的资源：https://pan.baidu.com/s/16KimethzWKN2xYyu1IbnSw ，提取码：rmb9（官网上下载速度比较慢，建议通过百度云去下载）。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/SEEGER%E5%AE%89%E8%A3%85.png?raw=true )
  
### 2.3.2. 下载nRF5_SDK

选择对应的nRF5_SDK版本进行下载下载链接为：https://developer.nordicsemi.com/nRF5_SDK/nRF5_SDK_v15.x.x/ ，也可以直接通过百度云去下载我上传的资源，
  
内容|地址|提取码
|:-:|:-:|:-:|
`15.2`SDK|https://pan.baidu.com/s/15TbJwBNJS9M528NGeitDjQ|49fp|
`15.2`SDK offline文档|https://pan.baidu.com/s/1070Mta5dQWbsTMF6T4P7ig|gr5l|
`15.3`SDK|https://pan.baidu.com/s/1r9FYNFryrPZINsgzSWSHeg|193o|
`15.3`SDK offline文档|https://pan.baidu.com/s/1hiFg50Ml0urB6gPVpMjWaQ|c7kz|
  
官网下载比较慢，建议用百度云下载，这里建议把`15.2`和`15.3`两个版本都下载下来，以备后用。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/nRF5SDK%E4%B8%8B%E8%BD%BD.png?raw=true )
  
### 2.3.3. 下载nRF5命令行工具

nRF5命令行工具用于Nordic Semiconductor的nRF51和nRF52系列SoC的开发、编程和调试。
  
nRF5命令行工具包括以下组件：
  
* nrfjprog可执行文件：用于通过Segger J-LINK编程器和调试器进行编程的工具。
* mergehex可执行文件：可以将最多三个.HEX文件合并为一个文件。
* nrfjprog DLL：一个DLL，导出用于编程和控制nRF51和nRF52系列设备的函数，并允许开发人员使用DLL API创建自己的开发工具。
* SEGGER J-Link软件和文档包（仅包含在Windows安装程序中）。
  
下载链接为：https://www.nordicsemi.com/Software-and-Tools/Development-Tools/nRF5-Command-Line-Tools/Download#infotabs ，也可以通过百度云下载：https://pan.baidu.com/s/1l5dyAdRC3luBxSsU-zyUsA ，提取码：t93p。
  
安装期间会弹出如下对话框，这是因为SES安装的过程也安装了J-Link，所以会提示，直接点击<kbd>Ok</kbd>。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%AE%89%E8%A3%85%E8%BF%87%E7%A8%8B.png?raw=true )
  
期间因为需要安装Microsoft Visual C++版本需要重启电脑，建议把一些你需要保存的东西先保存了，然后点击重启。
  
如果没有更换目录的要求，一直<kbd>Next</kbd>到成功安装即可。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E5%91%BD%E4%BB%A4%E8%A1%8C%E5%B7%A5%E5%85%B7%E5%AE%89%E8%A3%853.png?raw=true )
  
### 2.3.4. 测试是否成功

通过上面的一些安装操作，接下来就把板子通过数据线与电脑USB口相连，看电脑是否提示J-Link连接了。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E8%BF%9E%E6%8E%A5%E7%94%B5%E8%84%91.png?raw=true )

## 2.4. 初识nRF52840板子

这里列出一点nRF52840板子的一些内容，毕竟要在这上面写代码，知道一点这块板子的硬件结构还是有点好处的，当然这部分是可以跳过不用看的。

nRF52840支持Bluetooth 5/Bluetooth mesh/Thread/Zigbee/802.15.4/ANT/2.4G，拥有一颗64MHz的Cortex M4F架构的CPU，搭载1MB Flash + 256Kb RAM。

nRF52840板子全貌如下图所示，板子左半部分为一个正版的J-Link OB，引出了烧录接口，可给其它设备烧录程序。板子右半部分就是nRF52840了。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-16/nRF52840%E5%85%A8%E8%B2%8C.png?raw=true)

# 3. 实战体验：一个使用ndn-lite的应用示例

## 3.1. 需求介绍
  
这是一个应用示例，展示了使用ndn-lite在Android手机和nRF52840开发板之间进行**ndn通信、安全登录和信任策略切换**的基本功能。
  
具体来说，这个应用程序由两部分组成：**Android手机中的用户应用程序**和**nRF52840开发板中的ndn-lite应用程序**。**用户应用程序**是一个通用的Android应用程序，它在可用设备、基本设备信息和turst策略选项等方面提供用户界面。**ndn-lite应用程序**使用ndn-lite来提供基于ndn的通信、安全登录和信任策略切换功能等。
  
目前，该应用程序使用BLE作为面在Android手机和开发板之间传输数据包。下面开始这两方面的工作。
  
## 3.2. Andorid应用程序
  
具体参考这个[NDN-IoT-Android](https://github.com/gujianxiao/NDN-IoT-Android )库。因为要的用到蓝牙相关的内容，模拟器是无法使用蓝牙功能的，所以建议使用真机来进行测试。
  
**要求**：
  
* 安卓手机（>= 6.0)
* 支持蓝牙5.0以上
  
### 3.2.1. 下载NDN-IoT-Android库

下载NDN-IoT-Android到自己的电脑中，下载链接为：https://github.com/gujianxiao/NDN-IoT-Android.git 。可以通过百度云进行下载：https://pan.baidu.com/s/1Kx9c-xPQ5TTQccOz4DFzTQ ，提取码: fgsj。<br/>
打开git命令行(如果系统中没有安装git工具，可以去Git官网上去下载安装一下，下载地址为：https://git-scm.com/download/win ，也可以直接通过百度云链接下载：https://pan.baidu.com/s/1hkbYz7sJpxxTNbeEXPqOlQ ，提取码: vv7g)，输入`git cloen https://github.com/gujianxiao/NDN-IoT-Android.git`，
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/%E4%B8%8B%E8%BD%BDNDN-IoT-Android.png?raw=true )
  
### 3.2.2. 用Android Studio去打开这个Project
 
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%89%93%E5%BC%80Project.png?raw=true )
  
这里需要等待一下，因为AS会通过Gradle来构建这个Project。
  
在Gradle构建工程之后，如果出现AS提示下面这幅图片的的错误：
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%9E%84%E5%BB%BAProject%E9%97%AE%E9%A2%981.png?raw=true )
  
这个错误是提示当前我还没有下载对应版本的SDK，这里点击最下面的`Install missing SDK package`进行安装即可，接下来就会出现SDK安装的界面，如下图所示，选择`Accpet`之后，直接<kbd>Next</kbd>即可。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/AS%E6%9E%84%E5%BB%BAProject%E9%97%AE%E9%A2%981%E8%A7%A3%E5%86%B31.png?raw=true )
  
期间，如果遇到了AS提示下面这图片所示的，是要我们更新一下Gradle插件，直接<kbd>Update</kbd>更新就可以了。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/Gradle%E6%8F%92%E4%BB%B6%E6%9B%B4%E6%96%B0.png?raw=true )
  
经过上面的操作，应该是没有再报什么错误了，接下来就是编译运行这个App。
  
### 3.2.3. 编译运行App

**前期准备**
  
* 上网查询一下如何让手机打开**开发者模式**，每个牌子的手机都是不一样的，这里以小米手机为例，`打开设置 -> 我的设备 -> 全部参数 -> 连续点击数次MIUI版本那栏`就可以打开开发者模式。
* 通过USB连接电脑，并将USB的用途选择`传输文件`。
  
<p align = "center">
  <img src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/USB%E7%94%A8%E9%80%94.png?raw=true" width = "300px" />
</p>
  
* 在手机中找到**开发者选项**，打开**USB调试**。具体打开位置以小米手机为例：`打开设置 -> 更多设置 -> 开发者选项 -> USB调试`然后点击打开（如左图所示），之后就会出现一个密钥确认的界面（如右图所示），点击确定，电脑右下角状态栏就会有看到有手机连接了。
  
<p align="center"> 
<img width="300px" src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/%E6%89%93%E5%BC%80USB%E8%B0%83%E8%AF%95.png?raw=true"/><img width="300px" src="https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-12/USB%E6%8C%91%E5%90%88%E9%80%82%E5%AF%86%E9%92%A5.png?raw=true"/>
</p>
  
进过上面的前期准备，手机应该连接上了电脑了，下面开始正式通过AS编译安装App啦！
  
点击所下图所示的 Run app “斜三角”图标（图片所示的“锤子”图标是编译工程，但不会直接对手机安装app，点击“斜三角”图标就可以编译完了直接安装应用到手机上了），就会弹出一个选择安装app的手机列表，然后选择<kbd>OK</kbd>即可。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/%E7%BC%96%E8%AF%91App.png?raw=true )
  
接下来AS就会对NDN-IoT-Android这个工程进行编译。编译完成之后，手机会弹出一个对话框让你进行选择是否安装这个App，直接点击<kbd>继续安装</kbd>进行安装。
  
<p align="center">
  <img src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/app%E5%AE%89%E8%A3%85.png?raw=true" width = "300px"/>
</p>
  
手机自动安装完这个app之后默认会直接打开这个app，给与相应的权限之后，发现App会直接闪退。**那是因为手机蓝牙没有开启**。开启手机蓝牙之后，App运行的界面如下图
  
<p align = "center">
  <img src = "https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/NDN-IoTApp%E8%BF%90%E8%A1%8C.png?raw=true" width = "300px"/>
</p>
  
NDN-IoT-Android 这个App到这里也就安装完成了，从这个源代码可以看利用了ndn-lite这个库，如下图所示，在后面肯定会深入的学习得到，暂时知道这里已经使用了。接下来就是对nRF52840开发板进行安装操作了。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/ndn-liteApp%E4%BD%BF%E7%94%A8.png?raw=true )
  
## 3.3. nRF52840开发板程序

### 3.3.1. 下载nRFProject到本地

nRFProject工程链接地址为：链接: https://pan.baidu.com/s/1L9qydUhBlRB3ffUdknuFXw ，提取码: 3eqf。
  
 ![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/nRF%E4%B8%8B%E8%BD%BD.png?raw=true )
  
### 3.3.2. 修改nRFProject中的SDK和ndn-lite路径

**前期准备**：
进行这步操作之前，首先把ndn-lite和nRF52_SDK都下载下来。nRF52_SDK在上面的步骤已经提到过了，应该都下载下来了，直接解压到相应的目录下即可，这里使用的是nRF52_15.2版本。ndn-lite可以通过百度云盘下载：https://pan.baidu.com/s/1oyFMxZOIcBiuDoOedUSROQ ,提取码: 7ny9。
  
下面就正式开始修改SDK和ndn-lite路径了。
  
打开下载好的nRFProject，会看到里面有个``ndn_lite_nRF52840_example.emProject``这个文件，然后用文本编辑器打开，我这里用nodepad++打开。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/ndn-lite-nRFProject.png?raw=true )
  
这里需要修改文件中指定的SDK和ndn-lite的路径，因为我把ndn-lite和nRF52_SDK都放在了nRFProject的上一级目录，所以我把原来文件像这这样的`../../nRF5_SDK_15.2.0_9412b96/xx`和`../../ndn-lite/xx`全都进行修改。
  
用notepad++可以很方便的进行修改，<kbd>Ctrl + F</kbd>就可以进行全局的替换。下面两幅图片就是notepad++修改sdk和ndn-lite路径的图片。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/ndn-lite%E8%B7%AF%E5%BE%84%E4%BF%AE%E6%94%B9.png?raw=true )
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/sdk%E8%B7%AF%E5%BE%84%E4%BF%AE%E6%94%B9.png?raw=true )
  
  
然后用SES打开这个nRFProject工程。Build编译这个工程，发现弹出密钥认证，如下图所示，因为这是第一次使用SES，之后就不弹出了，当然SES也是免费认证的，所以点击红色框框标出来的进行认证即可。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/SES%E5%AF%86%E9%92%A5.png?raw=true )
  
然后输入一些信息，邮箱地址请填写正确，之后SES会把密钥通过邮箱发送给你。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/SES%E5%AF%86%E9%92%A5%E8%AE%A4%E8%AF%81.png?raw=true )
  
点击<kbd>Request License</kbd>之后，会收到一封邮件，邮件的内容就会包含密钥，然后把密钥复制填写到下图所示中。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-13/%E8%BE%93%E5%85%A5%E5%AF%86%E9%92%A5.png?raw=true )
  
### 3.3.3. Build编译nRF52Project

点击SES的<kbd>Build</kbd>进行编译。下面说明我在编译的时候遇到的一些问题。
  
#### 3.3.3.1. 找不到micro_ecc_lib_nrf52.a文件？

（1）**问题描述**：
  
SES输出的提示：
`cannot find ../nRF5_SDK_15.2.0_9412b96/external/micro-ecc/nrf52hf_armgcc/armgcc/micro_ecc_lib_nrf52.a: No such file or directory`：也就是提示找不到micro_ecc_lib_nrf52.a这个文件。显示如下图所示。
  
![](https://raw.githubusercontent.com/ZoharAndroid/MarkdownImages/master/%E9%97%AE%E9%A2%981.png )
  
（2）**解决办法和步骤**：
  
**A. 在Windows下安装gcc**，这里需要下载[MinGW](https://mirrors.xtom.com.hk/osdn//mingw/68260/mingw-get-setup.exe )，下载地址为：https://mirrors.xtom.com.hk/osdn//mingw/68260/mingw-get-setup.exe 。百度云下载地址为：https://pan.baidu.com/s/1BrSoJ_-XgXHox3OK97S9Hw ，提取码: y4rq。
  
把有关gcc以及make相关的大部分都进行标记安装一下。注意要将MinGW加入环境变量，如下图所示。先创建一个MinGW的变量，然后在Path中进行添加。
  
![添加WinGW到环境变量中](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/%E6%B7%BB%E5%8A%A0MinGW%E7%8E%AF%E5%A2%83%E5%8F%98%E9%87%8F.png?raw=true )
  
这里在安装make的时候，发现MinGW中找不到对应的make包，所以，可以通过`mingw-get install mingw32-make`来安装，如下图所示，这里也就是为什么要把WinGW添加到系统环境变量，如果不添加到系统环境变量之中，会提示该命令找不到。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/make%E5%AE%89%E8%A3%85.png?raw=true )
  
**B. 下载交叉编译工具**，一开始系统是没有装这个交叉编译工具的，所以需要单独安装这个交叉编译工具。下载地址：https://developer.arm.com/open-source/gnu-toolchain/gnu-rm/downloads 。百度云下载的地址为：https://pan.baidu.com/s/1WDjKX8mX-sNRoZXSPtRJJw ，提取码: 4wxj。
  
![](https://raw.githubusercontent.com/ZoharAndroid/MarkdownImages/master/%E4%BA%A4%E5%8F%89%E7%BC%96%E8%AF%91%E5%B7%A5%E5%85%B7.png )
  
选择Windows下的版本进行安装，建议安装到电脑的默认位置，最后完成的时候建议选择添加到环境变量。
  
**C. 修改配置文件**。找到nRF52840 SDK中的gcc配置文件，并进行修改。具体配置路径为：`\nRF5_SDK_15.2.0_9412b96\components\toolchain\gcc`
  
![](https://raw.githubusercontent.com/ZoharAndroid/MarkdownImages/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E4%BD%8D%E7%BD%AE.png )
  
在该路径下，打开文件为：`Makefile.windows`，修改内容如下图（具体的修改按照下载的交叉编译工具的具体版本和路径来修改）
  
![](https://raw.githubusercontent.com/ZoharAndroid/MarkdownImages/master/%E4%BF%AE%E6%94%B9%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6.png )
  
**D. 打开终端，找到nRFSDK的armgcc位置**，具体位置如：`xx\nRF5_SDK_15.2.0_9412b96\external\micro-ecc\nrf52hf_armgcc\armgcc`，然后`mingw32-make`去make一下。
  
如果发现报错了，如下图，根据提示是`\mirco-ecc`目录下缺少文件，把文件放到`\micro-ecc`目录下，这个文件下载地址为：https://pan.baidu.com/s/1vj5EaRtg4X-qAsh4meuWdw ，提取码: wfcz。
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/mingw-32%20make%E9%94%99%E8%AF%AF.png?raw=true )
  
下面这幅图片就是我添加文件之后的nRF52_SDK的文件夹：
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/micro-ecc%E6%96%87%E4%BB%B6%E4%BD%8D%E7%BD%AE.png?raw=true )
  
当然如果没有报这个错误，那么就不用管了，应该就直接显示编译成功了，会生成SES提示缺失的那个`micro_ecc_lib_nrf52.a`文件了。
  
![](https://raw.githubusercontent.com/ZoharAndroid/MarkdownImages/master/2019-2-28/make%E7%BB%93%E6%9E%9C.png )
  
**E. 编译成功**
  
![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/%E7%BC%96%E8%AF%91%E6%88%90%E5%8A%9F.png?raw=true )
  
## 3.4. 实现效果

对于演示效果，这里需要用到两块nRF52840板子。
  
### 3.4.1. 将nRFProject分别烧录到nRF52840板

#### 3.4.1.1. 烧录第一块板子

先将nRFProject烧录第一块nRF52840板子中。
  
在SES软件中，先Build编译一下nRFProject（这一步骤在3.3.3节已经提过了）。编译完后，选择`Target -> Connect J-Link`，如下图所示，这里是通过J-Link将板子与电脑相连。
  
![J-Link连接](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/J-Link%E8%BF%9E%E6%8E%A5.png?raw=true )
  
然后把nRPject下载到板子中，选择`Target -> Download xx`，如下图所示。
  
![download下载](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/download%E4%B8%8B%E8%BD%BD.png?raw=true )
  
#### 3.4.1.2. 烧录第二块板子

* 在SES中工程结构目录中，找到`hardcode-experimentation.h`文件，将`#define BOARD_1`注释掉，然后将`#define BOARD_2`取消注释。如下图所示。
  
![修改代码](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-14/%E4%BF%AE%E6%94%B9%E4%BB%A3%E7%A0%81.png?raw=true )
  
然后Build -> PC连接第二块板子 -> J-Link Conncet -> 重新烧录到第二块板子,这些步骤都和上面提到过的烧录第一块板子一样的，不再赘述。
  
### 3.4.2. 显示效果

1. 板子一上电，LED3就会闪烁3次，这表示的板子正在进行初始化相关的工作（观看效果请点击：https://pan.baidu.com/s/1jIuPspph3ZCYO6Yovnrb7Q ，提取码: a9ib，如果显示“正在转码。。。”，刷新一下网页就可以看了）。
2. 第二次闪烁，表示的是设备在手机上已经完成了安全的sign-on（观看效果请点击：链接: https://pan.baidu.com/s/1BNagCaJn8NByoqaW_86GpA 提取码: 2t96）。
3. 可以按按钮1去关闭LED1，或者去按按钮2去关闭LED1（观看效果请点击： https://pan.baidu.com/s/1eaRquGcrgWCtaNMREXk_qg ，提取码: 8bpv）。
4. 如果有两块板子你可以按按钮3去发送命令兴趣包去打开另一块板子的LED1（观看效果请点击：https://pan.baidu.com/s/1AStjhpMK9p4QUcxqMXGQUQ ，提取码: 6n9p）。

# 4. nRF52上的BLE Mesh

因为后面会涉及到ble mesh与ndn-lite的结合，所以这里先看看在nRF52上运行的BLE Mesh相关的内容。如果没有用到可以省略不看。这里Nordic公司提供了nRF5 SDK for Mesh实现了BLE Mesh，下载地址为：https://www.nordicsemi.com/Software-and-Tools/Software/nRF5-SDK-for-Mesh/Download#infotabs 。

## 4.1. 相关慨念

### 4.1.1. BLE Mesh相关慨念

蓝牙mesh基于蓝牙4.0规范的低功耗部分，并与该协议共享​​最低层。在广播中，蓝牙mesh物理表示与现有的蓝牙低功耗设备兼容，因为mesh消息包含在蓝牙低功耗广告分组的有效载荷内。但是，蓝牙mesh指定了一个全新的Host层，虽然共享了一些概念，但蓝牙mesh与低功耗蓝牙的HOST层不兼容。具体内容请查看：https://infocenter.nordicsemi.com/index.jsp?topic=%2Fcom.nordic.infocenter.meshsdk.v3.1.0%2Fmd_doc_introduction_basic_concepts.html

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-17/ble%20mesh%20and%20ble.png?raw=true)

* **应用领域**

蓝牙mesh主要针对简单的控制和监控应用，如光控或传感器数据采集。数据包格式针对小型控制数据包进行了优化，发出单个命令或报告，不适用于数据流或其他高带宽应用程序。

使用蓝牙mesh会导致比传统蓝牙低功耗应用更高的功耗。这主要是因为需要保持接收不断运行。蓝牙mesh网络最多支持32767个设备，最大网络直径为126跳。

* **网络拓扑和中继**

蓝牙网状网是一种基于广播的网络协议，其中网络中的每个设备都向无线电范围内的所有设备发送和接收所有消息。网状网络中没有连接的概念。网络中的任何设备可以中继来自任何其他设备的消息，这使得网状设备可以通过让一个或多个其他设备将消息中继到目的地来向无线电范围之外的设备发送消息。此属性还允许设备随时移动和进出网络。

* **中继**

蓝牙mesh网通过中继消息扩展网络范围。任何mesh设备都可以配置为充当中继，并且不需要专用的中继设备来构建网络。中继一次则会减少生存时间（TTL）值，如果TTL为2或更高，则转发它们。这种无向中继被称为消息泛滥，并确保消息传递的高概率，而不需要任何关于网络拓扑的信息。网状配置文件规范不提供任何路由机制，所有消息都由所有中继转发，直到TTL值达到零。为了避免消息被反复转发，所有网状设备都维护消息缓存。此缓存用于过滤设备已处理的数据包。

基于洪泛的消息中继方法可能导致大量冗余流量，这可能会影响网络的吞吐量和可靠性。因此，强烈建议限制网络中的中继数量。

* **GATT协议**

为了让不支持接收mesh数据包的BLE支持mesh数据包，蓝牙网状网定义了一个单独的协议，用于通过BLE GATT协议隧道化mesh消息。为此，网状配置文件规范定义了GATT承载和相应的GATT代理协议。该协议允许传统BLE设备通过建立与启用代理功能的网状设备的GATT连接来参与mesh网络。

* **地址**

将设备添加到网络时，会为其分配一系列代表它的**单播地址**。设备的单播地址无法更改，并且始终是顺序的。单播地址空间支持在单个网状网络中具有32767个单播地址。任何应用程序都可以使用单播地址直接向设备发送消息。

**组地址**作为网络配置过程的一部分进行分配和分配。组地址可以表示任意数量的设备，并且设备可以是任意数量的组的一部分。网状网络中最多可以有16127个通用组地址。

**虚拟地址**可以被认为是组地址的特殊形式，并且可以用于表示任意数量的设备。每个虚拟地址都是从文本标签生成的128位UUID。虚拟地址不必由网络配置设备跟踪，并且以这种方式，用户可以在部署之前生成虚拟地址，或者可以在网络中的设备之间临时生成地址。

* **安全**

Bluetooth Mesh采用多种安全措施来防止第三方干扰和监控：

  * 授权认证
  * 消息加密
  * 私有密钥
  * Replay保护

### 4.1.2. nRF52 for Mesh体系结构



## 4.2. 实例操作：nRF52上运行一个BLE Mesh的例子




# 5. ndn-lite学习与使用

这部分将会深入去学习ndn-lite相关的内容，对使用ndn-lite打下基础。

## 5.1. ndn-lite体系结构
  
ndn-lite的详细介绍请见：https://github.com/named-data-iot/ndn-lite/wiki 。ndn-lite库旨在提供多个核心NDN网络栈。该库允许应用程序直接有访问控制、服务发现、模式化信任等功能。

ndn-lite系统结构图如下：

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-17/iot-framework.jpg?raw=true)

## 5.2. ndn-lite库的代码结构

打开SES软件，可以看到ndn-lite的代码结构如下图所示。

![](https://github.com/ZoharAndroid/MarkdownImages/blob/master/2019-6-17/ndn-lite%E4%BB%A3%E7%A0%81%E7%BB%93%E6%9E%84.png?raw=true)

* `./encode`目录：NDN包的编码和解码。
* `./forwarder`目录：NDN轻量级的转发实现和网络Face抽象。
* `./face`目录：网络face和应用face的实现。每个face实例可能需要硬件/OS适配。
* `./securtiy`目录：安全支持。
* `./app-support`目录：访问控制、服务发现和其他可以促进应用程序开发的高级模块。
* `./adaptation`目录：硬件/操作系统适配。当使用ndn-lite时，开发人员应该为他们的应用程序开发所使用的平台/OS选择一个或多个适配。

