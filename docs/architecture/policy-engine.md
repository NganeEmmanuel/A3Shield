# 🧠 A3Shield Policy Engine Architecture

## Status

Draft

---

## 📌 Overview

The A3Shield Policy Engine is responsible for evaluating access requests using **Attribute-Based Access Control (ABAC)** principles.

It enables dynamic, context-aware authorization decisions based on structured policies.

---

## 🎯 Objectives

* Support fine-grained access control
* Enable dynamic policy evaluation
* Provide extensible policy definitions
* Integrate with RBAC and PBAC systems

---

## 🧩 Core Model

Access decisions are based on:

* Subject
* Resource
* Action
* Context

Decision Function:

```
Decision = f(Subject, Resource, Action, Context)
```

---

## 🧱 Core Components

### Policy

Defines access rules with conditions and effects.

```json
{
    "effect": "ALLOW",
    "subject": { "role": "admin" },
    "resource": "document",
    "action": "read",
    "condition": "resource.ownerId == subject.id"
}
```

### RequestContext

Encapsulates runtime request data.

```java
class RequestContext {
    Subject subject;
    Resource resource;
    Action action;
    Map<String, Object> attributes;
}
```

### PolicyEngine

Evaluates policies and returns a decision.

```java
interface PolicyEngine {
    Decision evaluate(RequestContext context);
}
```
```java
enum Decision {
    ALLOW,
    DENY
}
```
### Condition Engine

Evaluates logical expressions within policies.

---

## 🔄 Evaluation Flow

```
Request →
   Load Policies →
      Match Policies →
         Evaluate Conditions →
            Combine Decisions →
               Return Result
```

---

## 🔍 Policy Matching

Policies are filtered based on:

* Subject attributes
* Resource type
* Action

Only relevant policies are evaluated.

---

## 🧠 Condition Evaluation

Conditions are evaluated using a **custom expression engine**.

### Example:

```
resource.ownerId == subject.id
```

#### 🧠 Example End-to-End
Request:
```json
{
    "subject": { "type": "USER", "id": 1, "role": "user" }, // type can be USER, SERVICE, etc.
    "resource": { "ownerId": 1 },
    "action": "read" // write, delete, etc.
}
```

Policy:
```json
{
  "version": "v1",
  "id": "policy-1",
  "effect": "ALLOW",
  "subject": {
    "type": "USER",
    "role": "user"
  },
  "resource": "document",
  "action": "read",
  "condition": "resource.ownerId == subject.id",
  "updatedAt": "timestamp"
}
```

Evaluation:
```json
"subject.type == USER" → true
"subject.role == user" → true
"resource.ownerId == subject.id" → true
"action == read" → true
"all conditions true → ALLOW"
```

---

## 🔤 Policy DSL

Supported features:

* Equality and comparison operators
* Logical operators (AND, OR)
* Attribute access (subject, resource, context)

---

## ⚖️ Decision Strategy

Conflict resolution follows:

* DENY overrides ALLOW
* Default decision is DENY

---

## 🧱 Internal Architecture

```
PolicyEngine
   ├── PolicyRepository
   ├── PolicyMatcher
   ├── ConditionParser
   ├── ConditionEvaluator
   └── DecisionCombiner
```

---

## 🔌 Extensibility

The engine is designed to support:

* Custom policy formats
* Pluggable evaluation strategies
* External data sources

---

## 📌 Summary

The policy engine is the core of A3Shield, enabling advanced and flexible access control through dynamic policy evaluation.
