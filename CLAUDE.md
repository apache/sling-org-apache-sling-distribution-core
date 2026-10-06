# Apache Sling Distribution Core

## Overview

This bundle provides the concrete infrastructure for the Apache Sling Content Distribution (SCD) module: agent management, queue implementations, content serialization (FileVault / Jackrabbit Vault), HTTP transport, and OSGi servlet endpoints. It implements the API contracts defined in `sling-org-apache-sling-distribution-api` and serves as the common runtime layer that journal-based and job-based distribution implementations build on top of.

## Tech Stack

- Java 8, OSGi bundle (bnd-maven-plugin, `sling-bundle-parent` v66)
- Apache Jackrabbit / Jackrabbit Vault 3.6.4 for content serialization
- Apache HttpComponents 4.x for HTTP transport
- OSGi DS annotations (`@Component`, `@Reference`)
- Testing: JUnit 4, Mockito 4, Sling OSGi Mock, Sling Mock (Oak-backed), Pax Exam for integration tests

## Build & Test Commands

```
mvn clean install                      # build, unit tests (excludes *IT.java)
mvn verify                             # build + integration tests (Pax Exam)
mvn test -Dtest=SomeUnitTest           # single unit test class
mvn failsafe:integration-test -Dit.test=SomeIT   # single IT class
```

Integration tests (`*IT.java`) are excluded from `maven-surefire-plugin` and run via `maven-failsafe-plugin`. They use Pax Exam and spin up an OSGi container, so they are slow and require network access to download bundles.

## Architecture

Key packages under `org.apache.sling.distribution`:

| Package | Purpose |
|---|---|
| `impl` | `DefaultDistributor` — main entry point; orchestrates agent lookup and dispatch |
| `agent.spi` / `agent.impl` | `DistributionAgent` SPI and concrete agent types (Simple, Sync, Forward) |
| `packaging` / `packaging.impl` | `DistributionPackage`, `DistributionPackageBuilder`, `DistributionPackageInfo`; Vault-based serialization under `impl` |
| `serialization` / `serialization.impl.vlt` | `DistributionContentSerializer` and FileVault implementation |
| `queue` / `queue.spi` / `queue.impl` | `DistributionQueue` SPI; job-handling, resource-backed, and simple (in-memory) queue impls |
| `transport` / `transport.impl` | HTTP transport; `DistributionTransportHandler` for pulling/pushing packages over HTTP |
| `trigger` / `trigger.impl` | `DistributionTrigger` SPI; scheduled, JCR event-based, and remote-event-based triggers |
| `servlet` | Sling servlet endpoints for agent status, queue, log, import, export, and trigger management |
| `monitor` | Felix HealthCheck integration (`DistributionQueueHealthCheck`) |
| `resources` | Virtual Sling resource types for agent/queue/log REST representations |

Data flow: `DefaultDistributor` → resolves `DistributionAgent` OSGi service → agent calls `DistributionPackageBuilder` to serialize JCR content → places `DistributionQueueItem` in a `DistributionQueue` → transport sends it.

## Conventions & Gotchas

- The `agent.spi` and `queue.spi` sub-packages are SPI (stable, versioned); `*.impl` packages are internal and not exported.
- There are **three queue implementations**: job-handling (backed by Sling Jobs), resource-backed (JCR nodes), and simple (in-memory). Journal-based distribution (sling-org-apache-sling-distribution-journal) replaces these with a journal-aware queue; don't conflate the two.
- Integration tests annotated with `*IT.java` require the full OSGi container; running them locally may fail if artifact resolution is blocked by a corporate Maven proxy. Run `mvn verify -Pit` (or without `-P`, since failsafe binds to `verify` phase automatically).
- `bnd.bnd` at the repo root controls OSGi bundle headers in addition to the POM; check both when changing exported packages.
- Jackrabbit Vault serialization produces binary-less packages by default in the journal distribution path — binaries are stored in the shared blob store and referenced by ID, not inlined.
