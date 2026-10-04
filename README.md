ফ্রি ফায়ার মোবাইল চ্যানেল 
<div align="center">
<img src="logo.png" width="180" alt="FolkPatch logo">

[![Latest Release](https://img.shields.io/github/v/release/LyraVoid/FolkPatch?label=Release&logo=github)](https://github.com/LyraVoid/FolkPatch/releases/latest)
[![Telegram](https://img.shields.io/badge/Telegram-FolkPatch-blue?logo=telegram)](https://t.me/FolkPatch)
[![License](https://img.shields.io/github/license/LyraVoid/FolkPatch?logo=gnu)](/LICENSE)

</div>

**Language:** [English](./README.md) / [中文](./README_CN.md) / [日本語](./README_JA.md)

FolkPatch is a Root management application built on [KernelPatch](https://github.com/LyraVoid/KernelPatch). It combines a KernelPatch-based Root implementation, system and kernel module management, Lua plugins, Shizuku integration, and a configurable modern interface in one manager.

[Read the full documentation](https://fp.mysqil.com/)

<table>
  <tr>
    <td><img alt="FolkPatch home screen" src="docs/1.png"></td>
    <td><img alt="FolkPatch module interface" src="docs/2.png"></td>
    <td><img alt="FolkPatch settings interface" src="docs/3.png"></td>
  </tr>
  <tr>
    <td><img alt="FolkPatch feature interface" src="docs/4.png"></td>
    <td><img alt="FolkPatch plugin interface" src="docs/5.png"></td>
    <td><img alt="FolkPatch customization interface" src="docs/6.png"></td>
  </tr>
</table>

---

## Overview

FolkPatch provides:

- KernelPatch-based Root without recompiling the kernel
- APM system modules with batch installation and backup support
- KPM kernel modules, including automatic loading
- Lightweight APD Lua plugins
- Shizuku service management and startup integration
- Runtime controls for network isolation, path hiding, kernel spoofing, mount hiding, and unmount policies
- Importable themes, wallpapers, custom fonts, and multiple home layouts
- English, Chinese, Japanese, and additional community translations

## Requirements

- ARM64 Android device
- Android kernel 3.18 through 6.15
- A supported KernelPatch installation

The exact compatibility matrix and installation steps are available in the [documentation](https://fp.mysqil.com/).

## Interface

- Multiple home layouts, including ListUI, GridUI, FocusUI, CircleUI, DashboardUI, and StatsUI
- FocusUI as the default home layout for new installations
- System theme colors, custom colors, wallpapers, and custom fonts
- Configurable navigation styles and dashboard cards
- Optional automatic update checks

## Modules and Extensions

### APM

APM provides a Magisk-like system module system with installation, removal, enable and disable controls, batch operations, and full backups.

### KPM

KPM provides kernel module support for `inline-hook` and `syscall-table-hook` use cases, including automatic module loading.

### APD Lua Plugins

APD plugins run Lua scripts in the `apd` lifecycle without modifying system files or injecting a kernel module. They support configuration, quick actions, background daemons, and persistent logs.

Plugin documentation:

- English: https://fp.mysqil.com/en/modules/plugin/
- Chinese: https://fp.mysqil.com/modules/plugin/

## Security

The SuperKey has higher privileges than a normal Root session. A weak or exposed key can allow unauthorized control of the device. Use a strong key and keep it private.

## Translation

English and Chinese are the reference languages. Translation corrections should be submitted only for the target language. New language contributions are welcome when the entire translated file is included in the pull request.

## Download and Installation

1. Download the latest package from the [Releases page](https://github.com/LyraVoid/FolkPatch/releases/latest).
2. Install the package on the Android device.
3. Follow the in-app setup flow and read the [documentation](https://fp.mysqil.com/) before installing modules or enabling runtime controls.

## Credits

- [KernelPatch](https://github.com/LyraVoid/KernelPatch) - core Root implementation
- [Magisk](https://github.com/topjohnwu/Magisk) - magiskpolicy
- [KernelSU](https://github.com/tiann/KernelSU) - application UI and module system reference
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) - interface design reference
- [APatch](https://github.com/bmax121/APatch) - upstream project
- [MMRL](https://github.com/MMRLApp/MMRL) - module repository format reference
- [Shizuku](https://github.com/RikkaApps/Shizuku) - built-in Shizuku service

## License

FolkPatch is licensed under the [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.html).

If you modify FolkPatch or integrate it into another project and distribute the result, the complete project must be released under GPLv3. Binary distributions must provide or promise complete, readable source code. The software is provided without warranty, and any violation terminates the GPLv3 grant.

## Community

- Telegram: [@FolkPatch](https://t.me/FolkPatch)
