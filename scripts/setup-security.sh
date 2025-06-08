#!/bin/bash

# ATMConnect Security Setup Script
# This script generates secure keys and configurations for the application

set -e

echo "🔐 ATMConnect Security Setup"
echo "============================="

# Create directories
mkdir -p config/security
mkdir -p logs
mkdir -p keystore

# Generate JWT secret key (512 bits)
echo "📋 Generating JWT secret key..."
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# Generate database encryption key
echo "📋 Generating database encryption key..."
DB_ENCRYPTION_KEY=$(openssl rand -hex 32)

# Generate API key for external services
echo "📋 Generating API keys..."
API_KEY=$(openssl rand -hex 32)

# Generate SSL certificate for development (if not exists)
if [ ! -f "keystore/atmconnect.p12" ]; then
    echo "🔒 Generating SSL certificate for development..."
    keytool -genkeypair \
        -alias atmconnect \
        -keyalg RSA \
        -keysize 2048 \
        -storetype PKCS12 \
        -keystore keystore/atmconnect.p12 \
        -storepass changeit \
        -validity 365 \
        -dname "CN=localhost, OU=ATMConnect, O=BankingDemo, L=City, ST=State, C=US" \
        -ext SAN=dns:localhost,ip:127.0.0.1
fi

# Create environment file with secure defaults
echo "📝 Creating environment configuration..."
cat > .env << EOF
# ATMConnect Security Configuration
# Generated on: $(date)
# WARNING: Keep this file secure and never commit to version control

# JWT Configuration
JWT_SECRET_KEY=${JWT_SECRET}
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=86400000

# Database Security
DB_ENCRYPTION_KEY=${DB_ENCRYPTION_KEY}
DB_USERNAME=\${DB_USERNAME:-atmuser}
DB_PASSWORD=\${DB_PASSWORD:-}

# API Security
API_KEY=${API_KEY}
API_RATE_LIMIT_PER_MINUTE=60
API_AUTH_RATE_LIMIT_PER_MINUTE=5

# SSL Configuration
SSL_KEY_STORE_PASSWORD=changeit
SSL_KEY_ALIAS=atmconnect

# Security Monitoring
SECURITY_MONITORING_ENABLED=true
SECURITY_ALERT_EMAIL=\${SECURITY_ALERT_EMAIL:-security@atmconnect.com}

# Bluetooth Security
BLUETOOTH_ENCRYPTION_KEY=\$(openssl rand -hex 32)
BLUETOOTH_DEVICE_TIMEOUT_SECONDS=30

# Transaction Security
MAX_DAILY_WITHDRAWAL=2000.00
MAX_TRANSACTION_AMOUNT=1000.00
TRANSACTION_TIMEOUT_MINUTES=5
OTP_EXPIRATION_MINUTES=5

# Account Security
MAX_FAILED_LOGIN_ATTEMPTS=3
ACCOUNT_LOCKOUT_DURATION_MINUTES=30
SESSION_TIMEOUT_MINUTES=15
PASSWORD_MIN_LENGTH=8

# Network Security
ALLOWED_ORIGINS=https://localhost:3000,https://app.atmconnect.com
CORS_MAX_AGE=3600
CSRF_TOKEN_VALIDITY_SECONDS=3600

# Logging
LOG_LEVEL=INFO
SECURITY_LOG_LEVEL=WARN
AUDIT_LOG_RETENTION_DAYS=90

# Development Settings (Override in production)
SPRING_PROFILES_ACTIVE=development
H2_CONSOLE_ENABLED=false
ACTUATOR_MANAGEMENT_SECURITY=true
DEBUG_LOGGING_ENABLED=false
EOF

# Create production environment template
echo "📝 Creating production environment template..."
cat > .env.production.template << EOF
# ATMConnect Production Configuration Template
# Copy to .env.production and fill in actual values

# CRITICAL: Generate new secrets for production
JWT_SECRET_KEY=GENERATE_NEW_512_BIT_KEY
DB_ENCRYPTION_KEY=GENERATE_NEW_256_BIT_KEY
API_KEY=GENERATE_NEW_256_BIT_KEY

# Database (Use encrypted connection)
DB_URL=jdbc:postgresql://localhost:5432/atmconnect?ssl=true&sslmode=require
DB_USERNAME=production_user
DB_PASSWORD=SECURE_DATABASE_PASSWORD

# SSL (Use proper certificates)
SSL_KEY_STORE_PATH=/path/to/production/keystore.p12
SSL_KEY_STORE_PASSWORD=SECURE_KEYSTORE_PASSWORD
SSL_TRUST_STORE_PATH=/path/to/truststore.p12
SSL_TRUST_STORE_PASSWORD=SECURE_TRUSTSTORE_PASSWORD

# Security Monitoring
SECURITY_MONITORING_ENABLED=true
SECURITY_ALERT_EMAIL=security@yourdomain.com
SIEM_ENDPOINT=https://your-siem-service.com/api/events

# Network Security
ALLOWED_ORIGINS=https://app.yourdomain.com
TRUSTED_PROXY_IPS=192.168.1.0/24

# Production Settings
SPRING_PROFILES_ACTIVE=production
H2_CONSOLE_ENABLED=false
ACTUATOR_MANAGEMENT_SECURITY=true
DEBUG_LOGGING_ENABLED=false
LOG_LEVEL=WARN
SECURITY_LOG_LEVEL=ERROR

# Compliance
PCI_COMPLIANCE_MODE=true
AUDIT_LOG_ENCRYPTION=true
DATA_RETENTION_DAYS=2555  # 7 years for financial records
EOF

# Create security checklist
echo "📋 Creating security checklist..."
cat > SECURITY_CHECKLIST.md << EOF
# ATMConnect Security Checklist

## Pre-Deployment Security Verification

### 🔐 Cryptographic Security
- [ ] JWT secret keys are cryptographically secure (512+ bits)
- [ ] Database encryption keys are properly generated
- [ ] SSL certificates are valid and properly configured
- [ ] All hardcoded secrets have been removed from code
- [ ] Key rotation procedures are documented and tested

### 🌐 Network Security
- [ ] HTTPS is enforced in production
- [ ] CORS is properly configured with specific origins
- [ ] CSRF protection is enabled
- [ ] Security headers are properly set
- [ ] Rate limiting is configured and tested

### 🔍 Input Validation
- [ ] All user inputs are validated and sanitized
- [ ] SQL injection protection is verified
- [ ] XSS protection is implemented
- [ ] File upload restrictions are in place
- [ ] Maximum request size limits are set

### 👤 Authentication & Authorization
- [ ] Multi-factor authentication is working
- [ ] Session management is secure
- [ ] Token expiration is properly configured
- [ ] Account lockout mechanisms are tested
- [ ] Password policies are enforced

### 📊 Monitoring & Logging
- [ ] Security event monitoring is active
- [ ] Audit logs are properly configured
- [ ] Sensitive data is not logged
- [ ] Log integrity protection is enabled
- [ ] Alerting systems are tested

### 🏦 Banking-Specific Security
- [ ] Transaction validation is comprehensive
- [ ] Daily withdrawal limits are enforced
- [ ] OTP generation and validation is secure
- [ ] Device registration is properly secured
- [ ] Bluetooth communication is encrypted

### 📱 Mobile/Device Security
- [ ] Device fingerprinting is implemented
- [ ] Certificate pinning is configured
- [ ] Bluetooth pairing is secure
- [ ] Device registration validation works
- [ ] Session hijacking protection is active

### 🔧 Infrastructure Security
- [ ] Database connections are encrypted
- [ ] API endpoints are properly secured
- [ ] File permissions are restrictive
- [ ] Environment variables are secured
- [ ] Backup security is implemented

### 📋 Compliance
- [ ] PCI DSS requirements are met
- [ ] GDPR compliance is verified
- [ ] Financial regulations are followed
- [ ] Audit trail is complete
- [ ] Data retention policies are implemented

### 🧪 Testing
- [ ] Security penetration testing completed
- [ ] Vulnerability scanning passed
- [ ] Authentication bypass testing done
- [ ] Rate limiting testing verified
- [ ] Error handling testing completed

## Emergency Procedures

### Security Incident Response
1. **Immediate Actions**
   - Block suspicious IPs
   - Revoke compromised tokens
   - Alert security team
   - Preserve evidence

2. **Investigation**
   - Analyze security logs
   - Identify attack vectors
   - Assess data exposure
   - Document findings

3. **Recovery**
   - Patch vulnerabilities
   - Reset compromised credentials
   - Notify affected users
   - Update security measures

### Contact Information
- Security Team: security@atmconnect.com
- Incident Response: incident@atmconnect.com
- Compliance Officer: compliance@atmconnect.com

EOF

# Set appropriate permissions
chmod 600 .env
chmod 600 .env.production.template
chmod 755 scripts/setup-security.sh
chmod 644 SECURITY_CHECKLIST.md

echo ""
echo "✅ Security setup completed successfully!"
echo ""
echo "📁 Files created:"
echo "   - .env (JWT secrets and configuration)"
echo "   - .env.production.template (Production template)"
echo "   - SECURITY_CHECKLIST.md (Security verification checklist)"
echo "   - keystore/atmconnect.p12 (SSL certificate for development)"
echo ""
echo "⚠️  IMPORTANT SECURITY NOTES:"
echo "   1. The .env file contains sensitive secrets - keep it secure!"
echo "   2. Never commit .env files to version control"
echo "   3. Generate new secrets for production environments"
echo "   4. Follow the security checklist before deployment"
echo "   5. Regularly rotate keys and certificates"
echo ""
echo "🚀 Next steps:"
echo "   1. Review and complete the security checklist"
echo "   2. Set up production environment variables"
echo "   3. Configure monitoring and alerting"
echo "   4. Run security tests and vulnerability scans"
echo ""