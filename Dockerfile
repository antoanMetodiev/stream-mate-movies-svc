# Стъпка 1: Избиране на базовия образ
FROM openjdk:17-jdk-slim AS build

# Задаваме работната директория
WORKDIR /app

# Копираме build.gradle и settings.gradle (ако имаш такъв файл)
COPY build.gradle .
COPY settings.gradle .  

# Изтегляме зависимостите
RUN gradle --no-daemon build --offline

# Копираме изходния код
COPY src ./src

# Билдване на приложението
RUN gradle build --no-daemon

# Стъпка 2: Минимален контейнер за изпълнение на Spring Boot приложението
FROM openjdk:17-jdk-slim

# Задаваме работната директория
WORKDIR /app

# Копираме JAR файла от build стъпката
COPY --from=build /app/build/libs/myapp.jar /app/myapp.jar

# Стартиране на приложението
CMD ["java", "-jar", "myapp.jar"]
