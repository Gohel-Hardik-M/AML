# AML

Anti-money-laundering transaction monitoring application with an Angular frontend and a Spring Boot backend.

## Project layout

- `backend/` - Spring Boot API, Flyway migrations, and backend tests
- `frontend/` - Angular application
- `backend/AML.postman_collection1.0` - Postman API collection

## Prerequisites

- Java 17 or newer
- Node.js and npm
- PostgreSQL running locally

## First-time setup

1. Create the `aml_master` PostgreSQL database.
2. Create `backend/src/main/resources/application-dev.properties` locally. Do not commit it; it contains database, mail, JWT, and encryption settings.
3. Install frontend dependencies:

   ```powershell
   Set-Location frontend
   npm ci
   ```

## Run the application

Start the backend in one terminal:

```powershell
Set-Location backend
./mvnw spring-boot:run
```

Start the frontend in another terminal:

```powershell
Set-Location frontend
npm start
```

Open `http://localhost:4200`. The Angular development proxy forwards `/api` requests to the backend at `http://localhost:8080`.

On Windows PowerShell, use `./mvnw.cmd spring-boot:run` if `./mvnw` is not recognized.

## Validation

```powershell
Set-Location backend
./mvnw test

Set-Location ../frontend
npm run build
```

The production frontend build currently reports a bundle-size warning from the configured Angular budget, but it completes successfully.