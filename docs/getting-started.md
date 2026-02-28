# Getting Started

## Prerequisites

- Java 11+
- sbt

## Run locally

```bash
sbt test
sbt run
```

The API starts on port `8083` by default.

## Sample config

The included sample game configuration is:

`src/main/resources/game_configs/space.yaml`

## Example UI layer

The repository includes a PHP example that acts as a trusted middle layer between UI logic and the Omen API:

`src/main/php/`
