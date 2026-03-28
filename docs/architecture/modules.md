# 🧩 A3Shield Module Architecture

## Status

Draft

---

## 📌 Overview

A3Shield is structured as a collection of **independent but composable modules**, each responsible for a specific aspect of authentication and authorization.

This modular design allows developers to:

* Use only the components they need
* Extend functionality easily
* Maintain clear separation of concerns

---

## 🧱 Core Modules

### 🔹 a3-core

**Purpose:**
Provides shared abstractions, interfaces, and domain models used across all modules.

**Responsibilities:**

* Define core interfaces (Subject, Resource, Action)
* Shared data structures
* Common utilities

---

### 🔐 a3-auth

**Purpose:**
Handles user authentication and identity verification.

**Responsibilities:**

* User login and registration
* Credential validation
* Password hashing
* Multi-factor authentication (future)

**Output:**

* Authenticated principal

---

### 🎟 a3-token

**Purpose:**
Manages token-based authentication and session handling.

**Responsibilities:**

* JWT generation and validation
* Token refresh
* Token revocation strategies

---

### 👤 a3-user

**Purpose:**
Manages user data and identity lifecycle.

**Responsibilities:**

* User CRUD operations
* Role assignment
* Account state management

---

### 🔑 a3-access

**Purpose:**
Implements traditional access control models.

**Responsibilities:**

* Role-Based Access Control (RBAC)
* Permission-Based Access Control (PBAC)
* Role-permission mapping
* Permission evaluation

```
                ┌────────────────────────┐
                │   A3 Control Plane     │
                │------------------------│
                │ Auth (login, JWT)      │
                │ Policy Management      │
                │ User/Role Management   │
                │ Policy Storage (DB)    │
                └──────────┬─────────────┘
                           │
                 (policy sync / fetch)
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼──────┐  ┌───────▼──────┐  ┌────────▼──────┐
│ Order Service│  │Payment Service│  │User Service  │
│--------------│  │--------------│  │---------------│
│ A3 SDK       │  │ A3 SDK       │  │ A3 SDK        │
│ (enforcement)│  │ (enforcement)│  │ (enforcement) │
└──────────────┘  └──────────────┘  └───────────────┘
```

#### a3-enforcer → what microservices import
#### a3-policy-service → central policy system


---

### 🧠 a3-policy-engine

**Purpose:**
Provides dynamic and context-aware access control through policy evaluation.

**Responsibilities:**

* Attribute-Based Access Control (ABAC)
* Policy definition and execution
* Context-based decision making

**Note:**
This module represents the **core innovation of A3Shield**.

---

### 🚪 a3-gateway

**Purpose:**
Acts as the entry point for incoming requests and enforces security policies.

**Responsibilities:**

* Request interception
* Authentication pipeline execution
* Authorization enforcement
* Routing (future)

---

### 🔌 a3-sdk

**Purpose:**
Provides client libraries for integrating A3Shield into applications.

**Responsibilities:**

* Simplify API usage
* Provide abstractions for developers
* Enable multi-language support

---

## 🔗 Module Interaction

* `a3-gateway` orchestrates request handling
* `a3-auth` and `a3-token` handle identity
* `a3-access` performs role/permission checks
* `a3-policy-engine` performs advanced evaluation

---

## 📦 Deployment Model

Initially deployed as a **single application**, with clear boundaries that allow future separation into independent services.

---

## 📌 Summary

The modular architecture of A3Shield ensures flexibility, scalability, and maintainability, enabling both simple and advanced use cases.
