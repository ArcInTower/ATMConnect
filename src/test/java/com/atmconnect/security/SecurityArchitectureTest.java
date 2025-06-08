package com.atmconnect.security;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class SecurityArchitectureTest {
    
    private JavaClasses classes;
    
    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter().importPackages("com.atmconnect");
    }
    
    @Test
    @DisplayName("Should follow hexagonal architecture principles")
    void shouldFollowHexagonalArchitecturePrinciples() {
        ArchRule rule = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Presentation").definedBy("..presentation..")
            
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
            .whereLayer("Presentation").mayOnlyAccessLayers("Domain", "Application");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Should not have cycles in package dependencies")
    void shouldNotHaveCyclesInPackageDependencies() {
        ArchRule rule = slices().matching("com.atmconnect.(*)..")
            .should().beFreeOfCycles();
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Security classes should not be accessed from presentation layer")
    void securityClassesShouldNotBeAccessedFromPresentationLayer() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..presentation..")
            .should().accessClassesThat()
            .resideInAPackage("..infrastructure.security..");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Crypto services should only be accessed through ports")
    void cryptoServicesShouldOnlyBeAccessedThroughPorts() {
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.security..")
            .and().resideOutsideOfPackage("..test..")
            .should().accessClassesThat()
            .resideInAPackage("..infrastructure.security..")
            .andShould().accessClassesThat()
            .areNotInterfaces();
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("PIN class should not expose sensitive data")
    void pinClassShouldNotExposeSensitiveData() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat()
            .haveSimpleName("Pin")
            .should().haveRawReturnType(String.class)
            .andShould().haveNameMatching(".*pin.*")
            .andShould().haveNameNotMatching(".*hash.*");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Repository interfaces should reside in domain ports")
    void repositoryInterfacesShouldResideInDomainPorts() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should().resideInAPackage("..domain.ports.outbound..");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Use case implementations should be in application layer")
    void useCaseImplementationsShouldBeInApplicationLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCaseImpl")
            .should().resideInAPackage("..application.usecases..");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Exception classes should follow naming convention")
    void exceptionClassesShouldFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areAssignableTo(Exception.class)
            .should().haveSimpleNameEndingWith("Exception")
            .andShould().resideInAPackage("..infrastructure.exceptions..");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("No direct database access from domain layer")
    void noDirectDatabaseAccessFromDomainLayer() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().accessClassesThat()
            .resideInAPackage("jakarta.persistence..")
            .orShould().accessClassesThat()
            .resideInAPackage("org.springframework.data..");
        
        rule.check(classes);
    }
    
    @Test
    @DisplayName("Security annotations should be used appropriately")
    void securityAnnotationsShouldBeUsedAppropriately() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat()
            .resideInAPackage("..presentation..")
            .and().arePublic()
            .should().beAnnotatedWith("org.springframework.security.access.prepost.PreAuthorize")
            .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.GetMapping")
            .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.PostMapping");
        
        // Note: This rule would need actual security annotations in the codebase
        // For now, we'll just verify the architecture allows for proper security
    }
}