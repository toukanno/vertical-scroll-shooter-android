# Vertical Scroll Shooter (Android)

縦スクロール型スペースシューティングゲーム for Android

## ゲーム内容

- プレイヤーの宇宙船を操作して敵を撃墜
- 画面をタップした位置に向かって自動移動
- 弾は自動発射
- ステージをクリアするとボスが登場

## 敵の種類

| 敵タイプ | 説明 | HP | 点数 |
|---------|------|-----|-----|
| BASIC   | 通常の敵 | 1 | 100 |
| FAST    | 高速でジグザグ移動 | 1 | 150 |
| TANK    | 動きは遅いが高耐久 | 3〜 | 200 |
| BOSS    | ステージボス・拡散弾 | 20〜 | 1000 |

## パワーアップ

- ❤ ライフ回復
- ★ 弾の強化（最大3段階: 1→2→3way）
- ◆ 一時的な無敵シールド

## 操作方法

- タップ: 宇宙船を移動（タップした位置へ移動）
- 弾は自動発射

## Tech Stack

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin |
| プラットフォーム | Android |
| 描画 | カスタム SurfaceView (60 FPS ゲームループ) |
| 入力 | タッチ入力処理 |
| 当たり判定 | `RectF.intersects` |
| 最小SDK | API 24 (Android 7.0) |
| ターゲットSDK | API 34 (Android 14) |

## Quick Start

### 必要環境

- Android Studio (Hedgehog 以降推奨)
- JDK 17+
- Android SDK (API 24 〜 34)
- 実機または Android Emulator

### ビルド方法

```bash
# Android Studio で開く、またはコマンドラインから
./gradlew assembleDebug
```

ビルド成功後、`app/build/outputs/apk/debug/app-debug.apk` が生成されます。  
Android Studio の **Run** ボタンから直接端末へインストールすることもできます。

## Contributing

バグ報告・機能提案・プルリクエストは歓迎です！

1. このリポジトリを Fork する
2. フィーチャーブランチを作成する (`git checkout -b feature/your-feature`)
3. 変更をコミットする (`git commit -m 'feat: add your feature'`)
4. ブランチをプッシュする (`git push origin feature/your-feature`)
5. Pull Request を作成する

## License

このプロジェクトは [MIT License](LICENSE) のもとで公開されています。
