# WebTechCBD
Full-stack web application using Spring Boot (REST API), JavaScript/jQuery/Bootstrap (frontend), and MySQL.

## Requirements
- Java 21
- MySQL 8+
- Maven (or use the included `./mvnw`)

## Setup
1. Create a MySQL database (optional, auto-created when the app starts).
2. Configure environment variables if you are not using the defaults:
   - `DB_URL` (default: `jdbc:mysql://localhost:3306/webcbd?createDatabaseIfNotExist=true`)
   - `DB_USERNAME` (default: `root`)
   - `DB_PASSWORD` (default: `root`)
   - `JWT_SECRET` (default is set in `application.yml`)
3. Run the application:
```bash
./mvnw spring-boot:run
```

The app runs on `http://localhost:8082`.

## Default Admin User
Seed data is created from `src/main/resources/data.sql`:
- Username: `admin`
- Password: `admin`

## API Documentation (Postman)
Import the collection from:
`docs/postman/WebCBD.postman_collection.json`

The collection defines a `baseUrl` variable (default `http://localhost:8082`) and uses a `token` variable for JWT-protected requests.
