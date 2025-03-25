# Стъпка 1: Избиране на базовия образ
FROM openjdk:17-jdk-slim AS build

# Инсталираме нужните инструменти за сваляне и разархивиране
RUN apt-get update && apt-get install -y wget unzip \
    && wget https://services.gradle.org/distributions/gradle-7.6-bin.zip -P /tmp \
    && unzip /tmp/gradle-7.6-bin.zip -d /opt \
    && ln -s /opt/gradle-7.6/bin/gradle /usr/local/bin/gradle \
    && rm -rf /tmp/*

# Задаваме работната директория
WORKDIR /app

# Копираме build.gradle и settings.gradle (ако имаш такъв файл)
COPY build.gradle .
COPY settings.gradle .  # Ако използваш settings.gradle, ако не - може да я премахнеш

# Изтегляме зависимостите
RUN gradle --no-daemon build --offline

# Копираме изходния код
COPY src ./src

# Билдване на приложението
RUN gradle build --no-daemon

# Стъпка 2: Минимален контейнер за изпълнение на Spring Boot приложението
FROM openjdk:17-jdk-slim

# Задаваме работната директория
WORKDIR .

# Копираме JAR файла от build стъпката
COPY --from=build /app/build/libs/myapp.jar /app/myapp.jar

# Стартиране на приложението
CMD ["java", "-jar", "myapp.jar"]
