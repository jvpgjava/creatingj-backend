package io.github.jvpgjava.creatingj.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "io.github.jvpgjava.creatingj",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainMustRemainFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.servlet..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..",
                    "io.swagger.v3..",
                    "org.springframework.ai..",
                    "..application..",
                    "..adapter..",
                    "..bootstrap.."
            );

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdaptersOrWebOrSpringAi = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..",
                    "..bootstrap..",
                    "org.springframework.web..",
                    "org.springframework.ai..",
                    "jakarta.servlet..",
                    "io.swagger.v3.."
            );

    @ArchTest
    static final ArchRule restControllersMustLiveInVersionedWebControllerPackage = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage(
                    "..adapter.in.web.controller.v1..",
                    "..adapter.in.web.controller.v2.."
            );

    @ArchTest
    static final ArchRule controllersMustNotDependOnOutboundAdaptersDbOrSpringAi = noClasses()
            .that().resideInAPackage("..adapter.in.web.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter.out..",
                    "org.springframework.jdbc..",
                    "org.springframework.data..",
                    "jakarta.persistence..",
                    "javax.sql..",
                    "org.springframework.ai.."
            );

    @ArchTest
    static final ArchRule controllersMustNotContainSwaggerAnnotationsOnClass = noClasses()
            .that().resideInAPackage("..adapter.in.web.controller..")
            .should().beAnnotatedWith(io.swagger.v3.oas.annotations.tags.Tag.class)
            .orShould().beAnnotatedWith(io.swagger.v3.oas.annotations.Operation.class);

    @ArchTest
    static final ArchRule controllerMethodsMustNotContainSwaggerAnnotations = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..adapter.in.web.controller..")
            .should().notBeAnnotatedWith(io.swagger.v3.oas.annotations.Operation.class)
            .andShould().notBeAnnotatedWith(io.swagger.v3.oas.annotations.responses.ApiResponse.class)
            .andShould().notBeAnnotatedWith(io.swagger.v3.oas.annotations.responses.ApiResponses.class);

    @ArchTest
    static final ArchRule apiDocsInterfacesMustLiveInVersionedApidocsPackage = classes()
            .that().haveSimpleNameEndingWith("ApiDocs")
            .should().beInterfaces()
            .andShould().resideInAnyPackage(
                    "..adapter.in.web.apidocs.v1..",
                    "..adapter.in.web.apidocs.v2.."
            );

    @ArchTest
    static final ArchRule agentModuleMustNeverExecuteLocalCommandsOrReadServerFilesystem = noClasses()
            .that().resideInAPackage("..agent..")
            .should().dependOnClassesThat().haveNameMatching(
                    "java\\.lang\\.ProcessBuilder|java\\.lang\\.Runtime|java\\.nio\\.file\\.Files"
            );
}
