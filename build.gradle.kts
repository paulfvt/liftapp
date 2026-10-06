// Les plugins sont déclarés ici sans être appliqués, puis appliqués module par module.
// AGP 9 intègre Kotlin : le plugin org.jetbrains.kotlin.android n'est plus nécessaire.
// Le plugin Compose porte la version de Kotlin du catalogue : il aligne aussi le compilateur Kotlin dessus.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
}
