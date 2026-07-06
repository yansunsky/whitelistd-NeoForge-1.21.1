# Whitelistd (NeoForge 1.21.1)

高级白名单工具，用于替代 / 增强原版白名单。本仓库基于 [ruattd/whitelistd](https://github.com/ruattd/whitelistd)（1.20.1 Architectury，GPLv3）移植到 **NeoForge 1.21.1**。

## 特性

- 可插拔存储后端（当前实现：**JSON**；HTTP / MySQL / MongoDB 规划中）
- 灵活的玩家搜索模式：按名 / 按 UUID / 二者皆可
- 离线玩家 UUID 自动补全（**Record** 机制）
- 通过 Mixin 接管原版 `DedicatedPlayerList.isWhiteListed`，与原版白名单可共存（模组关闭时透明交还控制权）
- 完整的服务器指令：`/whitelistd`（别名 `/wld`）与 `/record`

## 许可证

**GNU GPL 3.0**。原版权归 ruattd / Forest Craft 所有；NeoForge 1.21.1 移植版权归 yansunsky。详见 `LICENSE` 与 `NOTICE`。

## 构建与运行

需要 NeoForge 1.21.1 + ModDevGradle（Java 21）。

```bash
./gradlew build        # 构建
./gradlew runClient    # 运行客户端
./gradlew runServer    # 运行服务端
./gradlew runData      # 生成数据
```

> 若需使用 LittleSkin 等第三方皮肤站登录测试，请将 `authlib-injector-1.2.7.jar` 放到 `run/` 目录（该目录已在 `.gitignore` 中忽略，不会入库）。`build.gradle` 已为该 jar 注入 `-javaagent` 参数。

## 状态

移植进行中。当前已完成**基础初始化**（构建配置、许可证、文档）；业务逻辑代码（配置 / 存储 / 指令 / Mixin / Record）将按阶段实现，详见 `docs/`（本地辅助文档，不入库）。
