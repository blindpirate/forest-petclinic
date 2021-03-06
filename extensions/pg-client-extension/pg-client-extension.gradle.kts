val configurePublication: Project.() -> Unit by rootProject.ext
configurePublication()

val libs: (String) -> String by rootProject.ext

dependencies {
    api(libs("vertx-pg-client"))
    implementation(project(":core"))
}
