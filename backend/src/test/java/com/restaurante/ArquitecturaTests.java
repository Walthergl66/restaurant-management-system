package com.restaurante;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guardia de la arquitectura por capas dentro de cada modulo Spring Modulith.
 *
 * <p>Complementa a {@link ModularityTests}, que protege las fronteras entre modulos
 * (un modulo solo puede usar la API publica de otro). Aqui se protege lo que
 * Modulith no ve: las cuatro capas internas de cada modulo
 * ({@code domain}, {@code application}, {@code web}, {@code infrastructure}).
 *
 * <p>Tabla de dependencias permitidas:
 * <ul>
 *   <li>{@code domain} -> {@code domain} + {@code shared.domain}</li>
 *   <li>{@code application} -> {@code domain} + {@code infrastructure} (repositorios)
 *       + API publica de otros modulos</li>
 *   <li>{@code web} -> {@code application} + {@code domain} (enums, Money,
 *       excepciones y entidades, que el DTO usa para auto-mapearse)</li>
 *   <li>{@code infrastructure} -> {@code domain} + {@code application}</li>
 * </ul>
 *
 * <p>No se usa una regla de "sin ciclos entre capas" porque en este diseno los
 * ciclos {@code application <-> web} (los DTO son hoy la entrada del caso de uso)
 * y {@code application <-> infrastructure} (los repositorios viven en
 * infrastructure) son estructurales. Cuando la deuda de
 * {@code application -> web} llegue a cero, esa regla si sera aplicable.
 */
@AnalyzeClasses(packages = "com.restaurante", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Arquitectura por capas")
class ArquitecturaTests {

    /** Las cuatro capas internas de un modulo. */
    private static final Set<String> CAPAS = Set.of("domain", "application", "web", "infrastructure");

    /** Modulo abierto: cualquiera puede usar shared sin romper Modulith. */
    private static final String MODULO_ABIERTO = "shared";

    /**
     * Excepcion unica y deliberada a "el dominio no depende de infrastructure".
     * El dominio es la fila JPA (active record), asi que el mapeo de columnas
     * vive en el dominio y necesita el AttributeConverter de Money. Es el precio
     * consciente de esa decision; cualquier otra dependencia hacia
     * infrastructure (repositorios, seguridad, web) si es un error y la regla lo
     * rechaza.
     */
    private static final String CLASE_CONVERSOR_MONEDA = "com.restaurante.shared.infrastructure.MoneyConverter";

    /**
     * Deuda conocida: la capa application usa los DTO de web como contrato de
     * entrada. El nombre de la capa interna no aporta (un Command o un Request
     * son el mismo tipo), asi que se migra a records Command/Result en
     * application y se baja este numero a cero. Estas marcas son el tope: si
     * sube, el test falla.
     *
     * <p>Los topes son la medicion de ArchUnit, no un conteo de lineas de
     * import: 47 dependencias hacia 17 clases. Si se migra un servicio a
     * Command/Result, hay que bajar aqui los dos numeros en el mismo commit.
     */
    private static final int MAX_IMPORTS_DE_APPLICATION_A_WEB = 47;
    private static final int MAX_CLASES_DE_APPLICATION_CON_DTO_WEB = 17;

    // ---------------------------------------------------------------------
    // Reglas duras: hoy estan en verde y deben seguir en verde.
    // ---------------------------------------------------------------------

    @ArchTest
    static final ArchRule domainNoDependeDeWeb = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..web..")
            .as("la capa domain no debe depender de la capa web");

    @ArchTest
    static final ArchRule domainNoDependeDeApplication = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .as("la capa domain no debe depender de la capa application");

    @ArchTest
    static final ArchRule domainNoDependeDeInfrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat(resideInAPackage("..infrastructure..")
                    .and(not(assignableTo(CLASE_CONVERSOR_MONEDA))))
            .as("la capa domain no debe depender de la capa infrastructure, "
                    + "salvo el conversor de Money (" + CLASE_CONVERSOR_MONEDA + ")");

    @ArchTest
    static final ArchRule webNoDependeDeInfrastructure = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .as("la capa web no debe depender de la capa infrastructure");

    // ---------------------------------------------------------------------
    // Pruebas propias: necesitan contar o comparar modulos, algo que las reglas
    // declarativas de ArchUnit no expresan con precision.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("ninguna capa accede a la capa interna de otro modulo")
    void ningunaCapaAccedeACapaInternaDeOtroModulo() {
        List<String> ofensas = new ArrayList<>();
        for (JavaClass origen : clases()) {
            String capaOrigen = capaDe(origen.getPackageName());
            if (capaOrigen == null) {
                continue;
            }
            String moduloOrigen = moduloDe(origen.getPackageName());
            for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                String paqueteDestino = dependencia.getTargetClass().getPackageName();
                if (capaDe(paqueteDestino) == null) {
                    continue;
                }
                String moduloDestino = moduloDe(paqueteDestino);
                if (!moduloDestino.equals(moduloOrigen) && !MODULO_ABIERTO.equals(moduloDestino)) {
                    ofensas.add(origen.getName() + " -> " + dependencia.getDescription());
                }
            }
        }
        assertTrue(ofensas.isEmpty(),
                () -> "un modulo solo puede usar la API publica de otro modulo, no sus capas:\n  "
                        + String.join("\n  ", ofensas));
    }

    @Test
    @DisplayName("shared no depende de ningun modulo de negocio")
    void sharedNoDependeDeModulosDeNegocio() {
        List<String> ofensas = new ArrayList<>();
        for (JavaClass origen : clases()) {
            if (!MODULO_ABIERTO.equals(moduloDe(origen.getPackageName()))) {
                continue;
            }
            for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                String moduloDestino = moduloDe(dependencia.getTargetClass().getPackageName());
                if (moduloDestino != null && !MODULO_ABIERTO.equals(moduloDestino)) {
                    ofensas.add(origen.getName() + " -> " + dependencia.getDescription());
                }
            }
        }
        assertTrue(ofensas.isEmpty(),
                () -> "shared no debe conocer modulos de negocio:\n  " + String.join("\n  ", ofensas));
    }

    @Test
    @DisplayName("la deuda de application hacia web no crece")
    void laDeudaDeApplicationHaciaWebNoCrece() {
        Set<String> imports = new TreeSet<>();
        Set<String> clasesConDto = new LinkedHashSet<>();
        for (JavaClass origen : clases()) {
            if (!"application".equals(capaDe(origen.getPackageName()))) {
                continue;
            }
            for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                if (!"web".equals(capaDe(dependencia.getTargetClass().getPackageName()))) {
                    continue;
                }
                imports.add(origen.getSimpleName() + " -> " + dependencia.getTargetClass().getSimpleName());
                clasesConDto.add(origen.getName());
            }
        }
        String detalle = " (hoy son " + imports.size() + " dependencias en " + clasesConDto.size() + " clases)";
        assertTrue(imports.size() <= MAX_IMPORTS_DE_APPLICATION_A_WEB,
                () -> "application no debe importar mas DTOs de web de los " + MAX_IMPORTS_DE_APPLICATION_A_WEB
                        + " actuales" + detalle + ":\n  " + String.join("\n  ", imports));
        assertTrue(clasesConDto.size() <= MAX_CLASES_DE_APPLICATION_CON_DTO_WEB,
                () -> "ningun servicio nuevo debe adoptar el patron" + detalle + ":\n  "
                        + String.join("\n  ", clasesConDto));
    }

    /** Modulo de un paquete del proyecto, o null si no lo es. */
    private static String moduloDe(String paquete) {
        String[] p = paquete.split("\\.");
        return p.length >= 3 && "com".equals(p[0]) && "restaurante".equals(p[1]) ? p[2] : null;
    }

    /** Capa de un paquete del proyecto, o null si el paquete no es una capa. */
    private static String capaDe(String paquete) {
        String[] p = paquete.split("\\.");
        if (p.length < 4 || !"com".equals(p[0]) || !"restaurante".equals(p[1])) {
            return null;
        }
        return CAPAS.contains(p[3]) ? p[3] : null;
    }

    private static JavaClasses clases() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.restaurante");
    }
}
