# Règles R8 propres à l'app. Hilt, Room et kotlinx.serialization fournissent déjà les leurs.

# ML Kit (lecture des étiquettes) crée ses composants par leur nom, lu dans le manifeste. R8 ne voit pas cet appel et
# retirait leur constructeur : la reconnaissance de texte ne trouvait plus rien et l'app plantait au premier scan
# (version release seulement).
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}

# WorkManager 2.7.1, amené par Glance (le widget), crée ses « InputMerger » par leur nom et sa règle ne garde pas leur
# constructeur : aucune tâche de fond ne démarrait et le widget restait en chargement (version release seulement).
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
