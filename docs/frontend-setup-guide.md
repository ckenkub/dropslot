# DropSlot Frontend Setup Guide

## Overview

This guide provides a comprehensive plan for setting up the Next.js frontend application for the DropSlot e-commerce reservation platform.

## 🎯 **Frontend Architecture**

### **Tech Stack**
- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: TanStack Query + React Context
- **Forms**: React Hook Form + Zod validation
- **Authentication**: NextAuth.js
- **PWA**: Service Workers + Web App Manifest

### **Key Features**
- **Responsive Design**: Mobile-first approach
- **PWA Support**: Offline functionality, push notifications
- **Real-time Updates**: WebSocket/SSE integration
- **Accessibility**: WCAG 2.1 AA compliance
- **Performance**: Optimized loading and caching

## 📁 **Project Structure**

```
frontend/
├── 📁 public/                    # Static assets
│   ├── favicon.ico              # App favicon
│   ├── manifest.json            # PWA manifest
│   ├── icons/                   # PWA icons
│   └── robots.txt               # SEO robots file
├── 📁 src/
│   ├── 📁 app/                  # Next.js App Router
│   │   ├── 📁 (auth)/           # Authentication routes
│   │   │   ├── login/           # Login page
│   │   │   ├── register/        # Registration page
│   │   │   └── forgot-password/ # Password reset
│   │   ├── 📁 (dashboard)/      # Protected routes
│   │   │   ├── stores/          # Store management
│   │   │   ├── products/        # Product catalog
│   │   │   ├── drops/           # Drop management
│   │   │   └── reservations/    # Booking system
│   │   ├── 📁 api/              # API routes (if needed)
│   │   ├── globals.css          # Global styles
│   │   ├── layout.tsx           # Root layout
│   │   ├── page.tsx             # Home page
│   │   └── loading.tsx          # Loading UI
│   ├── 📁 components/           # Reusable components
│   │   ├── 📁 ui/               # Base UI components
│   │   │   ├── Button.tsx       # Button component
│   │   │   ├── Input.tsx        # Input component
│   │   │   ├── Modal.tsx        # Modal component
│   │   │   └── Table.tsx        # Table component
│   │   ├── 📁 forms/            # Form components
│   │   │   ├── LoginForm.tsx    # Login form
│   │   │   ├── ProductForm.tsx  # Product form
│   │   │   └── ReservationForm.tsx # Reservation form
│   │   ├── 📁 layout/           # Layout components
│   │   │   ├── Header.tsx       # App header
│   │   │   ├── Sidebar.tsx      # Navigation sidebar
│   │   │   └── Footer.tsx       # App footer
│   │   └── 📁 features/         # Feature-specific components
│   │       ├── 📁 auth/         # Authentication components
│   │       ├── 📁 stores/       # Store management components
│   │       ├── 📁 products/     # Product components
│   │       ├── 📁 drops/        # Drop components
│   │       └── 📁 reservations/ # Reservation components
│   ├── 📁 lib/                  # Utility libraries
│   │   ├── 📁 api/              # API client functions
│   │   │   ├── client.ts        # HTTP client setup
│   │   │   ├── auth.ts          # Authentication API
│   │   │   ├── stores.ts        # Store API
│   │   │   ├── products.ts      # Product API
│   │   │   ├── drops.ts         # Drop API
│   │   │   └── reservations.ts  # Reservation API
│   │   ├── 📁 auth/             # Authentication utilities
│   │   │   ├── config.ts        # Auth configuration
│   │   │   └── providers.ts     # Auth providers
│   │   ├── 📁 utils/            # General utilities
│   │   │   ├── cn.ts            # Class name utility
│   │   │   ├── format.ts        # Data formatting
│   │   │   └── validation.ts    # Validation helpers
│   │   ├── 📁 hooks/            # Custom React hooks
│   │   │   ├── useAuth.ts       # Authentication hook
│   │   │   ├── useStores.ts     # Store management hook
│   │   │   └── useReservations.ts # Reservation hook
│   │   ├── 📁 types/            # TypeScript type definitions
│   │   │   ├── api.ts           # API response types
│   │   │   ├── auth.ts          # Authentication types
│   │   │   └── entities.ts      # Domain entity types
│   │   └── 📁 constants/        # Application constants
│   │       ├── routes.ts        # Route definitions
│   │       └── config.ts        # App configuration
│   ├── 📁 styles/               # Additional styles
│   │   └── components.css       # Component-specific styles
│   └── 📁 contexts/             # React contexts
│       ├── AuthContext.tsx      # Authentication context
│       ├── StoreContext.tsx     # Store context
│       └── ThemeContext.tsx     # Theme context
├── 📁 types/                    # Global type definitions
├── 📁 .next/                    # Next.js build output (generated)
├── 📁 node_modules/             # Dependencies (generated)
├── 📁 .vscode/                  # VS Code configuration
├── package.json                 # Dependencies and scripts
├── next.config.js               # Next.js configuration
├── tailwind.config.js           # Tailwind CSS configuration
├── tsconfig.json                # TypeScript configuration
├── .eslintrc.json               # ESLint configuration
├── .prettierrc                  # Prettier configuration
└── README.md                    # Frontend documentation
```

## 📦 **Package Dependencies**

### **Core Dependencies**
```json
{
  "next": "14.0.0",
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "typescript": "^5.0.0",
  "@types/react": "^18.2.0",
  "@types/react-dom": "^18.2.0",
  "@types/node": "^20.0.0"
}
```

### **UI & Styling**
```json
{
  "tailwindcss": "^3.3.0",
  "autoprefixer": "^10.4.0",
  "postcss": "^8.4.0",
  "@headlessui/react": "^1.7.0",
  "@heroicons/react": "^2.0.0",
  "lucide-react": "^0.294.0"
}
```

### **Forms & Validation**
```json
{
  "react-hook-form": "^7.48.0",
  "zod": "^3.22.0",
  "@hookform/resolvers": "^3.3.0"
}
```

### **State Management & Data Fetching**
```json
{
  "@tanstack/react-query": "^5.0.0",
  "@tanstack/react-query-devtools": "^5.0.0"
}
```

### **Authentication**
```json
{
  "next-auth": "^4.24.0"
}
```

### **HTTP Client**
```json
{
  "axios": "^1.6.0",
  "swr": "^2.2.0"
}
```

### **PWA & Offline Support**
```json
{
  "next-pwa": "^5.6.0",
  "workbox-webpack-plugin": "^7.0.0"
}
```

### **Development Dependencies**
```json
{
  "@types/uuid": "^9.0.0",
  "eslint": "^8.0.0",
  "eslint-config-next": "^14.0.0",
  "prettier": "^3.0.0",
  "@typescript-eslint/eslint-plugin": "^6.0.0",
  "@typescript-eslint/parser": "^6.0.0"
}
```

## ⚙️ **Configuration Files**

### **package.json**
```json
{
  "name": "dropslot-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "format": "prettier --write .",
    "type-check": "tsc --noEmit",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  },
  "dependencies": {
    // ... dependencies listed above
  },
  "devDependencies": {
    // ... dev dependencies listed above
  }
}
```

### **next.config.js**
```javascript
/** @type {import('next').NextConfig} */
const withPWA = require('next-pwa')({
  dest: 'public',
  disable: process.env.NODE_ENV === 'development'
})

module.exports = withPWA({
  experimental: {
    appDir: true,
  },
  images: {
    domains: ['localhost', 'api.dropslot.com'],
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ]
  },
})
```

### **tailwind.config.js**
```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx}',
    './src/components/**/*.{js,ts,jsx,tsx}',
    './src/app/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
          900: '#1e3a8a',
        },
        secondary: {
          50: '#f8fafc',
          500: '#64748b',
          600: '#475569',
          900: '#0f172a',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
```

### **tsconfig.json**
```json
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["dom", "dom.iterable", "es6"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [
      {
        "name": "next"
      }
    ],
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"],
      "@/components/*": ["./src/components/*"],
      "@/lib/*": ["./src/lib/*"],
      "@/types/*": ["./src/lib/types/*"],
      "@/utils/*": ["./src/lib/utils/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

## 🎨 **Component Architecture**

### **Design System**
- **Colors**: Primary (blue), Secondary (slate), Success (green), Warning (yellow), Error (red)
- **Typography**: Inter font family with consistent sizing scale
- **Spacing**: 4px base unit with consistent spacing scale
- **Shadows**: Subtle shadows for depth and hierarchy
- **Border Radius**: Consistent rounding for modern look

### **Component Patterns**
- **Compound Components**: Related components grouped together
- **Render Props**: Flexible component APIs
- **Custom Hooks**: Reusable logic extraction
- **Composition**: Building complex UIs from simple components

### **State Management**
- **Server State**: TanStack Query for API data
- **Client State**: React Context for app-wide state
- **Form State**: React Hook Form for form management
- **URL State**: Next.js router for navigation state

## 🔐 **Authentication Setup**

### **NextAuth.js Configuration**
```typescript
// src/lib/auth/config.ts
export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: 'credentials',
      credentials: {
        email: { label: 'Email', type: 'email' },
        password: { label: 'Password', type: 'password' }
      },
      async authorize(credentials) {
        // API call to backend authentication
        const response = await apiClient.post('/auth/login', credentials)
        return response.data.user
      }
    })
  ],
  pages: {
    signIn: '/login',
    signUp: '/register',
  },
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.accessToken = user.accessToken
        token.refreshToken = user.refreshToken
      }
      return token
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken
      session.refreshToken = token.refreshToken
      return session
    }
  }
}
```

### **Protected Routes**
```typescript
// src/components/auth/ProtectedRoute.tsx
import { useSession } from 'next-auth/react'
import { useRouter } from 'next/router'
import { useEffect } from 'react'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredRole?: string[]
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { data: session, status } = useSession()
  const router = useRouter()

  useEffect(() => {
    if (status === 'loading') return

    if (!session) {
      router.push('/login')
      return
    }

    if (requiredRole && !requiredRole.includes(session.user.role)) {
      router.push('/unauthorized')
      return
    }
  }, [session, status, router, requiredRole])

  if (status === 'loading') {
    return <div>Loading...</div>
  }

  if (!session) {
    return null
  }

  return <>{children}</>
}
```

## 🌐 **API Integration**

### **HTTP Client Setup**
```typescript
// src/lib/api/client.ts
import axios from 'axios'
import { getSession } from 'next-auth/react'

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
})

apiClient.interceptors.request.use(async (config) => {
  const session = await getSession()
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Handle token refresh or redirect to login
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
```

### **API Hooks with TanStack Query**
```typescript
// src/lib/hooks/useStores.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import apiClient from '@/lib/api/client'

export function useStores() {
  return useQuery({
    queryKey: ['stores'],
    queryFn: async () => {
      const response = await apiClient.get('/stores')
      return response.data
    },
  })
}

export function useCreateStore() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (storeData: CreateStoreData) => {
      const response = await apiClient.post('/stores', storeData)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stores'] })
    },
  })
}
```

## 📱 **PWA Configuration**

### **Web App Manifest**
```json
// public/manifest.json
{
  "name": "DropSlot",
  "short_name": "DropSlot",
  "description": "E-commerce reservation platform",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#3b82f6",
  "icons": [
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

### **Service Worker**
```typescript
// src/lib/pwa/sw.ts
import { registerRoute } from 'workbox-routing'
import { StaleWhileRevalidate } from 'workbox-strategies'
import { precacheAndRoute } from 'workbox-precaching'

precacheAndRoute(self.__WB_MANIFEST)

registerRoute(
  ({ url }) => url.pathname.startsWith('/api/'),
  new StaleWhileRevalidate({
    cacheName: 'api-cache',
  })
)

self.addEventListener('push', (event) => {
  const data = event.data?.json()
  self.registration.showNotification(data.title, {
    body: data.body,
    icon: '/icons/icon-192x192.png',
  })
})
```

## 🚀 **Development Workflow**

### **Local Development**
```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Open browser
open http://localhost:3000
```

### **Code Quality**
```bash
# Run linter
npm run lint

# Fix linting issues
npm run lint:fix

# Format code
npm run format

# Type checking
npm run type-check
```

### **Testing**
```bash
# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

## 📋 **Implementation Checklist**

### **Phase 1: Foundation** ✅
- [x] Project structure setup
- [x] Package.json configuration
- [x] TypeScript configuration
- [x] Tailwind CSS setup
- [x] ESLint and Prettier configuration

### **Phase 2: Core Components**
- [ ] Authentication system
- [ ] API client setup
- [ ] Base UI components
- [ ] Layout components
- [ ] Form components

### **Phase 3: Feature Implementation**
- [ ] Store management pages
- [ ] Product catalog pages
- [ ] Drop management pages
- [ ] Reservation system pages
- [ ] User profile pages

### **Phase 4: Advanced Features**
- [ ] PWA functionality
- [ ] Real-time updates
- [ ] Push notifications
- [ ] Offline support
- [ ] Performance optimization

### **Phase 5: Production**
- [ ] Testing implementation
- [ ] Build optimization
- [ ] Deployment configuration
- [ ] Monitoring setup

## 🎯 **Next Steps**

1. **Initialize Next.js Project**
   ```bash
   npx create-next-app@latest frontend --typescript --tailwind --app
   cd frontend
   npm install [additional dependencies]
   ```

2. **Set Up Project Structure**
   - Create the folder structure outlined above
   - Configure TypeScript paths
   - Set up component organization

3. **Implement Core Components**
   - Base UI components (Button, Input, Modal, etc.)
   - Layout components (Header, Sidebar, Footer)
   - Authentication components

4. **API Integration**
   - Set up HTTP client
   - Create API hooks
   - Implement error handling

5. **Feature Development**
   - Authentication flow
   - Store management
   - Product catalog
   - Drop system
   - Reservation booking

This comprehensive setup guide provides everything needed to build a professional, scalable Next.js frontend for the DropSlot platform. The architecture follows industry best practices and ensures maintainability, performance, and user experience excellence.