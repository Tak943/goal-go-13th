# ベースの環境 Javaが動くOSを持ってくる
FROM eclipse-temurin:17-jdk-alpine

# containerの中での作業部屋を /app と名付け
WORKDIR /app

# Spring Bootのアプリ本体（いままでのJavaコードをまとめて.jarに固めたもの）を、箱の作業部屋にコピーする
COPY build/libs/*.jar app.jar

# この箱は8080番ポートで通信を受け付ける
EXPOSE 8080

# 箱の起動時に自動で実行するコマンド（java -jar app.jar）
ENTRYPOINT [ "java", "-jar", "app.jar" ]