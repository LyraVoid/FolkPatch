<div align="center">
<img src='logo.png' width='500px' alt="FolkPatch logo">

[![Latest Release](https://img.shields.io/github/v/release/matsuzaka-yuki/FolkPatch?label=Release&logo=github)](https://github.com/LyraVoid/FolkPatch/releases/latest)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/FolkPatch)
[![GitHub License](https://img.shields.io/github/license/matsuzaka-yuki/FolkPatch?logo=gnu)](/LICENSE)

</div>

🌏 **README 语言:** [**English**](./README_EN.md) / [**中文**](./README.md) / [**日本語**](./README_JA.md)

FolkPatch - 专注界面优化与功能扩展的Root管理工具

通过我们的综合文档快速开始。无论是安装使用、模块管理，还是自定义设置，文档涵盖了您成功使用FolkPatch所需的所有内容。

[📚 阅读完整文档](https://fp.mysqil.com/) →

<table>
  <tr>
    <td><img alt="" src="docs/1.png"></td>
    <td><img alt="" src="docs/2.png"></td>
    <td><img alt="" src="docs/3.png"></td>
  <tr>
  <tr>
    <td><img alt="" src="docs/4.png"></td>
    <td><img alt="" src="docs/5.png"></td>
    <td><img alt="" src="docs/6.png"></td>
  <tr>
</table>

---

## ✨ 介绍

### 🎨 核心功能
- [x] 基于 KernelPatch 的 Root 实现
- [x] 无需重新编译内核即可 Hook 内核函数

### 📱 前置要求

- **必须：** 基于 ARM64 架构且 Linux 内核版本 3.18 至 6.15 的 Android 设备

### 🎨 管理器的界面与设计
- [x] 全新的 UI 与交互体验优化
- [x] 个性化壁纸支持
- [x] 国际化支持
- [x] 动画性能与交互流畅度优化
- [x] 界面视觉细节与动态效果提升
- [x] 支持手动关闭自动更新检查，将版本升级的主导权交还给用户

### 📦 模块相关
- [x] APM: 类 Magisk 模块系统 , 支持批量刷入与全量备份
- [x] KPM: 内核模块系统(支持 inline-hook 与 syscall-table-hook) , 支持自动加载
- [x] 通过商店可以下载热门的 APM 或 KPM

### 插件系统（Lua）

插件是介于 APM 与 KPM 之间的轻量级扩展：不修改系统文件、不注入内核，只在 `apd` 生命周期中运行 Lua 回调。

#### 插件包标准格式

插件以 zip 包或目录形式安装，安装后位于 `/data/adb/plugins/<id>/`。zip 包结构：

```text
my-plugin.zip
├── plugin.json     # 清单文件（必需，声明元数据与依赖）
└── main.lua        # 入口脚本（必需，或由清单 entry 指定）
```

`plugin.json` 示例：

```json
{
  "id": "my-plugin",
  "name": "My Plugin",
  "author": "your-name",
  "version": "1.0.0",
  "description": "What this plugin does",
  "license": "MIT",
  "min_version": 0,
  "depends": ["another-plugin"],
  "entry": "main.lua"
}
```

字段说明：

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | 插件标识，须与目录名一致，仅限字母数字 `-_.` |
| `name` | 否 | 显示名称，缺省回退为 `id` |
| `author` | 否 | 作者 |
| `version` | 否 | 版本号 |
| `description` | 否 | 描述 |
| `license` | 否 | 许可证 |
| `min_version` | 否 | 要求的 APD 最低版本码（数字） |
| `depends` | 否 | 依赖的其他插件 `id` 列表，安装与运行时会校验 |
| `entry` | 否 | 入口 Lua 文件，缺省为 `main.lua` |

安装时会校验 `min_version` 与 `depends`；`zip` 内文件可置于根目录或单一顶层目录，自动识别。

#### 入口脚本

`main.lua` 必须返回一个 Lua table，可选声明生命周期回调：

```lua
return {
    post_fs_data = function()
        info("plugin loaded")
    end,
    service = function()
        -- service 阶段适合轻量的后台初始化
    end,
    boot_completed = function()
    end,
}
```

支持的回调为 `post_fs_data`、`post_mount`、`service` 和 `boot_completed`。运行时提供 `PLUGIN_ID`、`PLUGIN_DIR`、`info()` 和 `warn()`；Lua 标准库可用，因此插件以 root 权限运行，只应安装可信代码。

#### 插件 API

插件运行环境额外提供以下能力：

| 函数 | 说明 |
|------|------|
| `getprop(name)` | 读取系统属性，返回字符串 |
| `setprop(name, value)` | 设置系统属性（绕过只读限制），返回布尔 |
| `exec(...)` | 运行命令，返回 `{ ok, code, stdout, stderr }` 表；可用 `exec("cmd", "arg")` 或 `exec("sh", "-c", "...")` |
| `write_file(path, content)` | 写入文本文件，返回布尔 |
| `read_file(path)` | 读取文件内容，返回字符串 |
| `sysctl(key, value)` | 写内核参数（如 `sysctl("vm/swappiness", "60")`），返回布尔 |
| `chmod(path, mode)` | 设置权限（数字或字符串，如 `0755`） |
| `mkdir(path, recursive)` | 创建目录 |
| `rm(path)` | 删除文件或空目录 |
| `start_daemon(function, interval_secs)` | 把指定回调作为后台守护进程，每隔 `interval_secs` 秒循环执行 |
| `get_config(key)` | 读取本插件保存的用户配置值（字符串） |
| `set_config(key, value)` | 保存本插件的一个用户配置值 |
| `info(msg)` / `warn(msg)` | 输出日志 |
| `PLUGIN_ID` / `PLUGIN_DIR` | 当前插件标识与目录 |

#### Action 操作

插件可声明 `action` 回调，在管理端显示「运行」按钮，用于手动执行一个操作（如手动清理）：

```lua
return {
    action = function()
        -- 手动触发的操作逻辑
        info("manual clean triggered")
    end,
}
```

```sh
apd plugin action <id>        # 命令行触发
```

#### 用户配置

插件可在 `plugin.json` 中声明配置项，管理端会显示「配置」按钮弹出编辑框，配置保存到 `/data/adb/plugins/<id>/config.json`：

```json
{
  "id": "my-plugin",
  "config": [
    { "key": "clean_hour", "label": "Cleaning hour (0-23)", "labels": { "zh": "清理时间", "ja": "クリーニング時間" }, "type": "number", "default": 4 },
    { "key": "clear_all", "label": "Also clear running app caches", "labels": { "zh": "同时清理运行中应用缓存" }, "type": "bool", "default": false },
    { "key": "mode", "label": "Mode", "type": "select", "options": ["auto", "manual"] }
  ]
}
```

`type` 支持 `text` / `number` / `bool` / `select`。插件内用 `get_config`/`set_config` 读写，命令行：

```sh
apd plugin config --id my-plugin list
apd plugin config --id my-plugin get clean_hour
apd plugin config --id my-plugin set clean_hour 3
apd plugin config --id my-plugin delete clean_hour
```

#### 定时循环事件

插件支持两种定时循环方式：

**1. 声明 `main` 回调（推荐）**：插件返回的 table 若包含 `main` 函数，系统会在 `service` 阶段自动将其作为后台守护进程启动，循环执行（默认间隔 1 秒，可在循环内自行 `sleep` 控制节奏）：

```lua
return {
    main = function()
        while true do
            -- 每分钟检查一次
            if os.time() % 60 == 0 then
                info("tick")
            end
            os.execute("sleep 30")
        end
    end,
}
```

`main` 守护进程独立于 apd 启动流程运行，不会阻塞其他插件，也不随单个阶段调用退出。

**2. `start_daemon`**：在任意回调里手动启动后台循环：

```lua
return {
    service = function()
        start_daemon("main", 60)  -- 每 60 秒调用一次本插件的 main
    end,
    main = function()
        -- 定时任务逻辑
    end,
}
```

停止方式：停用插件（`apd plugin disable <id>`）后重启，守护进程不再启动；已运行的 daemon 可用 `kill` 终止。

手动测试定时循环：

```sh
apd plugin daemon <id> <function> <interval_secs>
```

#### 示例插件

仓库内提供两个成品示例：

- `examples/plugins/hello-plugin/`：演示 `getprop`、`setprop`、`exec`、文件读写等 API
- `examples/plugins/cache-cleaner/`：**定时清缓存**成品插件，每天凌晨自动清理应用与系统缓存

**定时清缓存**使用示例：

```sh
apd plugin install /path/to/cache-cleaner.zip   # 或直接放入目录
apd plugin run cache-cleaner clean_now          # 立即手动清理一次
apd plugin action cache-cleaner                 # 或点击管理端的「运行」按钮
```

配置项在管理端点「配置」按钮编辑（清理时间、是否清理运行中应用缓存），或直接改 `config.json`。插件声明了 `action` 和 `main` 回调：`action` 供手动触发，`main` 开机后作为后台守护进程自动运行。

#### 生命周期与状态

- 创建空文件 `disable` 可停用插件，删除即可恢复
- 插件在 `post-fs-data`、`post-mount`、`service`、`boot-completed` 阶段被 `apd` 自动执行
- 单个插件出错会被记录，不影响其他插件

#### 管理命令

```sh
apd plugin list                 # 列出插件（JSON，含元数据、action、配置项）
apd plugin install <zip>        # 从 zip 安装
apd plugin uninstall <id>       # 卸载
apd plugin enable <id>          # 启用
apd plugin disable <id>         # 停用
apd plugin run <id> <function>  # 手动运行某个回调
apd plugin action <id>          # 运行插件的 action 回调
apd plugin daemon <id> <function> <secs>  # 以守护进程循环运行某回调
apd plugin config --id <id> ... # 查看/修改插件配置（list/get/set/delete）
apd plugin log <id>             # 查看插件最近执行日志
apd plugin clear-log <id|all>   # 清除指定或全部插件日志
```

在应用内可从「首页 → 右上角更多菜单 → 插件」或「设置 → 模块 → 插件」进入插件管理页：支持下拉刷新、zip 安装、卸载、启停开关、快捷操作、配置编辑，以及查看持久化执行日志（支持导出分享）。

### ⚡ 技术特性
- [x] 基于 [KernelPatch](https://github.com/bmax121/KernelPatch/)

## 🚀 下载安装

### 📦 使用指导

1. **下载安装：**
   从 [发布页面](https://github.com/LyraVoid/FolkPatch/releases/latest) 下载最新版安装包

2. **安装应用：**
   安装最新版安装包到你的 Android 设备

3. **开始使用：**
  阅读 https://fp.mysqil.com/

## 🙏 开源致谢

本项目基于以下开源项目：

- [KernelPatch](https://github.com/bmax121/KernelPatch/) - 核心组件
- [Magisk](https://github.com/topjohnwu/Magisk) - magiskpolicy
- [KernelSU](https://github.com/tiann/KernelSU) - 应用UI和类似Magisk的模块支持
- [Sukisu-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) - 参考一些界面的设计
- [APatch](https://github.com/bmax121/APatch) - 上游分支
- [MMRL](https://github.com/MMRLApp/MMRL) - 模块仓库数据格式参考和数据源

## 📄 许可证

- FolkPatch 遵循 [GNU General Public License v3 (GPL-3)](http://www.gnu.org/copyleft/gpl.html) 许可证开源 , 作为二改者或分发者 , 您需遵守以下标准:
- 若您修改了代码或在项目中集成了 FolkPatch 并向第三方分发 , 您的整个项目必须同样采用 GPLv3 协议开源
- 分发二进制文件时 , 必须主动提供或承诺提供完整且可读的源代码
- 严禁对软件授权本身收取许可费 , 您可以针对分发、技术支持或定制开发收费
- 分发行为即代表您授予所有用户使用该项目涉及的您的相关专利
- 本软件“按原样”提供 , 不含任何担保 , 原作者不对因使用本软件造成的任何损失负责
- 任何违反上述条款的行为将导致您的 GPLv3 授权自动终止 , 届时 , 您将失去分发 FolkPatch 的合法权利 , 原作者保留依法追究著作权侵权责任(包括但不限于申请停止侵权禁令、经济赔偿及下架违规项目)的权利
## 💬 社区交流

### FolkPatch讨论交流
- Telegram 频道: [@FolkPatch](https://t.me/FolkPatch)
