# paymenthub-ee-bff

Backend for the PaymentHub EE operations web console: the REST API the console calls to
look at transfers, batches, users and roles, plus the OAuth2 endpoints it logs in with.
This repository holds the migrated `ph-ee-operations-app`.

[![License](https://img.shields.io/badge/License-MPL--2.0-blue.svg)](LICENSE)

## What it does

- Multi-tenant REST API under `/api/v1/**`. Every request carries a `Platform-TenantId`
  header (or a `tenantIdentifier` request parameter), which selects the tenant database.
- OAuth2 endpoints: `/oauth/token` (password, refresh_token and client_credentials
  grants), `/oauth/token_key` and `/oauth/check_token`. Tokens are RSA-signed JWTs.
- Database schema per tenant, managed by Flyway. Migrations live in
  `src/main/resources/sql/migrations`.

## Running it

The application needs a MySQL server with the core schema (`tenants`) and one schema per
tenant already created — the JDBC url does not create databases.

```
docker compose up
```

The compose file starts MySQL and the application together with the `bb` profile, which
declares the tenants `tn01` and `tn02`. On startup the app creates the tenant rows,
applies the Flyway migrations to every schema, and listens on port 5000.

To run it against a MySQL you already have:

```
./gradlew bootJar
FINERACT_DATASOURCE_CORE_HOST=127.0.0.1 SPRING_PROFILES_ACTIVE=bb \
  java -jar build/libs/paymenthub-ee-bff-*.jar
```

## Getting a token

`client` is the console's client, with an empty secret; `mifos` is the seeded admin user.

```
curl -X POST http://localhost:5000/oauth/token \
  -H 'Platform-TenantId: tn01' \
  -d 'grant_type=password&client_id=client&client_secret=&username=mifos&password=password'
```

Then call the API with `Authorization: Bearer <access_token>` and the same
`Platform-TenantId` header. The token's audience must contain the tenant, so a token
issued for `tn01` is rejected on `tn02`.

## Build

Java 21 and Gradle (use the wrapper). Library versions come from
`org.mifos:paymenthub-ee-bom`, so do not hardcode them in `build.gradle`.

```
./gradlew build
```

## Branches

- `dev` is the active development branch — all PRs should target `dev`.
- `main` holds released versions.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md).
