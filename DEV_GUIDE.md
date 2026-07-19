# Syncari Developer Run Guide

This guide explains how to run the extracted Syncari workspace locally, what config files need to exist, where to put them, and which commands to use.

The workspace contains two source snapshots for each side of the product:

- `backend-master`: backend baseline snapshot.
- `backend-SYN-20253`: backend branch/version snapshot with SyncAI additions.
- `spectrum-master`: frontend baseline snapshot.
- `spectrum-SYN-20252`: frontend branch/version snapshot with SyncAI UI additions.

Use the matching pair depending on what you are testing:

- Baseline product: `backend-master` + `spectrum-master`
- SyncAI branch/version: `backend-SYN-20253` + `spectrum-SYN-20252`

These folders are not Git checkouts in this extracted workspace, so treat them as plain source folders.

## 1. Prerequisites

Install these locally:

- Java 11
- Maven 3.x
- Node.js `20.18.3`
- npm `10.8.2`
- Docker, optional but useful for MongoDB/Redis
- MongoDB, required for backend runtime
- Redis, required for backend runtime
- Google Cloud credentials and Pub/Sub resources, required for full backend runtime
- SMTP/mail credentials, required for mail flows
- Third-party connector credentials if testing OAuth/integrations

Check versions:

```powershell
java -version
mvn -version
node -v
npm -v
docker --version
```

Expected frontend engine versions from `spectrum-master/package.json`:

```text
node: v20.18.3
npm: 10.8.2
```

## 2. Recommended Local Modes

There are three practical ways to run this workspace.

### Mode A: Frontend only with mock server

Use this when you only want to inspect Spectrum UI behavior without running Java services.

Runs:

- Spectrum React dev server
- Spectrum mock server
- Optional Spectrum proxy configured with `USE_MOCK=true`

Does not require:

- MongoDB
- Redis
- GCP credentials
- backend Java services

### Mode B: Frontend with remote/integration backend

Use this when you have access to a shared backend environment.

Runs:

- Spectrum React dev server
- Spectrum proxy

Proxy points `/arcade` calls to a remote `ARCADE_TARGET`, for example:

```powershell
$env:ARCADE_TARGET='http://arcade.integration.syncari.net/'
```

### Mode C: Full local frontend + backend

Use this when you need local backend development.

Runs:

- MongoDB
- Redis
- `arcade` backend service
- optionally `karibu`
- optionally `viper`
- Spectrum proxy
- Spectrum frontend

This mode needs many local properties and secrets.

## 3. Frontend Setup

Choose either `spectrum-master` or `spectrum-SYN-20252`.

Examples below use `spectrum-master`. Replace the folder name with `spectrum-SYN-20252` if working on SyncAI frontend.

### 3.1 Create `.env`

File to create:

```text
spectrum-master/.env
```

Copy from:

```text
spectrum-master/.env.example
```

PowerShell:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
Copy-Item .env.example .env
```

Current `.env.example` contains:

```env
REACT_APP_ZENDESK_API_KEY=REPLACE_ME
REACT_APP_BOTCO_AI_API_KEY=REPLACE_ME
REACT_APP_ZENDESK_URL='https://d3v-syncari.zendesk.com/'
```

For local-only UI work, you can usually keep these values as placeholders unless testing Zendesk/Botco flows.

Do not put production secrets into `.env`.

### 3.2 Install frontend dependencies

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm install
```

If dependency resolution fails after switching branches/snapshots:

```powershell
npm run clean-build
```

That script removes build artifacts and node modules, installs proxy dependencies, and rebuilds.

### 3.3 Start the frontend dev server

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run start
```

The `start` script runs:

```text
npm run convert-less-to-ts && node scripts/start.js
```

Important note: after changing LESS variables, run:

```powershell
npm run convert-less-to-ts
```

The app normally expects API traffic to go through the proxy at:

```text
http://localhost:8088
```

## 4. Spectrum Proxy Setup

The proxy lives here:

```text
spectrum-master/proxy
```

It is a Node/Express proxy/gateway for Spectrum. It serves static built frontend assets in production mode and proxies `/arcade` API calls to Arcade.

### 4.1 Proxy environment variables

The actual proxy env vars are documented in `spectrum-master/proxy/src/index.js` and `proxy/src/utils/ConfigUtil.js`.

Use these:

| Variable | Default | Purpose |
|---|---:|---|
| `ARCADE_TARGET` | `http://localhost:8080` | Backend Arcade target for `/arcade` calls |
| `ARCADE_LOG_LEVEL` | `info` | Use `debug` to log redirected URLs |
| `SPECTRUM_PORT` | `8088` | Port the proxy listens on |
| `SECURE_COOKIES` | `true` | Use `false` for local HTTP |
| `DISABLE_XSRF` | `false` | Use `true` only for local development |
| `USE_MOCK` | `false` | Use `true` to route selected calls to mock server |
| `NODE_ENV` | unset/dev | Production behavior changes some mock/proxy behavior |

### 4.2 Install proxy dependencies

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
npm install
```

Or from the Spectrum root:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run proxy-install
```

### 4.3 Run proxy against local backend

This assumes Arcade is running on `http://localhost:8080`.

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
$env:ARCADE_TARGET='http://localhost:8080'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
$env:ARCADE_LOG_LEVEL='debug'
npm run dev
```

### 4.4 Run proxy against integration backend

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
$env:ARCADE_TARGET='http://arcade.integration.syncari.net/'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
$env:ARCADE_LOG_LEVEL='debug'
npm run dev
```

There is also a package script:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run proxy:dev:integration
```

That script is written in Unix-style env syntax, so on Windows PowerShell the explicit `$env:ARCADE_TARGET=...` flow is usually safer.

### 4.5 Run proxy against mock server

Start mock server:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run start-mock
```

In another terminal:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
$env:USE_MOCK='true'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
npm run start
```

## 5. Backend Setup

Choose either `backend-master` or `backend-SYN-20253`.

Examples below use `backend-master`. Replace with `backend-SYN-20253` if working on SyncAI backend.

### 5.1 Backend modules

The root Maven POM includes:

- `utils`
- `connector`
- `core`
- `restutils`
- `dbm`
- `visions`
- `viper`
- `arcade`
- `karibu`

Other present folders:

- `microbenchmarks`
- `smokescreen`
- `ci`

Main runnable services:

- `arcade`: primary REST API for Spectrum/browser UI.
- `karibu`: API service with v1/v2 endpoints; configured to run on port `8081`.
- `viper`: sync/pipeline execution worker/service; commonly run on a separate port such as `8083`.
- `dbm`: database migration/management module.

### 5.2 Backend config files already present

These files exist in the repo:

```text
backend-master/arcade/src/main/resources/application.properties
backend-master/karibu/src/main/resources/application.properties
backend-master/viper/src/main/resources/application.properties
backend-master/dbm/src/main/resources/application.properties
```

Do not hardcode personal secrets directly into these checked-in resource files.

Instead, create a local-only properties file and pass it at runtime.

Recommended local file:

```text
backend-master/local-dev.properties
```

For the SyncAI backend snapshot:

```text
backend-SYN-20253/local-dev.properties
```

### 5.3 Minimal local backend properties template

Create:

```text
backend-master/local-dev.properties
```

Template:

```properties
# Core Spring profile
spring.profiles.active=local

# MongoDB
mongodb.uri=mongodb://localhost:27017/syncari_local
mongodb.readOnlyUri=mongodb://localhost:27017/syncari_local

# Redis
redis.host=localhost
redis.port=6379
redis.password=
new.redis.host=localhost
new.redis.port=6379
new.redis.password=
entitycache.redis.db.host=localhost
entitycache.redis.db.port=6379
entitycache.redis.db.password=

# Service hosts
api.server.host=http://localhost:8080
spectrum.server.host=http://localhost:8088
webhook.server.host=http://localhost:8080
webhook.karibu.server.host=http://localhost:8081

# Environment
environment.name=local
syncari.env.name=local
syncari.jwt.security.secret=local-dev-secret-change-me

# Proxy
proxy.enabled=false
proxy.host=
proxy.port=

# GCP placeholders
gcp.project=local-dev
gcp.cf.project=local-dev
gcp.credentials.key=
gcp.cloud.functions.url=http://localhost:8080
gcp.cfdeployer.credentials.key=
gcp.cfexecutor.credentials.key=
gcp.cfexecutor.sa.email=
gcp.ts.bgsa.credentials.key=
cf.vpc.connector=

# Pub/Sub placeholders
eventlog.topic.name=local-event-log
eventlog.subscription.name=local-event-log-sub
generic.topic.name=local-generic
generic.subscription.name=local-generic-sub
webhook.topic.name=local-webhook
webhook.subscription.name=local-webhook-sub
viper.topic.name=local-viper
viper.subscription.name=local-viper-sub
error.notification.topic.name=local-error-notification
error.notification.subscription.name=local-error-notification-sub
dfi.result.notification.topic.name=local-dfi-result
dfi.result.notification.subscription.name=local-dfi-result-sub

# Mail placeholders
mail.port=1025
mail.host=localhost
mail.username=
mail.password=
mail.from.for.error=local-errors@example.com
mail.plg.username=
mail.plg.password=
error.notification.email=local-errors@example.com

# Storage/CDN placeholders
syncari.asset.cdn=http://localhost:8088
gcs.bucket.name=local-syncari-bucket
gcs.cf.bucket.name=local-syncari-cf-bucket

# Zendesk placeholders
zendesk.shared.secret=
zendesk.sso.secret=
zendesk.support.url=
zendesk.support.url.alias=

# Connector/OAuth placeholders
clearbit.api.key=
syncari.slack.client.id=
syncari.slack.client.secret=
syncari.slack.synapse.signing.secret=
syncari.salesloft.client.id=
syncari.salesloft.client.secret=
syncari.hubspot.client.id=
syncari.hubspot.client.secret=
syncari.gsuite.client.id=
syncari.gsuite.client.secret=
syncari.salesintel.apikey=

# Datastore placeholders
syncari.datastore.host=localhost
syncari.datastore.user=
syncari.datastore.password=
syncari.datastore.cert=

# Workramp/SAML placeholders
workramp.internal.privateKey=
workramp.internal.certificate=
workramp.academies.privateKey=
workramp.academies.certificate=
workramp.sp.saml.internal.metadata=
workramp.sp.saml.academies.metadata=

# Permit.io placeholders
permit.io.api.key=
permit.io.pdp.endpoint=
```

This template is enough to satisfy many placeholder keys, but it does not guarantee that every backend feature can run fully. Features that call GCP, Pub/Sub, GCS, external OAuth providers, real mail, or real connector APIs still need real credentials and reachable services.

### 5.4 Start local MongoDB and Redis

Option 1: use local installed services.

Option 2: use Docker:

```powershell
docker run --name syncari-mongo -p 27017:27017 -d mongo:6
docker run --name syncari-redis -p 6379:6379 -d redis:7
```

If containers already exist:

```powershell
docker start syncari-mongo
docker start syncari-redis
```

### 5.5 Build backend modules

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn clean install -DskipTests
```

If you only need a specific service and its dependencies:

```powershell
mvn clean install -DskipTests -pl arcade -am
```

For Viper:

```powershell
mvn clean install -DskipTests -pl viper -am
```

### 5.6 Run Arcade locally

Arcade is the main backend target for Spectrum proxy.

Expected local port:

```text
8080
```

PowerShell:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn -pl arcade spring-boot:run "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./local-dev.properties"
```

If Spring does not pick up the additional properties file in your shell, use JVM arguments:

```powershell
mvn -pl arcade spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.config.additional-location=file:./local-dev.properties"
```

After startup, verify:

```powershell
Invoke-WebRequest http://localhost:8080/arcade/api/v1/health -UseBasicParsing
```

If that exact health endpoint is unavailable, check the module's health controller route or watch startup logs for the bound port.

### 5.7 Run Karibu locally

Karibu has `server.port=8081` in:

```text
backend-master/karibu/src/main/resources/application.properties
```

Command:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn -pl karibu spring-boot:run "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./local-dev.properties"
```

Expected local port:

```text
8081
```

### 5.8 Run Viper locally

The backend README suggests:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn clean install -DskipTests -pl viper spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083"
```

With local config:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn -pl viper spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083 --spring.config.additional-location=file:./local-dev.properties"
```

Expected local port:

```text
8083
```

## 6. Full Local Startup Order

Use this order for a full local stack:

1. Start MongoDB.
2. Start Redis.
3. Create/update `backend-master/local-dev.properties`.
4. Build backend modules.
5. Start `arcade` on `8080`.
6. Start `karibu` on `8081` if the feature needs it.
7. Start `viper` on `8083` if testing sync execution/pipeline worker flows.
8. Create/update `spectrum-master/.env`.
9. Install frontend dependencies.
10. Start Spectrum proxy on `8088`.
11. Start Spectrum frontend dev server.

Suggested terminal layout:

```text
Terminal 1: MongoDB
Terminal 2: Redis
Terminal 3: backend-master arcade
Terminal 4: backend-master karibu
Terminal 5: backend-master viper
Terminal 6: spectrum-master proxy
Terminal 7: spectrum-master frontend
```

## 7. Frontend Test Commands

From `spectrum-master`:

```powershell
npm run test
npm run test-ci
npm run test-coverage
npm run tsc
npm run eslint
```

Cypress local open mode:

```powershell
npm run cypress:localhost:open -- --env username=myownusername@example.com,password=myownpassword
```

Cypress headless local mode:

```powershell
npm run cypress:localhost:headless
```

Against an external server:

```powershell
$env:CYPRESS_BASE_URL='http://spectrum.integration.syncari.net'
npx cypress run --browser chrome --env username=myownusername@example.com,password=myownpassword
```

## 8. Backend Test Commands

Run all backend tests:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn test
```

Run tests for one module:

```powershell
mvn -pl core test
mvn -pl arcade test
mvn -pl karibu test
mvn -pl viper test
```

Build without tests:

```powershell
mvn clean install -DskipTests
```

## 9. Storybook

From `spectrum-master`:

```powershell
npm run storybook
```

Default Storybook port:

```text
6006
```

Build static Storybook:

```powershell
npm run build-storybook
```

## 10. Production-like Frontend Build

From `spectrum-master`:

```powershell
npm run build-all
```

This runs:

- proxy build
- frontend build
- deployment prep
- Docker image build

Run container:

```powershell
npm run run-container
```

The existing `run-container` script contains a macOS command:

```text
ipconfig getifaddr en0
```

On Windows, replace that with your host IP or use an explicit Docker command:

```powershell
docker run -e SPECTRUM_PORT=80 `
  -e ARCADE_TARGET=http://host.docker.internal:8080/ `
  -e NODE_ENV=production `
  -e ARCADE_LOG_LEVEL=debug `
  -e SECURE_COOKIES=false `
  -d -p 80:80 spectrum:latest
```

## 11. SyncAI Snapshot Notes

If working on the SyncAI branch/version:

Use:

```text
backend-SYN-20253
spectrum-SYN-20252
```

Backend SyncAI files added in the snapshot include:

- `arcade/src/main/java/com/syncari/api/rest/controllers/SyncAIController.java`
- `core/src/main/java/com/syncari/core/service/syncai/SyncAIService.java`
- `core/src/main/java/com/syncari/core/service/syncai/ConversationContextManager.java`
- `core/src/main/java/com/syncari/core/service/syncai/RuleConfigValidator.java`
- `core/src/main/java/com/syncari/core/ratelimit/*`
- `core/src/main/java/com/syncari/core/security/InputSanitizer.java`
- `restutils/src/main/java/com/syncari/restutils/dto/syncai/*`

Frontend SyncAI files added in the snapshot include:

- `src/pages/sync-studio/pipeline/data-quality/sync-ai/SyncAIPanel.tsx`
- `src/pages/sync-studio/pipeline/data-quality/sync-ai/SyncAIChatWindow.tsx`
- `src/pages/sync-studio/pipeline/data-quality/sync-ai/SyncAIChatMessage.tsx`
- `src/pages/sync-studio/pipeline/data-quality/sync-ai/useSyncAI.hooks.ts`
- `src/pages/sync-studio/pipeline/data-quality/sync-ai/*.scss`
- `src/assets/icons/sync-ai-dfi-btn.png`
- `src/store/ui/uiSlice.ts`

The same run pattern applies, but create the config files in the SYN folders:

```text
backend-SYN-20253/local-dev.properties
spectrum-SYN-20252/.env
```

## 12. Config File Checklist

Create these for baseline local development:

```text
spectrum-master/.env
backend-master/local-dev.properties
```

Create these for SyncAI snapshot development:

```text
spectrum-SYN-20252/.env
backend-SYN-20253/local-dev.properties
```

No committed proxy config file is required. Set proxy env vars in the terminal before running proxy commands.

## 13. Common Problems

### Package missing or module not found

From Spectrum README:

```powershell
npm run clean-build
```

Or reinstall both root and proxy dependencies:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm install
cd proxy
npm install
```

### LESS constants are stale

Run:

```powershell
npm run convert-less-to-ts
```

### Frontend cannot reach backend

Check:

- Is `arcade` running?
- Is proxy running?
- Is `ARCADE_TARGET` correct?
- Is `DISABLE_XSRF=true` for local dev?
- Is `SECURE_COOKIES=false` for local HTTP?
- Is Spectrum using the proxy port expected by package config?

Recommended proxy env:

```powershell
$env:ARCADE_TARGET='http://localhost:8080'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
$env:ARCADE_LOG_LEVEL='debug'
$env:SPECTRUM_PORT='8088'
```

### Backend fails on unresolved placeholders

That means a key like `${mongodb.uri}` or `${gcp.project}` was not provided.

Fix by adding it to:

```text
backend-master/local-dev.properties
```

and running with:

```powershell
"-Dspring-boot.run.arguments=--spring.config.additional-location=file:./local-dev.properties"
```

### Backend connects to external services unexpectedly

Some features expect GCP Pub/Sub, GCS, mail, connector OAuth providers, Permit.io, Zendesk, or datastore credentials.

For pure local development, avoid flows that trigger those integrations unless you have real dev credentials.

### Docker frontend cannot reach backend

Use this inside Docker on Windows:

```text
http://host.docker.internal:8080/
```

instead of:

```text
http://localhost:8080/
```

## 14. Safe Secret Handling

Do not commit real values for:

- MongoDB production URI
- Redis password
- GCP service account key
- JWT secret
- OAuth client secrets
- Slack signing secret
- HubSpot/Salesloft/GSuite secrets
- Zendesk secrets
- Workramp private keys/certificates
- SMTP passwords
- Permit.io API key

Keep real values in:

- local-only properties files
- shell environment variables
- IDE run configurations
- a secret manager

## 15. Quick Start Commands

### Fastest frontend mock flow

Terminal 1:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
Copy-Item .env.example .env
npm install
npm run start-mock
```

Terminal 2:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
npm install
$env:USE_MOCK='true'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
npm run start
```

Terminal 3:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run start
```

### Frontend with local Arcade

Terminal 1:

```powershell
docker start syncari-mongo
docker start syncari-redis
```

Terminal 2:

```powershell
cd C:\Users\laksh\Downloads\Syncari\backend-master
mvn -pl arcade spring-boot:run "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./local-dev.properties"
```

Terminal 3:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master\proxy
$env:ARCADE_TARGET='http://localhost:8080'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
$env:ARCADE_LOG_LEVEL='debug'
npm run dev
```

Terminal 4:

```powershell
cd C:\Users\laksh\Downloads\Syncari\spectrum-master
npm run start
```

