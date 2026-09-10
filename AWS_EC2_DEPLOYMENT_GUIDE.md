# Dance7 AWS EC2 Deployment Guide (single Ubuntu instance)

Topology: one EC2 Ubuntu instance runs Nginx (80/443) → Next.js (`127.0.0.1:3000`)
and Spring Boot (`127.0.0.1:8080`). PostgreSQL stays on Neon. Public surface:

- Frontend: `https://app.dance7.com` → Next.js
- Backend: `https://app.dance7.com/api/*` → Spring Boot (the `/api` prefix is
  preserved; controllers are mapped under `/api`, so no URI rewrite is needed)

No application redesign. No code changes required — configuration only.

## 1. EC2 sizing

- Minimum: `t3.small` (2 vCPU, 2 GiB) — JVM (`-Xms256m -Xmx1g`) + Node + Nginx.
- Recommended: `t3.medium` (2 vCPU, 4 GiB) for headroom during builds.
- Storage: 30 GB gp3 (Maven repo + `node_modules` + logs). Use an Elastic IP and
  point DNS `A app.dance7.com → <elastic-ip>`.
- OS: Ubuntu 24.04 LTS. Install: Temurin JDK 21, Node 20 LTS, Nginx, Certbot.

```bash
sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx \
  unzip curl git ufw
# Temurin 21
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-21-jdk maven
# Node 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt install -y nodejs
```

## 2. Ports, security groups, firewall

Security group (inbound): `22/tcp` (your IP only), `80/tcp` (0.0.0.0/0),
`443/tcp` (0.0.0.0/0). Nothing else public — **not** 3000/8080.

```bash
sudo ufw default deny incoming && sudo ufw default allow outgoing
sudo ufw allow from <your-ip> to any port 22
sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable
```

## 3. Production build commands

Backend (from repo `backend/`):
```bash
mvn -DskipTests package   # → target/dance7-api-0.0.1-SNAPSHOT.jar
```

Frontend (from repo root):
```bash
NEXT_PUBLIC_API_URL=https://app.dance7.com/api npm ci
NEXT_PUBLIC_API_URL=https://app.dance7.com/api npm run build
```
(`NEXT_PUBLIC_*` is baked at build time — rebuild after any URL change.)

## 4. Environment (`/opt/dance7/backend.env`, root-only `chmod 600`)

```bash
DATABASE_URL=jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/<db>?sslmode=require
DATABASE_USERNAME=<neon-user>
DATABASE_PASSWORD=<neon-password>
JWT_SECRET=<openssl rand -base64 48>   # never use the dev default
CORS_ALLOWED_ORIGINS=https://app.dance7.com
COOKIE_SECURE=true
```

## 5. Backend startup (systemd)

`/etc/systemd/system/dance7-api.service`:
```ini
[Unit]
Description=Dance7 Spring Boot API
After=network.target

[Service]
User=www-data
WorkingDirectory=/opt/dance7/api
EnvironmentFile=/opt/dance7/backend.env
ExecStart=/usr/bin/java -Xms256m -Xmx1g -jar /opt/dance7/api/dance7-api-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## 6. Frontend startup (systemd)

`/etc/systemd/system/dance7-web.service`:
```ini
[Unit]
Description=Dance7 Next.js frontend
After=network.target dance7-api.service

[Service]
User=www-data
WorkingDirectory=/opt/dance7/web
Environment=NODE_ENV=production
Environment=PORT=3000
ExecStart=/usr/bin/npm start -- -p 3000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable: `sudo systemctl daemon-reload && sudo systemctl enable --now dance7-api dance7-web`.

## 7. Nginx reverse proxy (`/etc/nginx/sites-available/dance7`)

```nginx
upstream dance7_web { server 127.0.0.1:3000; }
upstream dance7_api { server 127.0.0.1:8080; }

server {
    listen 80;
    server_name app.dance7.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name app.dance7.com;

    ssl_certificate     /etc/letsencrypt/live/app.dance7.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.dance7.com/privkey.pem;

    client_max_body_size 10m;

    location /api/ {
        proxy_pass http://dance7_api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://dance7_web;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable: `sudo ln -s /etc/nginx/sites-available/dance7 /etc/nginx/sites-enabled/ && sudo nginx -t && sudo systemctl reload nginx`.
Same-host deployment keeps auth cookies first-party; `COOKIE_SECURE=true` (HTTPS) with `SameSite=None` works, and CORS origin is exactly `https://app.dance7.com`.

## 8. SSL (Let's Encrypt)

```bash
sudo certbot --nginx -d app.dance7.com   # auto-renews via systemd timer
sudo certbot renew --dry-run
```

## 9. Deployment script (`/opt/dance7/deploy.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail
cd /opt/dance7/repo && git pull origin main
cd backend && mvn -DskipTests package
cp target/dance7-api-0.0.1-SNAPSHOT.jar /opt/dance7/api/
cd /opt/dance7/repo
export NEXT_PUBLIC_API_URL=https://app.dance7.com/api
npm ci && npm run build
rm -rf /opt/dance7/web/.next && cp -r .next /opt/dance7/web/  # or rsync repo
sudo systemctl restart dance7-api dance7-web
curl -sf http://127.0.0.1:8080/actuator/health
curl -sf https://app.dance7.com/api/actuator/health
```

Verify: `https://app.dance7.com` loads, OWNER login works, `/api` calls return
`200` (no CORS errors — same host), `/actuator/health` is `UP`.

## 10. Rollback

- App: `git checkout <last-good-sha>` (or `git revert`), rerun `deploy.sh`.
- Data: additive migrations only (`ddl-auto: update`); Neon point-in-time restore if needed.
- TLS: `sudo certbot rollback` on Nginx config mistakes.
