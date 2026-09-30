# E-Commerce Order System

A production-style e-commerce backend built with Spring Boot — featuring JWT authentication, a transactional checkout engine with proven rollback safety, role-based access control, caching, automated testing, and full Docker containerization with CI/CD.

**Live demo (Swagger UI):** https://ecommerce-order-system-re5w.onrender.com/swagger-ui.html

> Hosted on Render's free tier — the app may take 30–60 seconds to wake up on the first request after inactivity.

---

## Overview

This backend handles the full lifecycle of an online order: browsing products, managing a cart, checking out, and tracking an order through to delivery — all while enforcing real business rules like stock validation, atomic transactions, and role-based permissions.

## Tech Stack

- **Java 17** · **Spring Boot 4**
- **Spring Data JPA** + **MySQL** (Aiven, cloud-hosted)
- **Spring Security** with **JWT** authentication
- **Spring Cache** (Caffeine)
- **JUnit 5 + Mockito** — 22 automated tests
- **Docker** + **Docker Compose**
- **GitHub Actions** — CI pipeline running tests on every push
- **Springdoc OpenAPI** (Swagger UI)
- Deployed on **Render**, database on **Aiven**

## Key Features

- **Transactional checkout** — converts a cart into an order, deducts stock, creates order items and a payment record as a single atomic unit. Verified with both manual SQL testing and automated tests that a mid-checkout failure rolls back *everything*, with zero partial state.
- **JWT authentication** — BCrypt-hashed passwords, stateless token-based auth, role-based endpoint protection (`CUSTOMER` vs `ADMIN`).
- **Order lifecycle state machine** — enforced transitions (`CREATED → PAID → SHIPPED → DELIVERED`, or `CREATED → CANCELLED`), with cancellation automatically restoring stock.
- **Caching** — product listings are cached with Caffeine and automatically evicted on create/update/delete.
- **Global exception handling** — every error returns clean, consistent JSON instead of raw stack traces.
- **22 automated tests** covering category CRUD, the checkout rollback scenario, and the full order state machine — all running via GitHub Actions on every push.
- **Fully containerized** — one `docker compose up` runs the entire stack (app + MySQL) locally.

## Architecture


DTOs are used for all request/response bodies, keeping the API contract independent of the database schema.

## API Overview

| Area | Example endpoints |
|---|---|
| Auth | `POST /api/auth/login` |
| Users | `POST /api/users/register` |
| Categories | `GET /api/categories`, admin-only write endpoints |
| Products | `GET /api/products` (paginated, filterable, cached), admin-only write endpoints |
| Cart | `GET /api/cart`, `POST /api/cart/items` |
| Checkout | `POST /api/checkout` |
| Orders | `GET /api/orders`, `POST /api/orders/{id}/pay`, `POST /api/orders/{id}/cancel` |
| Admin Orders | `POST /api/admin/orders/{id}/ship`, `POST /api/admin/orders/{id}/deliver` |

Full interactive documentation is available at `/swagger-ui.html`, both locally and on the live deployment above.

## Running Locally

**Requirements:** Java 17, Maven, MySQL, or just Docker.

### Option 1 — Docker (recommended)

```bash
docker compose up --build
```

This starts the app and a MySQL container together. The app is available at `http://localhost:8080`.

### Option 2 — Manually

1. Copy `src/main/resources/application.properties.example` to `application.properties` and fill in your own database credentials.
2. Create a MySQL database named `ecommerce_db`.
3. Run:
```bash
mvn spring-boot:run
```

## Running Tests

```bash
mvn test
```

22 tests run in seconds — no database required, since services are tested in isolation with Mockito.

## What This Project Demonstrates

This project was built incrementally to intentionally cover the skills expected of a job-ready backend developer: not just CRUD, but atomic multi-step business logic, security, testing, containerization, and deployment — the full path from a local IntelliJ project to a live, publicly accessible, tested, and automatically-deployed API.