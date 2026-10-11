plugins {
    id("java")
}

group = "io.github.kalmemarq"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.3"
val jomlVersion = "1.10.9"
val lwjglNatives = "natives-windows"
val imguiVersion = "1.92.7.1"
val fastUtilVersion = "8.5.18"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-mimalloc")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-spng")
    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-mimalloc::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-openal::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-sdl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-spng::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")

    implementation("io.github.spair:imgui-java-binding:${imguiVersion}")
    implementation("io.github.spair:imgui-java-natives-windows:${imguiVersion}")

    implementation("org.joml:joml:$jomlVersion")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("it.unimi.dsi:fastutil:$fastUtilVersion")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

val shadersDir = file("src/main/resources/shaders")
val generatedShadersDir = layout.buildDirectory.dir("generated/shaders")

tasks.register<Exec>("convertWgslShaders") {
    group = "build"
    description = "Converts wgsl shaders to vsh/fsh via naga + spirv-cross"

    inputs.dir(shadersDir)
    outputs.dir(generatedShadersDir)

    workingDir = projectDir

    commandLine("python3", "scripts/wgsl2glsl.py", generatedShadersDir.get().asFile.absolutePath)

    onlyIf {
        shadersDir.exists() && shadersDir.listFiles()?.any { it.extension == "wgsl" } == true
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("convertWgslShaders")

    exclude("**/shaders/include")
    exclude("**/*.wgsl")

    from(generatedShadersDir) {
        into("shaders")
    }
}

tasks.test {
    useJUnitPlatform()
}