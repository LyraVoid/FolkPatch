<div align="center">
<img src='logo.png' width='500px' alt="FolkPatch logo">

[![Latest Release](https://img.shields.io/github/v/release/matsuzaka-yuki/FolkPatch?label=Release&logo=github)](https://github.com/LyraVoid/FolkPatch/releases/latest)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/FolkPatch)
[![GitHub License](https://img.shields.io/github/license/matsuzaka-yuki/FolkPatch?logo=gnu)](/LICENSE)

</div>

🌏 **README の言語:** [**English**](./README_EN.md) / [**中文**](./README.md) / [**日本語**](./README_JA.md)

FolkPatch - インターフェースの最適化と拡張機能に重視した Root 管理ツール

KernelPatch をベースに構築され、安定した Root 機能とともに、まったく新しいインターフェース体験と APM / KPM / プラグインの三段階拡張体系を提供します。包括的なドキュメントですぐに始めましょう——インストール、モジュール管理、プラグイン開発、パーソナライズ設定まで、すべてが揃っています。

[📚 完全なドキュメントを読む](https://fp.mysqil.com/) →

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

## ✨ 紹介

### 🎨 コア機能
- [x] KernelPatch ベースの Root 実装
- [x] カーネルの再コンパイルなしでカーネル関数をフック可能

### 📱 前提条件

- **必須：** ARM64 アーキテクチャベースで Linux カーネルバージョン 3.18 から 6.15 の Android デバイス

### 🎨 マネージャーのインターフェースとデザイン
- [x] 全く新しい UI とインタラクションエクスペリエンスの最適化
- [x] パーソナライズされた壁紙サポート
- [x] 国際化サポート
- [x] アニメーションパフォーマンスとインタラクションの滑らかさの最適化
- [x] インターフェースの視覚的詳細と動的効果の向上
- [x] 自動更新チェックの手動無効化をサポートし、バージョンアップグレードの主導権をユーザーに返還

### 📦 モジュールと拡張体系

FolkPatch はカーネルからユーザー空間まで、あらゆるカスタマイズニーズに対応する三段階の拡張機能を提供します：

- [x] **APM**: Magisk ライクなモジュールシステム、一括フラッシュとフルバックアップをサポート
- [x] **KPM**: カーネルモジュールシステム（inline-hook と syscall-table-hook をサポート）、自動ロードをサポート
- [x] **プラグイン（APD Lua Plugin）**: 軽量な Lua スクリプト拡張、APM と KPM の中間に位置
- [x] 内蔵ストアから人気のある APM、KPM、プラグインをワンクリックでダウンロード可能

### 🧩 プラグインシステム（APD Lua Plugin）

プラグインは FolkPatch 独自の軽量なユーザー空間拡張で、APM（システムレベルモジュール）と KPM（カーネルレベルモジュール）の中間に位置します。システムファイルの変更やカーネルへの注入を行わず、`apd` ライフサイクル内で Lua スクリプトを実行します。従来のモジュールと異なり、インストール後すぐに反映され再起動は不要、開発のハードルもより低くなっています。

サポート機能：

- 有効/無効スイッチ
- クイックアクションとユーザー設定 UI
- 定時デーモンタスク（バックグラウンドポーリング実行）
- 永続的な実行ログ、閲覧・エクスポート・共有をサポート

プラグインの完全なドキュメント（パッケージ形式、API、ライフサイクル、管理コマンド）はこちら:

- 🇬🇧 English: https://fp.mysqil.com/en/modules/plugin/
- 🇨🇳 中文: https://fp.mysqil.com/modules/plugin/

### ⚡ 技術的特徴
- [x] [KernelPatch](https://github.com/LyraVoid/KernelPatch/) に基づいています

## 🚀 ダウンロードとインストール

1. **ダウンロード:**
   [リリースページ](https://github.com/LyraVoid/FolkPatch/releases/latest)から最新のインストーラーを取得

2. **インストール:**
   インストーラーを Android デバイスにインストールし、アプリ内のガイドに従って完了

3. **使用開始:**
   [完全なドキュメント](https://fp.mysqil.com/)を読んでモジュール管理やプラグイン開発などの高度な使い方をチェック

## 🙏 オープンソースクレジット

このプロジェクトは以下のオープンソースプロジェクトに基づいています:

- [KernelPatch](https://github.com/LyraVoid/KernelPatch/) - コアコンポーネント
- [Magisk](https://github.com/topjohnwu/Magisk) - magiskpolicy
- [KernelSU](https://github.com/tiann/KernelSU) - アプリ UI と Magisk ライクなモジュールサポート
- [Sukisu-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) - 一部のインターフェースデザインを参照
- [APatch](https://github.com/bmax121/APatch) - 上流ブランチ
- [MMRL](https://github.com/MMRLApp/MMRL) - モジュールリポジトリのデータ形式参考およびデータソース
- [Shizuku](https://github.com/RikkaApps/Shizuku) - 組み込み Shizuku サービス

## 📄 ライセンス

- FolkPatch は [GNU General Public License v3 (GPL-3)](http://www.gnu.org/copyleft/gpl.html) ライセンスの下でオープンソースされています。変更者または配布者として、以下の基準を遵守する必要があります:
- コードを変更した場合、またはプロジェクトに FolkPatch を統合して第三者に配布する場合、プロジェクト全体も GPLv3 ライセンスの下でオープンソースする必要があります
- バイナリファイルを配布する場合、完全かつ読み取り可能なソースコードを積極的に提供するか、提供することを約束する必要があります
- ソフトウェアライセンス自体に対するライセンス料の徴収を厳禁します。配布、技術サポート、カスタム開発に対して料金を請求できます
- 配布行為は、プロジェクトに関連するすべてのユーザーにあなたの関連特許の使用権を付与することを意味します
- 本ソフトウェアは「現状のまま」提供され、いかなる保証もありません。原作者は本ソフトウェアの使用による損失について責任を負いません
- 上記の条項に違反すると GPLv3 ライセンスは自動的に終了します。その際、FolkPatch を配布する正当な権利を失い、原作者は著作権侵害の責任を追求する権利（侵害停止命令の申請、経済的賠償、違反プロジェクトの削除を含むがこれらに限定されない）を留保します

## 💬 コミュニティとディスカッション

### FolkPatch ディスカッションとコミュニケーション
- Telegram チャンネル: [@FolkPatch](https://t.me/FolkPatch)
