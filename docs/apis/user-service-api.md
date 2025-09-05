# User Service API Specification

## Overview

The User Service manages user accounts, authentication, and authorization for the DropSlot platform. This service handles user registration, login, profile management, and role-based access control.

```yaml
openapi: 3.0.3
info:
  title: User Service API
  description: User management and authentication service
  version: 1.0.0
  contact:
    name: User Service Team
servers:
  - url: http://user-service:8080/api/v1
    description: User Service (internal)
  - url: http://localhost:8081/api/v1
    description: User Service (local development)
```

## Service Responsibilities

- **User Registration**: Create new user accounts
- **Authentication**: JWT token generation and validation
- **Authorization**: Role-based access control
- **Profile Management**: User profile updates
- **Password Management**: Password reset and security
- **User Search**: Administrative user queries

## Database Schema

```sql
-- User Service Database: user_db
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

## API Endpoints

### Authentication

#### Register User
```yaml
paths:
  /auth/register:
    post:
      summary: Register a new user account
      tags: [Authentication]
      operationId: registerUser
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserRegistrationRequest'
      responses:
        '201':
          description: User registered successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the created user
        '400':
          $ref: '#/components/responses/ValidationError'
        '409':
          description: User already exists
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Login
```yaml
  /auth/login:
    post:
      summary: Authenticate user and generate tokens
      tags: [Authentication]
      operationId: authenticateUser
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                  format: email
                  description: User email address
                  example: user@example.com
                password:
                  type: string
                  minLength: 8
                  description: User password
                  example: securePassword123
              required:
                - email
                - password
      responses:
        '200':
          description: Authentication successful (only when account status is ACTIVE)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthenticationResponse'
        '401':
          description: Invalid credentials or account not active (email not verified)
          content:
            application/json:
              schema:
                type: object
                properties:
                  type:
                    type: string
                  title:
                    type: string
                  status:
                    type: integer
                  detail:
                    type: string
        '429':
          $ref: '#/components/responses/RateLimitError'
```

#### Refresh Token
```yaml
  /auth/refresh:
    post:
      summary: Refresh access token using refresh token
      tags: [Authentication]
      operationId: refreshToken
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                refreshToken:
                  type: string
                  description: Valid refresh token
                  example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
              required:
                - refreshToken
      responses:
        '200':
          description: Token refreshed successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                    description: New access token
                  refreshToken:
                    type: string
                    description: New refresh token
                  expiresIn:
                    type: integer
                    description: Access token expiration time in seconds
                    example: 3600
        '401':
          $ref: '#/components/responses/UnauthorizedError'
```

#### Password Reset Request
```yaml
  /auth/password/reset:
    post:
      summary: Request password reset
      tags: [Authentication]
      operationId: requestPasswordReset
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                  format: email
                  description: User email address
                  example: user@example.com
              required:
                - email
      responses:
        '202':
          description: Password reset email sent
        '400':
          $ref: '#/components/responses/ValidationError'
        '429':
          $ref: '#/components/responses/RateLimitError'
```

#### Verify Email
```yaml
  /auth/verify/send:
    post:
      summary: Send email verification code
      tags: [Authentication]
      operationId: sendVerification
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                  format: email
                  description: User email address
      responses:
        '200':
          description: Verification code sent
        '400':
          $ref: '#/components/responses/ValidationError'

  /auth/verify:
    post:
      summary: Verify user email address using a code
      tags: [Authentication]
      operationId: verifyEmail
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                email:
                  type: string
                  format: email
                code:
                  type: string
                  description: Verification code
              required:
                - email
                - code
      responses:
        '200':
          description: Email verified successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  message:
                    type: string
        '400':
          $ref: '#/components/responses/ValidationError'
        '410':
          description: Token expired
          content:
            application/json:
              schema:
                $ref: '#/components/responses/GoneError'
```

### User Management

#### Get Current User Profile
```yaml
  /me:
    get:
      summary: Get current authenticated user profile
      tags: [Users]
      operationId: getCurrentUser
      security:
        - bearerAuth: []
      responses:
        '200':
          description: User profile retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
```

#### Update Current User Profile
```yaml
    patch:
      summary: Update current user profile
      tags: [Users]
      operationId: updateCurrentUser
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                  minLength: 1
                  maxLength: 255
                  description: User's full name
                  example: "John Doe Updated"
                email:
                  type: string
                  format: email
                  description: User's email address
                  example: "newemail@example.com"
              additionalProperties: false
      responses:
        '200':
          description: User profile updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '409':
          description: Email already in use
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Change Password
```yaml
  /me/password:
    patch:
      summary: Change current user password
      tags: [Users]
      operationId: changePassword
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                currentPassword:
                  type: string
                  description: Current password for verification
                  example: "currentPassword123"
                newPassword:
                  type: string
                  minLength: 8
                  description: New password
                  example: "newSecurePassword456"
              required:
                - currentPassword
                - newPassword
              additionalProperties: false
      responses:
        '200':
          description: Password changed successfully
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          description: Incorrect current password
          content:
            application/json:
              schema:
                type: object
                properties:
                  type:
                    type: string
                    example: "https://api.dropslot.com/errors/forbidden"
                  title:
                    type: string
                    example: "Forbidden"
                  status:
                    type: integer
                    example: 403
                  detail:
                    type: string
                    example: "Current password is incorrect"
```

### Administrative Endpoints

#### List Users (Admin Only)
```yaml
  /users:
    get:
      summary: Get list of users (Admin only)
      tags: [Administration]
      operationId: listUsers
      security:
        - bearerAuth: []
      parameters:
        - name: email
          in: query
          schema:
            type: string
            format: email
          description: Filter by email
        - name: status
          in: query
          schema:
            type: string
            enum: [ACTIVE, INACTIVE, SUSPENDED]
          description: Filter by user status
        - name: role
          in: query
          schema:
            type: string
          description: Filter by role code
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
      responses:
        '200':
          description: Users retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/UserResponse'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer
                  size:
                    type: integer
                  number:
                    type: integer
                  first:
                    type: boolean
                  last:
                    type: boolean
        '403':
          $ref: '#/components/responses/ForbiddenError'
```

#### Get User by ID (Admin Only)
```yaml
  /users/{userId}:
    get:
      summary: Get user by ID (Admin only)
      tags: [Administration]
      operationId: getUserById
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: User ID
      responses:
        '200':
          description: User retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update User Roles (Admin Only)
```yaml
  /users/{userId}/roles:
    patch:
      summary: Update user roles (Admin only)
      tags: [Administration]
      operationId: updateUserRoles
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: User ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                roleCodes:
                  type: array
                  items:
                    type: string
                  description: List of role codes to assign
                  example: ["CUSTOMER", "STORE_MANAGER"]
                  minItems: 1
              required:
                - roleCodes
              additionalProperties: false
      responses:
        '200':
          description: User roles updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Suspend User (Admin Only)
```yaml
  /users/{userId}/suspend:
    post:
      summary: Suspend user account (Admin only)
      tags: [Administration]
      operationId: suspendUser
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: User ID
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                reason:
                  type: string
                  maxLength: 500
                  description: Reason for suspension
                  example: "Violation of terms of service"
                suspendUntil:
                  type: string
                  format: date-time
                  description: Suspension end date (optional)
                  example: "2024-12-31T23:59:59Z"
              required:
                - reason
              additionalProperties: false
      responses:
        '200':
          description: User suspended successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Reactivate User (Admin Only)
```yaml
  /users/{userId}/reactivate:
    post:
      summary: Reactivate suspended user (Admin only)
      tags: [Administration]
      operationId: reactivateUser
      security:
        - bearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: User ID
        - $ref: '#/components/parameters/Idempotency-Key'
      responses:
        '200':
          description: User reactivated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

## Data Models

### User Registration Request
```yaml
components:
  schemas:
    UserRegistrationRequest:
      type: object
      properties:
        email:
          type: string
          format: email
          description: User email address
          example: user@example.com
        password:
          type: string
          minLength: 8
          maxLength: 128
          description: User password (must contain uppercase, lowercase, number, special char)
          example: SecurePass123!
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: User's full name
          example: "John Doe"
        acceptTerms:
          type: boolean
          description: Terms of service acceptance
          example: true
      required:
        - email
        - password
        - name
        - acceptTerms
      additionalProperties: false
```

### User Response
```yaml
    UserResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Unique user identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        email:
          type: string
          format: email
          description: User email address
          example: user@example.com
        name:
          type: string
          description: User's full name
          example: "John Doe"
        status:
          type: string
          enum: [ACTIVE, INACTIVE, SUSPENDED]
          description: User account status
          example: ACTIVE
        emailVerifiedAt:
          type: string
          format: date-time
          nullable: true
          description: Email verification timestamp
          example: "2024-01-15T10:30:00Z"
        roles:
          type: array
          items:
            $ref: '#/components/schemas/RoleResponse'
          description: User roles
        createdAt:
          type: string
          format: date-time
          description: Account creation timestamp
          example: "2024-01-15T10:30:00Z"
        updatedAt:
          type: string
          format: date-time
          description: Last update timestamp
          example: "2024-01-15T10:30:00Z"
      required:
        - id
        - email
        - name
        - status
        - roles
        - createdAt
        - updatedAt
```

### Authentication Response
```yaml
    AuthenticationResponse:
      type: object
      properties:
        accessToken:
          type: string
          description: JWT access token
          example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        refreshToken:
          type: string
          description: JWT refresh token
          example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        user:
          $ref: '#/components/schemas/UserResponse'
        expiresIn:
          type: integer
          description: Access token expiration time in seconds
          example: 3600
        tokenType:
          type: string
          description: Token type
          example: Bearer
      required:
        - accessToken
        - refreshToken
        - user
        - expiresIn
        - tokenType
```

### Role Response
```yaml
    RoleResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Role identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        code:
          type: string
          description: Role code for programmatic use
          example: CUSTOMER
        name:
          type: string
          description: Human-readable role name
          example: "Customer"
      required:
        - id
        - code
        - name
```

## Security

### Authentication
```yaml
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT token for API authentication
```

### Authorization
- **Public Endpoints**: `/auth/register`, `/auth/login`, `/auth/password/reset`
- **User Endpoints**: `/me`, `/me/*` (authenticated users only)
- **Admin Endpoints**: `/users`, `/users/*` (admin role required)

## Error Responses

### Common Error Schemas
```yaml
  responses:
    ValidationError:
      description: Input validation failed
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/validation"
              title:
                type: string
                example: "Validation Error"
              status:
                type: integer
                example: 400
              detail:
                type: string
                example: "Input validation failed"
              instance:
                type: string
                example: "/api/v1/auth/register"
              errors:
                type: array
                items:
                  type: object
                  properties:
                    field:
                      type: string
                      example: "email"
                    message:
                      type: string
                      example: "Email format is invalid"
                    rejectedValue:
                      type: string
                      example: "invalid-email"
            required:
              - type
              - title
              - status
              - errors

    UnauthorizedError:
      description: Authentication required or invalid
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/unauthorized"
              title:
                type: string
                example: "Unauthorized"
              status:
                type: integer
                example: 401
              detail:
                type: string
                example: "Authentication required"

    ForbiddenError:
      description: Insufficient permissions
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/forbidden"
              title:
                type: string
                example: "Forbidden"
              status:
                type: integer
                example: 403
              detail:
                type: string
                example: "Insufficient permissions"

    NotFoundError:
      description: Resource not found
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/not-found"
              title:
                type: string
                example: "Not Found"
              status:
                type: integer
                example: 404
              detail:
                type: string
                example: "User not found"

    ConflictError:
      description: Resource conflict
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/conflict"
              title:
                type: string
                example: "Conflict"
              status:
                type: integer
                example: 409
              detail:
                type: string
                example: "Email already exists"

    RateLimitError:
      description: Rate limit exceeded
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/rate-limit"
              title:
                type: string
                example: "Too Many Requests"
              status:
                type: integer
                example: 429
              detail:
                type: string
                example: "Rate limit exceeded"
              retryAfter:
                type: integer
                description: Seconds to wait before retrying
                example: 60

    GoneError:
      description: Resource no longer available
      content:
        application/json:
          schema:
            type: object
            properties:
              type:
                type: string
                example: "https://api.dropslot.com/errors/gone"
              title:
                type: string
                example: "Gone"
              status:
                type: integer
                example: 410
              detail:
                type: string
                example: "Token has expired"
```

## Events Published

The User Service publishes events to Apache Kafka for other services to consume:

### User Events
- `user.registered` - New user account created
- `user.email.verified` - User email verified
- `user.profile.updated` - User profile modified
- `user.password.changed` - User password updated
- `user.suspended` - User account suspended
- `user.reactivated` - User account reactivated
- `user.role.assigned` - Role assigned to user
- `user.role.revoked` - Role revoked from user

### Event Schema Example
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "user.registered",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "user-service",
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174001",
    "email": "user@example.com",
    "name": "John Doe",
    "roles": ["CUSTOMER"]
  }
}
```

## Service Dependencies

### Consumes Events From
- None (User Service is a source of user events)

### Publishes Events To
- **Notification Service**: User registration, password reset
- **Analytics Service**: User metrics and behavior
- **Store Service**: User role changes for store access

## Monitoring & Health Checks

### Health Check Endpoint
```yaml
paths:
  /actuator/health:
    get:
      summary: Service health check
      responses:
        '200':
          description: Service is healthy
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                    example: "UP"
                  components:
                    type: object
                    properties:
                      db:
                        type: object
                        properties:
                          status:
                            type: string
                            example: "UP"
                      redis:
                        type: object
                        properties:
                          status:
                            type: string
                            example: "UP"
```

### Metrics Endpoints
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/info` - Application information

## Configuration

### Environment Variables
```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=user_db
DB_USERNAME=user_service
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Service Configuration
SERVER_PORT=8080
LOG_LEVEL=INFO
```

## Development Guidelines

### Code Style
- Follow Spring Boot conventions
- Use constructor injection for dependencies
- Implement comprehensive error handling
- Write unit and integration tests

### Security Best Practices
- Hash passwords with BCrypt
- Validate all input data
- Implement rate limiting
- Use parameterized queries
- Log security events

### Performance Considerations
- Implement caching for frequently accessed data
- Use database indexes appropriately
- Monitor query performance
- Implement connection pooling

---

**User Service API v1.0.0**
**Last Updated**: December 2024
**Contact**: user-service-team@dropslot.com