# ==========================================
# Giai đoạn 1: Build mã nguồn bằng Maven & JDK 21
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ mã nguồn & build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Giai đoạn 2: Runtime image siêu nhẹ bằng JRE 21 Alpine
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cài đặt múi giờ Việt Nam (Asia/Ho_Chi_Minh)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Ho_Chi_Minh

# Copy file JAR từ builder sang
COPY --from=builder /app/target/seasonal_work-1.0.jar app.jar

# Mở cổng 8083 của ứng dụng AntGo
EXPOSE 8083

# Thiết lập JVM flags tối ưu bộ nhớ container
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
