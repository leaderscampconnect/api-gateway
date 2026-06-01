FROM eclipse-temurin:17
COPY target/APIGateway-0.0.1-SNAPSHOT.jar apigetway.jar
EXPOSE 9001
ENTRYPOINT ["java","-jar","apigetway.jar"]
