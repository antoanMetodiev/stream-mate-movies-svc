# Стъпка 1: Избиране на базовия образ (Java 17 в този пример)
FROM openjdk:17-jdk-slim AS build

# Стъпка 2: Задаваме директорията за работата на контейнера
WORKDIR /app

# Стъпка 3: Копираме pom.xml и изтегляме зависимостите, за да се възползваме от кеширането на слоевете
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Стъпка 4: Копираме целия изходен код в контейнера
COPY src ./src

# Стъпка 5: Билдване на приложението
RUN mvn clean install -DskipTests

# Стъпка 6: Създаване на минимален контейнер за изпълнение на Spring Boot приложението
FROM openjdk:17-jdk-slim

# Стъпка 7: Задаваме директорията за приложението
WORKDIR /app

# Стъпка 8: Копираме само резултата от билдването (JAR файлът) от предишния слой
COPY --from=build /app/target/myapp.jar /app/myapp.jar

# Стъпка 9: Стартиране на приложението
CMD ["java", "-jar", "myapp.jar"]