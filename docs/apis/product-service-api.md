# Product Service API Specification

## Overview

The Product Service manages product catalogs, inventory, and search functionality for the DropSlot platform. This service handles product information, categorization, and availability across multiple store tenants.

```yaml
openapi: 3.0.3
info:
  title: Product Service API
  description: Product catalog and inventory management service
  version: 1.0.0
  contact:
    name: Product Service Team
servers:
  - url: http://product-service:8080/api/v1
    description: Product Service (internal)
  - url: http://localhost:8083/api/v1
    description: Product Service (local development)
```

## Service Responsibilities

- **Product Catalog Management** - Product creation and information management
- **Inventory Tracking** - Stock levels and availability
- **Product Search** - Full-text search and filtering
- **Categorization** - Product organization and classification
- **Multi-tenant Support** - Store-specific product isolation
- **Image Management** - Product photo handling

## Database Schema

```sql
-- Product Service Database: product_db
CREATE TABLE products (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    brand VARCHAR(255),
    category VARCHAR(255),
    subcategory VARCHAR(255),
    tags TEXT[],
    base_price_cents BIGINT NOT NULL,
    compare_at_price_cents BIGINT,
    cost_price_cents BIGINT,
    currency VARCHAR(3) DEFAULT 'USD',
    track_inventory BOOLEAN DEFAULT true,
    inventory_quantity INTEGER DEFAULT 0,
    inventory_policy VARCHAR(20) DEFAULT 'deny', -- deny, allow
    low_stock_threshold INTEGER DEFAULT 0,
    weight_grams INTEGER,
    dimensions JSONB, -- {length, width, height, unit}
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    is_digital BOOLEAN DEFAULT false,
    requires_shipping BOOLEAN DEFAULT true,
    tax_category VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(store_id, sku)
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID REFERENCES products(id),
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    position INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    width INTEGER,
    height INTEGER,
    file_size_bytes INTEGER,
    content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    UNIQUE(product_id, position)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID REFERENCES products(id),
    sku VARCHAR(100),
    name VARCHAR(255),
    option1 VARCHAR(255),
    option2 VARCHAR(255),
    option3 VARCHAR(255),
    price_cents BIGINT,
    compare_at_price_cents BIGINT,
    cost_price_cents BIGINT,
    inventory_quantity INTEGER DEFAULT 0,
    weight_grams INTEGER,
    is_active BOOLEAN DEFAULT true,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE product_categories (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES product_categories(id),
    position INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(store_id, slug)
);

CREATE INDEX idx_products_store_id ON products(store_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_is_active ON products(is_active);
CREATE INDEX idx_products_search ON products USING gin(to_tsvector('english', name || ' ' || description));
```

## API Endpoints

### Product Management

#### Create Product
```yaml
paths:
  /products:
    post:
      summary: Create a new product
      tags: [Products]
      operationId: createProduct
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductCreateRequest'
      responses:
        '201':
          description: Product created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
          headers:
            Location:
              schema:
                type: string
                format: uri
              description: URL to access the created product
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '409':
          description: Product SKU already exists for this store
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### Get Product by ID
```yaml
  /products/{productId}:
    get:
      summary: Get product details
      tags: [Products]
      operationId: getProductById
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
        - name: includeVariants
          in: query
          schema:
            type: boolean
            default: true
          description: Include product variants in response
        - name: includeImages
          in: query
          schema:
            type: boolean
            default: true
          description: Include product images in response
      responses:
        '200':
          description: Product retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Product
```yaml
    put:
      summary: Update product information
      tags: [Products]
      operationId: updateProduct
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductUpdateRequest'
      responses:
        '200':
          description: Product updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### List Products
```yaml
  /products:
    get:
      summary: Get list of products
      tags: [Products]
      operationId: listProducts
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store ID
        - name: category
          in: query
          schema:
            type: string
          description: Filter by category
        - name: brand
          in: query
          schema:
            type: string
          description: Filter by brand
        - name: isActive
          in: query
          schema:
            type: boolean
            default: true
          description: Filter by active status
        - name: isFeatured
          in: query
          schema:
            type: boolean
          description: Filter by featured status
        - name: minPrice
          in: query
          schema:
            type: number
            format: float
          description: Minimum price filter
        - name: maxPrice
          in: query
          schema:
            type: number
            format: float
          description: Maximum price filter
        - name: search
          in: query
          schema:
            type: string
          description: Full-text search query
        - name: tags
          in: query
          schema:
            type: array
            items:
              type: string
          description: Filter by tags
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
                      $ref: '#/components/schemas/ProductSummaryResponse'
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

#### Delete Product
```yaml
    delete:
      summary: Delete product
      tags: [Products]
      operationId: deleteProduct
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
      responses:
        '204':
          description: Product deleted successfully
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Product has active drops or reservations
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

### Product Images

#### Upload Product Image
```yaml
  /products/{productId}/images:
    post:
      summary: Upload product image
      tags: [ProductImages]
      operationId: uploadProductImage
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                image:
                  type: string
                  format: binary
                  description: Image file (JPEG, PNG, WebP)
                altText:
                  type: string
                  maxLength: 255
                  description: Alt text for accessibility
                position:
                  type: integer
                  minimum: 0
                  description: Image position/order
                isPrimary:
                  type: boolean
                  description: Set as primary image
              required:
                - image
      responses:
        '201':
          description: Image uploaded successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductImageResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Product Image
```yaml
  /products/{productId}/images/{imageId}:
    put:
      summary: Update product image metadata
      tags: [ProductImages]
      operationId: updateProductImage
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
        - name: imageId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Image ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                altText:
                  type: string
                  maxLength: 255
                  description: Alt text for accessibility
                position:
                  type: integer
                  minimum: 0
                  description: Image position/order
                isPrimary:
                  type: boolean
                  description: Set as primary image
      responses:
        '200':
          description: Image updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductImageResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Delete Product Image
```yaml
    delete:
      summary: Delete product image
      tags: [ProductImages]
      operationId: deleteProductImage
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
        - name: imageId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Image ID
      responses:
        '204':
          description: Image deleted successfully
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Product Variants

#### Create Product Variant
```yaml
  /products/{productId}/variants:
    post:
      summary: Create product variant
      tags: [ProductVariants]
      operationId: createProductVariant
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductVariantCreateRequest'
      responses:
        '201':
          description: Variant created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductVariantResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Update Product Variant
```yaml
  /products/{productId}/variants/{variantId}:
    put:
      summary: Update product variant
      tags: [ProductVariants]
      operationId: updateProductVariant
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
        - name: variantId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Variant ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductVariantUpdateRequest'
      responses:
        '200':
          description: Variant updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductVariantResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

### Categories

#### Create Category
```yaml
  /categories:
    post:
      summary: Create product category
      tags: [Categories]
      operationId: createCategory
      security:
        - bearerAuth: []
      parameters:
        - $ref: '#/components/parameters/Idempotency-Key'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CategoryCreateRequest'
      responses:
        '201':
          description: Category created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CategoryResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '409':
          description: Category slug already exists for this store
          content:
            application/json:
              schema:
                $ref: '#/components/responses/ConflictError'
```

#### List Categories
```yaml
  /categories:
    get:
      summary: Get product categories
      tags: [Categories]
      operationId: listCategories
      parameters:
        - name: storeId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by store ID
        - name: parentId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by parent category
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
          description: Categories retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/CategoryResponse'
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

### Inventory Management

#### Update Inventory
```yaml
  /products/{productId}/inventory:
    patch:
      summary: Update product inventory
      tags: [Inventory]
      operationId: updateInventory
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
          description: Product ID
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                inventoryQuantity:
                  type: integer
                  minimum: 0
                  description: New inventory quantity
                  example: 50
                lowStockThreshold:
                  type: integer
                  minimum: 0
                  description: Low stock alert threshold
                  example: 10
                inventoryPolicy:
                  type: string
                  enum: [deny, allow]
                  description: Inventory policy when out of stock
                  example: "deny"
              additionalProperties: false
      responses:
        '200':
          description: Inventory updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '403':
          $ref: '#/components/responses/ForbiddenError'
        '404':
          $ref: '#/components/responses/NotFoundError'
```

#### Bulk Inventory Update
```yaml
  /products/inventory/bulk:
    patch:
      summary: Bulk update inventory for multiple products
      tags: [Inventory]
      operationId: bulkUpdateInventory
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                updates:
                  type: array
                  items:
                    type: object
                    properties:
                      productId:
                        type: string
                        format: uuid
                        description: Product ID
                      inventoryQuantity:
                        type: integer
                        minimum: 0
                        description: New inventory quantity
                      lowStockThreshold:
                        type: integer
                        minimum: 0
                        description: Low stock alert threshold
                    required:
                      - productId
                      - inventoryQuantity
                  minItems: 1
                  maxItems: 100
                  description: List of inventory updates
              required:
                - updates
              additionalProperties: false
      responses:
        '200':
          description: Inventory updates processed successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  successCount:
                    type: integer
                    description: Number of successful updates
                    example: 95
                  failureCount:
                    type: integer
                    description: Number of failed updates
                    example: 5
                  failures:
                    type: array
                    items:
                      type: object
                      properties:
                        productId:
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

### Product Create Request
```yaml
components:
  schemas:
    ProductCreateRequest:
      type: object
      properties:
        storeId:
          type: string
          format: uuid
          description: Store ID
          example: "123e4567-e89b-12d3-a456-426614174000"
        name:
          type: string
          minLength: 1
          maxLength: 255
          description: Product name
          example: "Air Jordan 1 High"
        sku:
          type: string
          minLength: 1
          maxLength: 100
          pattern: '^[A-Z0-9_-]+$'
          description: Stock keeping unit (unique per store)
          example: "AJ1-HIGH-001"
        description:
          type: string
          maxLength: 5000
          description: Product description
          example: "Classic basketball sneaker with premium leather"
        shortDescription:
          type: string
          maxLength: 500
          description: Short product description
          example: "Iconic high-top basketball sneaker"
        brand:
          type: string
          maxLength: 255
          description: Product brand
          example: "Nike"
        category:
          type: string
          maxLength: 255
          description: Product category
          example: "Sneakers"
        subcategory:
          type: string
          maxLength: 255
          description: Product subcategory
          example: "Basketball"
        tags:
          type: array
          items:
            type: string
            maxLength: 50
          maxItems: 20
          description: Product tags for search
          example: ["basketball", "classic", "leather"]
        basePriceCents:
          type: integer
          minimum: 0
          maximum: 99999999
          description: Base price in cents
          example: 17000
        compareAtPriceCents:
          type: integer
          minimum: 0
          maximum: 99999999
          description: Compare at price in cents (for sales)
          example: 20000
        costPriceCents:
          type: integer
          minimum: 0
          maximum: 99999999
          description: Cost price in cents
          example: 8500
        currency:
          type: string
          minLength: 3
          maxLength: 3
          description: ISO 4217 currency code
          example: "USD"
        trackInventory:
          type: boolean
          description: Whether to track inventory
          example: true
        inventoryQuantity:
          type: integer
          minimum: 0
          description: Initial inventory quantity
          example: 100
        inventoryPolicy:
          type: string
          enum: [deny, allow]
          description: Policy when out of stock
          example: "deny"
        lowStockThreshold:
          type: integer
          minimum: 0
          description: Low stock alert threshold
          example: 10
        weightGrams:
          type: integer
          minimum: 0
          description: Product weight in grams
          example: 500
        dimensions:
          type: object
          description: Product dimensions
          properties:
            length:
              type: number
              format: float
              minimum: 0
              example: 30.5
            width:
              type: number
              format: float
              minimum: 0
              example: 20.0
            height:
              type: number
              format: float
              minimum: 0
              example: 12.0
            unit:
              type: string
              enum: [cm, in]
              example: "cm"
        isActive:
          type: boolean
          description: Product active status
          example: true
        isFeatured:
          type: boolean
          description: Featured product status
          example: false
        isDigital:
          type: boolean
          description: Digital product flag
          example: false
        requiresShipping:
          type: boolean
          description: Physical shipping required
          example: true
        taxCategory:
          type: string
          maxLength: 100
          description: Tax category for calculations
          example: "clothing"
        metadata:
          type: object
          description: Additional product metadata
          additionalProperties: true
          example: {"color": "black", "size": "10"}
      required:
        - storeId
        - name
        - sku
        - basePriceCents
        - currency
      additionalProperties: false
```

### Product Response
```yaml
    ProductResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Product unique identifier
          example: "123e4567-e89b-12d3-a456-426614174000"
        storeId:
          type: string
          format: uuid
          description: Store identifier
          example: "123e4567-e89b-12d3-a456-426614174001"
        name:
          type: string
          description: Product name
          example: "Air Jordan 1 High"
        sku:
          type: string
          description: Stock keeping unit
          example: "AJ1-HIGH-001"
        description:
          type: string
          nullable: true
          description: Product description
          example: "Classic basketball sneaker with premium leather"
        shortDescription:
          type: string
          nullable: true
          description: Short product description
          example: "Iconic high-top basketball sneaker"
        brand:
          type: string
          nullable: true
          description: Product brand
          example: "Nike"
        category:
          type: string
          nullable: true
          description: Product category
          example: "Sneakers"
        subcategory:
          type: string
          nullable: true
          description: Product subcategory
          example: "Basketball"
        tags:
          type: array
          items:
            type: string
          nullable: true
          description: Product tags
          example: ["basketball", "classic", "leather"]
        basePriceCents:
          type: integer
          description: Base price in cents
          example: 17000
        compareAtPriceCents:
          type: integer
          nullable: true
          description: Compare at price in cents
          example: 20000
        costPriceCents:
          type: integer
          nullable: true
          description: Cost price in cents
          example: 8500
        currency:
          type: string
          description: Currency code
          example: "USD"
        trackInventory:
          type: boolean
          description: Inventory tracking enabled
          example: true
        inventoryQuantity:
          type: integer
          description: Current inventory quantity
          example: 100
        inventoryPolicy:
          type: string
          enum: [deny, allow]
          description: Inventory policy
          example: "deny"
        lowStockThreshold:
          type: integer
          description: Low stock threshold
          example: 10
        weightGrams:
          type: integer
          nullable: true
          description: Product weight in grams
          example: 500
        dimensions:
          type: object
          nullable: true
          description: Product dimensions
          properties:
            length:
              type: number
              format: float
              example: 30.5
            width:
              type: number
              format: float
              example: 20.0
            height:
              type: number
              format: float
              example: 12.0
            unit:
              type: string
              example: "cm"
        isActive:
          type: boolean
          description: Product active status
          example: true
        isFeatured:
          type: boolean
          description: Featured product status
          example: false
        isDigital:
          type: boolean
          description: Digital product flag
          example: false
        requiresShipping:
          type: boolean
          description: Physical shipping required
          example: true
        taxCategory:
          type: string
          nullable: true
          description: Tax category
          example: "clothing"
        images:
          type: array
          items:
            $ref: '#/components/schemas/ProductImageResponse'
          description: Product images
        variants:
          type: array
          items:
            $ref: '#/components/schemas/ProductVariantResponse'
          description: Product variants
        metadata:
          type: object
          nullable: true
          description: Additional metadata
          additionalProperties: true
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
        - name
        - sku
        - basePriceCents
        - currency
        - trackInventory
        - inventoryQuantity
        - inventoryPolicy
        - lowStockThreshold
        - isActive
        - isFeatured
        - isDigital
        - requiresShipping
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
                example: "/api/v1/products"
              errors:
                type: array
                items:
                  type: object
                  properties:
                    field:
                      type: string
                      example: "sku"
                    message:
                      type: string
                      example: "SKU format is invalid"
                    rejectedValue:
                      type: string
                      example: "invalid-sku"
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
                example: "Product not found"

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
                example: "Product SKU already exists"
```

## Events Published

The Product Service publishes events to Apache Kafka for other services to consume:

### Product Events
- `product.created` - New product created
- `product.updated` - Product information modified
- `product.activated` - Product activated
- `product.deactivated` - Product deactivated
- `product.deleted` - Product deleted
- `product.inventory.updated` - Inventory level changed
- `product.low_stock` - Product reached low stock threshold

### Image Events
- `product.image.uploaded` - New image uploaded
- `product.image.updated` - Image metadata updated
- `product.image.deleted` - Image deleted

### Variant Events
- `product.variant.created` - New variant created
- `product.variant.updated` - Variant modified
- `product.variant.activated` - Variant activated
- `product.variant.deactivated` - Variant deactivated

### Event Schema Example
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "product.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "product-service",
  "data": {
    "productId": "123e4567-e89b-12d3-a456-426614174001",
    "storeId": "123e4567-e89b-12d3-a456-426614174002",
    "name": "Air Jordan 1 High",
    "sku": "AJ1-HIGH-001",
    "basePriceCents": 17000,
    "currency": "USD",
    "inventoryQuantity": 100
  }
}
```

## Service Dependencies

### Consumes Events From
- **Store Service**: `store.created`, `store.updated` (for product availability)
- **User Service**: `user.registered` (for store ownership validation)

### Publishes Events To
- **Drop Service**: Product events for drop creation
- **Analytics Service**: All product events for business intelligence
- **Notification Service**: Low stock and product status change events

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
DB_NAME=product_db
DB_USERNAME=product_service
DB_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# File Upload Configuration
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=10MB
ALLOWED_IMAGE_TYPES=image/jpeg,image/png,image/webp
MAX_IMAGES_PER_PRODUCT=10

# Search Configuration
SEARCH_INDEX_UPDATE_INTERVAL=30000
SEARCH_CACHE_TTL=3600000

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

### Search Optimization
- Use PostgreSQL full-text search for product search
- Implement search result caching
- Optimize database indexes for search queries
- Handle search pagination efficiently

### Image Management
- Validate image formats and sizes
- Implement image resizing and optimization
- Use CDN for image delivery
- Implement proper error handling for uploads

### Inventory Management
- Implement atomic inventory updates
- Handle concurrent inventory modifications
- Implement low stock notifications
- Support bulk inventory operations

---

**Product Service API v1.0.0**
**Last Updated**: December 2024
**Contact**: product-service-team@dropslot.com