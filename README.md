# NextEdge AI

**A multi-tenant data automation platform: connect your data sources, sync them, and work with them through an AI-assisted operator console.**

🔗 **Live demo:** https://d1btvl7l2kals2.cloudfront.net/

Email : demo@nextedge.ai
Password : 5xt90ds7THic2tX4gUBQY8a7

Creds for demo usage.

---

## What it does

- **Connects to your data** through File/CSV, Amazon S3, PostgreSQL, MySQL and MongoDB connectors.
- **Keeps every tenant isolated.** Each organization (identified by a NextEdge ID) gets its own database, and automated tests confirm that one tenant can never read another tenant's data.
- **Runs sync jobs in the background** with dedicated worker services.
- **Uses AI** through Amazon Bedrock, behind a provider abstraction so the model can be swapped later.
- **Deploys to AWS automatically** from GitHub Actions, with no long-lived cloud keys stored anywhere.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript |
| Backend | Java 11, Spring, Maven |
| Proxy / static hosting | Node.js |
| Data | MongoDB, Redis |
| AI | Amazon Bedrock |
| Infrastructure | AWS (EC2, CloudFormation, Secrets Manager, SSM), Docker Compose, CloudFront |
| CI/CD | GitHub Actions with OIDC |

## Architecture

```
Browser ──► CloudFront ──► Node.js proxy (port 80, only public entry point)
                                │
                 ┌──────────────┴──────────────┐
                 ▼                             ▼
          REST API (arcade)             Additional API (karibu)
                 │
                 ▼
       Core services: tenant routing, security, AI provider
                 │
     ┌───────────┼─────────────┐
     ▼           ▼             ▼
  MongoDB      Redis      Sync workers (viper) ──► Connectors
 (1 DB per tenant)
```

- Everything except the web proxy runs on a private Docker network.
- Server access goes through AWS Systems Manager Session Manager; SSH is closed.
- On startup, system database migrations run first, then per-tenant migrations for every configured tenant.

## Repository layout

| Path | Purpose |
|---|---|
| `backend-master/arcade` | Main browser-facing REST API |
| `backend-master/core` | Domain models, tenant routing, services, migrations, security, AI provider abstraction |
| `backend-master/connector` | Connector framework and implementations |
| `backend-master/dbm` | System and per-tenant MongoDB migrations |
| `backend-master/viper` | Sync processing workers |
| `backend-master/karibu` | Additional API surface |
| `spectrum-master` | React/TypeScript operator console |
| `spectrum-master/proxy` | Node.js reverse proxy and static frontend runtime |
| `infrastructure/aws` | CloudFormation, deployment scripts, monitoring, backups, cost controls |
| `design-system` | Visual system and UI guidelines |

## Running it locally

**Backend** (Java 11, Maven 3.8+):

```bash
cd backend-master
mvn clean test
mvn clean package -DskipTests
```

**Frontend** (Node.js 20.18.3, npm 10.8.2):

```bash
cd spectrum-master
npm ci
cd proxy && npm ci && cd ..
npm run tsc -- --noEmit
npm run proxy-build
npm run build
```

**Full stack:** `docker-compose up` from the repository root.

Credentials are never stored in source, config files or GitHub. Deployed environments read them from AWS Secrets Manager at runtime.

## Deployment

Pushing to `main` triggers a GitHub Actions workflow that authenticates to AWS with short-lived OIDC credentials and deploys through SSM. Full details are in [`infrastructure/aws/README.md`](infrastructure/aws/README.md).

The live demo runs on a single EC2 instance in `ap-south-1`. It is a demo environment, not a high-availability production setup.

## Security

Every push is scanned for leaked secrets, critical dependency vulnerabilities, and build or test failures.
