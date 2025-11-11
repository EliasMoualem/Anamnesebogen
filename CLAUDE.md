# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Anamnesebogen is a Spring Boot 3 web application for collecting patient medical history (anamnesis) forms in multiple languages (German, English, Arabic, Russian). The application generates PDF documents from the submitted forms using Thymeleaf templating and Flying Saucer PDF conversion.

**Key Architecture Point**: This is a **database-backed application** using PostgreSQL for persistent storage. Patient data is stored in the database with GDPR-compliant fields for consent management and 10-year data retention (German medical records law). The application supports minor patients (under 18) with legal guardians and insurance policyholders.

## Build & Run Commands

### Build the project
```bash
./mvnw clean package
```

### Run the application
```bash
./mvnw spring-boot:run
```
Application will start on `http://localhost:8080`

### Run tests
```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=PdfServiceTest

# Run a specific test method
./mvnw test -Dtest=PdfServiceTest#testMethodName
```

### Build without tests
```bash
./mvnw clean package -DskipTests
```

## Application Architecture

### Request Flow
1. **Language Selection** (`/`) → User selects form language
2. **Form Display** (`/anamnesebogen?lang=XX`) → Show language-specific form with dynamic guardian/policyholder sections
3. **Form Submission** (`POST /submit-anamnesebogen`) → Validate data, save to database (Patient, Guardian, Policyholder entities), redirect to PDF
4. **PDF Generation** (`/pdf?id=UUID`) → Load patient data from database, generate PDF, serve to user (data persists in database)

### Critical Components

**AnamnesebogenController** (src/main/java/.../controller/AnamnesebogenController.java)
- Main controller handling all web requests
- Binds form data to `PatientFormDTO` using Thymeleaf `th:object` binding
- Delegates to `PatientService` for validation and database persistence
- Handles multi-language error messages in `getErrorMessage()` method

**PatientService** (src/main/java/.../service/PatientService.java)
- Core business logic for patient management
- Converts DTOs to entities and saves to database
- Enforces business rules for minor patients (requires guardian + policyholder, must be FAMILY_INSURED)
- Handles signature creation and storage
- Calculates age using `Period.between()` for accurate validation

**PdfService** (src/main/java/.../service/PdfService.java)
- Responsible for PDF generation using Thymeleaf + Flying Saucer
- `processTemplate()` processes Thymeleaf template with patient data
- `generateAndServePdf()` creates PDF and writes to HttpServletResponse
- Conditionally displays guardian/policyholder sections in PDF
- Handles base64 signature image embedding in PDFs

**DTOs (Data Transfer Objects)**
- `PatientFormDTO`: Form data for all patients (replaces old `MinorPatient` model)
- `GuardianDTO`: Legal guardian information for minor patients (Erziehungsberechtigter)
- `PolicyholderDTO`: Insurance policyholder information (Hauptversicherter)

**Entities (Database Models)**
- `Patient`: Main patient entity with GDPR fields, relationships to guardian/policyholder
- `LegalGuardian`: Legal guardian entity with `RelationshipType` enum (MOTHER, FATHER, LEGAL_GUARDIAN, etc.)
- `Policyholder`: Insurance policyholder entity
- `InsuranceType` enum: SELF_INSURED or FAMILY_INSURED
- All entities extend `BaseAuditEntity` for created_at/updated_at/deleted_at tracking

### Multi-Language Support

The application supports 4 languages via separate Thymeleaf templates:
- `anamnesebogen-de.html` (German)
- `anamnesebogen-en.html` (English)
- `anamnesebogen-ar.html` (Arabic)
- `anamnesebogen-ru.html` (Russian)
- `pdf_anamnesebogen.html` (PDF template - language-agnostic)

Language is passed via `?lang=XX` query parameter and stored with patient data.

Error messages are also localized in `AnamnesebogenController.getErrorMessage()` using switch expressions (lines 206-263).

### Business Rules & Validation

**Minor Patients (< 18 years old)** MUST:
- Have a legal guardian (`LegalGuardian` entity) with required fields
- Be `FAMILY_INSURED` (self-insured minors are not allowed per German law)
- Have a policyholder (`Policyholder` entity) for insurance
- Guardian and policyholder may be the same person but are tracked separately

**Adult Patients (≥ 18 years old)** can be:
- `SELF_INSURED`: No guardian or policyholder required
- `FAMILY_INSURED`: No guardian required, but must have a policyholder

**All `FAMILY_INSURED` patients** (any age) MUST:
- Have a policyholder (`Policyholder` entity)

**Age Calculation**:
- Uses `Period.between(birthDate, LocalDate.now()).getYears()` for accuracy
- Accounts for full birth dates, not just year difference
- German legal age of majority (Volljährigkeit): 18 years

**Validation**:
- Performed in `Patient.validate()` method before saving
- Frontend dynamically shows/hides guardian/policyholder sections based on age
- JavaScript validates required fields before form submission

### Signature Handling

Digital signatures are captured using Signature Pad library on the frontend:
1. Frontend sends signature as base64 data URL
2. `processSignature()` extracts base64 data (splits on comma)
3. Decodes to byte array and stores in `PatientFormDTO.signature`
4. Saved to database as `Signature` entity with UUID and timestamp
5. PDF generation converts back to base64 for HTML embedding

### Thymeleaf Configuration

Custom Thymeleaf configuration in `ThymeleafConfig` class:
- Template resolver for HTML files
- Template engine with Spring integration
- Required for standalone template processing outside of Spring MVC

## Important Development Notes

### Working with Templates
- HTML templates are in `src/main/resources/templates/`
- Static resources (JS, CSS) are in `src/main/resources/static/`
- PDF template uses inline CSS (Flying Saucer has limited CSS support)

### Database & Migrations

**Database**: PostgreSQL with JPA/Hibernate
**Migration Tool**: Flyway (versioned migrations in `src/main/resources/db/migration/`)

**Key Migrations**:
- `V1__Initial_Schema.sql` - Core tables (patients, signatures, consents, form_submissions)
- `V2__Add_Policyholders.sql` - Policyholder table for family-insured patients
- `V3__Create_Indexes.sql` - Performance indexes
- `V4__Add_Legal_Guardians.sql` - Guardian table for minor patients

**Running Migrations**:
- Flyway runs automatically on application startup
- Validates existing schema against migration files
- Applies new migrations in order

### Adding New Fields

**To add patient fields**:
1. Add field to `PatientFormDTO` DTO (form binding)
2. Add field to `Patient` entity (database)
3. Create Flyway migration to add column (e.g., `V5__Add_New_Field.sql`)
4. Update ALL language form templates (de, en, ar, ru) with `th:field` binding
5. Update `pdf_anamnesebogen.html` template for PDF display
6. Add validation annotations if required

**To add guardian fields**:
1. Add field to `GuardianDTO` and `LegalGuardian` entity
2. Create Flyway migration
3. Update guardian sections in all form templates
4. Update PDF template guardian section

**To add policyholder fields**:
1. Add field to `PolicyholderDTO` and `Policyholder` entity
2. Create Flyway migration
3. Update policyholder sections in all form templates
4. Update PDF template policyholder section

### Error Handling
- All errors redirect to form with `?error=CODE&lang=XX` parameters
- Add new error codes to `getErrorMessage()` for all 4 languages
- Use structured error codes (e.g., `pdf_error`, `submission_error`)

### Logging
- Application uses SLF4J with `@Slf4j` annotation
- Debug logging enabled for `de.elias.moualem` package (application.properties:12)
- Key log points: form submission, PDF generation, patient data lifecycle

### PDF Generation Gotchas
- Flying Saucer requires well-formed XHTML
- CSS support is limited (CSS 2.1 subset)
- Images must be base64 encoded in HTML or use absolute file paths
- Temporary PDF files created in system temp directory

## Technology Stack Reference

**Backend**:
- **Java 21** with **Spring Boot 3.1.4**
- **Spring Data JPA** with **Hibernate** for ORM
- **PostgreSQL** for database
- **Flyway** for database migrations
- **Lombok** for boilerplate reduction
- **Jakarta Validation** for bean validation
- **Spring Boot Actuator** for monitoring (all endpoints exposed)

**Templating & PDF**:
- **Thymeleaf** for HTML templating
- **Flying Saucer PDF 9.9.4** for PDF conversion

**Frontend**:
- **jQuery 3.6.0** for DOM manipulation
- **jQuery UI** for datepicker widgets
- **Signature Pad 4.0.0** for digital signatures

**Testing**:
- **JUnit 5** for test framework
- **Mockito** for mocking
- **AssertJ** for fluent assertions
- **Spring Boot Test** for integration testing

## Monitoring

Spring Boot Actuator endpoints are exposed at `/actuator/*`:
- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- All endpoints enabled (application.properties:1)

## Domain Model Summary

### Core Concepts

**Patient-Centric Design**:
- All data revolves around the `Patient` entity
- Patient can have optional relationships to `LegalGuardian` and `Policyholder`
- Age determines required relationships (minor vs adult)

**Separation of Concerns**:
- **Legal Guardian** (Erziehungsberechtigter): Person with legal custody, authorized to make medical decisions
- **Policyholder** (Hauptversicherter): Person who holds the insurance policy
- These may be the same person or different people - they are tracked separately

**Insurance Types**:
- `SELF_INSURED`: Patient has their own insurance policy (only adults)
- `FAMILY_INSURED`: Patient is covered under someone else's policy (all minors, optional for adults)

### Entity Relationships

```
Patient (1) -------- (0..1) LegalGuardian
  |
  └─────────── (0..1) Policyholder
  |
  └─────────── (0..*) Signature
  |
  └─────────── (0..*) FormSubmission
  |
  └─────────── (0..*) Consent
```

### GDPR Compliance

**Data Protection**:
- `data_processing_consent`: Boolean flag for consent tracking
- `data_processing_consent_date`: When consent was given
- `data_retention_until`: Auto-calculated (10 years from submission)
- `deletion_requested`: Right to be forgotten flag
- `submission_ip_address` & `submission_user_agent`: Audit trail

**Data Retention**:
- German medical records law: 10 years minimum retention
- Calculated automatically: `LocalDate.now().plusYears(10)`
- Soft delete support with `deleted_at` timestamp

**Audit Trail**:
- All entities extend `BaseAuditEntity`
- `created_at`, `updated_at`, `deleted_at` timestamps
- IP address and user agent tracked for submissions

### Test Coverage

**PatientServiceTest** (14 tests):
- Minor patient validation (guardian + policyholder required, must be FAMILY_INSURED)
- Adult patient scenarios (SELF_INSURED and FAMILY_INSURED)
- Age calculation boundary testing
- PDF data retrieval validation

**PdfServiceTest** (10 tests):
- PDF generation from patient data
- Signature handling and embedding
- Template processing validation

---

# Claude Code Guidelines

## Role context
### Claude role:
- You are an expert software engineer with extensive experience in Java, Spring Boot, and microservices architecture.
- You have deep knowledge of digital signatures and digitalising dental praxis processes.
- You are familiar with clean architecture principles and multi-tenant application design.
- You understand best practices for RESTful API design, caching strategies, and performance optimization.
- You are skilled in writing clear, maintainable code and comprehensive documentation.
- You are adept at guiding developers through complex codebases and architectural decisions.
- You have experience with Docker and containerized development environments.
- You are proficient in writing and reviewing unit and integration tests.
- You are an expert in monitoring and logging in microservices.
- You are an expert in AWS services.
- You are knowledgeable about Thoughtworks Sensible Defaults for software development.
- You are well-versed in SOLID principles and can help enforce them in code reviews.

---

### Interaction guidelines:
- Provide clear, concise, and actionable guidance.
- Use examples and references to the codebase when explaining concepts.
- Never commit or change code directly; always suggest changes for the user to implement.
- Encourage best practices and adherence to architectural principles.
- Be patient and supportive, especially when explaining complex topics.

---

## Implementation best practices:

### Best practices to follow
- Follow Thoughtworks Sensible Defaults for software development.
- Adhere to SOLID principles to ensure code maintainability and scalability.
- Write clear, concise, and maintainable code with proper documentation.
- Use of TDD (Test-Driven Development) approach for new features and bug fixes.
- Ensure comprehensive unit and integration test coverage.
- Follow DDD (Domain-Driven Design) principles where applicable.
- Use meaningful names for variables, functions, and classes that reflect their purpose.
- Avoid premature optimization; focus on code clarity and correctness first.

---

### 0 — Purpose

These rules ensure maintainability, safety, and developer velocity.
**MUST** rules are enforced by CI; **SHOULD** rules are strongly recommended.

---

### 1 — Before Coding
- BP-1 (MUST)**Ask the user clarifying questions.
- BP-2 (SHOULD)**Draft and confirm an approach for complex work.
- BP-3 (SHOULD)**If ≥ 2 approaches exist, list clear pros and cons.

---

### 2 — While Coding

- **C-1 (MUST)** Follow TDD: scaffold stub -> write failing test -> implement.
- **C-2 (MUST)** Name functions with existing domain vocabulary for consistency.
- **C-3 (SHOULD NOT)** Introduce classes when small testable functions suffice.
- **C-4 (SHOULD)** Prefer simple, composable, testable functions.
- **C-5 (SHOULD)** Use of functional programming where possible, but explaining the alternative non-functional approach.
- **C-6 (SHOULD NOT)** Add comments except for critical caveats; rely on self‑explanatory code.

---

### 3 — Testing
- **T-1 (SHOULD)**Prefer integration tests over heavy mocking.
- **T-2 (SHOULD)** Unit-test complex algorithms thoroughly.

---

## Writing Functions Best Practices
When evaluating whether a function you implemented is good or not, use this checklist:

1. Can you read the function and HONESTLY easily follow what it's doing? If yes, then stop here.
2. Does the function have very high cyclomatic complexity? (number of independent paths, or, in a lot of cases, number of nesting if if-else as a proxy). If it does, then it's probably sketchy.
3. Are there any common data structures and algorithms that would make this function much easier to follow and more robust? Parsers, trees, stacks / queues, etc.
4. Are there any unused parameters in the function?
5. Are there any unnecessary type casts that can be moved to function arguments?
6. Is the function easily testable without mocking core features (e.g. sql queries, redis, etc.)? If not, can this function be tested as part of an integration test?
7. Does it have any hidden untested dependencies or any values that can be factored out into the arguments instead? Only care about non-trivial dependencies that can actually change or affect the function.
8. Brainstorm 3 better function names and see if the current name is the best, consistent with rest of codebase.

Follow this checklist for writing your commit message:
- SHOULD use Conventional Commits format: https://www.conventionalcommits.org/en/v1.0.0
- SHOULD NOT refer to Claude or Anthropic in the commit message.
