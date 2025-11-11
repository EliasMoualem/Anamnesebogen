# Anamnesebogen - Quick Start Guide

**GDPR-Compliant Digital Anamnesis System for Dental Practices**

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Environment Configuration](#environment-configuration)
4. [Running the Application](#running-the-application)
5. [Testing the System](#testing-the-system)
6. [Troubleshooting](#troubleshooting)
7. [Next Steps](#next-steps)

---

## Prerequisites

### Required Software
- **Java 17** or higher (Currently using: Java 17.0.2)
- **PostgreSQL 18** (Already installed at: `C:\Program Files\PostgreSQL\18`)
- **Maven** (Included via Maven Wrapper: `./mvnw`)
- **Git** (For version control)

### Verify Installation
```bash
# Check Java version
java -version
# Should show: java version "17.0.2" or higher

# Check PostgreSQL version
"C:\Program Files\PostgreSQL\18\bin\psql.exe" --version
# Should show: psql (PostgreSQL) 18.0

# Check Maven
./mvnw --version
# Should show: Apache Maven 3.x.x
```

---

## Database Setup

### ✅ Step 1: Create Database (COMPLETED)
The database `anamnesebogen` has been created with:
- Database name: `anamnesebogen`
- User: `anamnesebogen_user`
- Password: `changeme` (change this in production!)
- Port: `5432` (default PostgreSQL port)

### Verify Database Creation
```bash
# Connect to PostgreSQL
cd "C:\Program Files\PostgreSQL\18\bin"
psql -U postgres -d anamnesebogen

# Inside psql, verify tables will be created by Flyway:
\dt
# Should show: No relations found (Flyway will create tables on first run)

# Exit psql
\q
```

---

## Environment Configuration

### Step 2: Set Database Password

**Option A: Session-Based (For Current Terminal Only)**
```powershell
# PowerShell (Recommended for testing)
$env:DB_PASSWORD = "changeme"

# Verify it's set
echo $env:DB_PASSWORD
```

**Option B: Permanent (System-Wide)**
```powershell
# PowerShell as Administrator
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'changeme', 'User')

# OR using CMD
setx DB_PASSWORD "changeme"

# Note: Close and reopen terminal after setting
```

**Option C: Application Properties (Not Recommended for Production)**
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.password=changeme
```

### Step 3: Verify Configuration

Check your database configuration in `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/anamnesebogen
spring.datasource.username=anamnesebogen_user
spring.datasource.password=${DB_PASSWORD:changeme}

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

---

## Running the Application

### Step 4: Build the Project

```bash
# Navigate to project directory
cd C:\Users\Elias\IdeaProjects\Anamnesebogen

# Clean and compile
./mvnw clean compile

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Compiling 20 source files
```

### Step 5: Run Flyway Migrations (First Time Only)

Flyway will automatically run when you start the application, but you can test it separately:

```bash
# Test Flyway migration
./mvnw flyway:migrate

# Expected output:
# [INFO] Successfully applied 1 migration to schema "public"
# [INFO] Schema version: 1
```

### Step 6: Start the Application

```bash
# Run the Spring Boot application
./mvnw spring-boot:run

# Expected output:
# Tomcat started on port(s): 8080 (http)
# Started AnamnesebogenApplication in X.XXX seconds
```

**The application is now running at:** `http://localhost:8080`

---

## Testing the System

### Test 1: Health Check

Open your browser or use curl:
```bash
# Browser
http://localhost:8080/actuator/health

# OR using curl
curl http://localhost:8080/actuator/health

# Expected response:
{"status":"UP"}
```

### Test 2: Access the Application

Open your browser:
```
http://localhost:8080/
```

**Expected behavior:**
1. Should show the language selection page
2. Select a language (DE, EN, AR, or RU)
3. Fill out the anamnesis form
4. Submit the form
5. PDF should be generated and downloaded

### Test 3: Verify Database Tables

Check if Flyway created all tables:
```bash
# Connect to database
cd "C:\Program Files\PostgreSQL\18\bin"
psql -U anamnesebogen_user -d anamnesebogen

# List all tables
\dt

# Expected output:
#  Schema |       Name        | Type  |       Owner
# --------+-------------------+-------+-------------------
#  public | audit_logs        | table | anamnesebogen_user
#  public | consents          | table | anamnesebogen_user
#  public | flyway_schema_history | table | anamnesebogen_user
#  public | form_submissions  | table | anamnesebogen_user
#  public | guardians         | table | anamnesebogen_user
#  public | patients          | table | anamnesebogen_user
#  public | signatures        | table | anamnesebogen_user

# Check a table structure
\d patients

# Count records (should be 0 initially)
SELECT COUNT(*) FROM patients;

# Exit
\q
```

### Test 4: Submit Test Form

1. **Navigate to:** `http://localhost:8080/`
2. **Select language:** German (de)
3. **Fill out the form with test data:**
   - First Name: `Max`
   - Last Name: `Mustermann`
   - Birth Date: `1990-01-01`
   - Email: `max@example.com`
   - Fill other fields as needed
4. **Sign the form** using the signature pad
5. **Submit the form**
6. **Download the generated PDF**

### Test 5: Verify Data in Database

After submitting a form:
```sql
-- Connect to database
psql -U anamnesebogen_user -d anamnesebogen

-- Check if patient was created
SELECT id, first_name, last_name, email_address, created_at
FROM patients;

-- Check form submission
SELECT id, submission_date, status, form_language
FROM form_submissions;

-- Check signature
SELECT id, signer_name, signed_at, signature_type, document_type
FROM signatures;

-- Check audit logs
SELECT timestamp, action_type, entity_type, description
FROM audit_logs
ORDER BY timestamp DESC
LIMIT 10;

-- Exit
\q
```

---

## Troubleshooting

### Issue 1: Connection Refused

**Error:** `Connection refused: connect`

**Solutions:**
1. Check if PostgreSQL is running:
   ```bash
   # Windows Services
   # Press Win+R, type: services.msc
   # Look for: postgresql-x64-18
   # Status should be: Running
   ```

2. Verify connection details:
   ```bash
   psql -U anamnesebogen_user -d anamnesebogen -h localhost -p 5432
   ```

### Issue 2: Authentication Failed

**Error:** `FATAL: password authentication failed`

**Solutions:**
1. Check environment variable:
   ```powershell
   echo $env:DB_PASSWORD
   ```

2. Verify password in database:
   ```bash
   psql -U postgres -d anamnesebogen
   # Then run: \du
   # Should show anamnesebogen_user role
   ```

3. Reset password:
   ```sql
   psql -U postgres -d anamnesebogen
   ALTER USER anamnesebogen_user WITH PASSWORD 'changeme';
   \q
   ```

### Issue 3: Flyway Migration Failed

**Error:** `FlywayException: Validate failed`

**Solutions:**
1. Check Flyway history:
   ```sql
   psql -U anamnesebogen_user -d anamnesebogen
   SELECT * FROM flyway_schema_history;
   ```

2. Clean and re-run (⚠️ DESTROYS ALL DATA):
   ```bash
   ./mvnw flyway:clean
   ./mvnw flyway:migrate
   ```

3. Baseline existing database:
   ```bash
   ./mvnw flyway:baseline
   ./mvnw flyway:migrate
   ```

### Issue 4: Port 8080 Already in Use

**Error:** `Port 8080 was already in use`

**Solutions:**
1. Change port in `application.properties`:
   ```properties
   server.port=8081
   ```

2. Or kill the process using port 8080:
   ```powershell
   # Find process using port 8080
   netstat -ano | findstr :8080

   # Kill the process (replace PID with actual process ID)
   taskkill /PID <PID> /F
   ```

### Issue 5: Java Version Mismatch

**Error:** `release version 21 not supported`

**Solution:** Already fixed! POM now uses Java 17.

Verify:
```bash
java -version
# Should show: java version "17.x.x"
```

---

## Application Endpoints

### Public Endpoints
- **Home/Language Selection:** `http://localhost:8080/`
- **Anamnesis Form:** `http://localhost:8080/anamnesebogen?lang=de`
- **Submit Form:** `POST http://localhost:8080/submit-anamnesebogen`
- **Generate PDF:** `http://localhost:8080/pdf?id={uuid}`

### Actuator Endpoints (Monitoring)
- **Health Check:** `http://localhost:8080/actuator/health`
- **Metrics:** `http://localhost:8080/actuator/metrics`
- **All Endpoints:** `http://localhost:8080/actuator`

---

## Database Schema Overview

### Tables Created by Flyway

| Table | Purpose | GDPR Compliance |
|-------|---------|-----------------|
| `patients` | Patient personal and medical data | ✅ Soft delete, retention policy |
| `guardians` | Legal guardian information | ✅ Linked to patient lifecycle |
| `form_submissions` | Form submission audit trail | ✅ Tracks all submissions |
| `signatures` | Electronic signatures | ✅ Tamper detection, eIDAS compliance |
| `consents` | GDPR consent records | ✅ Article 7 compliance |
| `audit_logs` | Comprehensive audit trail | ✅ Article 30 compliance |

### Key GDPR Features

1. **Right to Erasure (Article 17):**
   - Soft delete with `deleted_at` column
   - `deletion_requested` flag
   - Automated retention policy

2. **Consent Management (Article 7):**
   - Consent versioning
   - Withdrawal tracking
   - Proof of consent (signature, checkbox, etc.)

3. **Records of Processing (Article 30):**
   - Comprehensive audit logs
   - Who accessed what, when, and why
   - Security event tracking

4. **Data Minimization:**
   - Only essential fields collected
   - Conditional form logic (Phase 6)

---

## Next Steps

### Phase 1 Complete ✅
- [x] Database persistence with PostgreSQL
- [x] GDPR-compliant data models
- [x] Flyway migrations
- [x] Spring Data JPA repositories

### Phase 2: Security & Encryption (Next)
- [ ] AES-256 encryption for sensitive fields
- [ ] HTTPS/TLS configuration
- [ ] Audit logging with Spring AOP
- [ ] AWS KMS integration

### Phase 3: User Authentication & RBAC
- [ ] Spring Security integration
- [ ] Multi-factor authentication (MFA)
- [ ] Role-based access control
- [ ] Session management

### Phase 4: GDPR Compliance Features
- [ ] Consent management UI
- [ ] Data export (Right to Access)
- [ ] Data deletion (Right to Erasure)
- [ ] Retention policy automation

### Phase 5: Electronic Signature Compliance
- [ ] Advanced electronic signatures (AES)
- [ ] Document-specific signature rules
- [ ] Signature verification API
- [ ] eIDAS compliance

### Phase 6: Conditional Form Logic
- [ ] JSON-based form schema
- [ ] Dynamic form rendering
- [ ] Real-time conditional logic
- [ ] Form versioning

---

## Useful Commands Cheat Sheet

### Maven Commands
```bash
# Compile
./mvnw clean compile

# Run tests
./mvnw test

# Run specific test
./mvnw test -Dtest=PatientRepositoryTest

# Run application
./mvnw spring-boot:run

# Package (create JAR)
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

### PostgreSQL Commands
```bash
# Connect to database
psql -U anamnesebogen_user -d anamnesebogen

# List databases
\l

# List tables
\dt

# Describe table
\d patients

# View table data
SELECT * FROM patients;

# Count records
SELECT COUNT(*) FROM patients;

# Exit
\q
```

### Flyway Commands
```bash
# Run migrations
./mvnw flyway:migrate

# Validate migrations
./mvnw flyway:validate

# Show migration info
./mvnw flyway:info

# Clean database (⚠️ DESTROYS DATA)
./mvnw flyway:clean

# Baseline existing database
./mvnw flyway:baseline
```

---

## Important Files

### Configuration Files
- `pom.xml` - Maven dependencies and build configuration
- `src/main/resources/application.properties` - Application configuration
- `src/main/resources/db/migration/V1__Initial_Schema.sql` - Database schema

### Documentation
- `README.md` - Project overview
- `CLAUDE.md` - Development guidelines for AI assistants
- `QUICKSTART.md` - This file
- `setup-database.sql` - Database setup script

### Source Code
- `src/main/java/.../model/` - JPA entities (7 files)
- `src/main/java/.../repository/` - Spring Data repositories (6 files)
- `src/main/java/.../controller/` - Spring MVC controllers
- `src/main/java/.../service/` - Business logic services
- `src/main/resources/templates/` - Thymeleaf HTML templates

---

## Support & Documentation

### Resources
- **Spring Boot Documentation:** https://docs.spring.io/spring-boot/docs/3.1.4/reference/html/
- **Spring Data JPA:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- **Flyway Documentation:** https://flywaydb.org/documentation/
- **PostgreSQL Documentation:** https://www.postgresql.org/docs/18/
- **GDPR Compliance:** https://gdpr-info.eu/

### Getting Help
- Check `CLAUDE.md` for development best practices
- Review Flyway migration scripts for database schema
- Check application logs: `target/spring.log` (if configured)
- Review PostgreSQL logs: `C:\Program Files\PostgreSQL\18\data\log\`

---

## Production Deployment Checklist

Before deploying to production:

### Security
- [ ] Change default database password (`changeme`)
- [ ] Use strong, randomly generated passwords
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall rules
- [ ] Disable unnecessary Actuator endpoints
- [ ] Enable authentication for admin endpoints

### Database
- [ ] Set up automated backups
- [ ] Configure georedundant storage
- [ ] Test disaster recovery procedures
- [ ] Set up monitoring and alerts
- [ ] Configure connection pooling
- [ ] Optimize database indexes

### Compliance
- [ ] Review GDPR compliance checklist
- [ ] Implement data encryption at rest
- [ ] Set up audit log archival
- [ ] Configure data retention policies
- [ ] Test data deletion workflows
- [ ] Document data processing activities

### Performance
- [ ] Load testing
- [ ] Configure caching
- [ ] Optimize database queries
- [ ] Set up CDN for static assets
- [ ] Configure application monitoring

### AWS Deployment (Recommended)
- [ ] Set up RDS PostgreSQL (Frankfurt region: eu-central-1)
- [ ] Configure AWS KMS for encryption keys
- [ ] Set up S3 for PDF storage
- [ ] Configure CloudWatch for logging
- [ ] Set up AWS WAF for security
- [ ] Configure auto-scaling

---

## License & Credits

**Project:** Anamnesebogen - Digital Anamnesis System for Dental Practices

**Version:** 0.0.1-SNAPSHOT (Phase 1 Complete)

**Technology Stack:**
- Java 17
- Spring Boot 3.1.4
- PostgreSQL 18
- Thymeleaf
- Flying Saucer PDF

**Author:** Elias Moualem

**Last Updated:** November 7, 2025

---

**🎉 You're ready to start! Follow the steps above to get your GDPR-compliant dental anamnesis system up and running.**
