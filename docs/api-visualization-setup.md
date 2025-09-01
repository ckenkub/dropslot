# API Visualization Setup Guide

## Quick Start - Visualize Your APIs Now!

### 🚀 **Method 1: Online Swagger Editor (Immediate)**

1. **Open**: https://editor.swagger.io/
2. **Copy** any YAML section from your API specs (between ````yaml` and `````)
3. **Paste** into the left panel
4. **See** interactive documentation instantly!

**Try it now with this sample from User Service:**

```yaml
openapi: 3.0.3
info:
  title: User Service API
  description: User management and authentication service
  version: 1.0.0
servers:
  - url: http://localhost:8081/api/v1
paths:
  /auth/register:
    post:
      summary: Register a new user account
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
                  minLength: 8
                name:
                  type: string
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
                type: object
                properties:
                  id:
                    type: string
                    format: uuid
                  email:
                    type: string
                  name:
                    type: string
```

### 🎯 **Method 2: Desktop Applications (Recommended)**

#### Postman (Free & Powerful)
```bash
# 1. Download Postman: https://postman.com/downloads
# 2. Import API spec:
#    - Click "Import" button
#    - Select "Link" tab
#    - Paste: https://editor.swagger.io/ (then copy YAML)
# 3. View documentation in "APIs" tab
```

#### Insomnia (Free & Developer-Friendly)
```bash
# 1. Download: https://insomnia.rest/download
# 2. Create new request collection
# 3. Import from URL: https://editor.swagger.io/
# 4. View documentation panel
```

### 🐳 **Method 3: Docker (Professional)**

#### Quick Swagger UI Setup
```bash
# Create a simple docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  swagger-ui:
    image: swaggerapi/swagger-ui
    ports:
      - "8080:8080"
    environment:
      SWAGGER_JSON: /app/openapi.yaml
    volumes:
      - ./docs/apis/user-service.yaml:/app/openapi.yaml
EOF

# Run it
docker-compose up -d

# Open: http://localhost:8080
```

## 📋 **Step-by-Step: Extract YAML from Markdown**

### Manual Extraction (Quick)
1. **Open** any API spec file (e.g., `docs/apis/user-service-api.md`)
2. **Find** YAML sections between ````yaml` and ````` markers
3. **Copy** the YAML content
4. **Paste** into https://editor.swagger.io/
5. **View** interactive documentation!

### Automated Extraction Script

Create `extract-yaml.js` in your project root:

```javascript
const fs = require('fs');
const path = require('path');

function extractYamlFromMarkdown(markdownPath, outputPath) {
  const content = fs.readFileSync(markdownPath, 'utf8');

  // Extract YAML between ```yaml code blocks
  const yamlRegex = /```yaml\s*([\s\S]*?)```/g;
  let yamlContent = '';
  let match;

  while ((match = yamlRegex.exec(content)) !== null) {
    yamlContent += match[1] + '\n';
  }

  // Add OpenAPI header if not present
  if (!yamlContent.includes('openapi:')) {
    yamlContent = `openapi: 3.0.3
info:
  title: ${path.basename(markdownPath, '.md').replace('-', ' ').toUpperCase()}
  version: 1.0.0
${yamlContent}`;
  }

  fs.writeFileSync(outputPath, yamlContent);
  console.log(`✅ Extracted YAML: ${outputPath}`);
}

// Extract all API specs
const apiFiles = [
  'docs/apis/user-service-api.md',
  'docs/apis/store-service-api.md',
  'docs/apis/product-service-api.md',
  'docs/apis/drop-service-api.md',
  'docs/apis/reservation-service-api.md'
];

apiFiles.forEach(file => {
  const outputFile = file.replace('.md', '.yaml');
  if (fs.existsSync(file)) {
    extractYamlFromMarkdown(file, outputFile);
  }
});
```

**Run the script:**
```bash
# Install Node.js first, then:
node extract-yaml.js
```

## 🎨 **Advanced Visualization Options**

### Generate HTML Documentation
```bash
# Install ReDoc CLI
npm install -g redoc-cli

# Generate beautiful HTML docs
redoc-cli bundle docs/apis/user-service.yaml \
  --output docs/api-docs/user-service/index.html \
  --title "User Service API"

# Open in browser
open docs/api-docs/user-service/index.html
```

### Create Documentation Portal
```bash
# Generate all service docs
mkdir -p docs/api-docs

for service in user-service store-service product-service drop-service reservation-service; do
  redoc-cli bundle "docs/apis/${service}.yaml" \
    --output "docs/api-docs/${service}/index.html" \
    --title "${service} API"
done

# Create index page
cat > docs/api-docs/index.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>DropSlot API Documentation</title>
    <style>
        .api-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; padding: 20px; }
        .api-card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; text-align: center; }
        .api-card:hover { box-shadow: 0 4px 8px rgba(0,0,0,0.1); }
    </style>
</head>
<body>
    <h1>🛍️ DropSlot API Documentation</h1>
    <div class="api-grid">
        <div class="api-card">
            <h3>🌐 API Gateway</h3>
            <p>External unified API</p>
            <a href="api-gateway/index.html">📖 View Docs</a>
        </div>
        <div class="api-card">
            <h3>👤 User Service</h3>
            <p>Authentication & users</p>
            <a href="user-service/index.html">📖 View Docs</a>
        </div>
        <div class="api-card">
            <h3>🏪 Store Service</h3>
            <p>Stores & branches</p>
            <a href="store-service/index.html">📖 View Docs</a>
        </div>
        <div class="api-card">
            <h3>📦 Product Service</h3>
            <p>Catalog & inventory</p>
            <a href="product-service/index.html">📖 View Docs</a>
        </div>
        <div class="api-card">
            <h3>⏰ Drop Service</h3>
            <p>Scheduling & capacity</p>
            <a href="drop-service/index.html">📖 View Docs</a>
        </div>
        <div class="api-card">
            <h3>🎫 Reservation Service</h3>
            <p>Booking & check-in</p>
            <a href="reservation-service/index.html">📖 View Docs</a>
        </div>
    </div>
</body>
</html>
EOF
```

## 🛠️ **Developer Tools Integration**

### VS Code Extensions
```json
// Add to .vscode/extensions.json
{
  "recommendations": [
    "42Crunch.vscode-openapi",
    "redocly.openapi-vs-code",
    "mermade.openapi-lint"
  ]
}
```

### IntelliJ IDEA Integration
- **OpenAPI Plugin**: Built-in support for OpenAPI specs
- **REST Client**: Test endpoints directly from IDE
- **HTTP Client**: Create request files for testing

## 📊 **Comparison: Visualization Tools**

| Tool | Setup Time | Features | Best For |
|------|------------|----------|----------|
| **Swagger Editor** | 0 min | Live editing, validation | Quick prototyping |
| **Postman** | 5 min | Testing, collections, sharing | API testing & collaboration |
| **Insomnia** | 5 min | Clean UI, environments | Development workflow |
| **ReDoc** | 10 min | Beautiful docs, responsive | Production documentation |
| **Stoplight** | 15 min | Design, mocking, Git sync | Team collaboration |

## 🎯 **Recommended Workflow**

### For Individual Development
1. **Start with Swagger Editor** - Quick validation and visualization
2. **Move to Postman/Insomnia** - Test and document API usage
3. **Generate ReDoc** - Create shareable documentation

### For Team Development
1. **Set up Stoplight** - Collaborative API design
2. **Use GitHub integration** - Version control API specs
3. **Automate documentation** - Generate docs in CI/CD

### For Production
1. **Deploy ReDoc** - Public API documentation
2. **Set up developer portal** - Centralized API resources
3. **Monitor API usage** - Analytics and performance tracking

## 🚀 **Quick Test - Try It Now!**

1. **Go to**: https://editor.swagger.io/
2. **Copy** the sample YAML above
3. **Paste** into the editor
4. **Explore** the interactive documentation!

## 📞 **Need Help?**

- **Swagger Documentation**: https://swagger.io/docs/
- **OpenAPI Guide**: https://spec.openapis.org/
- **ReDoc Tutorials**: https://redocly.com/docs/
- **Postman Learning**: https://learning.postman.com/

---

**🎉 Your API specifications are ready to visualize! Choose the method that works best for your workflow.**