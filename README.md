# DropSlot - E-commerce Reservation Platform

[![Documentation](https://img.shields.io/badge/docs-view-blue)](docs/)
[![API Spec](https://img.shields.io/badge/API-OpenAPI%203.0-green)](docs/apis/api-gateway-specification.md)
[![Database](https://img.shields.io/badge/Database-Design-orange)](docs/database-design.md)

> A comprehensive e-commerce reservation platform that enables brands and stores to schedule limited product "drops" with time-boxed booking slots.

## 🚀 Quick Start

- **[📖 Full Documentation](docs/)** - Complete project documentation
- **[🔌 API Gateway Specification](docs/apis/api-gateway-specification.md)** - External client-facing APIs
- **[👤 User Service API](docs/apis/user-service-api.md)** - Authentication and user management
- **[🗄️ Database Design](docs/database-design.md)** - Schema and ER diagrams
- **[📋 Project Specification](docs/spec.md)** - Detailed requirements and features

## 🎯 Overview

DropSlot is a full-stack application that revolutionizes how brands manage limited product releases. Customers can reserve time slots for exclusive drops, join waitlists, and check in with QR codes.

### Key Features
- ⏰ **Time-boxed Drops**: Schedule product releases with capacity limits
- 📱 **Mobile-First**: PWA with QR code check-in functionality
- 🔐 **Multi-tenant**: Store and branch management system
- 📊 **Analytics**: Comprehensive reporting and metrics
- 🔗 **Integrations**: Payment processors, email, SMS, and webhooks

## 🏗️ Architecture

DropSlot implements a **microservices architecture** with 8 core business services:

### Core Services
- **👤 User Service** - Authentication, profiles, roles
- **🏪 Store Service** - Multi-tenant stores, branches, locations
- **📦 Product Service** - Product catalog, inventory, search
- **⏰ Drop Service** - Drop scheduling, capacity management
- **🎫 Reservation Service** - Booking system, waitlist management
- **📧 Notification Service** - Multi-channel communications
- **💳 Payment Service** - Payment processing and transactions
- **📊 Analytics Service** - Business intelligence and reporting

### Infrastructure
- **🌐 API Gateway** - Request routing and load balancing
- **🔍 Service Discovery** - Dynamic service location
- **⚙️ Configuration Service** - Centralized configuration management
- **🔗 Service Mesh (Istio)** - Service-to-service communication

## 🛠️ Tech Stack

### Frontend
- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **PWA** - Progressive Web App capabilities

### Backend Services
- **Spring Boot 3** - Java microservices framework
- **PostgreSQL 16** - Primary database per service
- **Redis** - Caching and session management
- **OpenAPI 3.0** - API documentation

### DevOps
- **Docker** - Containerization
- **Kubernetes** - Orchestration via Rancher Desktop
- **GitHub Actions** - CI/CD pipelines

## 📁 Project Structure

```
dropslot/
├── 📖 README.md                    # This file (project overview)
├── 📁 docs/                        # All documentation
│   ├── 📖 README.md               # Documentation index
│   ├── 📋 spec.md                 # Project specification
│   ├── 🏗️ microservices-architecture.md # Service architecture
│   ├── 📁 apis/                   # API specification suite
│   │   ├── 📖 README.md          # API specs overview
│   │   ├── 🌐 api-gateway-specification.md # External APIs
│   │   ├── 👤 user-service-api.md # User service detailed API
│   │   ├── 🏪 store-service-api.md # Store service API
│   │   ├── 📦 product-service-api.md # Product service API
│   │   ├── ⏰ drop-service-api.md # Drop service API
│   │   └── 🎫 reservation-service-api.md # Reservation service API
│   ├── 🗄️ database-design.md      # Database schema
│   ├── 🎯 project-reference.md    # Quick reference guide
│   ├── 📋 dropslot-requirements.md # Original requirements
│   ├── 📚 documentation-timing-recommendation.md
│   ├── 👁️ api-visualization-setup.md # Visualization guide
│   ├── ⚡ api-visualization-setup.md # Quick setup guide
│   └── 🎨 frontend-setup-guide.md # Frontend setup guide
├── 📁 er/                          # Entity diagrams
│   └── diagram.puml                # PlantUML ER diagram
├── 📁 src/                         # Source code (future)
└── 📁 .vscode/                     # VS Code settings
```

## 📚 Documentation

### 📋 Core Documentation

| Document | Description | Link |
|----------|-------------|------|
| 📋 **Project Spec** | Complete requirements and features | [docs/spec.md](docs/spec.md) |
| 🏗️ **Microservices Architecture** | Service design and communication | [docs/microservices-architecture.md](docs/microservices-architecture.md) |
| 🌐 **API Gateway Specification** | External client-facing APIs | [docs/apis/api-gateway-specification.md](docs/apis/api-gateway-specification.md) |
| 👤 **User Service API** | Authentication and user management | [docs/apis/user-service-api.md](docs/apis/user-service-api.md) |
| 🏪 **Store Service API** | Multi-tenant stores & branches | [docs/apis/store-service-api.md](docs/apis/store-service-api.md) |
| 📦 **Product Service API** | Product catalog & inventory | [docs/apis/product-service-api.md](docs/apis/product-service-api.md) |
| ⏰ **Drop Service API** | Drop scheduling & capacity | [docs/apis/drop-service-api.md](docs/apis/drop-service-api.md) |
| 🎫 **Reservation Service API** | Booking system & check-in | [docs/apis/reservation-service-api.md](docs/apis/reservation-service-api.md) |
| 🗄️ **Database Design** | Schema design and ER diagrams | [docs/database-design.md](docs/database-design.md) |
| 🎯 **Quick Reference** | Development reference guide | [docs/project-reference.md](docs/project-reference.md) |
| 📋 **Original Requirements** | Source requirements document | [docs/dropslot-requirements.md](docs/dropslot-requirements.md) |
| 📚 **Implementation Strategy** | Development timing recommendations | [docs/documentation-timing-recommendation.md](docs/documentation-timing-recommendation.md) |
| 👁️ **API Visualization Guide** | How to visualize API specs | [docs/api-visualization-setup.md](docs/api-visualization-setup.md) |
| ⚡ **API Visualization Setup** | Quick setup for API visualization | [docs/api-visualization-setup.md](docs/api-visualization-setup.md) |
| 🎨 **Frontend Setup Guide** | Next.js project setup and architecture | [docs/frontend-setup-guide.md](docs/frontend-setup-guide.md) |

### 🗂️ Additional Resources

| Resource | Description | Location |
|----------|-------------|----------|
| 🏗️ **Entity Diagrams** | PlantUML ER diagram | [er/diagram.puml](er/diagram.puml) |
| ⚙️ **VS Code Config** | Development environment settings | [.vscode/](.vscode/) |

## 🎯 User Roles

- **👤 Guest** - Browse drops and register
- **🛒 Customer** - Make reservations and manage profile
- **🏪 Store Manager** - Manage stores, products, and drops
- **⚙️ Admin** - System administration and analytics
- **💬 Support** - Customer assistance (read-only)

## 🚀 Getting Started

### Prerequisites
- Java 21
- Node.js 18+
- Docker & Rancher Desktop
- PostgreSQL 16

### Local Development
```bash
# Start local development environment
skaffold dev

# Access services
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
```

### Development Phases

1. **📋 [Design & Specification](docs/spec.md)** - Requirements and design ✅
2. **🔌 [API Design](docs/apis/api-gateway-specification.md)** - REST API contracts ✅
3. **🗄️ [Database Design](docs/database-design.md)** - Schema and relationships ✅
4. **💻 Implementation** - Backend and frontend development
5. **🚀 Deployment** - Production environment setup

## 📊 Database Schema

The system uses PostgreSQL with 13 core entities across 8 services:

- **Users** - Authentication and profiles
- **Roles** - Permission management
- **Stores** - Multi-tenant store management
- **Branches** - Physical store locations
- **Products** - Catalog items
- **Drops** - Time-boxed product releases
- **Slots** - Capacity-managed time slots
- **Reservations** - Confirmed bookings
- **Waitlist** - Queue management
- **Payments** - Financial transactions
- **Notifications** - Communication system
- **Webhook Endpoints** - External integrations
- **Webhook Events** - Integration tracking
- **Audit Logs** - Compliance and security

## 🔌 API Overview

### Authentication (User Service)
- `POST /auth/register` - User registration
- `POST /auth/login` - Authentication
- `POST /auth/refresh` - Token refresh

### Core Services APIs
- `POST /stores` - Store management
- `POST /products` - Product catalog
- `POST /drops` - Drop scheduling
- `POST /reservations` - Booking system
- `POST /payments` - Payment processing
- `GET /analytics/metrics` - Business metrics

### API Standards
- RESTful design with OpenAPI 3.0
- RFC 7807 Problem JSON for errors
- Idempotency keys for POST operations
- Pagination and sorting support

## 🤝 Contributing

1. Review the **[project specification](docs/spec.md)**
2. Check the **[microservices architecture](docs/microservices-architecture.md)**
3. Follow the **[API Gateway specification](docs/apis/api-gateway-specification.md)**
4. Use the **[reference guide](docs/project-reference.md)** for quick lookups
5. Submit pull requests with clear descriptions

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

- 📖 **[Documentation](docs/)** - Complete project docs
- 🐛 **Issues** - Report bugs and request features
- 💬 **Discussions** - General questions and community

---

**Built with ❤️ for seamless e-commerce reservations**

[![View Architecture](https://img.shields.io/badge/🏗️-Microservices%20Architecture-blue)](docs/microservices-architecture.md)
[![View API Gateway](https://img.shields.io/badge/🌐-API%20Gateway-green)](docs/apis/api-gateway-specification.md)
[![View User Service](https://img.shields.io/badge/👤-User%20Service-orange)](docs/apis/user-service-api.md)
[![View Database Design](https://img.shields.io/badge/Database-Design-purple)](docs/database-design.md)
[![View Project Spec](https://img.shields.io/badge/Spec-Requirements-blue)](docs/spec.md)
