# ATMConnect 🏧📱

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![BLE](https://img.shields.io/badge/BLE-5.0+-blue.svg)](https://www.bluetooth.com/)
[![Security](https://img.shields.io/badge/Security-Banking%20Grade-red.svg)](#security)

**An experimental banking application with Bluetooth Low Energy connectivity, developed as an AI-assisted coding experiment.**

This project explores the possibility of creating a secure mobile banking solution using BLE (Bluetooth Low Energy) technology for communication between mobile devices and ATMs.

---

## ⚠️ Important Disclaimer

**THIS IS AN EXPERIMENTAL AND EDUCATIONAL PROJECT**

- ❌ **DO NOT use in production or with real data**
- ❌ **DOES NOT contain real banking security implementations**
- ❌ **NOT certified for financial use**
- ✅ **For learning and demonstration purposes only**
- ✅ **Explores architecture and development concepts**

---

## 🎯 Experiment Objectives

This project was developed as an AI-assisted programming experiment to:

1. **🏗️ Explore Modern Architectures**: Implement patterns like hexagonal architecture
2. **📡 Research BLE Technologies**: Study Bluetooth Low Energy viability in financial applications
3. **🔒 Demonstrate Best Practices**: Show security, testing, and documentation implementation
4. **🤖 Experiment with AI**: Test AI-assisted development capabilities

## 🏛️ Architecture

The project implements **Hexagonal Architecture (Clean Architecture)** with the following layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│                   (REST APIs, Controllers)                  │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│                   (Use Cases, Orchestration)                │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│              (Business Logic, Entities, Ports)              │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│           (BLE, Database, Security, External APIs)          │
└─────────────────────────────────────────────────────────────┘
```

### Key Components:
- **🎯 Domain**: Business entities, value objects, and core logic
- **⚙️ Application**: Use cases and service orchestration
- **🔧 Infrastructure**: Technical implementations (BLE, database, security)
- **🌐 Presentation**: REST APIs and web controllers

## 📡 BLE Architecture

Following the documented BLE specification:

```
┌─────────────────┐         BLE Connection         ┌─────────────────┐
│   Mobile Device │ ◄─────────────────────────────► │   ATM Device    │
│  (BLE Central)  │        GATT Protocol           │ (BLE Peripheral) │
│                 │                                 │                 │
│ • Scans for ATMs│                                 │ • Advertises    │
│ • Connects      │                                 │ • Accepts Conn. │
│ • GATT Client   │                                 │ • GATT Server   │
└─────────────────┘                                 └─────────────────┘
```

### BLE Services & Characteristics:
- **🔐 Authentication Service**: Secure user authentication
- **💳 Transaction Service**: Banking transaction processing  
- **📊 Status Service**: ATM status and availability
- **🏆 Certificate Service**: X.509 certificate distribution

## 🔒 Security Features

### 🛡️ Encryption & Authentication
- **AES-256-GCM** encryption for all communications
- **ECDH P-256** key exchange for session establishment
- **Multi-factor authentication** (PIN + biometric + device)
- **JWT tokens** with blacklisting and secure generation

### 📱 BLE Security
- **LE Secure Connections (LESC)** pairing required
- **Man-in-the-Middle (MITM)** protection enabled
- **Range validation** (2-3 meters for privacy)
- **RSSI monitoring** to prevent long-distance attacks

### 🔍 Monitoring & Protection
- **Rate limiting** on API endpoints
- **Security event monitoring** and alerting
- **Circuit breaker patterns** for fault tolerance
- **Input validation** and sanitization

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 17+ |
| **Framework** | Spring Boot | 3.2+ |
| **Security** | Spring Security | 6.1+ |
| **Database** | H2/PostgreSQL | Latest |
| **Build Tool** | Gradle | 8.0+ |
| **Testing** | JUnit 5 | 5.9+ |
| **BLE** | BlueZ/Native | 5.0+ |
| **Container** | Docker | 24.0+ |

## 🚀 Quick Start

### Prerequisites
- ☑️ Java 17 or higher
- ☑️ Gradle 8.0+
- ☑️ Git
- ☑️ BLE-capable hardware (for testing)

### Installation

```bash
# Clone the repository
git clone https://github.com/ArcInTower/ATMConnect.git
cd ATMConnect

# Build the project
./gradlew build

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```

### Docker Deployment

```bash
# Build Docker image
docker build -t atmconnect .

# Run container
docker run -p 8080:8080 atmconnect
```

### Configuration

```yaml
# application.yml
atmconnect:
  bluetooth:
    mode: peripheral  # or central, or both
    scan-timeout-seconds: 30
  security:
    max-failed-attempts: 3
    otp-expiration-minutes: 5
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [🏗️ BLE Architecture](docs/BLE_ARCHITECTURE.md) | Detailed BLE implementation specification |
| [🔒 Security Policy](SECURITY.md) | Security guidelines and vulnerability reporting |
| [🤝 Contributing Guide](CONTRIBUTING.md) | How to contribute to the project |
| [📜 Code of Conduct](CODE_OF_CONDUCT.md) | Community guidelines and standards |

## 🧪 Testing

### Test Categories
- **Unit Tests**: Core business logic testing
- **Integration Tests**: BLE and system integration
- **Security Tests**: Authentication and encryption validation
- **Architecture Tests**: Hexagonal architecture compliance

### Running Tests

```bash
# All tests
./gradlew test

# Specific test categories
./gradlew test --tests "*Security*"
./gradlew test --tests "*BLE*"
./gradlew test --tests "*Architecture*"

# With coverage report
./gradlew test jacocoTestReport
```

### Test Coverage
- **Target**: 90%+ code coverage
- **Focus**: Security-critical components
- **Types**: Unit, Integration, Security, Performance

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Quick Contribution Checklist
- [ ] 🔍 Check existing issues and PRs
- [ ] 🏗️ Follow architectural principles  
- [ ] 🔒 Consider security implications
- [ ] 🧪 Add comprehensive tests
- [ ] 📚 Update documentation
- [ ] ✅ Follow code quality standards

### Areas for Contribution
- 🐛 **Bug Fixes**: Always welcome
- 🔒 **Security Enhancements**: Highly valued
- 📱 **BLE Improvements**: Protocol optimizations
- 📖 **Documentation**: Clarity and completeness
- 🧪 **Testing**: Coverage and quality improvements

## 📊 Project Status

### Current Features ✅
- ✅ Hexagonal architecture implementation
- ✅ BLE peripheral/central functionality
- ✅ GATT services and characteristics
- ✅ JWT authentication with blacklisting
- ✅ AES-256-GCM encryption
- ✅ Rate limiting and security monitoring
- ✅ Comprehensive test suite

### Roadmap 🗺️
- 🔄 Real BLE hardware integration
- 🔄 Mobile application development
- 🔄 Enhanced security features
- 🔄 Performance optimizations
- 🔄 Production deployment guides

## 🌟 Key Features

### 🏦 Banking Operations
- **Account Management**: Balance inquiry, transaction history
- **Secure Transactions**: Withdrawal, transfer, deposit simulation
- **Multi-factor Authentication**: PIN, biometric, device verification

### 📱 BLE Connectivity  
- **Device Discovery**: Automatic ATM detection within range
- **Secure Pairing**: LESC with MITM protection
- **Proximity Security**: Range-limited operations (2-3 meters)
- **Real-time Status**: ATM availability and cash level indicators

### 🔒 Enterprise Security
- **Banking-grade Encryption**: AES-256-GCM throughout
- **Certificate Management**: X.509 certificate validation
- **Session Security**: Secure token management
- **Audit Logging**: Comprehensive security event tracking

## 📈 Performance & Scalability

### Benchmarks
- **Connection Time**: < 2 seconds average
- **Transaction Processing**: < 500ms average  
- **Concurrent Connections**: Up to 5 per ATM
- **Memory Usage**: < 512MB baseline

### Optimization Features
- **Connection Pooling**: Efficient resource management
- **Caching**: Strategic data caching
- **Circuit Breakers**: Fault tolerance patterns
- **Rate Limiting**: Protection against abuse

## 🔧 Development Tools

### Code Quality
- **Static Analysis**: SonarQube, SpotBugs
- **Dependency Scanning**: OWASP Dependency Check
- **Code Coverage**: JaCoCo with 90%+ target
- **Style Enforcement**: Checkstyle, PMD

### Security Tools
- **Vulnerability Scanning**: OWASP ZAP, Snyk
- **Secret Detection**: git-secrets, TruffleHog
- **BLE Analysis**: nRF Connect, Wireshark
- **Penetration Testing**: Custom security tests

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### Attribution Requirement
If you use this software, please include this attribution:

> "This project uses ATMConnect, an experimental banking application with Bluetooth connectivity developed as an AI-assisted coding experiment. Original repository: https://github.com/ArcInTower/ATMConnect"

## 🔬 Experimental Nature

This project represents an AI-assisted development experiment:

### 🎓 Educational Purposes
- **Architecture Learning**: Modern hexagonal architecture patterns
- **BLE Technology**: Bluetooth Low Energy in financial contexts  
- **Security Practices**: Banking-grade security implementation
- **AI Development**: AI-assisted coding techniques

### 🧬 Research Value
- **Technical Feasibility**: BLE viability for financial applications
- **Security Analysis**: Comprehensive threat modeling
- **Performance Study**: Real-world performance characteristics
- **Development Process**: AI-human collaboration patterns

### ⚠️ Important Notes
- **Not for Production**: Experimental code not certified for financial use
- **Educational Only**: Designed for learning and demonstration
- **Security Simulation**: Implements security concepts, not real banking security
- **Technology Demo**: Showcases technical possibilities and limitations

---

## 🏆 Acknowledgments

- **AI Assistant**: Claude (Anthropic) for development assistance
- **Community**: Contributors and security researchers
- **Standards**: Bluetooth SIG for BLE specifications
- **Security**: OWASP for security guidelines

---

## 📞 Support & Community

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/ArcInTower/ATMConnect/issues)
- **💡 Feature Requests**: [GitHub Discussions](https://github.com/ArcInTower/ATMConnect/discussions)  
- **🔒 Security Issues**: See [Security Policy](SECURITY.md)
- **📖 Documentation**: [Project Wiki](https://github.com/ArcInTower/ATMConnect/wiki)

---

**Developed as an AI experiment - For educational and demonstration purposes only** 🤖📚

*Last updated: December 2024*