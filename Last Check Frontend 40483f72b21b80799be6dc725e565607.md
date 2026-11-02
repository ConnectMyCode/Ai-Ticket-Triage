# Last Check Frontend

Yes. I have the API contract you supplied for your AI Ticket Triage backend, along with the implementation details and the discrepancies you explicitly identified between the controller/DTO behavior and the Postman/Swagger documentation.

Before we build React components, I’ll separate what is established from what still needs verification. I will not invent response bodies or treat documentation examples as guaranteed runtime behavior.

## 1. Endpoint inventory

The contract contains 12 endpoints across authentication, tickets, administration, and health checks.

| # | Method | Endpoint | Authentication |
| --- | --- | --- | --- |
| 1 | POST | `/api/auth/signup` | Not required |
| 2 | POST | `/api/auth/login` | Not required |
| 3 | GET | `/api/test` | JWT required |
| 4 | POST | `/api/tickets` | JWT required |
| 5 | GET | `/api/tickets` | JWT required |
| 6 | PUT | `/api/tickets/{id}/status` | JWT required |
| 7 | PUT | `/api/tickets/{id}/close` | JWT required |
| 8 | POST | `/api/tickets/{id}/triage/save` | JWT required |
| 9 | POST | `/api/tickets/{id}/triage/run` | JWT required |
| 10 | POST | `/api/tickets/{id}/respond` | JWT required |
| 11 | GET | `/api/admin/tickets` | JWT required; admin email check |
| 12 | GET | `/api/health` | Confirm deployed security behavior |

Base URL: `http://13.203.220.149:8080`

For protected requests, the frontend sends:

```
Authorization: Bearer <jwt_token>
```

The contract says `/api/auth/**` and `/api/health/**` are permitted by the security configuration. However, I have not independently tested the current deployed instance in this turn.

## 2. Request and response contract

### Authentication

1. Sign up — `POST /api/auth/signup`

Request:

```
{
  "email": "test@example.com",
  "password": "password123"
}
```

- Password minimum: 8 characters.
- Controller returns `void`.
- The documented success response is `200 OK`.
- The exact response body and error format are not confirmed.

2. Log in — `POST /api/auth/login`

Request:

```
{
  "email": "test@example.com",
  "password": "password123"
}
```

Success response:

```
{
  "token": "<jwt_token>"
}
```

The frontend must save the returned token and attach it to protected requests. The token storage mechanism has not yet been chosen.

3. Test authentication — `GET /api/test`

- No request body.
- Requires JWT.
- Returns plain text, for example:

```
Logged in as: test@example.com
```

### Tickets

4. Create a ticket — `POST /api/tickets`

Request:

```
{
  "title": "Cannot upload photo",
  "description": "Uploading a photo displays an error.",
  "priority": "MEDIUM"
}
```

Fields:

- `title`: required string.
- `description`: required string.
- `priority`: optional; defaults to `LOW` when omitted or empty, according to the supplied contract.

Accepted priorities: `LOW`, `MEDIUM`, `HIGH`.

Confirmed implementation detail: the controller returns `void`. Do not build the frontend expecting a created-ticket object from this endpoint.

5. List the current user's tickets — `GET /api/tickets`

Example:

```
GET /api/tickets?page=0&size=5
```

Optional query parameters:

- `page`: zero-based page index.
- `size`: page size.
- `status`: optional status filter.
- `priority`: optional priority filter.

Accepted statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`.

Response type: `Page<TicketResponse>`. The illustrative structure is:

```
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
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 5,
  "first": true,
  "last": true,
  "empty": false
}
```

This is an example, not the guaranteed exact serialized response. Spring may include additional pagination fields.

Important: the supplied `TicketService.mapToResponse()` does not populate `finalResponse` or `resolutionNote`. We should treat those fields as potentially null in list responses.

6. Update ticket status — `PUT /api/tickets/{id}/status`

Request:

```
{
  "status": "IN_PROGRESS"
}
```

Accepted values: `OPEN`, `IN_PROGRESS`, `CLOSED`.

The controller returns `void`; do not expect an updated ticket object.

7. Close a ticket — `PUT /api/tickets/{id}/close`

- No request body.
- Ticket ID is supplied in the path.
- The successful operation sets status to `CLOSED`.
- The controller returns `void`.

8. Save triage results — `POST /api/tickets/{id}/triage/save`

Request:

```
{
  "category": "TECHNICAL",
  "sentiment": "NEGATIVE",
  "priority": "HIGH",
  "summary": "Error uploading photo",
  "replyDraft": "Thank you for reporting the photo upload issue."
}
```

The supplied `TriageSaveRequest` contains `category`, `sentiment`, `priority`, `summary`, and `replyDraft`.

Do not send `tags` to this endpoint. Although documentation examples include `tags`, the supplied request DTO does not.

The controller returns `void`.

9. Run AI triage — `POST /api/tickets/{id}/triage/run`

- No request body.
- Returns an `AiTriageResponse` JSON object, according to the supplied contract.

Example:

```
{
  "category": "TECHNICAL",
  "priority": "MEDIUM",
  "tags": ["upload", "photo", "error"],
  "sentiment": "NEGATIVE",
  "summary": "The user reports an error when uploading a photo.",
  "replyDraft": "Thank you for reporting the issue. We can help investigate the photo upload error."
}
```

The response fields are `category`, `priority`, `tags`, `sentiment`, `summary`, and `replyDraft`. The example values are illustrative AI output, not fixed values.

10. Respond to a ticket — `POST /api/tickets/{id}/respond`

Request:

```
{
  "finalResponse": "The issue has been investigated.",
  "resolutionNote": "Confirmed the cause of the upload failure."
}
```

Both fields are required by the supplied service behavior. The controller returns `void`.

### Administration

11. Get all tickets — `GET /api/admin/tickets`

Example:

```
GET /api/admin/tickets?page=0&size=5
```

- Requires JWT.
- The supplied service checks that the current user's email is exactly `admin@example.com`.
- Returns all tickets, with pagination and optional status/priority filters, rather than only the admin's own tickets.
- Response type: `Page<TicketResponse>`.

The admin email check is the documented implementation behavior; it is not a general role-based authorization system.

### Health

12. Health check — `GET /api/health`

Expected success example:

```
{
  "status": "UP",
  "database": "connected"
}
```

The supplied controller describes a `503 Service Unavailable` response if the database check fails, with a body shaped like:

```
{
  "status": "DOWN",
  "database": "error: ..."
}
```

The error text varies. Whether the deployed endpoint is publicly accessible must be verified against the current deployment.

## 3. What is uncertain and must not be assumed

These are the points I want to make explicit before frontend development.

| Item | What we know | What remains uncertain |
| --- | --- | --- |
| Signup response | Controller returns `void` | Exact deployed HTTP status, response body, and error format |
| Login errors | Successful response contains `token`  | Exact invalid-credentials status and error body |
| Ticket creation | Controller returns `void` | Exact success status and whether the response body is empty |
| Status update, close, save triage, respond | Controllers return `void`  | Exact success statuses and error response formats |
| Ticket pagination | Uses Spring `Page<TicketResponse>` | Exact JSON structure and default page size in the deployed instance |
| Status/priority filters | Both are supported | Exact query encoding for multiple values and case handling in every deployed endpoint |
| Health endpoint | Controller describes UP/connected and DOWN/error responses | Actual access behavior and runtime status |
| CORS | Supplied config allows `http://localhost:5173` and `http://3.7.223.110` | Whether the deployed backend image actually contains this config and whether the security group allows the requests |
| Token persistence | JWT Bearer authentication is required | We have not yet selected or implemented frontend token storage |

The documented global exception handler maps `ResourceNotFoundException` to `404` and `RuntimeException` to `400`, but we should not assume every possible security, validation, or unexpected error follows those mappings. Authentication failures may have empty response bodies.

## 4. CORS and environment contract

The frontend API client will use:

```
const API_BASE_URL = "http://13.203.220.149:8080";
```

During local development, the browser origin is `http://localhost:5173`. Once deployed, it becomes `http://3.7.223.110`, assuming we serve the built frontend through Nginx on HTTP port 80.

Both origins are in the configuration you supplied. We should not add an origin for a different IP unless the actual serving address changes.

One deployment caveat: the browser calling the backend directly requires the backend security group to permit inbound port `8080` from the browser client's network. CORS and security-group access are separate requirements. We will verify connectivity after the frontend is deployed, as you planned.

## 5. My recommendation before coding

We have enough information to begin the frontend without inventing endpoint contracts. We do not need to change your backend or database.

We should implement against the confirmed DTOs and paths, explicitly handle `void` responses without attempting to parse JSON from them, and keep error handling tolerant of empty or non-JSON responses. Before relying on any uncertain behavior, we can test the relevant endpoint in Postman or the browser.

The next step is to create the React/Vite project in IntelliJ IDEA and build the API integration in small pieces, starting with authentication.

| Signup response | Controller returns `void` | Exact deployed HTTP status, response body, and error format |
| --- | --- | --- |

This response is for user and admin signup.          

```json
{
    "message": "Email already exists"
}
```

**Below same for user and admin signup if successfull no response is send this is confirmed . 
200 OK**

Login Errors —> 

| Login errors | Successful response contains `token` | Exact invalid-credentials status and error body |
| --- | --- | --- |

```json
{
    "message": "Invalid credentials"
}
```

Ticket Created with body an Without body both do not have any response body its empty Status 200 is returned after successfull response…

Ticket Update → If didnt logged in with the credentials that has access to a specific ticket id and try to update it then response body is empty Status 403 returned. 

  If the ticket entered in {id}   is not present .  

```json
{
    "message": "Ticket not found"
}
```

Close Ticket   :  if ticket is already  closed  Status→ 400 Bad request

```json
{
    "message": "Ticket already closed"
}
```

Ai Triage run on CLosed ticket  or if ticket is not present or created:  

```json
{
    "message": "Cannot run AI on closed ticket"
}

{
    "message": "Ticket not found"
}

successfull response below:->  200 status 

{
    "category": "TECHNICAL",
    "priority": "HIGH",
    "tags": [
        "bug",
        "broken",
        "urgent"
    ],
    "sentiment": "NEGATIVE",
    "summary": "User reports that something broke, indicating a bug in the system.",
    "replyDraft": "Hello,\n\nThank you for bringing this to our attention. I’m sorry you’re experiencing an issue. Could you please provide more details about what specifically broke, any error messages you saw, and the steps leading up to the problem? This information will help us investigate and resolve the issue as quickly as possible.\n\nWe appreciate your patience and look forward to fixing this for you.\n\nBest regards,\nSupport Team"
}
```

Triage save 
If ticket is closed Error response :→ 

```json
{
    "message": "Invalid ticket status for triage"
}

This endpoint needs Json Body compulsourly and if not given below error is shown 
{
    "message": "JSON parse error: No content to map due to end-of-input"
}
NOTE: -> But i do not want to show this to client  i will make changes inside the Frontend when this error is shown i want to display a user friendly error I will make changes in React and not backend.

 

on success response body is empty and status is 200
```

Respond endpoint  

```json
It needs Json bodya as below -> 

{
  "finalResponse": "On going",
  "resolutionNote": "On going"
}

If ticket is already closed-> 
{
    "message": "Ticket already closed"
}

success 200   no response body its empty.  
```

Ticket pagination response 

```json
{
    "content": [
        {
            "id": 9,
            "title": "Cannot upload photo",
            "description": "Uploading photo is displaying an error cannot uplaod.",
            "createdAt": "2026-10-26T05:18:04.011224",
            "status": "OPEN",
            "priority": "MEDIUM",
            "finalResponse": null,
            "resolutionNote": null
        },
        {
            "id": 10,
            "title": "Bug",
            "description": "Something broke",
            "createdAt": "2026-10-26T05:18:14.997885",
            "status": "OPEN",
            "priority": "LOW",
            "finalResponse": null,
            "resolutionNote": null
        }
    ],
    "pageable": {
        "pageNumber": 1,
        "pageSize": 2,
        "sort": {
            "sorted": false,
            "empty": true,
            "unsorted": true
        },
        "offset": 2,
        "paged": true,
        "unpaged": false
    },
    "last": false,
    "totalPages": 3,
    "totalElements": 6,
    "first": false,
    "size": 2,
    "number": 1,
    "sort": {
        "sorted": false,
        "empty": true,
        "unsorted": true
    },
    "numberOfElements": 2,
    "empty": false
}
```

This endpoing accepts 2 params key and value pair. 

 ||||  page = 1    |||| size = 2        
[http://13.203.220.149:8080/api/tickets?page=1&size=2](http://13.203.220.149:8080/api/tickets?page=1&size=2)

For CORS 

Whether the deployed backend image actually contains this config and whether the security group allows the requests
I have configured this as i have created the Elastic Ip for Frontend and its configured correctly to the backend we will verify this when we run the frontend container inside frontend ec2 instance we will check it then… 

| Token persistence | JWT Bearer authentication is required | We have not yet selected or implemented frontend token storage |
| --- | --- | --- |

I do not know about this when we start creating frontend  I will start to work on it.   
NOTE:→ Explicitly remind me when we reach a point in frontend developement where we nee to build the Storage.

## CORS and token storage

- CORS: We’ll verify the deployed configuration and connectivity when the frontend is running on the frontend EC2 instance. No further backend changes are planned right now.
- JWT token storage: We’ll decide and implement storage when the frontend reaches the authentication integration stage. I’ll explicitly remind you at that point, before we begin making authenticated API calls.