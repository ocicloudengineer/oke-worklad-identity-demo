# Use Oracle's official JDK 21 JRE image
# This assumes you accept Oracle's Free Use Terms and Conditions for Oracle Java SE.
# If you need commercial use, ensure you have the appropriate Oracle Java SE Universal Subscription.
FROM container-registry.oracle.com/graalvm/graalvm-community

WORKDIR /app

# Copy the "fat JAR" created by Maven Assembly Plugin
# The name is artifactId-version-jar-with-dependencies.jar
COPY target/workload-identity-demo-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]