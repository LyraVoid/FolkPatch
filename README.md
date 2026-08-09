<div align="center">
<img src='logo.png' width='500px' alt="FolkPatch logo">

[![Latest Release](https://img.shields.io/github/v/release/matsuzaka-yuki/FolkPatch?label=Release&logo=github)](https://github.com/LyraVoid/FolkPatch/releases/latest)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/FolkPatch)
[![GitHub License](https://img.shields.io/github/license/matsuzaka-yuki/FolkPatch?logo=gnu)](/LICENSE)

</div>

🌏 **README 语言:** [**English**](./README_EN.md) / [**中文**](./README.md) / [**日本語**](./README_JA.md)

FolkPatch - 专注界面优化与功能扩展的 Root 管理工具

基于 KernelPatch 打造，在提供稳定 Root 能力的同时，带来全新的界面体验与 APM / KPM / 插件三层扩展体系。通过我们的综合文档快速上手——安装使用、模块管理、插件开发、个性化设置，一应俱全。

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

### 📦 模块与扩展体系

FolkPatch 提供三层扩展能力，覆盖从内核到用户态的各类定制需求：

- [x] **APM**：类 Magisk 模块系统，支持批量刷入与全量备份
- [x] **KPM**：内核模块系统（支持 inline-hook 与 syscall-table-hook），支持自动加载
- [x] **插件（APD Lua Plugin）**：轻量级 Lua 脚本扩展，介于 APM 与 KPM 之间
- [x] 通过内置商店可一键下载热门的 APM、KPM 与插件

### 🧩 插件系统（APD Lua Plugin）

插件是 FolkPatch 独创的轻量级用户态扩展，定位介于 APM（系统级模块）与 KPM（内核级模块）之间：不修改系统文件、不注入内核，只在 `apd` 生命周期中运行 Lua 脚本。相比传统模块，插件安装即时生效、无需重启，开发门槛更低。

支持能力：

- 启停开关与一键启用/禁用
- 快捷操作（Action）与用户配置界面
- 定时守护任务（后台轮询执行）
- 持久化执行日志，支持查看、导出与分享

插件的包格式、API、生命周期与管理命令等完整文档：

- 🇨🇳 中文：https://fp.mysqil.com/modules/plugin/
- 🇬🇧 English：https://fp.mysqil.com/en/modules/plugin/

### ⚡ 技术特性
- [x] 基于 [KernelPatch](https://github.com/LyraVoid/KernelPatch/)

## 🚀 下载安装

1. **下载：**
   从 [发布页面](https://github.com/LyraVoid/FolkPatch/releases/latest) 获取最新版安装包

2. **安装：**
   将安装包安装到你的 Android 设备，按应用内引导完成部署

3. **开始使用：**
   阅读 [完整文档](https://fp.mysqil.com/) 了解模块管理、插件开发等进阶用法

## 🙏 开源致谢

本项目基于以下开源项目：

- [KernelPatch](https://github.com/LyraVoid/KernelPatch/) - 核心组件
- [Magisk](https://github.com/topjohnwu/Magisk) - magiskpolicy
- [KernelSU](https://github.com/tiann/KernelSU) - 应用UI和类似Magisk的模块支持
- [Sukisu-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) - 参考一些界面的设计
- [APatch](https://github.com/bmax121/APatch) - 上游分支
- [MMRL](https://github.com/MMRLApp/MMRL) - 模块仓库数据格式参考和数据源
- [Shizuku](https://github.com/RikkaApps/Shizuku) - 内置 Shizuku 服务

## 📄 许可证

- FolkPatch 遵循 [GNU General Public License v3 (GPL-3)](http://www.gnu.org/copyleft/gpl.html) 许可证开源 , 作为二改者或分发者 , 您需遵守以下标准:
- 若您修改了代码或在项目中集成了 FolkPatch 并向第三方分发 , 您的整个项目必须同样采用 GPLv3 协议开源
- 分发二进制文件时 , 必须主动提供或承诺提供完整且可读的源代码
- 严禁对软件授权本身收取许可费 , 您可以针对分发、技术支持或定制开发收费
- 分发行为即代表您授予所有用户使用该项目涉及的您的相关专利
- 本软件“按原样”提供 , 不含任何担保 , 原作者不对因使用本软件造成的任何损失负责
- 任何违反上述条款的行为将导致您的 GPLv3 授权自动终止 , 届时 , 您将失去分发 FolkPatch 的合法权利 , 原作者保留依法追究著作权侵权责任(包括但不限于申请停止侵权禁令、经济赔偿及下架违规项目)的权利

## 💬 社区交流

### FolkPatch 讨论交流
- Telegram 频道: [@FolkPatch](https://t.me/FolkPatch)
