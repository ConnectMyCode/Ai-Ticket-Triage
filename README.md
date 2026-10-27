# spec.md

# Project 1 (Intermediate) – AI Support Ticket Triage System (SDD)

## 1) Goal

Build a support ticket system where a user creates customer tickets and the system uses AI to:

- classify category + priority,
- extract structured fields (product, issue type, sentiment),
- suggest tags,
- draft a recommended reply.

The user reviews/edits the AI output and saves it. The entire app is deployed and production-ready for a beginner.

Frontend: React.js , Javascript
Backend: Java + Spring Boot (REST)
DB: PostgreSQL
Deployment: Docker + PaaS

## 2) Roles

- User (single-user system for v1)
    - creates tickets
    - runs AI triage
    - reviews and finalizes triage + draft reply
    - changes ticket status (workflow)
    - views dashboard and metrics

## 3) Scope (strict boundaries)

IN SCOPE (v1):

- Text-only ticket creation (no email integration)
- AI triage on demand (button click)
- Review/edit before saving AI results
- Ticket workflow: OPEN → TRIAGED → RESPONDED → CLOSED
- Search/filter tickets and basic metrics
- Full deployment + README + tests

OUT OF SCOPE (v1):

- Multi-agent teams, role-based access
- Live chat, email sending/receiving
- Attachments + OCR
- Complex SLA engine
- Multi-tenant organizations

## 4) Core user stories

- As a user, I can sign up and log in.
- As a user, I can create a ticket with customer details and issue text.
- As a user, I can run "AI Triage" for a ticket.
- As a user, I can review AI outputs (category/priority/tags/draft reply) and edit them.
- As a user, I can mark a ticket as responded and store the final response text.
- As a user, I can close a ticket with a resolution note.
- As a user, I can filter/search tickets by status, priority, category, and text query.
- As a user, I can see basic metrics:
    - count by status
    - count by category
    - average time to triage (based on timestamps)

## 5) Acceptance criteria (definition of done)

### Auth & security

- Signup/login works.
- All endpoints (except auth) require authentication.
- A user can access only their tickets.

### Ticket workflow

- Ticket status values are enforced (cannot jump to invalid state).
- Status transitions:
    - OPEN → TRIAGED
    - TRIAGED → RESPONDED
    - RESPONDED → CLOSED
    - (Optional) allow reopening: CLOSED → OPEN (only via explicit action)

### AI triage

- Clicking AI triage produces:
    - category (one of a fixed set),
    - priority (P1–P4),
    - tags (list),
    - sentiment (positive/neutral/negative),
    - summary (2–4 lines),
    - suggested reply draft.
- Output is stored as an AI run record with request/response logged.
- If AI fails (timeout/bad output), UI shows a helpful error and ticket stays usable.

### UI (Appsmith)

- Create ticket page: form + submit + success/error messages.
- Ticket list page: table + filters + search.
- Ticket detail page:
    - shows ticket content
    - has “Run AI triage” button
    - shows AI result fields editable
    - save triage + save response + close ticket actions

### Production readiness (beginner level)

- Input validation for all write operations.
- Global error handler returns consistent JSON errors.
- App is deployable with Docker.
- README includes setup + env vars + deployed URLs.
- Minimum 5 tests exist and pass.

## 6) Data model (PostgreSQL)

### users

- id (UUID or bigserial, PK)
- email (unique, not null)
- password_hash (not null)
- created_at, updated_at

### tickets

- id (UUID or bigserial, PK)
- user_id (FK)
- customer_name (not null)
- customer_email (optional but validated if present)
- subject (not null, max length)
- description (not null, long text)
- status (OPEN | TRIAGED | RESPONDED | CLOSED)
- category (nullable until triaged)
- priority (nullable until triaged: P1 | P2 | P3 | P4)
- sentiment (nullable: POSITIVE | NEUTRAL | NEGATIVE)
- summary (nullable, text)
- tags (nullable; store as JSON array or a separate table; v1 can use JSON/text)
- final_response (nullable, text)
- resolution_note (nullable, text)
- triaged_at (nullable timestamp)
- responded_at (nullable timestamp)
- closed_at (nullable timestamp)
- created_at, updated_at

### ai_runs

- id (UUID or bigserial, PK)
- user_id (FK)
- ticket_id (FK)
- purpose (TRIAGE)
- model (string)
- prompt_version (string)
- status (SUCCESS | FAILED)
- error_message (nullable)
- raw_request (text/json)
- raw_response (text/json)
- created_at

## 7) Category + priority set (fixed for v1)

Categories (example set):

- BILLING
- LOGIN_AUTH
- BUG
- FEATURE_REQUEST
- PERFORMANCE
- INTEGRATION_API
- OTHER

Priority:

- P1 Critical (system down / security / payment failure)
- P2 High (major feature broken, many users)
- P3 Medium (workaround exists)
- P4 Low (minor issue, question)

## 8) API design (REST)

Base: /api

### Auth

- POST /api/auth/signup
- POST /api/auth/login

### Tickets

- POST /api/tickets
- GET /api/tickets
    - filters:
        - status
        - priority
        - category
        - q (search on subject/description/customer)
- GET /api/tickets/{id}
- PATCH /api/tickets/{id}
    - editable fields: subject, description, customer_name, customer_email
    - status changes via dedicated endpoints (below)

### Workflow actions (keep clear)

- POST /api/tickets/{id}/triage/run
    - triggers AI triage (server calls LLM API)
    - returns structured triage result
- POST /api/tickets/{id}/triage/save
    - body includes category, priority, tags, sentiment, summary, replyDraft (optional)
    - sets status TRIAGED and sets triaged_at
- POST /api/tickets/{id}/respond
    - body: { finalResponse: string }
    - sets status RESPONDED, responded_at
- POST /api/tickets/{id}/close
    - body: { resolutionNote?: string }
    - sets status CLOSED, closed_at

### Metrics (simple)

- GET /api/metrics/summary
    - counts by status
    - counts by category
    - avg time to triage (triaged_at - created_at) for triaged tickets

## 9) AI prompting requirements (quality guardrails)

- The backend asks AI for STRICT JSON output.
- The backend validates output:
    - category is one of allowed values
    - priority is P1–P4
    - tags is a list of <= 8 strings
    - summary length limits
- If output is invalid, treat as failure and return a helpful error.

## 10) Non-functional requirements

- Security:
    - Passwords hashed with BCrypt
    - JWT auth (expires)
    - CORS configured for Appsmith
- Observability:
    - Log request path + response status
    - Log AI run failures clearly
- Reliability:
    - Timeouts for AI calls
    - Save ai_runs for every attempt (success/failure)
- Testing:
    - auth tests
    - ticket creation test
    - triage run mocked test
    - authorization test (cannot access another user's ticket)
