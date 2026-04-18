# B2B API Marketplace for Indian Businesses

A complete B2B API marketplace platform enabling Indian businesses to discover, subscribe to, and monetize APIs. Built with Spring Boot, React, PostgreSQL, and integrated with Indian payment gateways.

## 🚀 Features

### For API Consumers
- **API Discovery**: Browse and search APIs by category
- **Instant Integration**: Generate API keys and start using APIs immediately
- **Usage Analytics**: Real-time dashboards showing API usage, costs, and performance
- **Flexible Billing**: Pay-per-use or subscription-based pricing
- **Documentation**: Interactive API documentation with code samples

### For API Providers
- **Easy Publishing**: Publish APIs with OpenAPI specification
- **Revenue Analytics**: Track earnings, usage patterns, and subscriber growth
- **Automated Settlements**: Weekly/monthly automated payments
- **Pricing Control**: Set custom pricing tiers and rate limits
- **Quality Monitoring**: Monitor API performance and uptime

### Security & Compliance
- ✅ **Multi-Factor Authentication** (TOTP-based)
- ✅ **JWT-based Authentication** with refresh tokens
- ✅ **API Key Management** with permissions and expiry
- ✅ **Rate Limiting** per user and subscription tier
- ✅ **KYC Verification** (PAN, GSTIN, bank account)
- ✅ **GST Compliance** with automatic tax calculation
- ✅ **Audit Logging** for all critical operations
- ✅ **PCI-DSS Ready** payment infrastructure

### Payment Integration
- 💳 **Razorpay** for card payments
- 📱 **UPI** payments
- 🏦 **Net Banking**
- 💰 **Wallet** integration
- 🧾 **Automated Invoicing** with GST

## 📋 Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **PostgreSQL 14** or higher
- **Docker & Docker Compose** (for containerized deployment)
- **Maven 3.8+**
- **Razorpay Account** (for payment integration)

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Database**: PostgreSQL 14+
- **Cache**: Valkey (Redis fork)
- **Search**: ElasticSearch 8.11
- **Security**: Spring Security 6, JWT, TOTP
- **Payment**: Razorpay SDK, UPI integration
- **Documentation**: SpringDoc OpenAPI 3

### Frontend
- **Framework**: React 18
- **Language**: TypeScript
- **State Management**: Zustand
- **Data Fetching**: TanStack Query
- **Styling**: Tailwind CSS
- **Forms**: React Hook Form + Zod
- **Charts**: Recharts

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Monitoring**: Prometheus + Grafana
- **API Gateway**: NGINX / Kong
- **CI/CD**: GitHub Actions

## 📁 Project Structure

```
api-marketplace/
├── backend/                    # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/apimarketplace/
│   │   │   │       ├── config/         # Security, CORS, Redis config
│   │   │   │       ├── controller/     # REST controllers
│   │   │   │       ├── entity/         # JPA entities
│   │   │   │       ├── repository/     # Data repositories
│   │   │   │       ├── service/        # Business logic
│   │   │   │       ├── security/       # JWT, MFA implementation
│   │   │   │       ├── dto/            # Data transfer objects
│   │   │   │       └── exception/      # Custom exceptions
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/       # Flyway migrations
│   │   └── test/                       # Unit & integration tests
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                   # React frontend
│   ├── public/
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── hooks/              # Custom hooks
│   │   ├── services/           # API services
│   │   ├── types/              # TypeScript types
│   │   ├── utils/              # Utility functions
│   │   └── App.tsx
│   ├── package.json
│   ├── tsconfig.json
│   └── Dockerfile
├── database/
│   ├── schema.sql              # Database schema
│   └── seed.sql                # Sample data
├── nginx/
│   └── nginx.conf              # NGINX configuration
├── monitoring/
│   ├── prometheus.yml          # Prometheus config
│   └── grafana/                # Grafana dashboards
├── docker-compose.yml
├── .env.template
└── README.md
```

## 🚀 Quick Start

### Option 1: Using Docker Compose (Recommended)

1. **Clone the repository**
```bash
git clone https://github.com/your-org/api-marketplace.git
cd api-marketplace
```

2. **Set up environment variables**
```bash
cp .env.template .env
# Edit .env and add your configuration
nano .env
```

3. **Start all services**
```bash
docker-compose up -d
```

4. **Access the application**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html
- Grafana: http://localhost:3001

### Option 2: Manual Setup

#### Backend Setup

1. **Install PostgreSQL and create database**
```bash
sudo -u postgres psql
CREATE DATABASE api_marketplace;
CREATE USER api_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE api_marketplace TO api_user;
\q
```

2. **Run database schema**
```bash
psql -U api_user -d api_marketplace -f database/schema.sql
```

3. **Install Valkey (Redis)**
```bash
# Using Docker
docker run -d -p 6379:6379 valkey/valkey:latest
```

4. **Configure application**
```bash
cd backend
cp src/main/resources/application.yml.template src/main/resources/application.yml
# Edit application.yml with your configuration
```

5. **Build and run backend**
```bash
mvn clean install
mvn spring-boot:run
```

#### Frontend Setup

1. **Install dependencies**
```bash
cd frontend
npm install
```

2. **Configure environment**
```bash
cp .env.template .env.local
# Edit .env.local with your API URL
```

3. **Start development server**
```bash
npm start
```

## 🔒 Security Setup

### 1. JWT Secret Generation
```bash
openssl rand -base64 32
# Copy output to JWT_SECRET in .env
```

### 2. MFA Setup
- Users can enable MFA from account settings
- Scan QR code with Google Authenticator
- Backup codes are generated automatically

### 3. API Key Security
- API keys are hashed with BCrypt before storage
- Keys are prefixed with `apim_` for identification
- Support for expiration and permissions

## 💳 Payment Gateway Setup

### Razorpay Configuration

1. **Create Razorpay Account**
   - Sign up at https://razorpay.com
   - Complete KYC verification

2. **Get API Credentials**
   - Go to Settings → API Keys
   - Generate Key ID and Secret
   - Add to `.env` file

3. **Set up Webhooks**
   - URL: `https://your-domain.com/api/payments/webhook`
   - Events: payment.captured, payment.failed

### UPI Integration

1. **Configure UPI Merchant ID**
```env
UPI_MERCHANT_ID=your_merchant_id
UPI_MERCHANT_NAME=Your Business Name
```

2. **Generate UPI Intent**
The system automatically generates UPI payment intents in the format:
```
upi://pay?pa=merchant@upi&pn=API Marketplace&am=100.00&tr=ORDER123&cu=INR
```

## 📊 Monitoring & Observability

### Prometheus Metrics
Access metrics at: http://localhost:9090

Key metrics:
- `api_calls_total` - Total API calls
- `api_response_time` - Response time histogram
- `payment_success_rate` - Payment success percentage
- `active_subscriptions` - Active subscription count

### Grafana Dashboards
Access dashboards at: http://localhost:3001

Default dashboards:
- **System Overview**: CPU, memory, disk usage
- **API Metrics**: Request rate, latency, errors
- **Business Metrics**: Revenue, subscriptions, user growth
- **Payment Analytics**: Transaction volume, success rate

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
mvn verify  # Integration tests
```

### Frontend Tests
```bash
cd frontend
npm test
npm run test:coverage
```

### API Testing
```bash
# Using curl
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Using Postman
# Import the collection from: docs/postman_collection.json
```

## 📝 API Documentation

### Interactive Documentation
Access Swagger UI at: http://localhost:8080/swagger-ui.html

### Key Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Rotate refresh token and issue a new session pair
- `POST /api/auth/logout` - Revoke a refresh token for the authenticated user
- `POST /api/auth/mfa/setup` - Setup MFA
- `POST /api/auth/mfa/verify` - Verify MFA code

#### APIs
- `GET /api/apis` - List all APIs
- `GET /api/apis/{id}` - Get API details
- `POST /api/apis` - Publish new API (providers only)
- `PUT /api/apis/{id}` - Update API

#### Subscriptions
- `POST /api/subscriptions` - Subscribe to API
- `GET /api/subscriptions` - Get user subscriptions
- `DELETE /api/subscriptions/{id}` - Cancel subscription

#### Billing
- `GET /api/invoices` - List invoices
- `GET /api/usage` - Get usage statistics
- `POST /api/payments/create-order` - Create payment order

#### Reporting, Filters, and Exports
- `GET /api/audit/page` - Search your audit logs with filters and pagination
- `GET /api/audit/admin/page` - Search all audit logs as an admin
- `GET /api/notifications/page` - Search your notifications with filters and pagination
- `GET /api/notifications/admin/page` - Search all notifications as an admin
- `GET /api/settlements/export.csv` - Download settlement data as CSV
- `GET /api/settlements/export.pdf` - Download settlement data as a formatted PDF
- `GET /api/admin/dashboard/operations` - View admin usage metrics for audits, notifications, and exports

##### Example Requests

Audit log search:
```bash
curl "http://localhost:8080/api/audit/page?eventType=SETTLEMENT_EXPORT_PDF&status=SUCCESS&page=0&size=20&sort=createdAt,desc"
```

Notification search:
```bash
curl "http://localhost:8080/api/notifications/page?channel=EMAIL&eventType=KYC_SUBMITTED&page=0&size=20"
```

Settlement export:
```bash
curl -L "http://localhost:8080/api/settlements/export.pdf?from=2026-04-01&to=2026-04-30&status=PAID" -o settlements.pdf
curl -L "http://localhost:8080/api/settlements/export.csv?from=2026-04-01&to=2026-04-30&status=PAID" -o settlements.csv
```

Admin usage dashboard:
```bash
curl "http://localhost:8080/api/admin/dashboard/operations"
```

Auth refresh:
```bash
curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-token>"
  }'
```

Auth logout:
```bash
curl -X POST "http://localhost:8080/api/auth/logout" \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-token>"
  }'
```

Auth response shape:
```json
{
  "accessToken": "<jwt-access-token>",
  "refreshToken": "<opaque-refresh-token>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "refreshExpiresInSeconds": 2592000,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "provider@apimarketplace.local",
    "fullName": "Demo Provider",
    "companyName": "FinEdge Systems",
    "role": "PROVIDER",
    "mfaEnabled": false,
    "kycStatus": "DRAFT"
  },
  "mfaEnabled": false
}
```

#### KYC and Compliance
- `POST /api/compliance/kyc/submit` - Submit a multi-method KYC application
- `GET /api/compliance/kyc/me` - Get the current user's KYC application with verification details
- `GET /api/compliance/kyc/pending` - Admin queue for pending KYC applications
- `POST /api/compliance/kyc/{id}/approve` - Approve a KYC application
- `POST /api/compliance/kyc/{id}/reject` - Reject a KYC application with a reason
- `POST /api/compliance/kyc/provider/webhook` - Receive async provider verification callbacks

Supported verification methods:
- `BANK_ACCOUNT`
- `PAN`
- `GST`
- `AADHAAR_BASIC`
- `AADHAAR_OCR`
- `DRIVING_LICENSE`
- `PASSPORT`
- `VOTER_ID`
- `FACE_MATCH`
- `FACE_LIVENESS`

Example KYC submission:
```bash
curl -X POST "http://localhost:8080/api/compliance/kyc/submit" \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "legalBusinessName": "Demo Provider Private Limited",
    "contactName": "Ritesh Sharma",
    "email": "provider@apimarketplace.local",
    "phoneNumber": "9876543210",
    "businessType": "Business",
    "panNumber": "ABCDE1234F",
    "gstin": "22ABCDE1234F1Z5",
    "bankAccountNumber": "123456789012",
    "bankIfsc": "HDFC0ABC123",
    "aadhaarNumber": "123412341234",
    "drivingLicenseNumber": "KA0120200001234",
    "passportNumber": "M1234567",
    "voterIdNumber": "ABC1234567",
    "selfieImageUrl": "https://cdn.example.com/selfie.jpg",
    "idDocumentImageUrl": "https://cdn.example.com/id-document.jpg",
    "verificationMethods": ["BANK_ACCOUNT","PAN","GST","AADHAAR_BASIC","AADHAAR_OCR","FACE_MATCH","FACE_LIVENESS"],
    "consentGiven": true,
    "registeredAddress": "Registered office, Bengaluru",
    "supportingDocuments": ["pan-card.pdf","gst-certificate.pdf","cancelled-cheque.pdf"]
  }'
```

Provider configuration:
```env
KYC_PROVIDER_ENABLED=false
KYC_PROVIDER_PREFER_REMOTE=true
KYC_PROVIDER_BASE_URL=https://your-kyc-provider.example.com
KYC_PROVIDER_API_KEY=replace-me
KYC_PROVIDER_WEBHOOK_SECRET=replace-me
KYC_PROVIDER_BANK_ACCOUNT_PATH=/bank-account-verification
KYC_PROVIDER_PAN_PATH=/pan-verification
KYC_PROVIDER_GST_PATH=/gst-verification
KYC_PROVIDER_AADHAAR_BASIC_PATH=/basic-aadhaar-check
KYC_PROVIDER_AADHAAR_OCR_PATH=/aadhaar-ocr-check
KYC_PROVIDER_DRIVING_LICENSE_PATH=/driving-license-verification
KYC_PROVIDER_PASSPORT_PATH=/passport-verification
KYC_PROVIDER_VOTER_ID_PATH=/voter-id-verification
KYC_PROVIDER_FACE_MATCH_PATH=/face-match-api
KYC_PROVIDER_FACE_LIVENESS_PATH=/face-liveness-check-api
```

Postman:
- Import the collection from `files/docs/postman_collection.json`

Provider notes:
- The adapter is split by AadhaarKYC product family rather than a single generic endpoint.
- Default paths follow the official product names from AadhaarKYC's public product pages and can be overridden per environment if your account uses different routes.

Provider callback example:
```bash
curl -X POST "http://localhost:8080/api/compliance/kyc/provider/webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "550e8400-e29b-41d4-a716-446655440000",
    "referenceId": "AADHAARKYC-REF-001",
    "method": "PAN",
    "status": "VERIFIED",
    "verified": true,
    "message": "PAN verified by provider",
    "score": 0.99,
    "signature": "<optional-hmac-signature>"
  }'
```

## 🌍 Deployment

### Production Checklist

- [ ] Change all default passwords
- [ ] Generate secure JWT secret
- [ ] Enable SSL/TLS certificates
- [ ] Configure production database
- [ ] Set up database backups
- [ ] Configure Redis persistence
- [ ] Enable rate limiting
- [ ] Set up monitoring alerts
- [ ] Configure log aggregation
- [ ] Enable WAF (Web Application Firewall)
- [ ] Perform security audit
- [ ] Load testing
- [ ] Set up CI/CD pipeline

### AWS Deployment

1. **RDS PostgreSQL**
   - Create RDS instance
   - Enable automatic backups
   - Set up read replicas

2. **ElastiCache (Valkey/Redis)**
   - Create cluster
   - Enable encryption

3. **ECS/EKS**
   - Deploy containers
   - Configure auto-scaling
   - Set up load balancer

4. **S3**
   - Store documents and logs
   - Enable versioning

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: https://docs.apimarketplace.com
- **Email**: support@apimarketplace.com
- **GitHub Issues**: https://github.com/your-org/api-marketplace/issues

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- Razorpay for payment integration
- All open-source contributors

## 📈 Roadmap

### Phase 1 (MVP) - ✅ Complete
- [x] User authentication with MFA
- [x] API listing and discovery
- [x] Subscription management
- [x] Payment integration
- [x] Usage tracking

### Phase 2 (Q2 2024)
- [ ] API analytics dashboard
- [ ] Webhook support
- [ ] SDK generation
- [ ] GraphQL APIs support
- [ ] API versioning

### Phase 3 (Q3 2024)
- [ ] Marketplace for API templates
- [ ] Developer community
- [ ] API certification program
- [ ] White-label solution
- [ ] International expansion

## 💡 Use Cases

1. **Fintech Startups**: Integrate KYC, PAN verification, credit scoring APIs
2. **E-commerce**: GST validation, logistics tracking, payment APIs
3. **SaaS Companies**: SMS, email, WhatsApp messaging APIs
4. **Banks**: Account validation, transaction APIs
5. **Insurance**: Policy verification, claim processing APIs

## 🎯 Key Differentiators

1. **India-First**: Built specifically for Indian regulatory requirements
2. **Compliance Ready**: GST, PAN, Aadhaar integration
3. **Local Payments**: UPI, Razorpay, net banking support
4. **Developer Experience**: Comprehensive SDKs and documentation
5. **Fair Pricing**: Transparent pricing with no hidden fees

---

**Made with ❤️ for Indian Businesses**
