# Store Service API Specification

## Overview

The Store Service manages multi-tenant store operations, including store creation, branch management, and geographic location services for the DropSlot platform.

```yaml
openapi: 3.0.3
info:
  title: Store Service API
  description: Multi-tenant store and branch management service
  version: 1.0.0
  contact:
    name: Store Service Team
servers:
  - url: http://store-service:8080/api/v1
    description: Store Service (internal)
  - url: http://localhost:8082/api/v1
    description: Store Service (local development)
```

## Service Responsibilities

- **Multi-tenant Store Management** - Store creation and configuration
- **Branch Location Management** - Physical store locations and hours
- **Geographic Services** - Location-based store discovery
- **Store Branding** - Logo, description, and store metadata
- **Tenant Isolation** - Data separation between store tenants

## Database Schema

```sql
-- Store Service Database: store_db
CREATE TABLE stores (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    tenant_key VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    website_url VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    timezone VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'USD',
    is_active BOOLEAN DEFAULT true,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE branches (
    id UUID PRIMARY KEY,
    store_id UUID REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255),
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    is_active BOOLEAN DEFAULT true,
    manager_id UUID,
    opening_hours JSONB,
    special_hours JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(store_id, slug)
);

CREATE TABLE store_configs (
    id UUID PRIMARY KEY,
    store_id UUID REFERENCES stores(id),
    config_key VARCHAR(255) NOT NULL,
    config_value JSONB,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(store_id, config_key)
);
```

## API Endpoints

### Store Management

#### Create Store
```yaml
paths:
  /stores:
    post:
      summary: Create a new store (Admin only)
      tags: [Stores]
      operationId: createStore
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StoreCreateRequest'
      responses:
        '201':
          description: Store created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StoreResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the created store
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '409':
          description: Store slug or tenant key already exists
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Store by ID
```yaml
    get:
      summary: Get store by ID
      tags: [Stores]
      operationId: getStoreById
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
      responses:
        '200':
          description: Store retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StoreResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Store
```yaml
    put:
      summary: Update store information
      tags: [Stores]
      operationId: updateStore
      security:
        - bearerAuth: []
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StoreUpdateRequest'
      responses:
        '200':
          description: Store updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/StoreResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### List Stores
```yaml
  /stores:
    get:
      summary: Get list of stores
      tags: [Stores]
      operationId: listStores
      parameters:
        - name: search
          in: query
          schema:
            type: string
          description: Search stores by name or description
        - name: city
          in: query
          schema:
            type: string
          description: Filter by city
        - name: country
          in: query
          schema:
            type: string
          description: Filter by country
        - name: latitude
          in: query
          schema:
            type: number
            format: float
          description: User latitude for distance calculation
        - name: longitude
          in: query
          schema:
            type: number
            format: float
          description: User longitude for distance calculation
        - name: radius
          in: query
          schema:
            type: number
            format: float
            default: 50.0
          description: Search radius in kilometers
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
        - $ref: '#/components/parameters/sort'
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
                      $ref: '#/components/schemas/StoreSummaryResponse'
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

#### Delete Store
```yaml
    delete:
      summary: Delete store (Admin only)
      tags: [Stores]
      operationId: deleteStore
      security:
        - bearerAuth: []
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
      responses:
        '204':
          description: Store deleted successfully
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Store has active branches or products
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

### Branch Management

#### Create Branch
```yaml
  /branches:
    post:
      summary: Create a new branch for a store
      tags: [Branches]
      operationId: createBranch
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BranchCreateRequest'
      responses:
        '201':
          description: Branch created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BranchResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '409':
          description: Branch slug already exists for this store
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Branch by ID
```yaml
  /branches/{branchId}:
    get:
      summary: Get branch details
      tags: [Branches]
      operationId: getBranchById
      parameters:
        - name: branchId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Branch ID
      responses:
        '200':
          description: Branch retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BranchResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Branch
```yaml
    put:
      summary: Update branch information
      tags: [Branches]
      operationId: updateBranch
      security:
        - bearerAuth: []
      parameters:
        - name: branchId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Branch ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BranchUpdateRequest'
      responses:
        '200':
          description: Branch updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BranchResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### List Branches
```yaml
  /branches:
    get:
      summary: Get list of branches
      tags: [Branches]
      operationId: listBranches
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store ID
        - name: city
          in: query
          schema:
            type: string
          description: Filter by city
        - name: country
          in: query
          schema:
            type: string
          description: Filter by country
        - name: latitude
          in: query
          schema:
            type: number
            format: float
          description: User latitude for distance calculation
        - name: longitude
          in: query
          schema:
            type: number
            format: float
          description: User longitude for distance calculation
        - name: radius
          in: query
          schema:
            type: number
            format: float
            default: 50.0
          description: Search radius in kilometers
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
          description: Branches retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/BranchSummaryResponse'
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

#### Get Store Branches
```yaml
  /stores/{storeId}/branches:
    get:
      summary: Get all branches for a store
      tags: [Branches]
      operationId: getStoreBranches
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
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
          description: Store branches retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/BranchSummaryResponse'
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

### Geographic Services

#### Search Nearby Stores
```yaml
  /stores/nearby:
    get:
      summary: Find stores near a location
      tags: [Geographic]
      operationId: searchNearbyStores
      parameters:
        - name: latitude
          in: query
          required: true
          schema:
            type: number
            format: float
            minimum: -90
            maximum: 90
          description: Latitude coordinate
        - name: longitude
          in: query
          required: true
          schema:
            type: number
            format: float
            minimum: -180
            maximum: 180
          description: Longitude coordinate
        - name: radius
          in: query
          schema:
            type: number
            format: float
            default: 50.0
            minimum: 0.1
            maximum: 500.0
          description: Search radius in kilometers
        - name: limit
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
          description: Maximum number of results
      responses:
        '200':
          description: Nearby stores found
          content:
            application/json:
              schema:
                type: object
                properties:
                  stores:
                    type: array
                    items:
                      $ref: '#/components/schemas/StoreNearbyResponse'
                  totalCount:
                    type: integer
        '400':
          $ref: '#/components/responses/ValidationError'
```

#### Search Nearby Branches
```yaml
  /branches/nearby:
    get:
      summary: Find branches near a location
      tags: [Geographic]
      operationId: searchNearbyBranches
      parameters:
        - name: latitude
          in: query
          required: true
          schema:
            type: number
            format: float
            minimum: -90
            maximum: 90
          description: Latitude coordinate
        - name: longitude
          in: query
          required: true
          schema:
            type: number
            format: float
            minimum: -180
            maximum: 180
          description: Longitude coordinate
        - name: radius
          in: query
          schema:
            type: number
            format: float
            default: 50.0
            minimum: 0.1
            maximum: 500.0
          description: Search radius in kilometers
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by specific store
        - name: limit
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
          description: Maximum number of results
      responses:
        '200':
          description: Nearby branches found
          content:
            application/json:
              schema:
                type: object
                properties:
                  branches:
                    type: array
                    items:
                      $ref: '#/components/schemas/BranchNearbyResponse'
                  totalCount:
                    type: integer
        '400':
          $ref: '#/components/responses/ValidationError'
```

### Store Configuration

#### Get Store Configuration
```yaml
  /stores/{storeId}/config:
    get:
      summary: Get store configuration
      tags: [Configuration]
      operationId: getStoreConfig
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
        - name: publicOnly
          in: query
          schema:
            type: boolean
            default: true
          description: Return only public configuration
      responses:
        '200':
          description: Store configuration retrieved
          content:
            application/json:
              schema:
                type: object
                additionalProperties:
                  type: object
                description: Configuration key-value pairs
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Store Configuration
```yaml
    patch:
      summary: Update store configuration
      tags: [Configuration]
      operationId: updateStoreConfig
      security:
        - bearerAuth: []
      parameters:
        - name: storeId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Store ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              additionalProperties:
                type: object
              description: Configuration key-value pairs to update
      responses:
        '200':
          description: Store configuration updated
          content:
            application/json:
              schema:
                type: object
                additionalProperties:
                  type: object
                description: Updated configuration
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

## Data Models

### Store Create Request
```yaml
components:
  schemas:
    StoreCreateRequest:
      type: object
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: Store name
          example: "Downtown Sneaker Store"
        slug:
          type: string
          pattern: '^[a-z0-9-]+$'
          minLength: 3
          maxLength: 100
          description: URL-friendly store identifier
          example: "downtown-sneakers"
        tenantKey:
          type: string
          pattern: '^[A-Z0-9_]+$'
          minLength: 3
          maxLength: 50
          description: Unique tenant identifier
          example: "STORE_001"
        description:
          type: string
          maxLength: 1000
          description: Store description
          example: "Premium sneaker boutique in downtown"
        logoUrl:
          type: string
          format: uri
          description: Store logo URL
          example: "https://example.com/logo.png"
        websiteUrl:
          type: string
          format: uri
          description: Store website URL
          example: "https://store.example.com"
        phone:
          type: string
          pattern: '^\+?[1-9]\d{1,14}$'
          description: Store phone number
          example: "+1234567890"
        email:
          type: string
          format: email
          description: Store contact email
          example: "contact@store.com"
        address:
          type: string
          maxLength: 500
          description: Store street address
          example: "123 Main Street"
        city:
          type: string
          maxLength: 255
          description: City
          example: "New York"
        state:
          type: string
          maxLength: 255
          description: State or province
          example: "NY"
        country:
          type: string
          minLength: 2
          maxLength: 2
          description: ISO 3166-1 alpha-2 country code
          example: "US"
        postalCode:
          type: string
          maxLength: 20
          description: Postal code
          example: "10001"
        latitude:
          type: number
          format: float
          minimum: -90
          maximum: 90
          description: Latitude coordinate
          example: 40.7128
        longitude:
          type: number
          format: float
          minimum: -180
          maximum: 180
          description: Longitude coordinate
          example: -74.0060
        timezone:
          type: string
          description: IANA timezone identifier
          example: "America/New_York"
        currency:
          type: string
          minLength: 3
          maxLength: 3
          description: ISO 4217 currency code
          example: "USD"
      required:
        - name
        - slug
        - tenantKey
        - country
      additionalProperties: false
```

### Store Response
```yaml
    StoreResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Store unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        name:
          type: string
          description: Store name
          example: "Downtown Sneaker Store"
        slug:
          type: string
          description: URL-friendly identifier
          example: "downtown-sneakers"
        tenantKey:
          type: string
          description: Unique tenant identifier
          example: "STORE_001"
        description:
          type: string
          nullable: true
          description: Store description
          example: "Premium sneaker boutique in downtown"
        logoUrl:
          type: string
          format: uri
          nullable: true
          description: Store logo URL
          example: "https://example.com/logo.png"
        websiteUrl:
          type: string
          format: uri
          nullable: true
          description: Store website URL
          example: "https://store.example.com"
        phone:
          type: string
          nullable: true
          description: Store phone number
          example: "+1234567890"
        email:
          type: string
          format: email
          nullable: true
          description: Store contact email
          example: "contact@store.com"
        address:
          type: string
          nullable: true
          description: Store street address
          example: "123 Main Street"
        city:
          type: string
          nullable: true
          description: City
          example: "New York"
        state:
          type: string
          nullable: true
          description: State or province
          example: "NY"
        country:
          type: string
          description: ISO 3166-1 alpha-2 country code
          example: "US"
        postalCode:
          type: string
          nullable: true
          description: Postal code
          example: "10001"
        latitude:
          type: number
          format: float
          nullable: true
          description: Latitude coordinate
          example: 40.7128
        longitude:
          type: number
          format: float
          nullable: true
          description: Longitude coordinate
          example: -74.0060
        timezone:
          type: string
          nullable: true
          description: IANA timezone identifier
          example: "America/New_York"
        currency:
          type: string
          description: ISO 4217 currency code
          example: "USD"
        isActive:
          type: boolean
          description: Store active status
          example: true
        branchCount:
          type: integer
          description: Number of active branches
          example: 3
        createdAt:
          type: string
          format: date-time
          description: Store creation timestamp
          example: "2024-01-15T10:30:00Z"
        updatedAt:
          type: string
          format: date-time
          description: Last update timestamp
          example: "2024-01-15T10:30:00Z"
      required:
        - id
        - name
        - slug
        - tenantKey
        - country
        - currency
        - isActive
        - createdAt
        - updatedAt
```

### Branch Create Request
```yaml
    BranchCreateRequest:
      type: object
      properties:
        storeId:
          type: string
          format: uuid
          description: Parent store ID
          example: "123e4567-e89b-12d3-a456-426614174000"
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: Branch name
          example: "Downtown Branch"
        slug:
          type: string
          pattern: '^[a-z0-9-]+$'
          minLength: 3
          maxLength: 100
          description: URL-friendly branch identifier
          example: "downtown"
        description:
          type: string
          maxLength: 1000
          description: Branch description
          example: "Main downtown location"
        phone:
          type: string
          pattern: '^\+?[1-9]\d{1,14}$'
          description: Branch phone number
          example: "+1234567890"
        email:
          type: string
          format: email
          description: Branch contact email
          example: "downtown@store.com"
        address:
          type: string
          maxLength: 500
          description: Branch street address
          example: "123 Main Street"
        city:
          type: string
          maxLength: 255
          description: City
          example: "New York"
        state:
          type: string
          maxLength: 255
          description: State or province
          example: "NY"
        country:
          type: string
          minLength: 2
          maxLength: 2
          description: ISO 3166-1 alpha-2 country code
          example: "US"
        postalCode:
          type: string
          maxLength: 20
          description: Postal code
          example: "10001"
        latitude:
          type: number
          format: float
          minimum: -90
          maximum: 90
          description: Latitude coordinate
          example: 40.7128
        longitude:
          type: number
          format: float
          minimum: -180
          maximum: 180
          description: Longitude coordinate
          example: -74.0060
        timezone:
          type: string
          description: IANA timezone identifier
          example: "America/New_York"
        managerId:
          type: string
          format: uuid
          description: Branch manager user ID
          example: "123e4567-e89b-12d3-a456-426614174001"
        openingHours:
          type: object
          description: Weekly opening hours
          example: {
            "monday": [{"open": "09:00", "close": "18:00"}],
            "tuesday": [{"open": "09:00", "close": "18:00"}],
            "wednesday": [{"open": "09:00", "close": "18:00"}],
            "thursday": [{"open": "09:00", "close": "18:00"}],
            "friday": [{"open": "09:00", "close": "20:00"}],
            "saturday": [{"open": "10:00", "close": "16:00"}],
            "sunday": []
          }
      required:
        - storeId
        - name
        - slug
        - address
        - city
        - country
        - latitude
        - longitude
      additionalProperties: false
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
                example: "/api/v1/stores"
              errors:
                type: array
                items:
                  type: object
                  properties:
                    field:
                      type: string
                      example: "slug"
                    message:
                      type: string
                      example: "Slug format is invalid"
                    rejectedValue:
                      type: string
                      example: "Invalid Slug!"
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
                example: "Store not found"

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
                example: "Store slug already exists"
```

## Events Published

The Store Service publishes events to Apache Kafka for other services to consume:

### Store Events
- `store.created` - New store created
- `store.updated` - Store information modified
- `store.activated` - Store activated
- `store.deactivated` - Store deactivated
- `store.deleted` - Store deleted

### Branch Events
- `branch.created` - New branch created
- `branch.updated` - Branch information modified
- `branch.activated` - Branch activated
- `branch.deactivated` - Branch deactivated
- `branch.deleted` - Branch deleted

### Event Schema Example
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "store.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "store-service",
  "data": {
    "storeId": "123e4567-e89b-12d3-a456-426614174001",
    "name": "Downtown Sneaker Store",
    "slug": "downtown-sneakers",
    "tenantKey": "STORE_001",
    "country": "US",
    "currency": "USD"
  }
}
```

## Service Dependencies

### Consumes Events From
- **User Service**: `user.registered` (for store creation attribution)

### Publishes Events To
- **Product Service**: Store events for product availability
- **Drop Service**: Store events for drop scheduling
- **Analytics Service**: All store and branch events
- **Notification Service**: Store activation/deactivation events

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
DB_NAME=store_db
DB_USERNAME=store_service
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Service Configuration
SERVER_PORT=8080
LOG_LEVEL=INFO

# Geographic Configuration
DEFAULT_SEARCH_RADIUS=50.0
MAX_SEARCH_RADIUS=500.0
DEFAULT_TIMEZONE=UTC
```

## Development Guidelines

### Code Style
- Follow Spring Boot conventions
- Use constructor injection for dependencies
- Implement comprehensive error handling
- Write unit and integration tests

### Geographic Best Practices
- Validate latitude/longitude coordinates
- Use appropriate spatial indexes
- Implement efficient distance calculations
- Cache geographic search results

### Multi-tenant Considerations
- Implement tenant isolation at database level
- Use tenant_key for data partitioning
- Validate tenant permissions for operations
- Implement tenant-specific configurations

---

**Store Service API v1.0.0**
**Last Updated**: December 2024
**Contact**: store-service-team@dropslot.com