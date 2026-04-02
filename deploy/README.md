# Server deployment scripts

This directory contains a minimal server-side deployment flow for Debian:

- pull source from GitHub
- build the Spring Boot JAR
- publish it to `/opt/<app>/releases/<timestamp>/app.jar`
- run as a `systemd` service from `/opt/<app>/current/app.jar`

## Files

- `bootstrap-server.sh`: one-shot bootstrap script intended to be run via `curl`.
- `install-server.sh`: installs runtime user, deploy command, and systemd files.
- `templates/deploy.sh.template`: rendered to `/usr/local/bin/deploy-<app>.sh`.
- `templates/app.service.template`: rendered to `/etc/systemd/system/<app>.service`.
- `templates/default.env.template`: rendered to `/etc/default/<app>` (if missing).

## Quick start (from server)

1) Install prerequisites (if needed):

```bash
sudo apt-get update
sudo apt-get install -y git curl openjdk-21-jdk
```

2) Run bootstrap:

```bash
curl -fsSL https://raw.githubusercontent.com/tcreswick/mind-the-gap-app/main/deploy/bootstrap-server.sh \
  | bash -s -- --repo-url https://github.com/tcreswick/mind-the-gap-app.git --branch main
```

The bootstrap script will:

- clone/update into `/srv/mind-the-gap-app`
- install service/deploy tooling
- run the first deployment

## Ongoing deploys

After bootstrap, deploy updates with:

```bash
sudo /usr/local/bin/deploy-mind-the-gap-app.sh
```

If deploy script templates changed in this repo, re-run install once so `/usr/local/bin/deploy-<app>.sh` is regenerated:

```bash
sudo bash deploy/install-server.sh --repo-url https://github.com/tcreswick/mind-the-gap-app.git --branch main --app-name mind-the-gap-app --src-dir /srv/mind-the-gap-app
```

## Service operations

```bash
sudo systemctl status mind-the-gap-app.service
sudo systemctl restart mind-the-gap-app.service
journalctl -u mind-the-gap-app.service -f
```

## Nginx reverse proxy

An example site config is provided at:

- `deploy/nginx/mind-the-gap.uk.conf`

To use it on Debian:

```bash
sudo cp deploy/nginx/mind-the-gap.uk.conf /etc/nginx/sites-available/mind-the-gap.uk.conf
sudo ln -s /etc/nginx/sites-available/mind-the-gap.uk.conf /etc/nginx/sites-enabled/mind-the-gap.uk.conf
sudo nginx -t
sudo systemctl reload nginx
```

## Configuration

- Runtime env vars: `/etc/default/mind-the-gap-app`
- Optional Spring config dir: `/etc/mind-the-gap-app/`
- Runtime data directory defaults to `/var/lib/mind-the-gap-app/data` via systemd `APP_DATA_DIRECTORY`.

If you override the data directory, make sure it is writable by the app user and included in
the service `ReadWritePaths` allow-list.

## Notes

- This project targets Java 21 (`pom.xml`), so your Debian server must have a Java 21 JDK available for build/runtime.
- If your Git host uses SSH, ensure the server user can access the repo key (or use an HTTPS URL with token auth).
- Maven local repo is stored under `/var/lib/<app>/.m2/repository` to avoid `/nonexistent/.m2` errors when using a no-login system user.
