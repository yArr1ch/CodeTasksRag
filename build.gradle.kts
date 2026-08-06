import org.gradle.api.plugins.JavaPluginExtension

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
		extensions.configure<JavaPluginExtension> {
			toolchain {
				languageVersion.set(JavaLanguageVersion.of(25))
			}
		}
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
		failOnNoDiscoveredTests = false
	}
}
