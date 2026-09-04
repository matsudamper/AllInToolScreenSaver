# AllInToolScreenSaver 固有ルール

## 参照
- Kotlin: https://kotlinlang.org/docs/coding-conventions.html
- Compose: https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md

## スコープ関数・null
- 戻り値を使わないのに戻り値ありスコープ関数を使わない（`run` NG / `apply` OK）
- 長い処理と `?.let { ... } ?: x` を避け、if で分岐する
- 副作用だけの `?.also` より if
- 不要な `?: ""` は `orEmpty()`。Listも同様
- このリポでは `emptyList()` より `listOf()`（Set/Mapも同様）
- `indexOf` は `takeIf { it >= 0 }` で -1 を意識しにくくする

## Compose
- UiStateにデフォルト値禁止

## 作業補助
- タスク開始時・完了時に `context.txt` を読み書きして状況を更新
- キャッシュクリア・クリーンビルド禁止（なんでもキャッシュのせいにしない）

## ビルド（逐次）
```sh
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
./gradlew detekt detektMain
./gradlew :app:lintDebug
./gradlew ktlintFormat
./gradlew pixel9api35DebugAndroidTest
```
