# DropSlot Project Reference Guide

## Core Project Information

**Project Name**: DropSlot
**Domain**: E-commerce / Reservations
**Purpose**: Allow brands/stores to schedule limited product "drops" with time-boxed booking slots

## Tech Stack

### Frontend
- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **PWA** - Progressive Web App capabilities

### Backend
- **Spring Boot 3** - Java microservices framework
- **PostgreSQL 16** - Primary database per service
- **Redis** - Caching and session management
- **Liquibase** - Database schema management with rollback support

### DevOps
- **Docker** - Containerization
- **Kubernetes** - Orchestration via Rancher Desktop
- **Skaffold + Jib** - Build and deployment automation
- **GitHub Actions** - CI/CD pipelines

## User Roles & Permissions

1. **Guest** - Browse drops and register
2. **Customer** - Make reservations and manage profile
3. **Store Manager** - Manage stores, products, and drops
4. **Admin** - System administration and analytics
5. **Support** - Customer assistance (read-only)

## Service Architecture

### Core Services
- **User Service** - Authentication, profiles, roles
- **Store Service** - Multi-tenant stores, branches, locations
- **Product Service** - Product catalog, inventory, search
- **Drop Service** - Drop scheduling, capacity management
- **Reservation Service** - Booking system, waitlist management
- **Notification Service** - Multi-channel communications
- **Payment Service** - Payment processing and transactions
- **Analytics Service** - Business intelligence and reporting

### Infrastructure Services
- **API Gateway** - Request routing and load balancing
- **Service Discovery** - Dynamic service location
- **Configuration Service** - Centralized configuration management

## Database Schema

### Core Entities (13 Tables)

#### Users Management
- **users** - User accounts and authentication
- **roles** - System roles and permissions
- **user_roles** - User-role assignments
- **user_sessions** - Active user sessions
- **user_preferences** - User settings and preferences
- **user_addresses** - User address book
- **user_devices** - Device registrations for push notifications

#### Security & Verification
- **email_verification_tokens** - Email verification
- **password_reset_tokens** - Password reset functionality

#### Business Entities
- **stores** - Multi-tenant store management
- **branches** - Physical store locations
- **products** - Product catalog and inventory
- **drops** - Time-boxed product releases
- **slots** - Capacity-managed time slots
- **reservations** - Confirmed bookings
- **waitlist** - Queue management for oversubscribed slots

#### Supporting Entities
- **payments** - Financial transactions
- **notifications** - Communication records
- **webhook_endpoints** - External integration configuration
- **webhook_events** - Integration event tracking
- **audit_logs** - Compliance and security logging

## API Endpoints Summary

### Authentication (User Service)
- `POST /auth/register` - User registration
- `POST /auth/login` - Authentication
- `POST /auth/refresh` - Token refresh
- `GET /users/me` - Current user profile
- `PUT /users/me` - Update profile

### Store Management (Store Service)
- `POST /stores` - Create store
- `GET /stores/{id}` - Get store details
- `PUT /stores/{id}` - Update store
- `POST /stores/{id}/branches` - Add branch
- `GET /branches` - List branches

### Product Management (Product Service)
- `POST /products` - Create product
- `GET /products` - List products with search
- `GET /products/{id}` - Get product details
- `PUT /products/{id}` - Update product
- `DELETE /products/{id}` - Delete product

### Drop Management (Drop Service)
- `POST /drops` - Create drop
- `GET /drops` - List drops with filters
- `GET /drops/{id}` - Get drop details
- `PUT /drops/{id}` - Update drop
- `GET /drops/{id}/slots` - Get drop slots

### Reservation System (Reservation Service)
- `POST /reservations` - Create reservation
- `GET /reservations/me` - User's reservations
- `DELETE /reservations/{id}` - Cancel reservation
- `POST /waitlist` - Join waitlist
- `GET /waitlist/{dropId}` - Check waitlist position

### Check-in System (Reservation Service)
- `GET /tickets/{reservationId}/qr` - Get QR code
- `POST /checkin` - Process check-in
- `GET /checkin/history` - Check-in history

### Admin & Analytics (Analytics Service)
- `GET /admin/metrics` - System metrics
- `GET /admin/analytics/reservations` - Reservation analytics
- `GET /admin/analytics/fill-rate` - Fill rate analysis
- `GET /admin/audit-logs` - Audit logs

## Business Rules & Constraints

### Reservation Rules
- **One active reservation per user per drop**
- **Reservation states**: PENDING → CONFIRMED → CHECKED_IN/CANCELLED/EXPIRED
- **Waitlist auto-promotion** when capacity becomes available
- **Per-user limits** enforced at service layer

### Capacity Management
- **Total capacity** = Reserved + Waitlist positions
- **Reserved ≤ Total capacity**
- **Waitlist activated** when Reserved = Total capacity
- **Real-time capacity updates** via WebSocket/SSE

### Security Constraints
- **Row-level security** for tenant isolation
- **Signed URLs** for QR codes and tickets
- **Rate limiting** per IP/device/user
- **Comprehensive audit logging**

## Development Environment

### Local Development Setup
```bash
# Start infrastructure services
docker-compose up -d

# Start application services
skaffold dev

# Access services
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
# PgAdmin: http://localhost:5050
# MailHog: http://localhost:8025
```

### Database Configuration
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Database**: dropslot
- **Username**: dropslot
- **Password**: dropslot123

### Service Ports
- **User Service**: 8081
- **Store Service**: 8082
- **Product Service**: 8083
- **Drop Service**: 8084
- **Reservation Service**: 8085
- **API Gateway**: 8080

## Liquibase Migration System

### Migration Structure
```
user-service/src/main/resources/db/changelog/
├── db.changelog-master.xml          # Master changelog
└── 01-create-user-tables.sql        # SQL migrations
```

### Migration Commands
```bash
# Apply migrations
liquibase update

# Rollback to specific changeset
liquibase rollback 001-create-extensions

# Check migration status
liquibase status

# Validate migrations
liquibase validate
```

### Rollback Support
- **Complete reversibility** for all changesets
- **Safe rollback** with dependency management
- **Production-ready** with transaction safety
- **Audit trail** of all schema changes

## Security Implementation

### Authentication & Authorization
- **JWT tokens** with refresh mechanism
- **Role-based access control** (RBAC)
- **Multi-tenant isolation** at data level
- **Password hashing** with BCrypt

### API Security
- **Bearer token authentication**
- **Rate limiting** and DDoS protection
- **Input validation** and sanitization
- **CORS configuration** for frontend domains

### Data Protection
- **Encryption at rest** for sensitive data
- **TLS 1.3** for data in transit
- **Row-level security** (RLS) in PostgreSQL
- **Audit logging** for compliance

## Performance & Scalability

### Database Optimization
- **Primary key indexes** (automatic)
- **Foreign key indexes** for joins
- **Composite indexes** for query patterns
- **Partial indexes** for filtered queries

### Caching Strategy
- **Redis** for session management
- **Application-level caching** for reference data
- **HTTP caching** for static resources
- **Database query caching** for expensive operations

### Query Optimization
- **N+1 query prevention** with JOIN FETCH
- **Pagination** for large result sets
- **Read replicas** for reporting queries
- **Connection pooling** with HikariCP

## Monitoring & Observability

### Application Metrics
- **Spring Boot Actuator** for health checks
- **Prometheus** for metrics collection
- **Grafana** for visualization
- **Custom business metrics** (reservation rates, etc.)

### Logging Strategy
- **Structured logging** with correlation IDs
- **ELK Stack** for log aggregation
- **Log levels** based on environment
- **Retention policies** for compliance

### Distributed Tracing
- **OpenTelemetry** for tracing
- **Jaeger** for trace visualization
- **End-to-end request tracking**
- **Performance bottleneck identification**

## Deployment Strategy

### Development Environment
- **Skaffold** for local development with hot reload
- **Local Kubernetes** via Rancher Desktop
- **Database per developer** pattern
- **Hot reload** for rapid iteration

### Staging Environment
- **Automated deployments** from develop branch
- **Integration testing** with real services
- **Performance testing** and load testing
- **Security scanning** and validation

### Production Environment
- **Kubernetes** with horizontal pod autoscaling
- **Blue-green deployments** for zero downtime
- **Multi-region deployment** for high availability
- **Automated rollback** capabilities

## API Standards & Conventions

### HTTP Status Codes
- **200 OK** - Successful request
- **201 Created** - Resource created
- **204 No Content** - Successful request, no content
- **400 Bad Request** - Validation errors
- **401 Unauthorized** - Authentication required
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **409 Conflict** - Business rule violation
- **429 Too Many Requests** - Rate limit exceeded
- **500 Internal Server Error** - System error

### Content Types
- **Request**: `application/json`
- **Response**: `application/json`
- **File Upload**: `multipart/form-data`
- **Binary Data**: `application/octet-stream`

### Date/Time Format
- **ISO 8601**: `2024-01-01T00:00:00Z`
- **Timezone**: UTC for all timestamps
- **Storage**: `TIMESTAMP WITH TIME ZONE`

### Currency Handling
- **Storage**: Integer cents (e.g., $19.99 = 1999)
- **Display**: Localized formatting
- **Precision**: 2 decimal places
- **Currency Codes**: ISO 4217 standard

## Error Handling

### Error Response Format
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

### Common Error Codes
- **VALIDATION_ERROR** - Input validation failed
- **RESOURCE_NOT_FOUND** - Entity not found
- **INSUFFICIENT_PERMISSIONS** - Access denied
- **BUSINESS_RULE_VIOLATION** - Business logic error
- **EXTERNAL_SERVICE_ERROR** - Third-party service failure
- **RATE_LIMIT_EXCEEDED** - Too many requests

## Testing Strategy

### Unit Testing
- **Framework**: JUnit 5 + Mockito
- **Coverage**: >80% target
- **Focus**: Business logic isolation
- **Mocking**: External dependencies

### Integration Testing
- **Framework**: Spring Boot Test
- **Database**: Testcontainers
- **Scope**: Service layer testing
- **Coverage**: API endpoints and data flow

### End-to-End Testing
- **Framework**: Cucumber + Selenium
- **Scope**: Complete user journeys
- **Environment**: Staging environment
- **Automation**: CI/CD pipeline integration

## Implementation Phases

### Phase 1: Foundation (4 weeks)
- User Service with authentication
- Store Service for multi-tenant management
- Product Service for catalog management
- Basic API Gateway setup

### Phase 2: Core Business Logic (4 weeks)
- Drop Service for scheduling
- Reservation Service for booking
- Waitlist management
- QR code generation

### Phase 3: Advanced Features (3 weeks)
- Notification Service
- Payment Service integration
- Analytics Service
- Advanced API Gateway features

### Phase 4: Production & Scale (3 weeks)
- Performance optimization
- Monitoring and alerting
- Security hardening
- Production deployment

## Success Metrics

### Technical Metrics
- **API Response Time**: <300ms (P95)
- **System Uptime**: 99.9% availability
- **Test Coverage**: >80% code coverage
- **Security Score**: OWASP A rating

### Business Metrics
- **User Registration**: Target 1000/day
- **Reservation Conversion**: >70% fill rate
- **Customer Satisfaction**: >4.5/5 rating
- **Time to Market**: <6 months to production

## Compliance & Security

### GDPR Compliance
- **Data Subject Rights**: Access, rectification, erasure
- **Consent Management**: Granular permission controls
- **Data Portability**: Export user data functionality
- **Breach Notification**: Automated security monitoring

### Security Standards
- **OWASP ASVS Level 2** compliance
- **ISO 27001** information security
- **PCI DSS** for payment processing
- **Regular security audits** and penetration testing

## Future Enhancements

### Advanced Features
- **Real-time Notifications** via WebSocket
- **AI-powered Recommendations** for products
- **Advanced Analytics** with machine learning
- **Mobile App** with native capabilities
- **Multi-language Support** for internationalization

### Technology Evolution
- **GraphQL API** for flexible queries
- **Event Streaming** with Apache Kafka
- **Service Mesh** with Istio
- **Serverless Functions** for specific workloads

## Support & Resources

### Documentation
- **[API Specifications](.)** - Complete API documentation
- **[Architecture Guide](../microservices-architecture.md)** - System design
- **[Database Design](../database-design.md)** - Schema documentation
- **[Implementation Plan](../backend-implementation-plan.md)** - Development roadmap

### Development Tools
- **PgAdmin**: Database management
- **Redis Commander**: Cache management
- **MailHog**: Email testing
- **Prometheus/Grafana**: Monitoring

### Team Resources
- **GitHub Repository**: Source code and issues
- **Confluence**: Internal documentation
- **Slack**: Team communication
- **Jira**: Project management

---

**This reference guide should be used for all future development decisions and implementation tasks related to the DropSlot project.**

*Last Updated: December 2024*
*Version: 1.0.0*