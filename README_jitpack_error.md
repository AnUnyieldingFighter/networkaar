一：发布错误
1.
Found tasks
Tasks:

⚠️   WARNING:
Gradle 'publishToMavenLocal' task not found. Please add the 'maven-publish' or 'maven' plugin.
See the documentation and examples: https://docs.jitpack.io

/usr/bin/env: ‘bash\r’: No such file or directory

Running: ./gradlew -Pgroup=com.github.AnUnyieldingFighter -Pversion=1.0 publishToMavenLocal

/usr/bin/env: ‘bash\r’: No such file or directory
/usr/bin/env: ‘bash\r’: No such file or directory
Build tool exit code: 127
⚠️ Build failed. See errors above.
2025-04-02T04:33:27.608091597Z
Exit code: 127

生成原因：
gradle 版本我是从gradle-4.10.1-all升级到了gradle-7.5-all.zip
但是gradle 目录下的gradle-wrapper，gradlew，gradlew.bat并没有升级，于是从其他项目找来了
高版本（gradle-7.3.3-bin.zip）的gradle-wrapper，gradlew，gradlew.bat

生成原因：gradle 目录下的gradle-wrapper，gradlew，gradlew.bat 不对
其所对应的是版本是：gradle-4.10.1-all
2.
Subscription is not active right now
Requested subscription: github.com/anunyieldingfighter.
Your subscriptions are listed in https://jitpack.io/w/user
Please contact Support or repository admins if you need assistance.

生成原因：
配置是从别的项目复制而来  忘记提交gradle文件夹下的wrapper

二：使用依赖
1. api 'com.github.AnUnyieldingFighter:networkaar:1.2'
会下载2个依赖
com.github.AnUnyieldingFighter.networkaar:retrofit:1.2
com.github.AnUnyieldingFighter.networkaar:retrofit-debug:1.2

2. api 'com.github.AnUnyieldingFighter.networkaar:retrofit:1.2'
下载指定依赖
 com.github.AnUnyieldingFighter.networkaar:retrofit:1.2