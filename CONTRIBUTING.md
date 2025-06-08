# Contributing to ATMConnect

Thank you for your interest in contributing to ATMConnect! This project is an experimental banking application with Bluetooth Low Energy connectivity, developed as an AI-assisted coding experiment.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 8.0+
- Git
- IDE with Spring Boot support (IntelliJ IDEA recommended)

### Development Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/ArcInTower/ATMConnect.git`
3. Navigate to project: `cd ATMConnect`
4. Build the project: `./gradlew build`
5. Run tests: `./gradlew test`

## 🏗️ Architecture Overview

ATMConnect follows **Hexagonal Architecture** principles:
- **Domain Layer**: Core business logic and entities
- **Application Layer**: Use cases and orchestration
- **Infrastructure Layer**: External integrations (BLE, database, security)
- **Presentation Layer**: REST APIs and controllers

### BLE Architecture
- **ATM = BLE Peripheral** (advertises services, accepts connections)
- **Mobile = BLE Central** (scans for ATMs, initiates connections)

## 📋 How to Contribute

### 1. Code Contributions
- **Bug Fixes**: Always welcome
- **New Features**: Please open an issue first to discuss
- **Security Improvements**: Highly appreciated
- **Documentation**: Help improve clarity and completeness

### 2. Testing Contributions
- Unit tests for new functionality
- Integration tests for BLE components
- Security test cases
- Performance benchmarks

### 3. Documentation Contributions
- Code documentation and JavaDoc
- Architecture documentation
- Setup and deployment guides
- API documentation

## 🔧 Development Guidelines

### Code Style
- Follow existing code conventions
- Use meaningful variable and method names
- Write self-documenting code
- Add JavaDoc for public APIs
- Maximum 120 characters per line

### Security Guidelines
- **Never commit secrets** or API keys
- Follow secure coding practices
- Implement proper input validation
- Use encryption for sensitive data
- Follow OWASP guidelines

### BLE Development
- Follow documented BLE architecture
- Use proper GATT service UUIDs
- Implement security requirements (encryption, authentication)
- Test with realistic BLE scenarios
- Consider power consumption and range

### Git Workflow
1. Create feature branch: `git checkout -b feature/your-feature-name`
2. Make small, focused commits
3. Write descriptive commit messages
4. Push to your fork: `git push origin feature/your-feature-name`
5. Create Pull Request

### Commit Message Format
```
type(scope): brief description

Detailed explanation if needed

- Change 1
- Change 2

Closes #issue-number
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## 🧪 Testing Standards

### Required Tests
- Unit tests for all new business logic
- Integration tests for BLE functionality
- Security tests for authentication/encryption
- Architecture tests for layer boundaries

### Test Quality
- Tests should be fast and reliable
- Use meaningful test names
- Follow AAA pattern (Arrange, Act, Assert)
- Mock external dependencies appropriately
- Aim for 90%+ code coverage

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "BLEArchitectureTest"

# Integration tests only
./gradlew integrationTest

# Security tests only
./gradlew test --tests "*Security*"
```

## 📚 Documentation Standards

### Code Documentation
- JavaDoc for all public classes and methods
- Inline comments for complex business logic
- Architecture decision records (ADRs) for major decisions
- README updates for new features

### API Documentation
- OpenAPI/Swagger specifications
- Request/response examples
- Error code documentation
- Security requirements

## 🔍 Code Review Process

### Pull Request Requirements
- [ ] All tests pass
- [ ] Code coverage meets requirements
- [ ] Documentation updated
- [ ] Security review completed
- [ ] Architecture compliance verified
- [ ] No secrets or sensitive data

### Review Criteria
- Code quality and maintainability
- Security considerations
- Performance implications
- Architecture alignment
- Test coverage and quality

## 🚨 Security Considerations

### Sensitive Areas
- BLE communication and pairing
- Cryptographic implementations
- Authentication and authorization
- Transaction processing
- Certificate management

### Security Review Checklist
- [ ] Input validation implemented
- [ ] Encryption used for sensitive data
- [ ] Authentication properly implemented
- [ ] Authorization checks in place
- [ ] No hardcoded secrets
- [ ] Secure communication protocols

## 🐛 Bug Reports

### Before Submitting
1. Check existing issues
2. Update to latest version
3. Provide minimal reproduction case

### Bug Report Template
```markdown
**Description**: Brief description of the bug

**Steps to Reproduce**:
1. Step one
2. Step two
3. Step three

**Expected Behavior**: What should happen

**Actual Behavior**: What actually happens

**Environment**:
- OS: [e.g., Ubuntu 22.04]
- Java Version: [e.g., Java 17]
- Gradle Version: [e.g., 8.0]
- BLE Hardware: [if applicable]

**Additional Context**: Any other relevant information
```

## 💡 Feature Requests

### Before Submitting
- Check if feature already exists
- Consider if it fits project scope
- Think about implementation complexity

### Feature Request Template
```markdown
**Feature Description**: Clear description of the feature

**Use Case**: Why is this feature needed?

**Proposed Solution**: How should it work?

**Alternatives Considered**: Other approaches considered

**Additional Context**: Any other relevant information
```

## 📖 Resources

### Documentation
- [BLE Architecture](docs/BLE_ARCHITECTURE.md)
- [Security Guidelines](docs/SECURITY.md)
- [API Documentation](docs/API.md)

### External Resources
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Bluetooth Low Energy Specification](https://www.bluetooth.com/specifications/specs/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [OWASP Security Guidelines](https://owasp.org/)

## 🤝 Community

### Communication
- Use GitHub Issues for bugs and features
- Use GitHub Discussions for questions
- Be respectful and constructive
- Help others when possible

### Code of Conduct
We follow a simple code of conduct:
- Be respectful and inclusive
- Focus on what's best for the project
- Accept constructive criticism gracefully
- Help create a welcoming environment

## 📄 License

By contributing to ATMConnect, you agree that your contributions will be licensed under the MIT License. See [LICENSE](LICENSE) for more details.

---

## 🙏 Thank You

Your contributions help make ATMConnect better for everyone. Whether it's a bug fix, new feature, documentation improvement, or helping other contributors, every contribution matters!

**Happy Coding!** 🎉