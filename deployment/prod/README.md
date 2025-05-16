# Production Deployment

This directory contains the configuration files needed to deploy the YNAB Importer application in a production environment using Docker.

## Prerequisites

- Docker and Docker Compose installed on the production server
- A built Docker image of the application (`iw-incubator:latest`)

## Configuration

1. Create a `.env` file based on the `.env.template`:

```bash
cp .env.template .env
```

2. Edit the `.env` file and set secure values for all environment variables:

```
# Database credentials
POSTGRES_PASSWORD=your-secure-password

# FIO Bank integration
FIO_TOKEN=your-fio-token
FIO_ENCRYPTIONKEY=your-encryption-key
FIO_SECURITY_ENCRYPTION_KEY=your-security-key

# YNAB integration
YNAB_TOKEN=your-ynab-token
```

## Building the Application Docker Image

Before deploying, you need to build the application Docker image:

```bash
# From the project root
sbtn docker:publishLocal
```

This will create a Docker image named `iw-incubator:latest`.

## Deployment

To start the services:

```bash
docker-compose up -d
```

The application will be available at:
- HTTP: http://your-server-ip:8080/

## Monitoring

You can check the logs of the application:

```bash
docker-compose logs -f app
```

## Backups

To backup the Postgres database:

```bash
docker-compose exec postgres pg_dump -U incubator incubator > backup_$(date +%Y%m%d_%H%M%S).sql
```

## Restoring from Backup

```bash
cat backup_file.sql | docker-compose exec -T postgres psql -U incubator incubator
```

## Updating the Application

To update the application:

1. Build a new version of the Docker image
2. Deploy it with:

```bash
docker-compose down
docker-compose up -d
```