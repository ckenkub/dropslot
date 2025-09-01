# Drop Service API Specification

## Overview

The Drop Service manages time-boxed product releases, including drop scheduling, capacity management, and slot allocation for the DropSlot platform.

```yaml
openapi: 3.0.3
info:
  title: Drop Service API
  description: Drop scheduling and capacity management service
  version: 1.0.0
  contact:
    name: Drop Service Team
servers:
  - url: http://drop-service:8080/api/v1
    description: Drop Service (internal)
  - url: http://localhost:8084/api/v1
    description: Drop Service (local development)
```

## Service Responsibilities

- **Drop Lifecycle Management** - Create, update, and manage product drops
- **Capacity Management** - Handle slot allocation and capacity limits
- **Scheduling** - Time-based drop management and calendar integration
- **Reservation Rules** - Configurable booking policies and restrictions
- **Performance Analytics** - Drop performance tracking and reporting

## Database Schema

```sql
-- Drop Service Database: drop_db
CREATE TABLE drops (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',
    reservation_rule JSONB,
    max_reservations_per_user INTEGER DEFAULT 1,
    allow_waitlist BOOLEAN DEFAULT true,
    auto_promote_waitlist BOOLEAN DEFAULT true,
    notify_on_start BOOLEAN DEFAULT true,
    notify_on_end BOOLEAN DEFAULT false,
    metadata JSONB,
    created_by UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE slots (
    id UUID PRIMARY KEY,
    drop_id UUID REFERENCES drops(id),
    name VARCHAR(255),
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity_total INTEGER NOT NULL,
    capacity_reserved INTEGER DEFAULT 0,
    capacity_waitlist INTEGER DEFAULT 0,
    price_modifier_cents INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE drop_analytics (
    id UUID PRIMARY KEY,
    drop_id UUID REFERENCES drops(id),
    total_views INTEGER DEFAULT 0,
    total_reservations INTEGER DEFAULT 0,
    total_waitlist INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5,2) DEFAULT 0.00,
    avg_reservation_time INTEGER, -- seconds
    peak_concurrent_users INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(drop_id, created_at::date)
);

CREATE INDEX idx_drops_store_id ON drops(store_id);
CREATE INDEX idx_drops_product_id ON drops(product_id);
CREATE INDEX idx_drops_status ON drops(status);
CREATE INDEX idx_drops_time_window ON drops(starts_at, ends_at);
CREATE INDEX idx_slots_drop_id ON slots(drop_id);
CREATE INDEX idx_slots_time_window ON slots(start_time, end_time);
```

## API Endpoints

### Drop Management

#### Create Drop
```yaml
paths:
  /drops:
    post:
      summary: Create a new drop
      tags: [Drops]
      operationId: createDrop
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DropCreateRequest'
      responses:
        '201':
          description: Drop created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the created drop
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '409':
          description: Product already has active drop in time range
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Drop by ID
```yaml
  /drops/{dropId}:
    get:
      summary: Get drop details
      tags: [Drops]
      operationId: getDropById
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - name: includeSlots
          in: query
          schema:
            type: boolean
            default: true
          description: Include slots in response
        - name: includeAnalytics
          in: query
          schema:
            type: boolean
            default: false
          description: Include analytics data
      responses:
        '200':
          description: Drop retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Drop
```yaml
    put:
      summary: Update drop information
      tags: [Drops]
      operationId: updateDrop
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DropUpdateRequest'
      responses:
        '200':
          description: Drop updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Time conflict with existing drops
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### List Drops
```yaml
  /drops:
    get:
      summary: Get list of drops
      tags: [Drops]
      operationId: listDrops
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store ID
        - name: productId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by product ID
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
          description: Filter by start date (inclusive)
        - name: endDate
          in: query
          schema:
            type: string
            format: date
          description: Filter by end date (inclusive)
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
                      $ref: '#/components/schemas/DropSummaryResponse'
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

#### Activate Drop
```yaml
  /drops/{dropId}/activate:
    post:
      summary: Activate a drop (make it live)
      tags: [Drops]
      operationId: activateDrop
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - $ref: '#/components/parameters/Idempotency-Key'
      responses:
        '200':
          description: Drop activated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
        '400':
          description: Invalid drop state for activation
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Cancel Drop
```yaml
  /drops/{dropId}/cancel:
    post:
      summary: Cancel a drop
      tags: [Drops]
      operationId: cancelDrop
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
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
                  example: "Product supply issues"
                notifyCustomers:
                  type: boolean
                  description: Send notification to customers
                  example: true
              required:
                - reason
              additionalProperties: false
      responses:
        '200':
          description: Drop cancelled successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Slot Management

#### Create Slot
```yaml
  /drops/{dropId}/slots:
    post:
      summary: Create a slot for a drop
      tags: [Slots]
      operationId: createSlot
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SlotCreateRequest'
      responses:
        '201':
          description: Slot created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SlotResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Time conflict with existing slots
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Slot by ID
```yaml
  /slots/{slotId}:
    get:
      summary: Get slot details
      tags: [Slots]
      operationId: getSlotById
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
          description: Slot retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SlotResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Slot
```yaml
    put:
      summary: Update slot information
      tags: [Slots]
      operationId: updateSlot
      security:
        - bearerAuth: []
      parameters:
        - name: slotId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Slot ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SlotUpdateRequest'
      responses:
        '200':
          description: Slot updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SlotResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### List Slots
```yaml
  /drops/{dropId}/slots:
    get:
      summary: Get slots for a drop
      tags: [Slots]
      operationId: listDropSlots
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - name: isActive
          in: query
          schema:
            type: boolean
            default: true
          description: Filter by active status
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
      responses:
        '200':
          description: Slots retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/SlotSummaryResponse'
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
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Get Slot Availability
```yaml
  /slots/{slotId}/availability:
    get:
      summary: Get real-time slot availability
      tags: [Slots]
      operationId: getSlotAvailability
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
          description: Availability retrieved successfully
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
                  lastUpdated: "2024-01-15T10:30:00Z"
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Analytics

#### Get Drop Analytics
```yaml
  /drops/{dropId}/analytics:
    get:
      summary: Get drop performance analytics
      tags: [Analytics]
      operationId: getDropAnalytics
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - name: startDate
          in: query
          schema:
            type: string
            format: date
          description: Analytics start date
        - name: endDate
          in: query
          schema:
            type: string
            format: date
          description: Analytics end date
      responses:
        '200':
          description: Analytics retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DropAnalyticsResponse'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Get Slot Analytics
```yaml
  /slots/{slotId}/analytics:
    get:
      summary: Get slot performance analytics
      tags: [Analytics]
      operationId: getSlotAnalytics
      security:
        - bearerAuth: []
      parameters:
        - name: slotId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Slot ID
        - name: startDate
          in: query
          schema:
            type: string
            format: date
          description: Analytics start date
        - name: endDate
          in: query
          schema:
            type: string
            format: date
          description: Analytics end date
      responses:
        '200':
          description: Analytics retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SlotAnalyticsResponse'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Bulk Operations

#### Bulk Create Slots
```yaml
  /drops/{dropId}/slots/bulk:
    post:
      summary: Create multiple slots for a drop
      tags: [Slots]
      operationId: bulkCreateSlots
      security:
        - bearerAuth: []
      parameters:
        - name: dropId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Drop ID
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                slots:
                  type: array
                  items:
                    $ref: '#/components/schemas/SlotCreateRequest'
                  minItems: 1
                  maxItems: 50
                  description: List of slots to create
              required:
                - slots
              additionalProperties: false
      responses:
        '200':
          description: Slots created successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  successCount:
                    type: integer
                    description: Number of successfully created slots
                    example: 45
                  failureCount:
                    type: integer
                    description: Number of failed creations
                    example: 5
                  slots:
                    type: array
                    items:
                      $ref: '#/components/schemas/SlotResponse'
                    description: Successfully created slots
                  failures:
                    type: array
                    items:
                      type: object
                      properties:
                        index:
                          type: integer
                          description: Index in the original array
                        error:
                          type: string
                          description: Error type
                        message:
                          type: string
                          description: Error message
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

## Data Models

### Drop Create Request
```yaml
components:
  schemas:
    DropCreateRequest:
      type: object
      properties:
        storeId:
          type: string
          format: uuid
          description: Store ID
          example: "123e4567-e89b-12d3-a456-426614174000"
        productId:
          type: string
          format: uuid
          description: Product ID
          example: "123e4567-e89b-12d3-a456-426614174001"
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: Drop name
          example: "Holiday Sneaker Drop 2024"
        description:
          type: string
          maxLength: 2000
          description: Drop description
          example: "Limited release holiday collection with exclusive colors"
        startsAt:
          type: string
          format: date-time
          description: Drop start time
          example: "2024-12-25T10:00:00Z"
        endsAt:
          type: string
          format: date-time
          description: Drop end time
          example: "2024-12-25T18:00:00Z"
        reservationRule:
          type: object
          description: Custom reservation rules
          properties:
            maxReservationsPerUser:
              type: integer
              minimum: 1
              maximum: 10
              default: 1
              description: Maximum reservations per user
            allowWaitlist:
              type: boolean
              default: true
              description: Enable waitlist
            autoPromoteWaitlist:
              type: boolean
              default: true
              description: Auto-promote from waitlist
            requirePhoneVerification:
              type: boolean
              default: false
              description: Require phone verification
            allowedUserGroups:
              type: array
              items:
                type: string
                enum: [VIP, EARLY_ACCESS, GENERAL]
              description: Restrict to specific user groups
          additionalProperties: false
        maxReservationsPerUser:
          type: integer
          minimum: 1
          maximum: 10
          default: 1
          description: Maximum reservations per user (deprecated, use reservationRule)
        allowWaitlist:
          type: boolean
          default: true
          description: Enable waitlist (deprecated, use reservationRule)
        autoPromoteWaitlist:
          type: boolean
          default: true
          description: Auto-promote from waitlist (deprecated, use reservationRule)
        notifyOnStart:
          type: boolean
          default: true
          description: Send notification when drop starts
        notifyOnEnd:
          type: boolean
          default: false
          description: Send notification when drop ends
        metadata:
          type: object
          description: Additional drop metadata
          additionalProperties: true
          example: {"theme": "holiday", "priority": "high"}
      required:
        - storeId
        - productId
        - name
        - startsAt
        - endsAt
      additionalProperties: false
```

### Drop Response
```yaml
    DropResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Drop unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        storeId:
          type: string
          format: uuid
          description: Store identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        productId:
          type: string
          format: uuid
          description: Product identifier
          example: "123e4567-e89b-12d3-a456-426614174002"
        name:
          type: string
          description: Drop name
          example: "Holiday Sneaker Drop 2024"
        description:
          type: string
          nullable: true
          description: Drop description
          example: "Limited release holiday collection with exclusive colors"
        startsAt:
          type: string
          format: date-time
          description: Drop start time
          example: "2024-12-25T10:00:00Z"
        endsAt:
          type: string
          format: date-time
          description: Drop end time
          example: "2024-12-25T18:00:00Z"
        status:
          type: string
          enum: [DRAFT, ACTIVE, CANCELLED, COMPLETED]
          description: Drop status
          example: ACTIVE
        reservationRule:
          type: object
          nullable: true
          description: Custom reservation rules
        maxReservationsPerUser:
          type: integer
          description: Maximum reservations per user
          example: 1
        allowWaitlist:
          type: boolean
          description: Waitlist enabled
          example: true
        autoPromoteWaitlist:
          type: boolean
          description: Auto-promotion enabled
          example: true
        notifyOnStart:
          type: boolean
          description: Start notification enabled
          example: true
        notifyOnEnd:
          type: boolean
          description: End notification enabled
          example: false
        slots:
          type: array
          items:
            $ref: '#/components/schemas/SlotResponse'
          description: Drop slots
        metadata:
          type: object
          nullable: true
          description: Additional metadata
          additionalProperties: true
        createdBy:
          type: string
          format: uuid
          nullable: true
          description: Creator user ID
          example: "123e4567-e89b-12d3-a456-426614174003"
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
        - storeId
        - productId
        - name
        - startsAt
        - endsAt
        - status
        - maxReservationsPerUser
        - allowWaitlist
        - autoPromoteWaitlist
        - notifyOnStart
        - notifyOnEnd
        - createdAt
        - updatedAt
```

### Slot Create Request
```yaml
    SlotCreateRequest:
      type: object
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: Slot name
          example: "Morning Session"
        description:
          type: string
          maxLength: 1000
          description: Slot description
          example: "First morning slot with priority access"
        startTime:
          type: string
          format: date-time
          description: Slot start time
          example: "2024-12-25T10:00:00Z"
        endTime:
          type: string
          format: date-time
          description: Slot end time
          example: "2024-12-25T12:00:00Z"
        capacityTotal:
          type: integer
          minimum: 1
          maximum: 10000
          description: Total capacity for this slot
          example: 100
        priceModifierCents:
          type: integer
          default: 0
          description: Price modifier in cents (+/- from base price)
          example: 500
        isActive:
          type: boolean
          default: true
          description: Slot active status
        position:
          type: integer
          minimum: 0
          default: 0
          description: Display position/order
      required:
        - startTime
        - endTime
        - capacityTotal
      additionalProperties: false
```

### Slot Response
```yaml
    SlotResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Slot unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        dropId:
          type: string
          format: uuid
          description: Parent drop identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        name:
          type: string
          nullable: true
          description: Slot name
          example: "Morning Session"
        description:
          type: string
          nullable: true
          description: Slot description
          example: "First morning slot with priority access"
        startTime:
          type: string
          format: date-time
          description: Slot start time
          example: "2024-12-25T10:00:00Z"
        endTime:
          type: string
          format: date-time
          description: Slot end time
          example: "2024-12-25T12:00:00Z"
        capacityTotal:
          type: integer
          description: Total capacity
          example: 100
        capacityReserved:
          type: integer
          description: Currently reserved spots
          example: 75
        capacityWaitlist:
          type: integer
          description: Current waitlist entries
          example: 25
        priceModifierCents:
          type: integer
          description: Price modifier in cents
          example: 500
        isActive:
          type: boolean
          description: Slot active status
          example: true
        position:
          type: integer
          description: Display position
          example: 1
        availableSpots:
          type: integer
          description: Calculated available spots
          example: 0
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
        - dropId
        - startTime
        - endTime
        - capacityTotal
        - capacityReserved
        - capacityWaitlist
        - priceModifierCents
        - isActive
        - position
        - availableSpots
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
                example: "/api/v1/drops"
              errors:
                type: array
                items:
                  type: object
                  properties:
                    field:
                      type: string
                      example: "startsAt"
                    message:
                      type: string
                      example: "Start time must be in the future"
                    rejectedValue:
                      type: string
                      example: "2024-01-01T10:00:00Z"
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
                example: "Drop not found"

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
                example: "Time conflict with existing drop"
```

## Events Published

The Drop Service publishes events to Apache Kafka for other services to consume:

### Drop Events
- `drop.created` - New drop created
- `drop.updated` - Drop information modified
- `drop.activated` - Drop activated and made live
- `drop.cancelled` - Drop cancelled
- `drop.completed` - Drop ended successfully
- `drop.started` - Drop start time reached
- `drop.ended` - Drop end time reached

### Slot Events
- `slot.created` - New slot created
- `slot.updated` - Slot information modified
- `slot.capacity.updated` - Slot capacity changed
- `slot.activated` - Slot activated
- `slot.deactivated` - Slot deactivated

### Analytics Events
- `drop.analytics.updated` - Drop analytics updated
- `slot.analytics.updated` - Slot analytics updated

### Event Schema Example
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "drop.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "drop-service",
  "data": {
    "dropId": "123e4567-e89b-12d3-a456-426614174001",
    "storeId": "123e4567-e89b-12d3-a456-426614174002",
    "productId": "123e4567-e89b-12d3-a456-426614174003",
    "name": "Holiday Sneaker Drop 2024",
    "startsAt": "2024-12-25T10:00:00Z",
    "endsAt": "2024-12-25T18:00:00Z",
    "status": "DRAFT"
  }
}
```

## Service Dependencies

### Consumes Events From
- **Store Service**: `store.created`, `store.updated` (for drop validation)
- **Product Service**: `product.created`, `product.updated` (for product availability)
- **Reservation Service**: `reservation.created`, `reservation.cancelled` (for capacity updates)

### Publishes Events To
- **Reservation Service**: Drop and slot events for reservation management
- **Notification Service**: Drop lifecycle events for customer notifications
- **Analytics Service**: All drop and slot events for business intelligence

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
DB_NAME=drop_db
DB_USERNAME=drop_service
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=drop-service

# Service Configuration
SERVER_PORT=8080
LOG_LEVEL=INFO

# Scheduling Configuration
DROP_SCHEDULER_ENABLED=true
DROP_START_NOTIFICATION_ADVANCE_MINUTES=60
DROP_END_NOTIFICATION_DELAY_MINUTES=30

# Capacity Configuration
DEFAULT_SLOT_CAPACITY=100
MAX_SLOT_CAPACITY=10000
MIN_SLOT_DURATION_MINUTES=15
MAX_SLOT_DURATION_HOURS=24
```

## Development Guidelines

### Code Style
- Follow Spring Boot conventions
- Use constructor injection for dependencies
- Implement comprehensive error handling
- Write unit and integration tests

### Scheduling Best Practices
- Use UTC for all time calculations
- Implement proper timezone handling
- Validate time conflicts before creation
- Use database constraints for time validation

### Capacity Management
- Implement atomic capacity updates
- Use database transactions for consistency
- Implement proper locking mechanisms
- Monitor capacity utilization

### Analytics Considerations
- Implement efficient analytics aggregation
- Use appropriate data types for metrics
- Consider data retention policies
- Implement real-time analytics where needed

---

**Drop Service API v1.0.0**
**Last Updated**: December 2024
**Contact**: drop-service-team@dropslot.com