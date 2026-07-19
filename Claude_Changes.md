# Claude_Changes.md — Credential Sanitization Log

This document records every change Claude made to sanitize the Syncari codebase before pushing to GitHub. It contains:
- **All original hardcoded values** (so you have a reference)
- **What they were replaced with** (env var name + fallback)
- **Where the actual values are stored** (`.env` file at repo root)
- **How to run the code locally** with the real values

---

## TL;DR — How to run locally after this sanitization

The actual credentials are stored in `.env` at the repo root (which is gitignored — it will NEVER be pushed to GitHub). To run the code locally with real credentials, load `.env` into your shell environment before starting the app:

**Linux / macOS / Git Bash on Windows:**
```bash
set -a; source .env; set +a
# Then run your usual mvn / java / spring-boot:run command
```

**PowerShell:**
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=#]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
# Then run mvn / java / spring-boot
```

**IntelliJ IDEA:** Edit Run Configuration → "Environment variables" → click 📁 icon → load from `.env` file (or paste contents).

The Java code uses `System.getenv().getOrDefault("VAR_NAME", "REPLACE_ME")`, and the `application.properties` files use Spring's `${VAR_NAME}` syntax — both pick up values from the OS environment.

---

## Sanitization approach

For every hardcoded secret found, I:
1. Replaced the literal string in the source file with an environment-variable lookup.
2. Recorded the original value in this document.
3. Stored the original value in `.env` (gitignored) so local runs still work.

Java pattern used:
```java
System.getenv().getOrDefault("ENV_VAR_NAME", "REPLACE_ME")
```

Properties pattern used (Spring Boot):
```properties
some.property=${ENV_VAR_NAME}
```

---

## 1. Properties files (already partially sanitized in working tree by user)

The user had already replaced many values with `${ENV_VAR}` references in their working tree before Claude got involved. Those are listed here for completeness.

### 1a. `backend-SYN-20253/arcade/src/main/resources/application.properties`

| Property | Original value | Replacement | Env var |
|---|---|---|---|
| `llm.openai.secret.key` | `openai_apikey` | `${LLM_OPENAI_SECRET_KEY}` | `LLM_OPENAI_SECRET_KEY` |
| `llm.claude.secret.key` | `claude_apikey` | `${LLM_CLAUDE_SECRET_KEY}` | `LLM_CLAUDE_SECRET_KEY` |
| `llm.gemini.secret.key` | `gemini_apikey` | `${LLM_GEMINI_SECRET_KEY}` | `LLM_GEMINI_SECRET_KEY` |

(These were placeholders, not real keys — but kept for consistency.)

### 1b. `backend-SYN-20253/**/test_application.properties` (and `backend-master/**/test_application.properties`)

| Property | Original value | Env var |
|---|---|---|
| `salesforce.password` | `@@DevSyncari68945368900243` | `SALESFORCE_PASSWORD` |
| `salesforce.token` | `ZmETaJbGzP4DYyl7QWqH9qsM` | `SALESFORCE_TOKEN` |
| `salesforce.nopermission.password` | `Syncari123` | `SALESFORCE_NOPERMISSION_PASSWORD` |
| `salesforce.nopermission.token` | `L0yfbFtKNAweEgMm8iPFsGpk` | `SALESFORCE_NOPERMISSION_TOKEN` |
| `spring.cloud.gcp.credentials.encoded-key` | (long base64 GCP service-account key for `developer@hopeful-sunset-238922.iam.gserviceaccount.com`) | `SPRING_CLOUD_GCP_CREDENTIALS_ENCODED_KEY` |
| `spring.cloud.gcp.cfdeployer.credentials.encoded-key` | (long base64 GCP service-account key for `cfmanager@hopeful-sunset-238922.iam.gserviceaccount.com`) | `SPRING_CLOUD_GCP_CFDEPLOYER_CREDENTIALS_ENCODED_KEY` |
| `spring.cloud.gcp.cfexecutor.credentials.encoded-key` | (same base64 GCP service-account key as cfdeployer) | `SPRING_CLOUD_GCP_CFEXECUTOR_CREDENTIALS_ENCODED_KEY` |
| `mail.sender.password` | `@syncari.mail.sender.password@` | `MAIL_SENDER_PASSWORD` |
| `syncari.slack.synapse.signing.secret` | `revoked_slack_token` | `SYNCARI_SLACK_SYNAPSE_SIGNING_SECRET` |
| `syncari.datastore.password` | `!SyncariDemo12#` | `SYNCARI_DATASTORE_PASSWORD` |
| `zendesk.shared.secret` | `32223475CACABA97743FB4909D6B2327FCE4B799798AAF6948D3AC48243E11F4` | `ZENDESK_SHARED_SECRET` |
| `zendesk.sso.secret` | `iaHOdAA11fI2qREmkCiLQu0Su4MEsasrbNYOHWx11Ix0j8zz` | `ZENDESK_SSO_SECRET` |
| `syncari.salesloft.client.secret` | (was already a `${...}` ref) | `SYNCARI_SALESLOFT_CLIENT_SECRET` |
| `workramp.internal.privateKey` | (was already a `${...}` ref) | `WORKRAMP_INTERNAL_PRIVATEKEY` |

---

## 2. Properties files (Claude sanitized in this session)

### 2a. `clearbit.api.key` — appears in **all** test_application.properties files

| File | Property | Original value | Replacement |
|---|---|---|---|
| `backend-master/visions/src/test/resources/test_application.properties:52` | `clearbit.api.key` | `sk_b67f11262c37c61d13b30181eaf10f36` | `${CLEARBIT_API_KEY}` |
| `backend-SYN-20253/visions/src/test/resources/test_application.properties:52` | same | same | same |
| `backend-master/core/src/test/resources/test_application.properties:51` | same | same | same |
| `backend-SYN-20253/core/src/test/resources/test_application.properties:51` | same | same | same |
| `backend-master/arcade/src/test/resources/test_application.properties:47` | same | same | same |
| `backend-SYN-20253/arcade/src/test/resources/test_application.properties:47` | same | same | same |
| `backend-master/karibu/src/test/resources/test_application.properties:47` | same | same | same |
| `backend-SYN-20253/karibu/src/test/resources/test_application.properties:47` | same | same | same |
| `backend-master/connector/src/test/resources/test_application.properties:46` | same | same | same |
| `backend-SYN-20253/connector/src/test/resources/test_application.properties:46` | same | same | same |
| `backend-master/viper/src/test/resources/test_application.properties:43` | same | same | same |
| `backend-SYN-20253/viper/src/test/resources/test_application.properties:43` | same | same | same |

**Env var:** `CLEARBIT_API_KEY=revoked_clearbit_key`

---

## 3. Java test files — `SynapseTestUtil.java`

Both copies (in `backend-master` and `backend-SYN-20253`) under `karibu/src/test/java/com/syncari/karibu/rest/util/SynapseTestUtil.java` had the same hardcoded credentials. Both files were sanitized identically.

### 3a. `getSalesforceSynapseRequestString()` (lines 30-32)

| Field | Original value | Env var |
|---|---|---|
| `password` | `@DevSyncari94403` | `TEST_SF_PASSWORD` |
| `token` | `DL74JJUeLBXIcistTW1UYjNO` | `TEST_SF_TOKEN` |

### 3b. `getSalesforceOauthSynapseRequestString()` (lines 49-52)

| Field | Original value | Env var |
|---|---|---|
| `clientId` | `3MVG9ahGHqp.k2_x8Wb3dBGQzLGEvAdJApCvw1uffJPrbT9NU9zr7bsPk0UT3VBXLl_4RuP8RzbDLD7xbRhFx` | `TEST_SF_OAUTH_CLIENT_ID` |
| `clientSecret` | `BA3B77C6C136D1FA8338B462CEA08B84893336DB9843F100D0B60B74C8A28562` | `TEST_SF_OAUTH_CLIENT_SECRET` |
| `accessToken` | `00D1b00000093t7!ARMAQBxNWY8IfUiBqhvzx02BpQh.8t6z482bDUXnNMLTlYupPSiu0xX9CXCZxjz_rWg336t6O8Lzy8eyRG3kApihN9.iTgaq` | `TEST_SF_OAUTH_ACCESS_TOKEN` |
| `refreshToken` | `5Aep861v6WkKJfmiwaRpHCMfc5M_.RnUB8HGPmEg3vXMqg1dPjV3HMMoTQyUPINJ8DfnTCMt.ijptXVoPI9IJV6` | `TEST_SF_OAUTH_REFRESH_TOKEN` |

### 3c. `getImpartnerSynapseRequestString()` (line 106)

| Field | Original value | Env var |
|---|---|---|
| `password` | `!SyncariRocks123` | `TEST_IMPARTNER_PASSWORD` |

### 3d. `getMarketoSynapseRequestString()` (lines 123-124)

| Field | Original value | Env var |
|---|---|---|
| `clientId` | `f52abd13-2e4f-4cdb-ba81-2a974a38d54f` | `TEST_MARKETO_CLIENT_ID` |
| `clientSecret` | `TC7Q8yMZl6hpMNYREgOEKQBsblGoXR7L` | `TEST_MARKETO_CLIENT_SECRET` |

### 3e. `getGoogleSheetSynapseRequestString()` (lines 142-144)

| Field | Original value | Env var |
|---|---|---|
| `clientId` | `220542989993-gp951g1ld82djgeub7qm9kub32lhuc49.apps.googleusercontent.com` | `TEST_GSHEETS_CLIENT_ID` |
| `clientSecret` | `sHZ9qZkhzw9vU8VyjPrbmva0` | `TEST_GSHEETS_CLIENT_SECRET` |
| `refreshToken` | `1//06wGMWIgX--UZCgYIARAAGAYSNwF-L9IrTP5Qa0keEwUOunQL6rfxI0vXUBlFWWPJ4HFaGGdtKrKfO9yTQoMb6nt3ADKFjZ23N20` | `TEST_GSHEETS_REFRESH_TOKEN` |

### 3f. `getBadSalesforceSynapseRequestString()` (lines 190, 194)

| Field | Original value | Env var |
|---|---|---|
| `password` | `U3luY2FyaVJvY2tzNjU0MzIx` (base64 of `SyncariRocks654321`) | `TEST_BAD_SF_PASSWORD` |
| `token` | `9RHWTo2qyKEQ57a1jqbHTy3VU` | `TEST_BAD_SF_TOKEN` |

### 3g. `getActiveMarketoConnector()` (lines 267, 270)

| Field | Original value | Env var |
|---|---|---|
| `clientId` | `f52abd13-2e4f-4cdb-ba81-2a974a38d54f` | `TEST_MARKETO_CLIENT_ID` (reused) |
| `consumerSecret` | `730324107956186355EE44339988002311CC01734284` | `TEST_MARKETO_CONSUMER_SECRET` |

(`clientSecret` on line 268 was already using `System.getenv()` before this session.)

---

## 4. Java test files — `GoogleSheetServiceTest.java`

Both copies (in `backend-master/connector/src/test/java/com/syncari/connector/service/` and `backend-SYN-20253/connector/src/test/java/com/syncari/connector/service/`) had:

| Field | Original value | Env var |
|---|---|---|
| `SECRET` | `sHZ9qZkhzw9vU8VyjPrbmva0` | `TEST_GSHEETS_CLIENT_SECRET` |
| `CLIENTID` | `220542989993-gp951g1ld82djgeub7qm9kub32lhuc49.apps.googleusercontent.com` | `TEST_GSHEETS_CLIENT_ID` |
| `syncariFolderId` | `1M7bRO-pZNUgrZUTcCLIKp0Jgcf1qnPlG` | `TEST_GSHEETS_FOLDER_ID` |
| `refreshToken` | `1//06wGMWIgX--UZCgYIARAAGAYSNwF-L9IrTP5Qa0keEwUOunQL6rfxI0vXUBlFWWPJ4HFaGGdtKrKfO9yTQoMb6nt3ADKFjZ23N20` | `TEST_GSHEETS_REFRESH_TOKEN` |

---

## 5. Java test files — `SalesIntelServiceTest.java`

Both copies (in `backend-master/core/src/test/java/com/syncari/core/enrich/salesintel/` and `backend-SYN-20253/core/src/test/java/com/syncari/core/enrich/salesintel/`) had:

| Field | Original value | Env var |
|---|---|---|
| `apiKey` | `YjFhNzM5MTMtNjg5ZS00ZGQ4LWEzZmYtYTBkYzAzNzc0ZGYw` (base64 of a UUID) | `TEST_SALESINTEL_API_KEY` |

---

## 6. Items intentionally **NOT** sanitized

These were inspected but left as-is because they are not secrets:

- `saml.x509.key=-----BEGIN CERTIFICATE-----...` — public X.509 certificate (public key material)
- `syncari.datastore.cert=LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t...` — public CA certificate (base64-encoded)
- `workramp.internal.certificate=MIICwjCCAaoCCQCtdYM6u6ZyHjANBgkqhkiG...` — public certificate
- Sandbox URLs (`https://syncariinc--unittests.sandbox.my.salesforce.com`)
- Test usernames/emails (`mary+pbo@syncari.com.unittests`, `eng@syncari.com.dev`)
- Hardcoded `hubspotClientId`, `hubspotSecretId`, etc. — these are placeholder strings, not real credentials
- `MKTOWS_183-LYQ-451_1` — Marketo munchkin/consumer key prefix, not secret
- `connector.put(...)` calls in `getUpdateMarketoSynapseRequestString` (`newClientId`, `newClietSecret`) — placeholder strings

---

## 7. Git history rewrite

Because the **original initial commit (`78a88db`)** contained all the hardcoded credentials in its diff, simply adding sanitized commits on top would still leak the secrets via `git log -p`. To prevent this:

1. `git update-ref -d HEAD` — removed the only commit (root commit, no parent), leaving all files staged.
2. Re-staged the working-tree changes (the sanitized versions).
3. Created a single new clean commit (`ba74ede`, then later replaced when more secrets were caught by GitHub's push protection).
4. After GitHub's secret scanning blocked the first attempt, repeated steps 1-3 with all the additional sanitizations from this session.

The final commit pushed to GitHub contains **zero hardcoded credentials** — only env-var references — even in `git log -p`.

---

## 8. Additional sanitizations (round 2)

After the first push attempt, GitHub's secret-scanning push protection rejected it and revealed more files. The following were also sanitized:

### 8a. Properties files

| File | Property | Original value | Replacement |
|---|---|---|---|
| `backend-master/connector/src/test/resources/test_application.properties:76` | `marketo.test.encryption.key` | `730324107956186355EE44339988002311CC01734284` | `${TEST_MARKETO_CONSUMER_SECRET}` |
| `backend-SYN-20253/connector/src/test/resources/test_application.properties:76` | same | same | same |
| `backend-master/visions/src/test/resources/test_application.properties:73` | `marketo.encryptionKey` | `730324107956186355EE44339988002311CC01734284` | `${TEST_MARKETO_CONSUMER_SECRET}` |
| `backend-SYN-20253/visions/src/test/resources/test_application.properties:73` | same | same | same |
| `backend-master/core/src/test/resources/test_application.properties:74` | `marketo.encryptionKey` | same | same |
| `backend-SYN-20253/core/src/test/resources/test_application.properties:74` | same | same | same |
| `backend-master/core/src/test/resources/test_application.properties:111` | `syncari.salesintel.apikey` | `YjFhNzM5MTMtNjg5ZS00ZGQ4LWEzZmYtYTBkYzAzNzc0ZGYw` | `${TEST_SALESINTEL_API_KEY}` |
| `backend-SYN-20253/core/src/test/resources/test_application.properties:111` | same | same | same |
| `backend-master/core/src/test/resources/test_application.properties:115` | `httpaction.simpleoauth.clientId` | `f52abd13-2e4f-4cdb-ba81-2a974a38d54f` | `${TEST_MARKETO_CLIENT_ID}` |
| `backend-SYN-20253/core/src/test/resources/test_application.properties:115` | same | same | same |

### 8b. `SynapseTestUtil.java` — line 146 (initially missed)

| Field | Original value | Env var |
|---|---|---|
| `folderId` | `1M7bRO-pZNUgrZUTcCLIKp0Jgcf1qnPlG` | `TEST_GSHEETS_FOLDER_ID` |

### 8c. `SchemaServiceTest.java` (both backend-master and backend-SYN-20253)

Path: `core/src/test/java/com/syncari/core/service/SchemaServiceTest.java`

| Line | Field | Original value | Env var |
|---|---|---|---|
| 1449 | `clientId` | `f52abd13-2e4f-4cdb-ba81-2a974a38d54f` | `TEST_MARKETO_CLIENT_ID` |
| 1452 | `consumerSecret` | `730324107956186355EE44339988002311CC01734284` | `TEST_MARKETO_CONSUMER_SECRET` |

### 8d. `SalesforceServiceTest.java` (both copies — value was in a comment)

Path: `connector/src/test/java/com/syncari/connector/service/SalesforceServiceTest.java`

| Line | Field | Original value | Env var |
|---|---|---|---|
| 1907 | `clientId` (commented out) | `3MVG9ahGHqp.k2_x8Wb3dBGQzLGEvAdJApCvw1uffJPrbT9NU9zr7bsPk0UT3VBXLl_4RuP8RzbDLD7xbRhFx` | `TEST_SF_OAUTH_CLIENT_ID` |

---

## 9. The `.env` files

After consolidation, the env files live next to the actual code:

- **`backend-master/.env`** (gitignored) — real values for all backend Java env vars, including the new `TEST_*` and `CLEARBIT_API_KEY` vars added in this session.
- **`backend-master/.env.example`** (committed) — same env-var names with `REPLACE_ME` placeholder values, so any developer cloning the repo knows what to set.
- **`spectrum-master/.env`** (gitignored) — frontend env vars (Zendesk, Botco, Cypress).
- **`spectrum-master/.env.example`** (committed) — frontend env-var names with `REPLACE_ME` placeholders.

Originally these `.env` files lived in standalone `backend/` and `frontend/` directories at the repo root, but those directories were removed and the files moved into `backend-master/` and `spectrum-master/` respectively (the actual Java/React project roots). Empty `backend-SYN-20253/` and `spectrum-SYN-20252/` Jira-branch variants were also deleted from the working tree (still recoverable from commit `adda4c1`).

The root `.gitignore` was simplified to:
```
.env
*.env
```
which covers `.env` files anywhere in the tree. `spectrum-master/.gitignore` already had `.env` covered locally.

To run the code locally, use the loader snippet from the TL;DR section at the top of this document — pointing at `backend-master/.env` for Java and either auto-loaded by `react-scripts` from `spectrum-master/.env` or loaded manually for the frontend.
