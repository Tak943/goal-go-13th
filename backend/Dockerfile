# ステージ1: ビルド環境（Javaのコードをビルドする係）
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 依存関係とソースコードをコピー
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# gradlewに実行権限を付与してビルド（テストはスキップ）
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# ステージ2: 本番実行環境（完成したアプリだけを動かす係）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ステージ1で作られた完成品（jar）だけをコピー
COPY --from=builder /app/build/libs/*.jar app.jar

# アプリの起動
ENTRYPOINT ["java", "-jar", "app.jar"]