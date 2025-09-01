# DropSlot API Specifications

## Overview

This directory contains comprehensive API specifications for all DropSlot microservices. Each service has detailed OpenAPI 3.0 specifications with complete endpoint documentation, request/response schemas, and authentication requirements.

## API Architecture

### Service Decomposition
DropSlot implements a microservices architecture with clear domain boundaries:

```
API Gateway (Spring Cloud Gateway)
├── User Service - Authentication & User Management
├── Store Service - Multi-tenant Store Operations
├── Product Service - Catalog & Inventory Management
├── Drop Service - Scheduling & Capacity Management
├── Reservation Service - Booking System & Check-in
├── Notification Service - Multi-channel Communications
├── Payment Service - Payment Processing & Transactions
└── Analytics Service - Business Intelligence & Reporting
```

### Communication Patterns
- **Synchronous:** REST APIs with OpenAPI 3.0 specifications
- **Asynchronous:** Event-driven with Apache Kafka
- **Cross-cutting:** Authentication, logging, monitoring

## API Specifications

### Core Business APIs

#### 🌐 [API Gateway Specification](api-gateway-specification.md)
**Purpose:** Unified entry point for external clients
**Endpoints:** 50+ aggregated endpoints
**Features:**
- Request routing and load balancing
- Authentication and rate limiting
- Response transformation and aggregation
- API versioning and documentation

#### 👤 [User Service API](user-service-api.md)
**Purpose:** User management and authentication
**Endpoints:** 15+ user-related endpoints
**Features:**
- User registration and login
- JWT token management
- Profile management
- Role-based access control
- Password reset and email verification

#### 🏪 [Store Service API](store-service-api.md)
**Purpose:** Multi-tenant store management
**Endpoints:** 12+ store management endpoints
**Features:**
- Store creation and configuration
- Branch management with geolocation
- Operating hours and settings
- Store hierarchy and relationships

#### 📦 [Product Service API](product-service-api.md)
**Purpose:** Product catalog and inventory
**Endpoints:** 18+ product management endpoints
**Features:**
- Product CRUD operations
- SKU management per store
- Category and search functionality
- Pricing and currency handling
- Image and media management

#### ⏰ [Drop Service API](drop-service-api.md)
**Purpose:** Drop scheduling and capacity
**Endpoints:** 14+ drop management endpoints
**Features:**
- Drop creation and scheduling
- Time slot management
- Capacity planning and limits
- Reservation rules configuration
- Drop lifecycle management

#### 🎫 [Reservation Service API](reservation-service-api.md)
**Purpose:** Booking system and check-in
**Endpoints:** 16+ reservation endpoints
**Features:**
- Reservation booking and management
- Waitlist functionality
- QR code generation and validation
- Check-in processing
- Reservation state transitions

### Supporting Service APIs

#### 📧 Notification Service API
**Purpose:** Multi-channel communication
**Features:**
- Email notifications (SendGrid)
- SMS notifications (Twilio)
- Push notifications (PWA)
- Template management
- Delivery tracking and retry logic

#### 💳 Payment Service API
**Purpose:** Payment processing and transactions
**Features:**
- Stripe/Omise integration
- Payment intent management
- Refund processing
- Transaction reconciliation
- Payment method storage

#### 📊 Analytics Service API
**Purpose:** Business intelligence and reporting
**Features:**
- Reservation analytics and metrics
- Fill rate and no-show analysis
- Customer behavior tracking
- Performance reporting
- Business intelligence dashboards

## API Standards & Conventions

### HTTP Methods
- `GET` - Retrieve resources (safe, idempotent)
- `POST` - Create resources (non-idempotent)
- `PUT` - Update resources (idempotent)
- `PATCH` - Partial updates (non-idempotent)
- `DELETE` - Remove resources (idempotent)

### Response Format
```json
{
  "data": { /* Primary resource data */ },
  "meta": {
    "timestamp": "2024-01-01T00:00:00Z",
    "requestId": "req-123456",
    "version": "1.0"
  },
  "links": {
    "self": "/api/v1/resource/123",
    "related": "/api/v1/resource/123/related",
    "collection": "/api/v1/resources"
  }
}
```

### Error Handling
```json
{
  "type": "https://api.dropslot.com/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/users",
  "timestamp": "2024-01-01T00:00:00Z",
  "requestId": "req-123456",
  "errors": [
    {
      "field": "email",
      "message": "Email address is required",
      "code": "REQUIRED_FIELD"
    }
  ]
}
```

### Pagination
```json
{
  "data": [ /* Array of resources */ ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "links": {
    "self": "/api/v1/resources?page=0&size=20",
    "next": "/api/v1/resources?page=1&size=20",
    "last": "/api/v1/resources?page=7&size=20"
  }
}
```

## Authentication & Security

### JWT Token Authentication
```bash
# Include in Authorization header
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### API Key Authentication (Service-to-Service)
```bash
# For internal service communication
X-API-Key: sk-1234567890abcdef
X-API-Secret: secret-0987654321fedcba
```

### Rate Limiting
- **Public endpoints:** 100 requests/minute per IP
- **Authenticated users:** 1000 requests/minute per user
- **Store managers:** 2000 requests/minute per user
- **Administrators:** 5000 requests/minute per user

Rate limit headers included in responses:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1704067200
```

## Data Validation

### Request Validation
- **Required fields** clearly marked
- **Data type validation** enforced
- **Business rule validation** implemented
- **Sanitization** for security

### Common Validation Rules
```json
{
  "email": {
    "type": "string",
    "format": "email",
    "maxLength": 255,
    "required": true
  },
  "phone": {
    "type": "string",
    "pattern": "^\\+?[0-9\\s\\-\\(\\)]+$",
    "maxLength": 20,
    "required": false
  },
  "price": {
    "type": "number",
    "minimum": 0,
    "maximum": 999999.99,
    "required": true
  }
}
```

## Versioning Strategy

### URL Path Versioning
```
https://api.dropslot.com/v1/users
https://api.dropslot.com/v1/stores
https://api.dropslot.com/v1/drops
```

### Version Compatibility
- **Major versions** (v1, v2) for breaking changes
- **Minor versions** for backward-compatible additions
- **Deprecation headers** for sunset features
- **Migration guides** for version transitions

## Testing & Quality Assurance

### API Testing Strategy
- **Unit Tests:** Service layer business logic
- **Integration Tests:** API endpoints with database
- **Contract Tests:** API specification compliance
- **End-to-End Tests:** Complete user journeys

### Test Data Management
- **Test Fixtures:** Consistent test data across services
- **Data Isolation:** Separate test databases
- **Cleanup Procedures:** Automatic test data removal
- **Performance Benchmarks:** API response time validation

## Monitoring & Observability

### API Metrics
- **Response Times:** P50, P95, P99 percentiles
- **Error Rates:** 4xx and 5xx status codes
- **Throughput:** Requests per second
- **Availability:** Uptime percentages

### Logging Standards
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "level": "INFO",
  "service": "user-service",
  "requestId": "req-123456",
  "userId": "user-789",
  "method": "POST",
  "path": "/api/v1/users",
  "statusCode": 201,
  "duration": 150,
  "message": "User created successfully"
}
```

## Development Workflow

### API-First Development
1. **Design API** specifications first
2. **Generate client SDKs** from OpenAPI specs
3. **Implement services** against contracts
4. **Test against specifications** for compliance
5. **Deploy with confidence** using contract testing

### Code Generation
```bash
# Generate Spring Boot controllers from OpenAPI
openapi-generator generate \
  -i api-specification.yaml \
  -g spring \
  -o generated-code

# Generate TypeScript clients for frontend
openapi-generator generate \
  -i api-specification.yaml \
  -g typescript-axios \
  -o frontend/src/api
```

## Deployment & Operations

### Environment Configuration
- **Development:** Local development with hot reload
- **Staging:** Integration testing environment
- **Production:** High-availability production environment

### Health Checks
```json
GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Service Discovery
- **Kubernetes DNS** for service-to-service communication
- **ConfigMaps** for environment-specific configuration
- **Secrets** for sensitive configuration data

## Security Considerations

### Data Protection
- **Encryption at Rest:** Database and file storage
- **Encryption in Transit:** TLS 1.3 for all communications
- **Data Sanitization:** Input validation and output encoding
- **Audit Logging:** Comprehensive security event logging

### Compliance
- **GDPR:** Data subject rights and consent management
- **OWASP:** Security best practices implementation
- **PCI DSS:** Payment data security (for payment service)
- **Data Residency:** Geographic data storage requirements

## Performance Optimization

### Caching Strategy
- **Redis** for session and temporary data
- **CDN** for static assets and images
- **Database Query Caching** for frequently accessed data
- **API Response Caching** for public data

### Database Optimization
- **Connection Pooling:** HikariCP configuration
- **Query Optimization:** Proper indexing and query planning
- **Read Replicas:** For high-read scenarios
- **Partitioning:** For large dataset management

## Future Enhancements

### Advanced Features
- **GraphQL API:** For flexible client queries
- **Webhook Support:** Real-time event notifications
- **API Analytics:** Usage patterns and performance insights
- **Rate Limiting:** Advanced throttling strategies

### Technology Evolution
- **gRPC Support:** For high-performance service communication
- **Event Streaming:** Enhanced Apache Kafka integration
- **Service Mesh:** Istio integration for advanced traffic management
- **AI/ML Integration:** Intelligent capacity planning and recommendations

## Support & Documentation

### API Documentation Access
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **ReDoc:** `http://localhost:8080/redoc`

### Developer Resources
- **API Playground:** Interactive API testing
- **SDK Downloads:** Generated client libraries
- **Postman Collections:** Pre-configured API tests
- **Code Examples:** Multi-language implementation samples

## Conclusion

The DropSlot API specifications provide a comprehensive, enterprise-grade foundation for the microservices architecture. Key principles include:

- **Consistency** across all service APIs
- **Security-first** approach with comprehensive authentication
- **Performance optimization** through caching and efficient design
- **Developer experience** with clear documentation and tooling
- **Scalability** through proper abstraction and separation of concerns
- **Maintainability** through standardized patterns and conventions

These specifications ensure that all services work together seamlessly while maintaining clear boundaries and responsibilities. The API-first approach enables parallel development across frontend and backend teams, reducing integration issues and accelerating time-to-market.

For questions or contributions to the API specifications, please refer to the individual service documentation or create an issue in the project repository.