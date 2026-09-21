package br.com.poc.pix;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testes de arquitetura que blindam os principios hexagonais (constitution P1).
 * Falham o build se o dominio vazar para framework/infra.
 */
@AnalyzeClasses(packages = "br.com.poc.pix", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dominio_nao_depende_de_framework_ou_infra =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "org.springframework.amqp..",
                            "com.rabbitmq..",
                            "jakarta.persistence..",
                            "java.sql..",
                            "javax.sql.."
                    )
                    .as("o dominio (..domain..) nao pode depender de Spring, JDBC, JPA ou RabbitMQ");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_adapters =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("o dominio (..domain..) nao pode depender dos adapters");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_application =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .as("o dominio (..domain..) nao pode depender da camada de aplicacao");
}
