# Legal Aid Matching Assistant

A comprehensive platform designed to connect individuals in need of legal assistance with qualified pro-bono lawyers and non-governmental organizations (NGOs).

---

## 🛑 Problem Statement

Access to legal aid is often a complex, opaque, and intimidating process for marginalized or low-income individuals. People who need legal help struggle to find affordable or pro-bono representation, while lawyers and NGOs willing to offer such services often lack a centralized platform to discover, vet, and communicate with clients efficiently. This disconnect results in delayed justice and a widening justice gap.

## 💡 Solution

The **Legal Aid Matching Assistant** bridges this gap by providing a smart, centralized platform that seamlessly connects clients, lawyers, and NGOs. Key features include:

- **Smart Matching System:** Matches cases with the right legal providers based on expertise, location, and case priority.
- **Secure Communication:** Real-time chat functionality (WebSocket-based) for secure and immediate communication between matched parties.
- **AI-Powered Assistance:** Integration with Groq AI to assist users in drafting case descriptions and answering basic legal FAQs.
- **Case & Evidence Management:** Tools for users to upload evidence and manage the lifecycle of their case from submission to resolution.
- **Role-Based Access Control:** Distinct profiles and permissions for Clients, Lawyers, NGOs, and Administrators.

---

## 🏗 Architecture

The platform uses a modern, scalable client-server architecture:
- **Frontend (Client):** A dynamic Single Page Application (SPA) built with React. It communicates with the backend via RESTful APIs for standard operations and WebSockets for real-time chat.
- **Backend (Server):** A robust Java Spring Boot application that handles business logic, security, and integration with third-party services like Google OAuth and Groq AI.
- **Database:** PostgreSQL is used for reliable and structured data persistence.
- **Deployment:** The entire stack is containerized using Docker and orchestrated with Docker Compose, with Nginx serving the frontend and proxying API requests.

---

## 🛠 Tech Stack

### Frontend
- **Framework:** React 19
- **Routing:** React Router DOM
- **State Management:** Zustand
- **Styling:** TailwindCSS, PostCSS
- **Real-time:** SockJS, StompJS (WebSockets)
- **Maps & Data:** React Leaflet, Recharts

### Backend
- **Framework:** Java 21, Spring Boot 3.4
- **Database & ORM:** PostgreSQL, Spring Data JPA
- **Security:** Spring Security, JWT (JSON Web Tokens), Google OAuth2
- **Real-time:** Spring WebSocket
- **Monitoring & Rate Limiting:** Spring Boot Actuator, Micrometer Prometheus, Bucket4j, Caffeine Cache

### Infrastructure & External APIs
- **Containerization:** Docker, Docker Compose
- **Web Server:** Nginx
- **AI Integration:** Groq API
- **Email Service:** Spring Mail (SMTP)

---

## 📊 Architecture Diagram

```mermaid
graph TD
    %% Define Nodes
    Client[Web Browser - React Frontend]
    Nginx[Nginx Reverse Proxy]
    API[Spring Boot Backend API]
    DB[(PostgreSQL Database)]
    GoogleAuth[Google OAuth Provider]
    GroqAI[Groq AI Service]
    SMTP[SMTP Email Server]

    %% Define Connections
    Client -->|HTTP / REST| Nginx
    Client -->|WebSocket| Nginx
    Nginx -->|Proxy| API
    
    API -->|JPA / JDBC| DB
    API -->|OAuth2| GoogleAuth
    API -->|HTTP REST| GroqAI
    API -->|SMTP| SMTP
    
    %% Styling
    classDef primary fill:#4F46E5,stroke:#3730A3,stroke-width:2px,color:#fff;
    classDef secondary fill:#10B981,stroke:#047857,stroke-width:2px,color:#fff;
    classDef external fill:#F59E0B,stroke:#B45309,stroke-width:2px,color:#fff;
    classDef database fill:#3B82F6,stroke:#1D4ED8,stroke-width:2px,color:#fff;
    
    class Client,Nginx primary;
    class API secondary;
    class DB database;
    class GoogleAuth,GroqAI,SMTP external;
```

---

## 🗄 Entity Relationship Diagram

Below is a simplified view of the core domain entities and their relationships.

```mermaid
erDiagram
    USER ||--o| PROFILE : has
    USER ||--o{ CASE : "creates / assigned to"
    USER ||--o{ APPOINTMENT : schedules
    USER ||--o{ CHAT_MESSAGE : "sends / receives"
    USER ||--o{ NOTIFICATION : receives
    
    CASE ||--o{ EVIDENCE_FILE : contains
    CASE ||--o{ MATCH : has
    
    MATCH }|--|| PROVIDER : "connects to (Lawyer/NGO)"
    
    PROFILE ||--o| LAWYER_PROFILE : "can be"
    PROFILE ||--o| NGO_PROFILE : "can be"
    
    USER {
        int id PK
        string email
        string role
    }
    
    CASE {
        int id PK
        string caseNumber
        string title
        string status
        int createdBy FK
    }
    
    MATCH {
        int id PK
        int caseId FK
        string providerType
        int providerId
        int score
    }
    
    CHAT_MESSAGE {
        int id PK
        string content
        int senderId FK
        int recipientId FK
    }
```
