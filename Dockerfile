# Стъпка 1: Избиране на базовия образ
FROM openjdk:17-jdk-slim AS build

# Обновяваме източниците и инсталираме wget и unzip
RUN apt-get update --allow-releaseinfo-change \
    && apt-get install -y wget unzip \
    && wget https://services.gradle.org/distributions/gradle-7.6-bin.zip -P /tmp \
    && unzip /tmp/gradle-7.6-bin.zip -d /opt \
    && ln -s /opt/gradle-7.6/bin/gradle /usr/local/bin/gradle \
    && rm -rf /tmp/*

# Копираме build.gradle и settings.gradle
COPY build.gradle .
COPY settings.gradle .

# Изтегляне на зависимостите и изграждане на проекта
RUN gradle build --no-daemon

# Копираме изходния код
COPY src ./src

# Билдване на приложението
RUN gradle build --no-daemon

# Стъпка 2: Минимален контейнер за изпълнение на Spring Boot приложението
FROM openjdk:17-jdk-slim

# Копираме JAR файла от build етапа
COPY --from=build /build/libs/myapp.jar /myapp.jar

# Стартираме приложението
CMD ["java", "-jar", "myapp.jar"]