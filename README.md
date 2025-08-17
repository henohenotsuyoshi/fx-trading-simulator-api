# FX Trading Simulator API

Java Spring Boot製のFX取引シミュレーターAPIです。VS Code Dev Containersを使用して統一された開発環境を提供します。

## Prerequisites（前提条件）

開発を始める前に、以下のソフトウェアがインストールされていることを確認してください：

### 必須ソフトウェア
- **Docker Desktop** (最新版)
  - [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
  - [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
  - [Docker Engine for Linux](https://docs.docker.com/engine/install/)
- **Visual Studio Code** (最新版)
  - [VS Code Download](https://code.visualstudio.com/download)
- **Dev Containers拡張機能**
  - VS Code内で `ms-vscode-remote.remote-containers` をインストール

### システム要件
- **メモリ**: 最低8GB RAM推奨（Docker + Gradle buildのため）
- **ディスク容量**: 最低5GB空き容量
- **OS**: macOS、Windows 10/11、または最新のLinuxディストリビューション

## セットアップ手順

### 1. リポジトリのクローン
```bash
git clone <repository-url>
cd fx-trading-simulator-api
```

### 2. VS Code Dev Containerで開く
1. VS Codeでプロジェクトフォルダを開く
2. コマンドパレット（Ctrl/Cmd + Shift + P）を開く
3. **"Dev Containers: Reopen in Container"** を選択
4. 初回起動時は自動的にDockerイメージがビルドされます（5-10分程度）

### 3. ビルドプロセス
Dev Containerが自動的に以下を実行します：
- Java 17環境のセットアップ
- Gradleビルドシステムの初期化
- Reladomo（Mithra）コード生成
- Spring Bootアプリケーションのビルド
- PostgreSQLデータベースの起動

## 起動確認方法

### 1. アプリケーションの動作確認

#### ブラウザでのアクセス
- **アプリケーション**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

#### コマンドでの確認
VS Code内のターミナルで以下を実行：
```bash
# アプリケーションの応答確認
curl http://localhost:8080

# Spring Boot Actuatorの確認（存在する場合）
curl http://localhost:8080/actuator/health
```

### 2. データベース接続確認
```bash
# PostgreSQLの接続テスト
docker exec -it fx-trading-simulator-api-db-1 psql -U fxuser -d fxtrade -c "SELECT version();"
```

### 3. コンテナ状態の確認
```bash
# 全コンテナの状態確認
docker ps

# アプリケーションログの確認
docker logs fx-trading-simulator-api-app-1

# データベースログの確認
docker logs fx-trading-simulator-api-db-1
```

## 開発環境の詳細

### 技術スタック
- **Java**: OpenJDK 17
- **フレームワーク**: Spring Boot 3.1.0
- **ビルドツール**: Gradle 7.6.1
- **データベース**: PostgreSQL 15
- **ORM**: Reladomo (Goldman Sachs)
- **API仕様**: OpenAPI 3 (SpringDoc)

### ポート設定
- **アプリケーション**: 8080
- **PostgreSQL**: 5432

### 環境変数
```bash
SPRING_PROFILES_ACTIVE=postgresql
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/fxtrade
SPRING_DATASOURCE_USERNAME=fxuser
SPRING_DATASOURCE_PASSWORD=fxpassword
```

## 開発作業の開始

### アプリケーションの再起動
```bash
# Gradleでの再起動
./gradlew bootRun

# またはDockerコンテナの再起動
docker compose restart app
```

### データベースのリセット
```bash
# データベースコンテナの再作成
docker compose down
docker volume rm fx-trading-simulator-api_postgres_data
docker compose up -d
```

### Mithra（Reladomo）コード生成
```bash
./gradlew mithraGenerateSources
```

## トラブルシューティング

### よくある問題と解決方法

#### 1. "Unable to access jarfile" エラー
```bash
# ビルドが失敗している可能性があります
./gradlew clean build --no-daemon
```

#### 2. メモリ不足でビルドが失敗する
```bash
# Docker Desktopのメモリ制限を8GB以上に設定
# またはGradle JVM オプションを調整
export GRADLE_OPTS="-Xmx2g"
```

#### 3. データベース接続エラー
```bash
# データベースコンテナの状態確認
docker logs fx-trading-simulator-api-db-1

# データベースの再起動
docker compose restart db
```

#### 4. Dev Container起動の失敗
```bash
# Dockerイメージのクリーンアップ
docker system prune -a
# その後、VS CodeでDev Containerを再度開く
```

### ログ確認方法
```bash
# アプリケーションログ（リアルタイム）
docker logs -f fx-trading-simulator-api-app-1

# ビルドログの確認
./gradlew build --info --stacktrace
```

## APIエンドポイント

アプリケーション起動後、以下のようなエンドポイントが利用可能です：
- 取引実行: `POST /api/exchange`
- 残高照会: `GET /api/balance`
- レート取得: `GET /api/rates`
- セッション管理: `GET /api/sessions`

詳細なAPI仕様は Swagger UI (http://localhost:8080/swagger-ui.html) で確認できます。

## チーム開発のベストプラクティス

1. **統一環境**: 全員がDev Containerを使用することで環境差異を排除
2. **依存関係**: `build.gradle`の変更時は`Dev Containers: Rebuild Container`を実行
3. **データベース**: 開発用データの共有には migration scriptを使用
4. **コード生成**: Mithra XMLファイル変更時は`mithraGenerateSources`を実行

## サポート

問題が発生した場合：
1. このREADMEのトラブルシューティングセクションを確認
2. Docker DesktopとVS Codeが最新版であることを確認
3. プロジェクトのIssueトラッカーで報告