FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java","-Xmx160m","-jar","/app/app.jar"]
