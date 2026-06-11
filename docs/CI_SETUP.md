# CI Setup

This project uses Jenkins for continuous integration, defined in the root `Jenkinsfile`.

## Pipeline Stages

1. **Checkout** — pulls the source from the configured SCM.
2. **Build** — compiles the project with `mvn compile`.
3. **Test** — runs the JUnit 5 suite with `mvn test` and publishes results.
4. **Package** — builds the shaded executable JAR with `mvn package` and archives it.

## Jenkins Requirements

- A JDK 20 tool installation named `JDK20`.
- A Maven 3 tool installation named `Maven3`.
- A pipeline job pointed at this repository, using the `Jenkinsfile` from SCM.

## Local Equivalent

```
mvn clean package
```

The resulting executable JAR is written to `target/cloudbalancer-1.0-SNAPSHOT.jar`.
