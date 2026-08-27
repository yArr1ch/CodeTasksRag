import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.pet"
version = "0.0.1-SNAPSHOT"

allprojects {
	repositories {
		mavenCentral()
	}
}

subprojects {
	plugins.withId("java") {
		pluginManager.apply("jacoco")

		extensions.configure<JavaPluginExtension> {
			toolchain {
				languageVersion.set(JavaLanguageVersion.of(25))
			}
		}

		tasks.withType<JacocoReport>().configureEach {
			dependsOn(tasks.named("test"))
			reports {
				xml.required.set(true)
				html.required.set(true)
				csv.required.set(false)
			}
		}

		if (project.name == "api-app" || project.name == "worker-app") {
			tasks.withType<JacocoCoverageVerification>().configureEach {
				dependsOn(tasks.named("test"))
				violationRules {
					rule {
						limit {
							counter = "LINE"
							value = "COVEREDRATIO"
							minimum = "0.70".toBigDecimal()
						}
						}
					}
				}
			}
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
		failOnNoDiscoveredTests = false
	}
}
