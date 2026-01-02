# Kaillux

![Java](https://img.shields.io/badge/Java-25-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A?logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-GPL--2.0-blue)

Kaillux is a Kaillera network server that enables online multiplayer for
emulators by routing player input over the network.

## Highlights

- Modernized Java 25 + Spring Boot build
- Admin API and health/metrics endpoints for ops visibility
- File-based access control with temporary bans and silencing
- Arch Linux packaging with systemd service

## Quick start

Build the server:

```bash
./gradlew build
```

Run the JAR:

```bash
java -jar build/kaillux.jar
```

## Installation

### Arch Linux

Build and install using the provided PKGBUILD:

```bash
cd packaging/archlinux
makepkg -si
```

Start the service:

```bash
sudo systemctl enable --now kaillux
```

Configuration files are installed to `/etc/kaillux/`.

### Docker

```bash
docker build -t kaillux .
docker run -p 8080:8080 -p 27888:27888/udp -p 27889-27899:27889-27899/udp kaillux
```

### Manual

Run directly with Java 25+:

```bash
java -jar build/kaillux.jar
```

## Configuration

- App config: `src/main/resources/application.properties`
- Access control rules: `src/main/resources/access.cfg`
- Logging: `src/main/resources/logback.xml`

For packaged installations (Arch Linux), config files are in `/etc/kaillux/`.

## Admin, health, and metrics

- Admin API: `/api/admin/**` (HTTP Basic; set `admin.username`/`admin.password`)
- Health probes: `/healthz`, `/livez`, `/readyz`
- Prometheus metrics: `/metrics`

These endpoints are intended for cluster-internal access only.

## License

GPL-2.0. See `LICENSE`.

## Credits

- **Original Author:** Paul Cowan
- **Contributors:** See [GitHub contributors](../../graphs/contributors)
