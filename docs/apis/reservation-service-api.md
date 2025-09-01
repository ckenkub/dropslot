# Reservation Service API Specification

## Overview

The Reservation Service manages the core booking system for DropSlot, including reservation creation, waitlist management, capacity tracking, and check-in functionality.

```yaml
openapi: 3.0.3
info:
  title: Reservation Service API
  description: Booking system and reservation management service
  version: 1.0.0
  contact:
    name: Reservation Service Team
servers:
  - url: http://reservation-service:8080/api/v1
    description: Reservation Service (internal)
  - url: http://localhost:8085/api/v1
    description: Reservation Service (local development)
```

## Service Responsibilities

- **Reservation Management** - Create, update, and cancel reservations
- **Capacity Tracking** - Real-time capacity management and availability
- **Waitlist Management** - Queue management and auto-promotion
- **Check-in System** - QR code validation and attendance tracking
- **Business Rules** - Enforce reservation policies and limits
- **Real-time Updates** - Live capacity and availability information

## Database Schema

```sql
-- Reservation Service Database: reservation_db
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    slot_id UUID NOT NULL,
    user_id UUID NOT NULL,
    state VARCHAR(20) DEFAULT 'PENDING',
    qr_code_url VARCHAR(500),
    customer_note TEXT,
    reservation_number VARCHAR(50) UNIQUE,
    priority_score INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    checked_in_at TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE(user_id, slot_id)
);

CREATE TABLE waitlist (
    id UUID PRIMARY KEY,
    slot_id UUID NOT NULL,
    user_id UUID NOT NULL,
    position INTEGER NOT NULL,
    state VARCHAR(20) DEFAULT 'WAITING',
    priority_score INTEGER DEFAULT 0,
    notification_sent BOOLEAN DEFAULT false,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, slot_id)
);

CREATE TABLE reservation_events (
    id UUID PRIMARY KEY,
    reservation_id UUID REFERENCES reservations(id),
    event_type VARCHAR(50) NOT NULL,
    old_state VARCHAR(20),
    new_state VARCHAR(20),
    metadata JSONB,
    created_by UUID,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE capacity_snapshots (
    id UUID PRIMARY KEY,
    slot_id UUID NOT NULL,
    capacity_total INTEGER NOT NULL,
    capacity_reserved INTEGER NOT NULL,
    capacity_waitlist INTEGER NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(slot_id, snapshot_time)
);

CREATE INDEX idx_reservations_slot_id ON reservations(slot_id);
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_state ON reservations(state);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);
CREATE INDEX idx_waitlist_slot_id ON waitlist(slot_id);
CREATE INDEX idx_waitlist_user_id ON waitlist(user_id);
CREATE INDEX idx_waitlist_position ON waitlist(slot_id, position);
```

## API Endpoints

### Reservation Management

#### Create Reservation
```yaml
paths:
  /reservations:
    post:
      summary: Create a new reservation
      tags: [Reservations]
      operationId: createReservation
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
                  example: "123e4567-e89b-12d3-a456-426614174000"
                customerNote:
                  type: string
                  maxLength: 500
                  description: Optional customer note
                  example: "Please call when ready"
              required:
                - slotId
              additionalProperties: false
      responses:
        '201':
          description: Reservation created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the reservation
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '409':
          description: Slot fully booked or user already has reservation
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
        '429':
          $ref: '#/components/responses/RateLimitError'
```

#### Get Reservation by ID
```yaml
  /reservations/{reservationId}:
    get:
      summary: Get reservation details
      tags: [Reservations]
      operationId: getReservationById
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Reservation ID
      responses:
        '200':
          description: Reservation retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Get User Reservations
```yaml
  /reservations:
    get:
      summary: Get current user's reservations
      tags: [Reservations]
      operationId: getUserReservations
      security:
        - bearerAuth: []
      parameters:
        - name: status
          in: query
          schema:
            type: string
            enum: [PENDING, CONFIRMED, CHECKED_IN, CANCELLED, EXPIRED]
          description: Filter by reservation status
        - name: slotId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by slot ID
        - name: dropId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by drop ID
        - name: upcomingOnly
          in: query
          schema:
            type: boolean
            default: false
          description: Show only upcoming reservations
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
      responses:
        '200':
          description: User reservations retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ReservationResponse'
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
        '401':
          $ref: '#/components/responses/UnauthorizedError'
```

#### Cancel Reservation
```yaml
  /reservations/{reservationId}/cancel:
    post:
      summary: Cancel a reservation
      tags: [Reservations]
      operationId: cancelReservation
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Reservation ID
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
                  description: Cancellation reason
                  example: "Cannot attend due to schedule conflict"
                notifyWaitlist:
                  type: boolean
                  default: true
                  description: Notify waitlist users of available spot
              required:
                - reason
              additionalProperties: false
      responses:
        '200':
          description: Reservation cancelled successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Reservation cannot be cancelled at this time
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Confirm Reservation
```yaml
  /reservations/{reservationId}/confirm:
    post:
      summary: Confirm a pending reservation
      tags: [Reservations]
      operationId: confirmReservation
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Reservation ID
        - $ref: '#/components/parameters/Idempotency-Key'
      responses:
        '200':
          description: Reservation confirmed successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Reservation cannot be confirmed
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

### Waitlist Management

#### Join Waitlist
```yaml
  /waitlist:
    post:
      summary: Join waitlist for a slot
      tags: [Waitlist]
      operationId: joinWaitlist
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
                  example: "123e4567-e89b-12d3-a456-426614174000"
                priority:
                  type: string
                  enum: [LOW, MEDIUM, HIGH, VIP]
                  default: "MEDIUM"
                  description: Waitlist priority level
              required:
                - slotId
              additionalProperties: false
      responses:
        '201':
          description: Joined waitlist successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WaitlistResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the waitlist entry
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '409':
          description: Already on waitlist or slot has availability
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Waitlist Position
```yaml
  /waitlist/{waitlistId}:
    get:
      summary: Get waitlist entry details
      tags: [Waitlist]
      operationId: getWaitlistEntry
      security:
        - bearerAuth: []
      parameters:
        - name: waitlistId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Waitlist entry ID
      responses:
        '200':
          description: Waitlist entry retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WaitlistResponse'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Get Slot Waitlist
```yaml
  /waitlist:
    get:
      summary: Get waitlist for a slot
      tags: [Waitlist]
      operationId: getSlotWaitlist
      security:
        - bearerAuth: []
      parameters:
        - name: slotId
          in: query
          required: true
          schema:
            type: string
            format: uuid
          description: Slot ID
        - name: includeUserDetails
          in: query
          schema:
            type: boolean
            default: false
          description: Include user details (admin only)
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
      responses:
        '200':
          description: Waitlist retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  slotId:
                    type: string
                    format: uuid
                  totalWaitlist:
                    type: integer
                  waitlistEntries:
                    type: array
                    items:
                      $ref: '#/components/schemas/WaitlistEntryResponse'
                  pagination:
                    type: object
                    properties:
                      totalElements:
                        type: integer
                      totalPages:
                        type: integer
                      size:
                          type: integer
                      number:
                        type: integer
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Leave Waitlist
```yaml
  /waitlist/{waitlistId}:
    delete:
      summary: Leave waitlist
      tags: [Waitlist]
      operationId: leaveWaitlist
      security:
        - bearerAuth: []
      parameters:
        - name: waitlistId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Waitlist entry ID
      responses:
        '204':
          description: Left waitlist successfully
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Check-in System

#### Get QR Code
```yaml
  /reservations/{reservationId}/qr:
    get:
      summary: Get QR code for reservation check-in
      tags: [CheckIn]
      operationId: getReservationQR
      security:
        - bearerAuth: []
      parameters:
        - name: reservationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Reservation ID
        - name: size
          in: query
          schema:
            type: integer
            minimum: 100
            maximum: 1000
            default: 300
          description: QR code size in pixels
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
                  reservationNumber:
                    type: string
                    description: Human-readable reservation number
                  expiresAt:
                    type: string
                    format: date-time
                    description: QR code expiration time
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Validate QR Code
```yaml
  /checkin/validate:
    post:
      summary: Validate QR code for check-in
      tags: [CheckIn]
      operationId: validateQRCode
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                qrCode:
                  type: string
                  description: QR code content or reservation ID
                  example: "123e4567-e89b-12d3-a456-426614174000"
                slotId:
                  type: string
                  format: uuid
                  description: Slot ID for validation
                  example: "123e4567-e89b-12d3-a456-426614174001"
              required:
                - qrCode
                - slotId
              additionalProperties: false
      responses:
        '200':
          description: QR code validated successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  valid:
                    type: boolean
                    example: true
                  reservation:
                    $ref: '#/components/schemas/ReservationResponse'
                  canCheckIn:
                    type: boolean
                    example: true
                  message:
                    type: string
                    example: "Valid reservation for this slot"
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          description: Invalid QR code or reservation not found
          content:
            application/json:
              schema:
                $ref: '#/components/responses/NotFoundError'
```

#### Check In Reservation
```yaml
  /checkin:
    post:
      summary: Check in reservation (Store Manager only)
      tags: [CheckIn]
      operationId: checkInReservation
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
                  example: "123e4567-e89b-12d3-a456-426614174000"
                notes:
                  type: string
                  maxLength: 500
                  description: Check-in notes
                  example: "Customer arrived 5 minutes early"
              required:
                - reservationId
              additionalProperties: false
      responses:
        '200':
          description: Check-in successful
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '401':
          $ref: '#/components/responses/UnauthorizedError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Reservation already checked in or invalid state
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

### Capacity Management

#### Get Slot Capacity
```yaml
  /slots/{slotId}/capacity:
    get:
      summary: Get real-time slot capacity information
      tags: [Capacity]
      operationId: getSlotCapacity
      parameters:
        - name: slotId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Slot ID
      responses:
        '200':
          description: Capacity information retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  slotId:
                    type: string
                    format: uuid
                  capacityTotal:
                    type: integer
                  capacityReserved:
                    type: integer
                  capacityWaitlist:
                    type: integer
                  availableSpots:
                    type: integer
                  waitlistEnabled:
                    type: boolean
                  estimatedWaitTime:
                    type: integer
                    nullable: true
                    description: Estimated wait time in minutes
                  nextPromotionTime:
                    type: string
                    format: date-time
                    nullable: true
                    description: When next waitlist promotion might occur
                  lastUpdated:
                    type: string
                    format: date-time
                example:
                  slotId: "123e4567-e89b-12d3-a456-426614174000"
                  capacityTotal: 100
                  capacityReserved: 75
                  capacityWaitlist: 25
                  availableSpots: 0
                  waitlistEnabled: true
                  estimatedWaitTime: 30
                  nextPromotionTime: "2024-01-15T11:00:00Z"
                  lastUpdated: "2024-01-15T10:30:00Z"
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Bulk Reservation Operations
```yaml
  /reservations/bulk/cancel:
    post:
      summary: Bulk cancel reservations (Admin only)
      tags: [Reservations]
      operationId: bulkCancelReservations
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
                reservationIds:
                  type: array
                  items:
                    type: string
                    format: uuid
                  minItems: 1
                  maxItems: 100
                  description: List of reservation IDs to cancel
                reason:
                  type: string
                  maxLength: 500
                  description: Cancellation reason
                  example: "Event cancelled due to unforeseen circumstances"
                notifyCustomers:
                  type: boolean
                  default: true
                  description: Send cancellation notifications
              required:
                - reservationIds
                - reason
              additionalProperties: false
      responses:
        '200':
          description: Bulk cancellation processed
          content:
            application/json:
              schema:
                type: object
                properties:
                  successCount:
                    type: integer
                    description: Number of successful cancellations
                    example: 95
                  failureCount:
                    type: integer
                    description: Number of failed cancellations
                    example: 5
                  failures:
                    type: array
                    items:
                      type: object
                      properties:
                        reservationId:
                          type: string
                          format: uuid
                        error:
                          type: string
                        message:
                          type: string
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
```

## Data Models

### Reservation Response
```yaml
components:
  schemas:
    ReservationResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Reservation unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        slotId:
          type: string
          format: uuid
          description: Slot identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        userId:
          type: string
          format: uuid
          description: User identifier
          example: "123e4567-e89b-12d3-a456-426614174002"
        state:
          type: string
          enum: [PENDING, CONFIRMED, CHECKED_IN, CANCELLED, EXPIRED]
          description: Reservation state
          example: CONFIRMED
        qrCodeUrl:
          type: string
          format: uri
          nullable: true
          description: QR code URL for check-in
          example: "https://api.dropslot.com/qr/abc123def456"
        customerNote:
          type: string
          nullable: true
          description: Customer note
          example: "Please call when ready"
        reservationNumber:
          type: string
          description: Human-readable reservation number
          example: "RSV-2024-001-ABC123"
        priorityScore:
          type: integer
          description: Reservation priority score
          example: 100
        createdAt:
          type: string
          format: date-time
          description: Creation timestamp
          example: "2024-01-15T10:30:00Z"
        confirmedAt:
          type: string
          format: date-time
          nullable: true
          description: Confirmation timestamp
          example: "2024-01-15T10:35:00Z"
        cancelledAt:
          type: string
          format: date-time
          nullable: true
          description: Cancellation timestamp
          example: "2024-01-15T11:00:00Z"
        checkedInAt:
          type: string
          format: date-time
          nullable: true
          description: Check-in timestamp
          example: "2024-01-15T12:15:00Z"
        expiresAt:
          type: string
          format: date-time
          nullable: true
          description: Expiration timestamp
          example: "2024-01-15T12:00:00Z"
      required:
        - id
        - slotId
        - userId
        - state
        - reservationNumber
        - priorityScore
        - createdAt
```

### Waitlist Response
```yaml
    WaitlistResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Waitlist entry unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        slotId:
          type: string
          format: uuid
          description: Slot identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        userId:
          type: string
          format: uuid
          description: User identifier
          example: "123e4567-e89b-12d3-a456-426614174002"
        position:
          type: integer
          description: Waitlist position
          example: 5
        state:
          type: string
          enum: [WAITING, PROMOTED, EXPIRED, CANCELLED]
          description: Waitlist entry state
          example: WAITING
        priorityScore:
          type: integer
          description: Priority score for promotion
          example: 150
        notificationSent:
          type: boolean
          description: Whether promotion notification was sent
          example: false
        expiresAt:
          type: string
          format: date-time
          nullable: true
          description: Waitlist entry expiration
          example: "2024-01-15T18:00:00Z"
        estimatedPromotionTime:
          type: string
          format: date-time
          nullable: true
          description: Estimated promotion time
          example: "2024-01-15T11:30:00Z"
        createdAt:
          type: string
          format: date-time
          description: Creation timestamp
          example: "2024-01-15T10:30:00Z"
        updatedAt:
          type: string
          format: date-time
          description: Last update timestamp
          example: "2024-01-15T10:30:00Z"
      required:
        - id
        - slotId
        - userId
        - position
        - state
        - priorityScore
        - notificationSent
        - createdAt
        - updatedAt
```

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
                example: "/api/v1/reservations"
              errors:
                type: array
                items:
                  type: object
                  properties:
                    field:
                      type: string
                      example: "slotId"
                    message:
                      type: string
                      example: "Slot ID is required"
                    rejectedValue:
                      type: string
                      example: null
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
                example: "Reservation not found"

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
```

## Events Published

The Reservation Service publishes events to Apache Kafka for other services to consume:

### Reservation Events
- `reservation.created` - New reservation created
- `reservation.confirmed` - Reservation confirmed
- `reservation.cancelled` - Reservation cancelled
- `reservation.expired` - Reservation expired
- `reservation.checked_in` - Customer checked in

### Waitlist Events
- `waitlist.joined` - User joined waitlist
- `waitlist.promoted` - User promoted from waitlist
- `waitlist.expired` - Waitlist entry expired
- `waitlist.cancelled` - User left waitlist

### Capacity Events
- `capacity.updated` - Slot capacity changed
- `capacity.full` - Slot reached full capacity
- `capacity.available` - Spots became available

### Event Schema Example
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "reservation.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "reservation-service",
  "data": {
    "reservationId": "123e4567-e89b-12d3-a456-426614174001",
    "slotId": "123e4567-e89b-12d3-a456-426614174002",
    "userId": "123e4567-e89b-12d3-a456-426614174003",
    "state": "PENDING",
    "reservationNumber": "RSV-2024-001-ABC123"
  }
}
```

## Service Dependencies

### Consumes Events From
- **Drop Service**: `drop.activated`, `drop.cancelled`, `slot.created`, `slot.updated`
- **User Service**: `user.registered`, `user.suspended`
- **Store Service**: `store.activated`, `store.deactivated`

### Publishes Events To
- **Notification Service**: All reservation and waitlist events
- **Analytics Service**: Reservation metrics and business events
- **Payment Service**: Reservation events for payment processing
- **Drop Service**: Capacity updates for slot management

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
                      kafka:
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
DB_NAME=reservation_db
DB_USERNAME=reservation_service
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=reservation-service

# Service Configuration
SERVER_PORT=8080
LOG_LEVEL=INFO

# Reservation Configuration
DEFAULT_RESERVATION_TIMEOUT_MINUTES=30
MAX_RESERVATIONS_PER_USER=5
RESERVATION_NUMBER_PREFIX=RSV
AUTO_CONFIRM_RESERVATIONS=true
AUTO_EXPIRE_RESERVATIONS=true

# Waitlist Configuration
MAX_WAITLIST_SIZE=1000
WAITLIST_EXPIRY_HOURS=24
AUTO_PROMOTE_WAITLIST=true
WAITLIST_PRIORITY_ENABLED=true

# Check-in Configuration
QR_CODE_EXPIRY_MINUTES=60
CHECK_IN_GRACE_PERIOD_MINUTES=15
ALLOW_EARLY_CHECK_IN=true
```

## Development Guidelines

### Code Style
- Follow Spring Boot conventions
- Use constructor injection for dependencies
- Implement comprehensive error handling
- Write unit and integration tests

### Reservation Logic
- Implement atomic reservation operations
- Use database transactions for consistency
- Handle concurrent reservation attempts
- Implement proper timeout handling

### Waitlist Management
- Implement fair queue ordering
- Handle priority-based promotion
- Implement automatic expiration
- Send timely notifications

### Capacity Management
- Implement real-time capacity tracking
- Use optimistic locking for updates
- Implement proper caching strategies
- Monitor capacity utilization

---

**Reservation Service API v1.0.0**
**Last Updated**: December 2024
**Contact**: reservation-service-team@dropslot.com