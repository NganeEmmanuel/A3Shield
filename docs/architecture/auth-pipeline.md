# 🔗 A3Shield Authentication & Authorization Pipeline

## Status

Draft

---

## 📌 Overview

A3Shield processes incoming requests through a structured **authentication and authorization pipeline**, ensuring that each request is validated and evaluated before access is granted.

The pipeline is implemented using the:

> **Chain of Responsibility Pattern**

This allows flexible, ordered, and extensible request processing.

---

## 🧠 Pipeline Flow

```
Incoming Request
   ↓
[Token Extraction]
   ↓
[Token Validation]
   ↓
[User Resolution]
   ↓
[RBAC Check]
   ↓
[PBAC Check]
   ↓
[ABAC Policy Evaluation]
   ↓
Decision (ALLOW / DENY)
```

---

## 🧩 Pipeline Components

### 1. Token Extraction

* Extract token from request headers
* Validate presence of authentication data

---

### 2. Token Validation

* Verify token integrity
* Check expiration and signature

---

### 3. User Resolution

* Load user details from token
* Attach user to request context

---

### 4. RBAC Evaluation

* Check role-based permissions

---

### 5. PBAC Evaluation

* Check direct permission assignments

---

### 6. ABAC Policy Evaluation

* Evaluate policies based on:

    * Subject attributes
    * Resource attributes
    * Action
    * Context

---

## 🧱 Core Interfaces

```java
interface AuthHandler {
    void handle(AuthContext context);
    void setNext(AuthHandler next);
}
```

---

## 🔗 Handler Chaining

Handlers are linked sequentially:

```java
AuthHandler chain =
    new TokenExtractionHandler()
        .linkWith(new TokenValidationHandler())
        .linkWith(new UserResolutionHandler())
        .linkWith(new RbacHandler())
        .linkWith(new AbacHandler());
```

---

## 🧠 AuthContext

The pipeline operates on a shared context object:

```java
class AuthContext {
    Request request;
    User user;
    Token token;
    Decision decision;
}
```

---

## 🔥 Benefits of This Approach

* Extensible pipeline (add/remove handlers easily)
* Clear separation of concerns
* Ordered execution control
* Reusable components

---

## ⚠️ Failure Handling

* Any handler can terminate the chain
* Errors result in immediate denial or response

---

## 📌 Summary

The authentication pipeline ensures that all requests are processed in a structured and secure manner, combining multiple access control strategies into a unified flow.
