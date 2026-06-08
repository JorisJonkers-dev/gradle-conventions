import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Target

plugins {
    java
}

interface JooqCodegenExtension {
    val schemaName: Property<String>
    val packageName: Property<String>
    val migrationLocations: ListProperty<String>
    val outputDirectory: DirectoryProperty
}

val jooqCodegen =
    extensions.create<JooqCodegenExtension>("jooqCodegen").apply {
        // These default to the org migration layout, but consumers can override them per project.
        schemaName.convention(providers.gradleProperty("extratoast.jooq.schema").orElse("public"))
        packageName.convention(providers.gradleProperty("extratoast.jooq.package").orElse("dev.extratoast.jooq.generated"))
        migrationLocations.convention(
            providers.gradleProperty("extratoast.jooq.migrationLocations")
                .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) }
                .orElse(listOf("filesystem:src/main/resources/db/migration")),
        )
        outputDirectory.convention(
            providers.gradleProperty("extratoast.jooq.outputDirectory")
                .orElse("generated/jooq")
                .map { layout.buildDirectory.dir(it).get() },
        )
    }

val generateJooq by tasks.registering {
    group = "jooq"
    description = "Generate jOOQ classes from Flyway SQL migrations using DDLDatabase."

    val projectDirectory = layout.projectDirectory
    outputs.dir(jooqCodegen.outputDirectory)
    inputs.property("schemaName", jooqCodegen.schemaName)
    inputs.property("packageName", jooqCodegen.packageName)
    inputs.property("migrationLocations", jooqCodegen.migrationLocations)
    inputs.files(
        jooqCodegen.migrationLocations.map { locations ->
            locations.map { location ->
                fileTree(projectDirectory.dir(location.removePrefix("filesystem:"))) {
                    include("**/*.sql")
                }
            }
        },
    )

    doLast {
        val schema = jooqCodegen.schemaName.get()
        val pkg = jooqCodegen.packageName.get()
        val outDir = jooqCodegen.outputDirectory.get().asFile.also { it.mkdirs() }

        val scripts = jooqCodegen.migrationLocations.get().joinToString(";") { location ->
            val path = location.removePrefix("filesystem:")
            projectDirectory.dir(path).asFile.absolutePath + "/*.sql"
        }

        GenerationTool.generate(
            Configuration()
                .withGenerator(
                    Generator()
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                                .withInputSchema(schema)
                                .withProperties(
                                    listOf(
                                        org.jooq.meta.jaxb.Property().withKey("scripts").withValue(scripts),
                                        org.jooq.meta.jaxb.Property().withKey("sort").withValue("flyway"),
                                        org.jooq.meta.jaxb.Property().withKey("defaultNameCase").withValue("lower"),
                                    ),
                                )
                                .withIncludes(".*")
                                .withExcludes("flyway_schema_history"),
                        )
                        .withTarget(
                            Target()
                                .withPackageName(pkg)
                                .withDirectory(outDir.absolutePath),
                        ),
                ),
        )
    }
}

sourceSets {
    main {
        java.srcDir(jooqCodegen.outputDirectory)
    }
}

tasks.named("compileJava") {
    dependsOn(generateJooq)
}

tasks.matching { it.name == "compileKotlin" }.configureEach {
    dependsOn(generateJooq)
}
