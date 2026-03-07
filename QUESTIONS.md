# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```
Generated from OpenAPI YAML:
Pros: Single source of truth, auto-generated code reduces boilerplate, API contracts are explicit, easier for client generation, documentation stays synchronized
Cons: Setup overhead, less flexibility for custom logic, requires tooling knowledge, migration complexity if schema changes frequently

Direct Coding:
Pros: Faster initial development, more control, easier to refactor, no code generation dependencies
Cons: Documentation drifts from implementation, inconsistent patterns across endpoints, harder to maintain API contracts, clients must reverse-engineer specs

My Choice: OpenAPI-first approach for all endpoints
Reasoning:

Consistency: All APIs follow the same pattern, reducing cognitive load and maintenance burden

Long-term scalability: As the API grows, generated code prevents drift between documentation and implementation

Client ecosystem: Mobile/frontend teams benefit from generated clients

Contract enforcement: OpenAPI acts as a guard against breaking changes

Time investment pays off: Initial setup cost is offset by reduced documentation and integration bugs

The direct coding approach is tempting for speed but creates technical debt. Once you have multiple APIs, maintaining consistency manually becomes error-prone. I'd recommend migrating Product and Store to OpenAPI generation to match Warehouse.

```
---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```
Focus on critical paths first: Prioritize tests for core business logic (e.g., warehouse creation, stock management) and high-risk areas like concurrency and database interactions, as seen in the failing testConcurrentReadsAreNonBlocking integration test.
Balance with constraints: Allocate 70% effort to unit tests (fast, isolated), 20% to integration tests (end-to-end flows), and 10% to exploratory/edge-case tests. Use parameterized tests for data-driven scenarios to maximize coverage without excessive code.

Types of Tests to Focus On:
Unit Tests: Core for domain logic (e.g., CreateWarehouseUseCase), using mocks for dependencies. They provide quick feedback and catch logic errors early.
Integration Tests: Essential for database operations, API endpoints, and concurrency (e.g., the WarehouseConcurrencyIT class). Test real components together to ensure data consistency and thread safety.
Parameterized Tests: Ideal for validating multiple inputs/outputs (e.g., warehouse validation rules) to cover edge cases efficiently without duplicating test code.

Ensuring Effective Coverage Over Time:
Tools and Metrics: Integrate JaCoCo or SonarQube in Maven builds to enforce minimum coverage thresholds (e.g., 80% line/branch coverage). Run tests in CI/CD pipelines to prevent regressions.
Maintenance Practices: Review coverage reports regularly; add tests for new features and refactor legacy code. Use test-driven development (TDD) for new logic to maintain quality.
Resource Optimization: Parallelize execution, and use in-memory databases (e.g., H2) for faster integration tests.
```
