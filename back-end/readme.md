# Legal Aid Matching Platform - Backend

## Overview

SpringBoot-based backend for the Legal Aid Matching Platform with JWT authentication, location-based matching, and real-time messaging.

## Tech Stack

- **Framework**: SpringBoot 3.x
- **Database**: PostgreSQL (production), SQLite (development)
- **Authentication**: JWT (access + refresh tokens), OAuth2 (Google/GitHub)
- **Location Services**: Google Maps API, Geolocation API
- **Real-time**: WebSockets
- **Build Tool**: Maven

## Dependencies

### Core

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Database

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
</dependency>
```

### Security & Authentication

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-client</artifactId>
</dependency>
```

### WebSockets

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### External APIs

```xml
<dependency>
    <groupId>com.google.maps</groupId>
    <artifactId>google-maps-services</artifactId>
    <version>2.2.0</version>
</dependency>
```
