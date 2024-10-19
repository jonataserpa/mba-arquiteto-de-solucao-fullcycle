#
# Build stage
#
FROM maven:3.8.3-openjdk-17 AS build
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY ./src/ $HOME/src
COPY ./pom.xml $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME/pom.xml clean install

#
# Package stage
#
FROM openjdk:17
COPY --from=build /usr/app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT java -jar /app/app.jar
