# RC17 本地动作内置版

本地构建将 199 个动作 JSON、受击姿态、吸收双方位置轨迹、战斗参数一并压缩到 Mod JAR。默认直接读取包内资源，客户端和服务端使用同一加载方式，不再要求单独的 `motions-private` 目录或 JVM 动作路径参数。

Minecraft 1.21.1、Java 21、NeoForge 21.1.244，以及 Epic Fight 21.17.3.1、GeckoLib 4.9.2 仍需安装；本次内置的是动作资料，不是这些前置 Mod。

## 构建

```text
gradlew.bat assemble -PbundleMotionDirectory=<完整动作目录>
```

构建将该目录的动作 JSON 和三份 `.data` 文件暂存到构建目录，再放进 `blacklight/bundled_motions/`。源动作不复制进版本控制目录。省略该参数的普通源码构建不包含这些资料；构建暂存目录会同步清理以避免混入上次的内置资料。

原有 `-Dblacklight.localWeaponMotions=<目录>` 保留给开发调试；一旦显式指定，整个动作资料源都切到该目录，不逐文件混用不同版本。玩家使用内置版时应移除旧的 JVM 动作路径参数。

本次只编译、打包及核对包内文件，不进行自动游戏测试。此包包含本地原作解码动作，未上传至公开仓库；原创源码的 GPL 声明不扩大到这部分资源。
