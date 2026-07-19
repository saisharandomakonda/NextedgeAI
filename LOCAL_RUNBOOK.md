# next edge Local Runbook

Repo root:

```text
C:\Users\Queso\Downloads\NextedgeAI
```

## One Command Startup

From the repo root:

```powershell
cd C:\Users\Queso\Downloads\NextedgeAI
npm run synca
```

This starts Redis, builds and starts Backend Arcade, builds and starts Spectrum Proxy, then starts the frontend. Press `Ctrl+C` in the same terminal to stop the backend, proxy, and frontend processes.

Use **4 PowerShell terminals**.

## Terminal 1: Redis

```powershell
docker start redis-syncari
docker exec -it redis-syncari redis-cli ping
```

Expected:

```text
PONG
```

If Redis container does not exist:

```powershell
docker run -d -p 6379:6379 --name redis-syncari redis
```

## Terminal 2: Backend Arcade

```powershell
cd C:\Users\Queso\Downloads\NextedgeAI\backend-master
mvn clean package "-DskipTests=true" "-Dmaven.test.skip=true"
cd C:\Users\Queso\Downloads\NextedgeAI\backend-master\arcade\target
java -jar arcade-DEV-SNAPSHOT.jar --spring.config.additional-location=file:C:/Users/Queso/Downloads/NextedgeAI/backend-master/local-dev.properties
```

Wait for:

```text
Tomcat started on port(s): 8080
Started Application
```

## Terminal 3: Spectrum Proxy

```powershell
cd C:\Users\Queso\Downloads\NextedgeAI\spectrum-master\proxy
$env:ARCADE_TARGET='http://localhost:8080'
$env:DISABLE_XSRF='true'
$env:SECURE_COOKIES='false'
$env:ARCADE_LOG_LEVEL='debug'
$env:SPECTRUM_PORT='8088'
npm run build
node dist/index.js
```

Wait for:

```text
Spectrum listening on port 8088
```

## Terminal 4: Frontend

```powershell
cd C:\Users\Queso\Downloads\NextedgeAI\spectrum-master
npx ts-node scripts/lessToTs.ts
node scripts/start.js
```

Open:

```text
http://localhost:3000/login
```

## Login

```text
Email: admin@syncari.com
Password: admin
```

This works only if the local DB seed exists. If login fails, it is usually DB data, not the frontend.

If login says session expired, clear browser cookies/site data for `localhost`, then refresh.

## Database Info

Mongo is local:

```text
Mongo URI: mongodb://localhost:27017/syncari_local
App DB: syncaridb
Customer DB: syncari_admin
Instance ID: syncari_admin
```

Check user/org:

```powershell
mongosh mongodb://localhost:27017/syncari_local
use syncaridb
db.user.find({}, {email:1,status:1,currentInstanceId:1}).pretty()
db.organization.find({}, {name:1,instances:1}).pretty()
```

Make sure [backend-master/local-dev.properties](backend-master/local-dev.properties) uses:

```properties
mongodb.uri=mongodb://localhost:27017/syncari_local
mongodb.readOnlyUri=mongodb://localhost:27017/syncari_local
```

## Flow

```text
Frontend 3000 -> Proxy 8088 -> Backend 8080 -> Mongo + Redis
```

## Quick Port Check

```powershell
netstat -ano -p tcp | findstr "3000 8080 8088 6379"
```

Expected:

```text
3000 frontend
8080 backend
8088 proxy
6379 redis
```

## If A Port Is Already Used

Find the PID:

```powershell
netstat -ano -p tcp | findstr 8080
```

Stop it:

```powershell
Stop-Process -Id <PID> -Force
```

Then rerun that terminal command.
