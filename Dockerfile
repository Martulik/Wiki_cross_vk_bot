FROM maven:3.8.5-openjdk-17
RUN mvn compile
RUN mvn clean install
RUN mvn exec:java -Dexec.mainClass="ru.wiki.game.Main"

