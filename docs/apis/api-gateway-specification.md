# DropSlot API Gateway Specification

## Overview

This document defines the **external API** for the DropSlot platform, accessed through the API Gateway. This unified API provides a simple, consistent interface for all client applications while internally routing requests to the appropriate microservices.

```yaml
openapi: 3.0.3
info:
  title: DropSlot API Gateway
  description: Unified external API for DropSlot e-commerce reservation platform
  version: 1.0.0
  contact:
    name: DropSlot API Team
    email: api@dropslot.com
servers:
  - url: https://api.dropslot.com/v1
    description: Production API Gateway
  - url: https://staging-api.dropslot.com/v1
    description: Staging API Gateway
  - url: http://localhost:8080/api/v1
    description: Local development
```

## Architecture

### API Gateway Responsibilities
- **Request Routing**: Route requests to appropriate microservices
- **Authentication**: JWT token validation and user context
- **Rate Limiting**: Protect services from abuse
- **Load Balancing**: Distribute traffic across service instances
- **Response Transformation**: Normalize responses from different services
- **Security**: CORS, input validation, security headers
- **Monitoring**: Request/response logging and metrics

### Service Routing

```yaml
# API Gateway Routing Configuration
routes:
  - service: user-service
    paths: ["/auth/*", "/users", "/me"]
  - service: store-service
    paths: ["/stores", "/branches"]
  - service: product-service
    paths: ["/products"]
  - service: drop-service
    paths: ["/drops", "/slots"]
  - service: reservation-service
    paths: ["/reservations", "/waitlist", "/tickets"]
  - service: notification-service
    paths: ["/notifications"]
  - service: payment-service
    paths: ["/payments"]
  - service: analytics-service
    paths: ["/analytics"]
```

## Authentication

All API requests require authentication except for user registration and login.

```yaml
security:
  - bearerAuth: []
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT token obtained from /auth/login
```

### Authentication Headers
```
Authorization: Bearer <jwt_token>
X-API-Key: <api_key>  # For service-to-service calls
X-Request-ID: <uuid>  # For request tracing
```

## Common Parameters

### Pagination
```yaml
parameters:
  page:
    name: page
    in: query
    schema:
      type: integer
      minimum: 0
      default: 0
    description: Page number (0-based)
  size:
    name: size
    in: query
    schema:
      type: integer
      minimum: 1
      maximum: 100
      default: 20
    description: Number of items per page
  sort:
    name: sort
    in: query
    schema:
      type: string
    description: Sort criteria (e.g., "createdAt,desc")
```

### Idempotency
```yaml
parameters:
  Idempotency-Key:
    name: Idempotency-Key
    in: header
    schema:
      type: string
      format: uuid
    required: false
    description: Unique key for idempotent requests
```

## API Endpoints

### Authentication (User Service)

#### Register User
```yaml
paths:
  /auth/register:
    post:
      summary: Register a new user account
      tags: [Authentication]
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
                  example: user@example.com
                password:
                  type: string
                  minLength: 8
                  example: "securePassword123"
                name:
                  type: string
                  example: "John Doe"
              required:
                - email
                - password
                - name
      responses:
        '201':
          description: User registered successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '409':
          description: User already exists
```

#### Login
```yaml
  /auth/login:
    post:
      summary: Authenticate user and get tokens
      tags: [Authentication]
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
                password:
                  type: string
              required:
                - email
                - password
      responses:
        '200':
          description: Authentication successful
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                    example: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                  refreshToken:
                    type: string
                    example: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                  user:
                    $ref: '#/components/schemas/UserResponse'
                  expiresIn:
                    type: integer
                    example: 3600
        '401':
          $ref: '#/components/responses/UnauthorizedError'
```

#### Refresh Token
```yaml
  /auth/refresh:
    post:
      summary: Refresh access token
      tags: [Authentication]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                refreshToken:
                  type: string
                  example: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
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
                  refreshToken:
                    type: string
                  expiresIn:
                    type: integer
```

### User Management (User Service)

#### Get Current User
```yaml
  /me:
    get:
      summary: Get current user profile
      tags: [Users]
      security:
        - bearerAuth: []
      responses:
        '200':
          description: User profile retrieved
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
```

#### Update Current User
```yaml
    patch:
      summary: Update current user profile
      tags: [Users]
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
                  example: "John Doe Updated"
                email:
                  type: string
                  format: email
                  example: "newemail@example.com"
      responses:
        '200':
          description: User updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
```

### Store Management (Store Service)

#### List Stores
```yaml
  /stores:
    get:
      summary: Get list of stores
      tags: [Stores]
      parameters:
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
        - name: search
          in: query
          schema:
            type: string
          description: Search stores by name or description
      responses:
        '200':
          description: Stores retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/StoreResponse'
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
```

#### Create Store
```yaml
    post:
      summary: Create a new store
      tags: [Stores]
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                  example: "Downtown Sneaker Store"
                slug:
                  type: string
                  example: "downtown-sneakers"
                tenantKey:
                  type: string
                  example: "store_001"
                description:
                  type: string
                  example: "Premium sneaker boutique in downtown"
                logoUrl:
                  type: string
                  format: uri
                  example: "https://example.com/logo.png"
              required:
                - name
                - slug
                - tenantKey
      responses:
        '201':
          description: Store created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StoreResponse'
```

#### Get Store Details
```yaml
  /stores/{storeId}:
    get:
      summary: Get store details
      tags: [Stores]
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Store details retrieved
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StoreResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Product Management (Product Service)

#### List Products
```yaml
  /products:
    get:
      summary: Get list of products
      tags: [Products]
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store
        - name: category
          in: query
          schema:
            type: string
          description: Filter by category
        - name: search
          in: query
          schema:
            type: string
          description: Search products
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
      responses:
        '200':
          description: Products retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ProductResponse'
```

#### Create Product
```yaml
    post:
      summary: Create a new product
      tags: [Products]
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                storeId:
                  type: string
                  format: uuid
                name:
                  type: string
                  example: "Air Jordan 1 High"
                sku:
                  type: string
                  example: "AJ1-HIGH-001"
                description:
                  type: string
                  example: "Classic basketball sneaker"
                priceCents:
                  type: integer
                  example: 17000
                currency:
                  type: string
                  default: "USD"
                category:
                  type: string
                  example: "Sneakers"
                images:
                  type: array
                  items:
                    type: string
                    format: uri
                  example: ["https://example.com/image1.jpg"]
              required:
                - storeId
                - name
                - sku
                - priceCents
      responses:
        '201':
          description: Product created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
```

### Drop Management (Drop Service)

#### List Drops
```yaml
  /drops:
    get:
      summary: Get list of drops
      tags: [Drops]
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store
        - name: status
          in: query
          schema:
            type: string
            enum: [DRAFT, ACTIVE, CANCELLED, COMPLETED]
          description: Filter by status
        - name: startDate
          in: query
          schema:
            type: string
            format: date
          description: Filter by start date
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
      responses:
        '200':
          description: Drops retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/DropResponse'
```

#### Create Drop
```yaml
    post:
      summary: Create a new drop
      tags: [Drops]
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                storeId:
                  type: string
                  format: uuid
                productId:
                  type: string
                  format: uuid
                name:
                  type: string
                  example: "Holiday Sneaker Drop"
                description:
                  type: string
                  example: "Limited release holiday collection"
                startsAt:
                  type: string
                  format: date-time
                  example: "2024-12-25T10:00:00Z"
                endsAt:
                  type: string
                  format: date-time
                  example: "2024-12-25T18:00:00Z"
                reservationRule:
                  type: object
                  description: Custom reservation rules
              required:
                - storeId
                - productId
                - name
                - startsAt
                - endsAt
      responses:
        '201':
          description: Drop created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
```

### Reservation System (Reservation Service)

#### Create Reservation
```yaml
  /reservations:
    post:
      summary: Create a new reservation
      tags: [Reservations]
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                slotId:
                  type: string
                  format: uuid
                  description: The slot to reserve
                customerNote:
                  type: string
                  maxLength: 500
                  description: Optional customer note
              required:
                - slotId
      responses:
        '201':
          description: Reservation created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '409':
          description: Slot fully booked or user already has reservation
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Cancel Reservation
```yaml
  /reservations/{reservationId}/cancel:
    post:
      summary: Cancel a reservation
      tags: [Reservations]
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Reservation cancelled successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
```

#### Join Waitlist
```yaml
  /waitlist:
    post:
      summary: Join waitlist for a slot
      tags: [Waitlist]
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                slotId:
                  type: string
                  format: uuid
                  description: The slot to join waitlist for
              required:
                - slotId
      responses:
        '201':
          description: Joined waitlist successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WaitlistResponse'
```

### Check-in System (Reservation Service)

#### Get QR Code
```yaml
  /tickets/{reservationId}/qr:
    get:
      summary: Get QR code for reservation check-in
      tags: [Tickets]
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: QR code generated successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  qrCodeUrl:
                    type: string
                    format: uri
                    description: URL to QR code image
                  reservationId:
                    type: string
                    format: uuid
```

#### Check In
```yaml
  /checkin:
    post:
      summary: Check in reservation (Store Manager only)
      tags: [CheckIn]
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                reservationId:
                  type: string
                  format: uuid
                  description: Reservation to check in
              required:
                - reservationId
      responses:
        '200':
          description: Check-in successful
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
```

## Data Models

### User Response
```yaml
components:
  schemas:
    UserResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        name:
          type: string
        status:
          type: string
          enum: [ACTIVE, INACTIVE, SUSPENDED]
        emailVerifiedAt:
          type: string
          format: date-time
          nullable: true
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - id
        - email
        - name
        - status
        - createdAt
        - updatedAt
```

### Store Response
```yaml
    StoreResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        slug:
          type: string
        tenantKey:
          type: string
        description:
          type: string
          nullable: true
        logoUrl:
          type: string
          format: uri
          nullable: true
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - id
        - name
        - slug
        - tenantKey
        - createdAt
        - updatedAt
```

### Product Response
```yaml
    ProductResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        storeId:
          type: string
          format: uuid
        name:
          type: string
        sku:
          type: string
        description:
          type: string
          nullable: true
        priceCents:
          type: integer
        currency:
          type: string
        category:
          type: string
          nullable: true
        images:
          type: array
          items:
            type: string
            format: uri
          nullable: true
        isActive:
          type: boolean
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - id
        - storeId
        - name
        - sku
        - priceCents
        - currency
        - isActive
        - createdAt
        - updatedAt
```

### Drop Response
```yaml
    DropResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        storeId:
          type: string
          format: uuid
        productId:
          type: string
          format: uuid
        name:
          type: string
        description:
          type: string
          nullable: true
        startsAt:
          type: string
          format: date-time
        endsAt:
          type: string
          format: date-time
        status:
          type: string
          enum: [DRAFT, ACTIVE, CANCELLED, COMPLETED]
        slots:
          type: array
          items:
            $ref: '#/components/schemas/SlotResponse'
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - id
        - storeId
        - productId
        - name
        - startsAt
        - endsAt
        - status
        - createdAt
        - updatedAt
```

### Reservation Response
```yaml
    ReservationResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        slotId:
          type: string
          format: uuid
        userId:
          type: string
          format: uuid
        state:
          type: string
          enum: [PENDING, CONFIRMED, CHECKED_IN, CANCELLED, EXPIRED]
        qrCodeUrl:
          type: string
          format: uri
          nullable: true
        customerNote:
          type: string
          nullable: true
        createdAt:
          type: string
          format: date-time
        confirmedAt:
          type: string
          format: date-time
          nullable: true
        cancelledAt:
          type: string
          format: date-time
          nullable: true
        checkedInAt:
          type: string
          format: date-time
          nullable: true
      required:
        - id
        - slotId
        - userId
        - state
        - createdAt
```

## Error Responses

### Common Error Response
```yaml
  responses:
    ValidationError:
      description: Validation error
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
                example: "Invalid input data"
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
                    message:
                      type: string
                    rejectedValue:
                      type: string
            required:
              - type
              - title
              - status

    UnauthorizedError:
      description: Authentication required
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
                example: "Resource not found"

    ConflictError:
      description: Business rule violation
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
                example: "Slot fully booked"
```

## Rate Limiting

The API Gateway implements rate limiting to protect services:

- **Public endpoints**: 100 requests per minute per IP
- **Authenticated users**: 1000 requests per minute per user
- **Store managers**: 2000 requests per minute per user
- **Administrators**: 5000 requests per minute per user

### Rate Limit Headers
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995200
X-RateLimit-Retry-After: 60
```

## Versioning

- **Current Version**: v1
- **Version Header**: `Accept: application/vnd.dropslot.v1+json`
- **Breaking Changes**: New major version (v2, v3, etc.)
- **Backward Compatibility**: Maintained within major versions

## Support

- **API Status**: https://status.dropslot.com
- **Documentation**: https://docs.dropslot.com
- **Support**: support@dropslot.com
- **Rate Limits**: Check `X-RateLimit-*` headers

---

**API Gateway Specification v1.0.0**
**Last Updated**: December 2024