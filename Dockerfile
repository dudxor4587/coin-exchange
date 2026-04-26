FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY app/build.gradle app/
COPY events-contract/build.gradle events-contract/
COPY common-core/build.gradle common-core/
COPY infra-notification/build.gradle infra-notification/
COPY domain-coin/build.gradle domain-coin/
COPY domain-user/build.gradle domain-user/
COPY domain-deposit/build.gradle domain-deposit/
COPY domain-withdraw/build.gradle domain-withdraw/
COPY domain-trade/build.gradle domain-trade/
COPY domain-order/build.gradle domain-order/
COPY domain-wallet/build.gradle domain-wallet/
RUN chmod +x gradlew && ./gradlew :app:dependencies --no-daemon || true
COPY app/src app/src
COPY events-contract/src events-contract/src
COPY common-core/src common-core/src
COPY infra-notification/src infra-notification/src
COPY domain-coin/src domain-coin/src
COPY domain-user/src domain-user/src
COPY domain-deposit/src domain-deposit/src
COPY domain-withdraw/src domain-withdraw/src
COPY domain-trade/src domain-trade/src
COPY domain-order/src domain-order/src
COPY domain-wallet/src domain-wallet/src
RUN ./gradlew :app:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
