# Run Dance7 Locally

Use three PowerShell terminals: PostgreSQL, backend, and frontend.

## 1. Start PostgreSQL

Open PowerShell as needed for service control:

```powershell
Start-Service postgresql-x64-18
Get-Service postgresql-x64-18
```

Expected status: `Running`.

Create the database once if it does not already exist:

```powershell
$env:PGPASSWORD = 'postgres123'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d postgres -c "CREATE DATABASE studioos;"
```

If PowerShell says the database already exists, continue.

## 2. Start the Backend

Open a new PowerShell terminal:

```powershell
cd C:\Users\manojkumar.nandachar\Documents\Project-1\backend

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$env:DATABASE_USERNAME = 'postgres'
$env:DATABASE_PASSWORD = 'postgres123'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/studioos'
$env:JWT_SECRET = 'ZGFuY2U3LWRldmVsb3BtZW50LXNlY3JldC1jaGFuZ2UtbWUtaW4tcHJvZHVjdGlvbi0zMmJ5dGVz'

& 'C:\Users\manojkumar.nandachar\.maven\maven-3.9.16\bin\mvn.cmd' spring-boot:run
```

Wait for:

```text
Tomcat started on port 8080
```

Verify the backend in another terminal:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected:

```json
{
  "status": "ok",
  "service": "dance7-api"
}
```

## 3. Start the Frontend

Open a third PowerShell terminal:

```powershell
cd C:\Users\manojkumar.nandachar\Documents\Project-1
npm.cmd install
npm.cmd run dev
```

Wait for:

```text
Local: http://localhost:3000
```

## 4. Open Dance7

Open this URL in your browser:

```text
http://localhost:3000/login
```

Test login:

```text
Email: test.user@dance7.com
Password: Test@123
```

Useful pages:

- Dashboard: `http://localhost:3000/dashboard`
- Students: `http://localhost:3000/students`
- Add student: `http://localhost:3000/students/new`
- Register: `http://localhost:3000/register`

## 5. Stop the Application

In the backend and frontend terminals, press `Ctrl+C`.

To stop PostgreSQL:

```powershell
Stop-Service postgresql-x64-18
```

## Troubleshooting

### Port 8080 is busy

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Stop-Process -Id <process-id> -Force
```

### Port 3000 is busy

```powershell
Get-NetTCPConnection -LocalPort 3000 -State Listen | Select-Object OwningProcess
Stop-Process -Id <process-id> -Force
```

### Backend cannot connect to PostgreSQL

```powershell
Test-NetConnection localhost -Port 5432
```

It must return `True`. Confirm PostgreSQL is running and that the password matches `DATABASE_PASSWORD`.

### Login redirects back to login

Restart the backend with the commands above, then clear browser cookies for `localhost` and sign in again.
