# DropSlot Project Documentation

## Overview

This directory contains all project documentation for the DropSlot e-commerce reservation platform.

## Documentation Structure

### 📋 Project Specifications
- **[`spec.md`](spec.md)** - Complete project specification with requirements breakdown
- **[`dropslot-requirements.md`](dropslot-requirements.md)** - Original detailed requirements document

### 🔌 API Documentation
- **[`apis/README.md`](apis/README.md)** - API specifications overview (per-service OpenAPI markdown)

### 🗄️ Database Design
- **[`database-design.md`](database-design.md)** - Database schema with Mermaid ER diagram
- **[`../er/diagram.puml`](../er/diagram.puml)** - Original PlantUML ER diagram

### 📚 Project Guidance
- **[`documentation-timing-recommendation.md`](documentation-timing-recommendation.md)** - Implementation strategy and timing recommendations
- **[`project-reference.md`](project-reference.md)** - Quick reference guide for development

## Quick Reference

### Tech Stack
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Backend**: Spring Boot 3, Java 21
- **Database**: PostgreSQL 16
- **DevOps**: Docker, Kubernetes, Rancher Desktop

### Key Features
- ⏰ **Time-boxed Drops**: Schedule product releases with capacity limits
- 📱 **Mobile-First**: PWA with QR code check-in functionality
- 🔐 **Multi-tenant**: Store and branch management system
- 📊 **Analytics**: Comprehensive reporting and metrics
- 🔗 **Integrations**: Payment processors, email, SMS, and webhooks

## Development Workflow

1. **📋 [Project Specification](spec.md)** - Start with project overview and requirements
2. **🔌 [API Specifications](apis/README.md)** - Review REST API contracts (per service)
3. **🗄️ [Database Design](database-design.md)** - Understand data architecture
4. **📚 [Reference Guide](project-reference.md)** - Quick development reference
5. **💻 Implementation** - Begin coding with approved specifications

## File Organization

```
dropslot/
├── 📖 README.md                    # Project overview (GitHub landing page)
├── 📁 docs/                        # All documentation
│   ├── 📖 README.md               # Documentation index (this file)
│   ├── 📋 spec.md                 # Project specification
│   ├── 📁 apis/                   # API specification suite
│   ├── 🗄️ database-design.md      # Database schema
│   ├── 📚 documentation-timing-recommendation.md
│   ├── 🎯 project-reference.md    # Quick reference guide
│   └── 📋 dropslot-requirements.md # Original requirements
├── 📁 er/                          # Entity diagrams
│   └── diagram.puml                # PlantUML ER diagram
├── 📁 src/                         # Source code (future)
└── 📁 .vscode/                     # VS Code settings
```

## Documentation Index

| Document | Description | Purpose | Status |
|----------|-------------|---------|--------|
| 📖 **[README.md](../README.md)** | Project overview | GitHub landing page | ✅ Complete |
| 📋 **[spec.md](spec.md)** | Project specification | Requirements & features | ✅ Complete |
| 🔌 **[apis/README.md](apis/README.md)** | API specifications | REST API contracts | ✅ Complete |
| 🗄️ **[database-design.md](database-design.md)** | Database design | Schema & ER diagram | ✅ Complete |
| 📚 **[documentation-timing-recommendation.md](documentation-timing-recommendation.md)** | Implementation strategy | Development guidance | ✅ Complete |
| 🎯 **[project-reference.md](project-reference.md)** | Quick reference | Development reference | ✅ Complete |
| 📋 **[dropslot-requirements.md](dropslot-requirements.md)** | Original requirements | Source document | ✅ Complete |

## Development Phases

### Phase 1: Design & Specification ✅
- [x] Requirements analysis
- [x] API specification (OpenAPI 3.0)
- [x] Database design (Mermaid ER diagram)
- [ ] Architecture review meeting
- [ ] Stakeholder sign-off

### Phase 2: Implementation (Next)
- [ ] Backend development (Spring Boot)
- [ ] Database implementation (PostgreSQL)
- [ ] Frontend development (Next.js)
- [ ] Integration testing
- [ ] User acceptance testing

### Phase 3: Deployment & Optimization
- [ ] Production deployment
- [ ] Performance optimization
- [ ] Monitoring setup
- [ ] Documentation updates

## Key Resources

### 🛠️ Development Setup
- **Local Development**: `skaffold dev`
- **Backend URL**: `http://localhost:8080`
- **Frontend URL**: `http://localhost:3000`
- **API Docs**: `http://localhost:8080/swagger-ui.html`

### 📊 Database
- **Primary**: PostgreSQL 16
- **Alternative**: Oracle 19c
- **ORM**: Spring Data JPA
- **Migrations**: Flyway

### 🔐 Authentication
- **Type**: JWT with refresh tokens
- **Security**: OWASP ASVS Level 2
- **Rate Limiting**: Redis-based

## Contributing

1. Review the **[project specification](spec.md)**
2. Check the **[API documentation](api-specification.md)**
3. Follow the **[database design](database-design.md)**
4. Use the **[reference guide](project-reference.md)** for quick lookups
5. Submit pull requests with clear descriptions

## Support

- 📖 **[Full Documentation](.)** - Complete project docs
- 🐛 **Issues** - Report bugs and request features
- 💬 **Discussions** - General questions and community

---

**📅 Last Updated**: December 2024
**🔖 Version**: 1.0.0
**📧 Contact**: DropSlot Development Team

[![View API Docs](https://img.shields.io/badge/API-OpenAPI%203.0-green)](api-specification.md)
[![View Database Design](https://img.shields.io/badge/Database-Design-orange)](database-design.md)
[![View Project Spec](https://img.shields.io/badge/Spec-Requirements-blue)](spec.md)
