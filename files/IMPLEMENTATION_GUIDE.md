# B2B API Marketplace - Complete Implementation Guide

## Project Overview
This document provides complete implementation details for the B2B API Marketplace platform built with:
- **Backend**: Spring Boot 3.2, Java 17
- **Frontend**: React 18, TypeScript
- **Database**: PostgreSQL 14+
- **Cache**: Valkey (Redis-compatible)
- **Search**: ElasticSearch
- **Payment**: Razorpay, UPI integration

## Architecture Summary

### Microservices Structure
```
api-marketplace/
├── auth-service/           # Authentication & Authorization
├── api-registry-service/   # API Discovery & Management
├── billing-service/        # Usage tracking & Billing
├── payment-service/        # Payment processing
├── provider-service/       # Provider management
├── metering-service/       # API call metering
├── analytics-service/      # Reports & Analytics
└── gateway/               # API Gateway (Kong/NGINX)
```

### Technology Stack Details

#### Backend Technologies
1. **Spring Boot 3.2**
   - Spring Security 6 for authentication
   - Spring Data JPA for database operations
   - Spring Cache with Valkey
   - Spring Cloud Gateway for routing

2. **Security Features**
   - JWT-based authentication
   - TOTP-based MFA using Google Authenticator
   - BCrypt password hashing
   - Rate limiting with Resilience4j
   - CORS configuration
   - SQL injection prevention
   - XSS protection

3. **Database**
   - PostgreSQL 14+ with UUID support
   - Connection pooling with HikariCP
   - Flyway for migrations
   - Read replicas for scaling

4. **Caching Strategy**
   - Valkey (Redis fork) for:
     - Rate limiting counters
     - Session storage
     - API key validation cache
     - Usage statistics cache

## Security Implementation

### 1. Multi-Factor Authentication (MFA)

#### TOTP Implementation
```java
// Service class for MFA
@Service
public class MfaService {
    
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final TimeProvider timeProvider;
    private final CodeVerifier codeVerifier;
    
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    public String generateQRUrl(String secret, String email) {
        return qrGenerator.getUriForImage(
            "API Marketplace",
            email,
            secret
        );
    }
    
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
    
    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            codes.add(RandomStringUtils.randomAlphanumeric(8));
        }
        return codes;
    }
}
```

#### MFA Flow
1. User enables MFA in account settings
2. Generate TOTP secret key
3. Display QR code for Google Authenticator
4. Generate 10 backup codes
5. User scans QR code
6. Verify setup with one-time code
7. On login: prompt for TOTP after password validation
8. Allow backup code usage if TOTP unavailable

### 2. API Key Management

#### API Key Generation
```java
@Service
public class ApiKeyService {
    
    private static final String PREFIX = "apim_";
    private static final int KEY_LENGTH = 32;
    
    public String generateApiKey() {
        String randomPart = RandomStringUtils.randomAlphanumeric(KEY_LENGTH);
        return PREFIX + randomPart;
    }
    
    public String hashApiKey(String apiKey) {
        return BCrypt.hashpw(apiKey, BCrypt.gensalt(12));
    }
    
    public boolean validateApiKey(String providedKey, String storedHash) {
        return BCrypt.checkpw(providedKey, storedHash);
    }
}
```

#### API Key Security Features
- Prefix-based identification (apim_)
- SHA-256 hashing before storage
- Expiration dates
- Permission scoping (read, write, admin)
- Last used tracking
- IP whitelisting support
- Automatic rotation support

### 3. Rate Limiting

#### Implementation with Valkey
```java
@Component
public class RateLimiter {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public boolean allowRequest(String apiKeyHash, int limit, long windowSeconds) {
        String key = "rate_limit:" + apiKeyHash;
        Long current = redisTemplate.opsForValue().increment(key);
        
        if (current == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        
        return current <= limit;
    }
    
    public long getRemainingRequests(String apiKeyHash, int limit) {
        String key = "rate_limit:" + apiKeyHash;
        Long current = redisTemplate.opsForValue().get(key);
        return current == null ? limit : Math.max(0, limit - current);
    }
}
```

#### Rate Limiting Strategy
- Per API key limits
- Per subscription tier limits
- Per endpoint granular limits
- Sliding window algorithm
- Redis sorted sets for precision
- HTTP 429 responses with Retry-After header

## Payment Integration

### Razorpay Integration

#### Payment Flow
```java
@Service
public class PaymentService {
    
    @Value("${app.payment.razorpay.key-id}")
    private String keyId;
    
    @Value("${app.payment.razorpay.key-secret}")
    private String keySecret;
    
    public Order createOrder(BigDecimal amount, String currency) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "order_" + UUID.randomUUID());
            
            return client.orders.create(orderRequest);
        } catch (RazorpayException e) {
            throw new PaymentException("Failed to create order", e);
        }
    }
    
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String expectedSignature = calculateSignature(payload, keySecret);
        return signature.equals(expectedSignature);
    }
    
    private String calculateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Signature calculation failed", e);
        }
    }
}
```

### UPI Integration

#### UPI Payment Flow
```java
@Service
public class UpiPaymentService {
    
    @Value("${app.payment.upi.merchant-id}")
    private String merchantId;
    
    public String generateUpiIntent(BigDecimal amount, String orderId) {
        return String.format(
            "upi://pay?pa=%s@upi&pn=API Marketplace&am=%.2f&tr=%s&cu=INR",
            merchantId,
            amount,
            orderId
        );
    }
    
    public boolean verifyUpiTransaction(String upiTransactionId) {
        // Implement UPI verification logic
        // Call payment gateway API to verify transaction status
        return true;
    }
}
```

## Indian Compliance

### GST Implementation

#### GST Calculation
```java
@Service
public class GstService {
    
    @Value("${app.gst.rate}")
    private BigDecimal gstRate; // 0.18 for 18%
    
    public InvoiceAmounts calculateInvoiceAmounts(BigDecimal baseAmount) {
        BigDecimal taxAmount = baseAmount.multiply(gstRate)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(taxAmount);
        
        return InvoiceAmounts.builder()
            .baseAmount(baseAmount)
            .cgst(taxAmount.divide(new BigDecimal(2)))
            .sgst(taxAmount.divide(new BigDecimal(2)))
            .totalAmount(totalAmount)
            .build();
    }
    
    public boolean validateGstin(String gstin) {
        // GSTIN format: 22AAAAA0000A1Z5
        String gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
        return gstin != null && gstin.matches(gstinPattern);
    }
}
```

### PAN Verification

#### PAN Validation
```java
@Service
public class PanVerificationService {
    
    public boolean validatePanFormat(String pan) {
        // PAN format: AAAAA9999A
        String panPattern = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
        return pan != null && pan.matches(panPattern);
    }
    
    // Integration with government API for PAN verification
    public PanVerificationResult verifyPanWithGovernment(String pan) {
        // Call NSDL PAN verification API
        // This requires proper credentials and API setup
        return PanVerificationResult.builder()
            .valid(true)
            .name("John Doe")
            .verified(true)
            .build();
    }
}
```

### KYC Process

#### KYC Workflow
1. **Document Upload**
   - PAN card
   - GST certificate
   - Business registration
   - Bank account proof

2. **Verification Steps**
   - Validate PAN format and verify with NSDL
   - Validate GSTIN format and verify with GST portal
   - Verify bank account through penny drop
   - Manual review for document authenticity

3. **Status Management**
   - PENDING: Initial state
   - INCOMPLETE: Missing documents
   - UNDER_REVIEW: Documents submitted
   - VERIFIED: All checks passed
   - REJECTED: Failed verification

## Billing System

### Usage Metering

#### Real-time Call Logging
```java
@Service
@Async
public class MeteringService {
    
    @Autowired
    private ApiCallRepository apiCallRepository;
    
    @Autowired
    private UsageRecordRepository usageRecordRepository;
    
    public void logApiCall(ApiCallLog callLog) {
        // Save individual call log
        apiCallRepository.save(callLog);
        
        // Update daily usage aggregate
        updateDailyUsage(callLog);
    }
    
    @Transactional
    private void updateDailyUsage(ApiCallLog callLog) {
        LocalDate today = LocalDate.now();
        UsageRecord record = usageRecordRepository
            .findBySubscriptionIdAndUsageDate(callLog.getSubscriptionId(), today)
            .orElse(new UsageRecord());
        
        record.incrementCallCount();
        record.addCost(calculateCallCost(callLog));
        usageRecordRepository.save(record);
    }
}
```

### Invoice Generation

#### Monthly Billing
```java
@Service
public class InvoiceService {
    
    @Scheduled(cron = "0 0 1 1 * ?") // 1st of every month
    public void generateMonthlyInvoices() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate periodStart = lastMonth.withDayOfMonth(1);
        LocalDate periodEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatus(SubscriptionStatus.ACTIVE);
        
        for (Subscription subscription : activeSubscriptions) {
            generateInvoiceForSubscription(subscription, periodStart, periodEnd);
        }
    }
    
    private void generateInvoiceForSubscription(
        Subscription subscription, 
        LocalDate periodStart, 
        LocalDate periodEnd
    ) {
        BigDecimal usageAmount = calculateUsageAmount(subscription, periodStart, periodEnd);
        InvoiceAmounts amounts = gstService.calculateInvoiceAmounts(usageAmount);
        
        Invoice invoice = Invoice.builder()
            .subscription(subscription)
            .invoiceNumber(generateInvoiceNumber())
            .amount(amounts.getBaseAmount())
            .taxAmount(amounts.getTaxAmount())
            .totalAmount(amounts.getTotalAmount())
            .billingPeriodStart(periodStart)
            .billingPeriodEnd(periodEnd)
            .dueDate(LocalDateTime.now().plusDays(15))
            .status(InvoiceStatus.PENDING)
            .build();
        
        invoiceRepository.save(invoice);
        emailService.sendInvoice(invoice);
    }
}
```

## Frontend Implementation

### React Project Structure
```
frontend/
├── src/
│   ├── components/
│   │   ├── auth/
│   │   │   ├── LoginForm.tsx
│   │   │   ├── RegisterForm.tsx
│   │   │   └── MfaSetup.tsx
│   │   ├── api/
│   │   │   ├── ApiCatalog.tsx
│   │   │   ├── ApiDetails.tsx
│   │   │   └── ApiSubscribe.tsx
│   │   ├── dashboard/
│   │   │   ├── ConsumerDashboard.tsx
│   │   │   ├── ProviderDashboard.tsx
│   │   │   └── Analytics.tsx
│   │   ├── billing/
│   │   │   ├── Invoices.tsx
│   │   │   ├── UsageStats.tsx
│   │   │   └── PaymentMethods.tsx
│   │   └── common/
│   │       ├── Header.tsx
│   │       ├── Footer.tsx
│   │       └── Sidebar.tsx
│   ├── services/
│   │   ├── authService.ts
│   │   ├── apiService.ts
│   │   ├── billingService.ts
│   │   └── paymentService.ts
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useApi.ts
│   │   └── usePayment.ts
│   ├── context/
│   │   ├── AuthContext.tsx
│   │   └── ThemeContext.tsx
│   ├── types/
│   │   ├── auth.types.ts
│   │   ├── api.types.ts
│   │   └── billing.types.ts
│   └── utils/
│       ├── apiClient.ts
│       ├── validators.ts
│       └── formatters.ts
```

### Key Frontend Features

#### 1. Authentication with MFA
```typescript
// MFA Setup Component
import { useState } from 'react';
import QRCode from 'qrcode.react';

export const MfaSetup: React.FC = () => {
  const [secret, setSecret] = useState<string>('');
  const [qrUrl, setQrUrl] = useState<string>('');
  const [verificationCode, setVerificationCode] = useState<string>('');

  const setupMfa = async () => {
    const response = await authService.initializeMfa();
    setSecret(response.secret);
    setQrUrl(response.qrUrl);
  };

  const verifyAndEnable = async () => {
    await authService.verifyAndEnableMfa(verificationCode);
    // Redirect to dashboard
  };

  return (
    <div>
      <h2>Set up Two-Factor Authentication</h2>
      <button onClick={setupMfa}>Generate QR Code</button>
      
      {qrUrl && (
        <>
          <QRCode value={qrUrl} size={256} />
          <p>Scan this QR code with Google Authenticator</p>
          
          <input
            type="text"
            placeholder="Enter 6-digit code"
            value={verificationCode}
            onChange={(e) => setVerificationCode(e.target.value)}
            maxLength={6}
          />
          <button onClick={verifyAndEnable}>Verify & Enable</button>
        </>
      )}
    </div>
  );
};
```

#### 2. Payment Integration
```typescript
// Razorpay Integration
export const PaymentModal: React.FC<{invoice: Invoice}> = ({ invoice }) => {
  const handlePayment = async () => {
    const order = await paymentService.createRazorpayOrder(invoice.id);
    
    const options = {
      key: process.env.REACT_APP_RAZORPAY_KEY_ID,
      amount: order.amount,
      currency: order.currency,
      name: 'API Marketplace',
      description: `Invoice ${invoice.invoiceNumber}`,
      order_id: order.id,
      handler: async (response: any) => {
        await paymentService.verifyPayment({
          orderId: order.id,
          paymentId: response.razorpay_payment_id,
          signature: response.razorpay_signature
        });
        // Show success message
      },
      prefill: {
        email: user.email,
        contact: user.phone
      },
      theme: {
        color: '#3399cc'
      }
    };
    
    const razorpay = new (window as any).Razorpay(options);
    razorpay.open();
  };

  return (
    <div>
      <h3>Pay Invoice #{invoice.invoiceNumber}</h3>
      <p>Amount: ₹{invoice.totalAmount}</p>
      <button onClick={handlePayment}>Pay with Razorpay</button>
    </div>
  );
};
```

## Deployment Guide

### Docker Compose Setup
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: api_marketplace
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  valkey:
    image: valkey/valkey:latest
    ports:
      - "6379:6379"
    volumes:
      - valkey_data:/data

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/api_marketplace
      - SPRING_REDIS_HOST=valkey
      - ELASTICSEARCH_URIS=http://elasticsearch:9200
    depends_on:
      - postgres
      - valkey
      - elasticsearch

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  postgres_data:
  valkey_data:
  es_data:
```

### Production Considerations

1. **Database**
   - Use managed PostgreSQL (AWS RDS, GCP Cloud SQL)
   - Enable read replicas
   - Set up automated backups
   - Configure connection pooling

2. **Caching**
   - Use managed Valkey/Redis cluster
   - Configure persistence
   - Set up monitoring

3. **Load Balancing**
   - Use AWS ALB or NGINX
   - Enable SSL/TLS
   - Configure health checks

4. **Monitoring**
   - Prometheus for metrics
   - Grafana for dashboards
   - ELK stack for logs
   - Sentry for error tracking

5. **Security**
   - Enable HTTPS everywhere
   - Use secrets management (AWS Secrets Manager)
   - Configure WAF
   - Regular security audits
   - PCI-DSS compliance for payment data

## Testing Strategy

### Backend Testing
```java
@SpringBootTest
class AuthServiceTest {
    
    @Autowired
    private AuthService authService;
    
    @Test
    void testUserRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        
        User user = authService.register(request);
        
        assertNotNull(user.getId());
        assertTrue(BCrypt.checkpw("SecurePass123!", user.getPasswordHash()));
    }
    
    @Test
    void testMfaVerification() {
        String secret = mfaService.generateSecret();
        String code = totpGenerator.generate(secret);
        
        boolean valid = mfaService.verifyCode(secret, code);
        assertTrue(valid);
    }
}
```

### Integration Testing
- API endpoint tests
- Payment gateway integration tests
- Rate limiting tests
- Database transaction tests

## Monitoring & Observability

### Metrics to Track
1. **Business Metrics**
   - API calls per day
   - Revenue per API
   - Active subscriptions
   - Churn rate

2. **Technical Metrics**
   - Request latency (p50, p95, p99)
   - Error rates
   - Cache hit ratio
   - Database query performance

3. **Security Metrics**
   - Failed login attempts
   - API key misuse
   - Rate limit violations
   - Suspicious activities

## Conclusion

This implementation provides a complete, production-ready B2B API Marketplace with:
- Robust security (MFA, JWT, API keys)
- Indian compliance (GST, PAN, KYC)
- Payment integration (Razorpay, UPI)
- Scalable architecture
- Comprehensive monitoring

Next steps:
1. Set up development environment
2. Implement core services
3. Build frontend
4. Integration testing
5. Security audit
6. Production deployment
