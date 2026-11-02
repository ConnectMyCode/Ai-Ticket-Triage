# Frontend Api Contract

# AI Ticket Triage — Frontend API Contract

**Base URL:** `http://13.203.220.149:8080`

| Environment | Frontend origin |
| --- | --- |
| Local development | `http://localhost:5173` |
| Deployed frontend | `http://3.7.223.110` |

**Authentication:** JWT Bearer token for protected endpoints.

**Content type:** `application/json` for JSON request bodies.

## 1. Authentication

### 1.1 Sign up

- **Method:** `POST`
- **Path:** `/api/auth/signup`
- **Authentication:** Not required
- **Request body:**

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

| Field | Type | Requirement |
| --- | --- | --- |
| `email` | string | Required; must be a valid email |
| `password` | string | Required; minimum 8 characters |
- **Success:** Postman documentation specifies `200 OK`.
- **Response body:** The controller returns `void`; do not expect a user object.
- **Documented errors:** `400 Bad Request` for invalid input or an email already in use; `500 Internal Server Error` for unexpected errors. Exact error bodies are not established here.

### 1.2 Log in

- **Method:** `POST`
- **Path:** `/api/auth/login`
- **Authentication:** Not required
- **Request body:**

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

- **Success:** `200 OK`

```json
{
  "token": "<jwt_token>"
}
```

Save the returned token and send it on protected requests:

```
Authorization: Bearer <jwt_token>
```

The Postman collection uses a `token` variable for this purpose.

### 1.3 Test the JWT

- **Method:** `GET`
- **Path:** `/api/test`
- **Authentication:** Required
- **Request body:** None
- **Success:** `200 OK`, plain-text response, for example:

```
Logged in as: test@example.com
```

The Postman documentation describes `401 Unauthorized` or `403 Forbidden` for missing, expired, or invalid tokens. The exact response depends on the security configuration and failure condition.

## 2. Tickets

### 2.1 Create a ticket

- **Method:** `POST`
- **Path:** `/api/tickets`
- **Authentication:** Required

**Request with priority:**

```json
{
  "title": "Cannot upload photo",
  "description": "Uploading a photo displays an error.",
  "priority": "MEDIUM"
}
```

**Request without priority:**

```json
{
  "title": "Bug",
  "description": "Something broke"
}
```

| Field | Type | Requirement |
| --- | --- | --- |
| `title` | string | Required |
| `description` | string | Required |
| `priority` | string | Optional; defaults to `LOW` when omitted or empty |

Accepted priority values: `LOW`, `MEDIUM`, `HIGH`.

- **Response behavior from controller:** Returns `void`.
- **Postman documentation:** Describes `200 OK` or `201 Created` and a created-ticket object.
- **Contract note:** The documented object response conflicts with the controller's `void` return type. Use the actual deployed response status and body as the final runtime reference.

### 2.2 Get the current user's tickets

- **Method:** `GET`
- **Path:** `/api/tickets`
- **Authentication:** Required
- **Request body:** None
- **Response:** `Page<TicketResponse>`

**Example request:**

```
GET /api/tickets?page=0&size=5
Authorization: Bearer <jwt_token>
```

Query parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `page` | integer | Zero-based page index; `0` is the first page |
| `size` | integer | Number of tickets requested per page |
| `status` | string/list | Optional status filter |
| `priority` | string/list | Optional priority filter |

The backend supports optional status and priority filters. When filters are omitted, it returns the authenticated user's tickets without those filters.

**Pagination response shape:**

```json
{
  "content": [
    {
      "id": 1,
      "title": "Login issue",
      "description": "Cannot log in",
      "createdAt": "2026-04-06T14:24:43.917317",
      "status": "OPEN",
      "priority": "LOW",
      "finalResponse": null,
      "resolutionNote": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "size": 5,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

This is an illustrative structure, not a guaranteed verbatim response. Spring Data's serialized pagination metadata can include additional fields.

**Important field caveat:** `TicketResponse` defines `finalResponse` and `resolutionNote`, but the supplied `TicketService.mapToResponse()` does not populate them. The frontend should not assume these values are populated in ticket-list responses.

The Postman documentation specifies a default page size of 20. If the frontend depends on that default, verify it against the deployed endpoint; the frontend can explicitly send `page` and `size`.

### 2.3 Update ticket status

- **Method:** `PUT`
- **Path:** `/api/tickets/{id}/status`
- **Authentication:** Required
- **Path parameter:** `id` — ticket ID
- **Request body:**

```json
{
  "status": "IN_PROGRESS"
}
```

Accepted status values: `OPEN`, `IN_PROGRESS`, `CLOSED`.

- **Response behavior from controller:** Returns `void`.
- **Postman documentation:** Describes `200 OK` and an updated ticket object.
- **Contract note:** The documented object response conflicts with the controller return type. Do not rely on a response object unless confirmed by a deployed request.

The service checks ticket ownership, rejects updates to closed tickets, and reports errors for missing tickets or unauthorized access. Exact HTTP status codes and error bodies should be taken from observed runtime responses, not inferred from the exception messages alone.

### 2.4 Close a ticket

- **Method:** `PUT`
- **Path:** `/api/tickets/{id}/close`
- **Authentication:** Required
- **Path parameter:** `id` — ticket ID
- **Request body:** None
- **Response behavior from controller:** Returns `void`.
- **Postman documentation:** Describes `200 OK` with an updated ticket object marked `CLOSED`.
- **Contract note:** The documented object response conflicts with the controller return type.

The service checks ownership and whether the ticket is already closed. A successful operation sets the ticket status to `CLOSED`.

### 2.5 Save triage results

- **Method:** `POST`
- **Path:** `/api/tickets/{id}/triage/save`
- **Authentication:** Required
- **Path parameter:** `id` — ticket ID
- **Request body:**

```json
{
  "category": "TECHNICAL",
  "sentiment": "NEGATIVE",
  "priority": "HIGH",
  "summary": "Error uploading photo",
  "replyDraft": "Thank you for reporting the photo upload issue."
}
```

The supplied `TriageSaveRequest` contains:

| Field | Type |
| --- | --- |
| `category` | string |
| `sentiment` | string |
| `priority` | `TicketPriority` |
| `summary` | string |
| `replyDraft` | string |

**Do not include `tags` in this request contract.** The Postman and Swagger examples contain `tags`, but the supplied request DTO does not.

- **Response behavior from controller:** Returns `void`.
- **Postman documentation:** Specifies `200 OK` for success and documents a `404 Not Found` example.
- **Contract note:** The exact deployed HTTP response and error body must be confirmed against runtime behavior.

The service requires category and sentiment and rejects triage of a closed ticket.

### 2.6 Run AI triage

- **Method:** `POST`
- **Path:** `/api/tickets/{id}/triage/run`
- **Authentication:** Required
- **Path parameter:** `id` — ticket ID
- **Request body:** None
- **Success:** `200 OK`, with an `AiTriageResponse` JSON object.

```json
{
  "category": "TECHNICAL",
  "priority": "MEDIUM",
  "tags": ["upload", "photo", "error"],
  "sentiment": "NEGATIVE",
  "summary": "The user reports an error when uploading a photo.",
  "replyDraft": "Thank you for reporting the issue. We can help investigate the photo upload error."
}
```

| Field | Type |
| --- | --- |
| `category` | string |
| `priority` | string |
| `tags` | array of strings |
| `sentiment` | string |
| `summary` | string |
| `replyDraft` | string |

This is the triage result DTO, which includes `tags`. It is distinct from the `TriageSaveRequest`, which does not.

### 2.7 Respond to a ticket

- **Method:** `POST`
- **Path:** `/api/tickets/{id}/respond`
- **Authentication:** Required
- **Path parameter:** `id` — ticket ID
- **Request body:**

```json
{
  "finalResponse": "The issue has been investigated.",
  "resolutionNote": "Confirmed the cause of the upload failure."
}
```

Both `finalResponse` and `resolutionNote` are required by the service.

- **Response behavior from controller:** Returns `void`.
- **Postman documentation:** Describes a successful `200 OK` response and suggests an updated ticket object, but that object is not confirmed by the controller return type.

The service requires the ticket to have been triaged first and rejects responses to closed tickets.

## 3. Admin tickets

### 3.1 Get all tickets

- **Method:** `GET`
- **Path:** `/api/admin/tickets`
- **Authentication:** Required
- **Request body:** None

**Example request:**

```
GET /api/admin/tickets?page=0&size=5
Authorization: Bearer <jwt_token>
```

Query parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `page` | integer | Zero-based page index |
| `size` | integer | Number of tickets per page |
| `status` | string/list | Optional status filter |
| `priority` | string/list | Optional priority filter |

**Response:** `Page<TicketResponse>`.

The supplied service implementation checks whether the current user's email is exactly `admin@example.com` and returns all tickets with the requested filters. It does not merely return tickets belonging to the admin.

The Postman documentation's description of this endpoint as returning the admin's own tickets and its use of a one-based page index do not match the supplied service behavior and Spring pagination convention.

## 4. Health check

### Check backend and database health

- **Method:** `GET`
- **Path:** `/api/health`
- **Authentication:** Confirm against the deployed security configuration.
- **Request body:** None

**Success example — `200 OK`:**

```json
{
  "status": "UP",
  "database": "connected"
}
```

The supplied controller also describes a `503 Service Unavailable` response when the database check fails, with a body shaped like:

```json
{
  "status": "DOWN",
  "database": "error: ..."
}
```

The database error text is variable.

## 5. General integration rules

1. Use `http://13.203.220.149:8080` as the frontend API base URL.
2. Use the reusable path parameter `{id}` for ticket-specific operations. A concrete ID in Swagger or a Postman example is a test value, not a different endpoint.
3. Send the JWT as `Authorization: Bearer <token>` for protected endpoints.
4. Use zero-based pagination: `page=0` is the first page.
5. Use the exact enum values supported by the backend:
    - Status: `OPEN`, `IN_PROGRESS`, `CLOSED`
    - Priority: `LOW`, `MEDIUM`, `HIGH`
6. Do not assume a ticket object is returned from create, update-status, close, save-triage, or respond operations. Their supplied controller methods return `void`.
7. Do not assume the error status or JSON body from a Java exception message. Use confirmed runtime responses where exact error handling matters.
8. Do not send `tags` in the triage-save request based on the supplied DTO.

## 6. Source reconciliation and items to verify

| Item | Source finding | Contract treatment |
| --- | --- | --- |
| Empty Swagger `200` response descriptions | Postman documents detailed responses for several endpoints | Use Postman examples as documentation, but distinguish them from controller-confirmed behavior |
| Test-specific ticket IDs in Swagger | Examples contain IDs such as `42` and `40` | Document reusable `{id}` paths |
| Ticket mutation responses | Postman examples claim updated objects; supplied controller methods return `void` | Do not promise an object response without runtime confirmation |
| Triage-save request | Postman/Swagger examples include `tags`; DTO has no `tags` | Omit `tags` from the request |
| Admin ticket listing | Postman description says the admin's own tickets; service returns all tickets after checking the admin email | Document all-ticket behavior |
| Pagination | Some documentation uses page `1` as the first page | Document zero-based pagination |
| Ticket-list response fields | DTO defines `finalResponse` and `resolutionNote`, but mapping does not populate them | Treat these fields as potentially null; verify actual JSON |
| Error responses | Postman contains some examples, but exception handling does not establish all HTTP status/body combinations | Mark unconfirmed cases rather than inventing responses |

**Authority order used for this contract:** supplied controller/DTO/service behavior for implementation details; Postman documentation for detailed documented examples and reported runtime responses; Swagger for endpoint descriptions and examples, with test IDs normalized to reusable paths.