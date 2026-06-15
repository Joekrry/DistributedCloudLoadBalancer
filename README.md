# CloudBalancer

Distributed cloud load balancer with encrypted file storage

A JavaFX desktop application backed by a dual SQLite/MySQL database setup, MQTT-driven
container scaling, and AES-encrypted, chunked file storage distributed across a pool of
Docker file-server containers.

## Features

- User accounts with PBKDF2-hashed passwords and admin/standard roles.
- AES-256 file encryption with chunking, CRC32 checksums, and SFTP-based chunk transfer.
- Load balancer with FCFS, Round Robin, Priority (with aging), and SJN scheduling.
- MQTT-based dynamic scaling and Docker container health checks.
- Local SQLite database with background synchronisation to a central MySQL database.
- Built-in terminal emulation for local and remote (file-server) commands.

## Requirements

- Java 20
- Maven 3
- Docker and Docker Compose

## Running

Start the supporting infrastructure:

```
docker compose up -d
```

Run the application:

```
mvn javafx:run
```

## Building

```
mvn clean package
```

Produces an executable JAR in `target/`.

## Testing

```
mvn test
```

## CI

See [docs/CI_SETUP.md](docs/CI_SETUP.md) for the Jenkins pipeline configuration.
