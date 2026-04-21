plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	maven("https://maven.shedaniel.me/")
	maven {
		name = "Terraformers"
		url = uri("https://maven.terraformersmc.com/")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("com.terraformersmc:modmenu:${providers.gradleProperty("mod_menu_version").get()}")
	api("me.shedaniel.cloth:cloth-config-fabric:${providers.gradleProperty("cloth_config_version").get()}") {
		exclude("net.fabricmc.fabric-api")
	}
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("cloth_config_version", providers.gradleProperty("cloth_config_version").get())

    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "cloth_config_version" to providers.gradleProperty("cloth_config_version").get()
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	inputs.property("projectName", project.name)

	from("LICENSE") {
		rename { "${it}_${project.name}" }
	}
}
