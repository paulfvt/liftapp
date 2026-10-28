package com.paulfvt.liftapp.progress.data

import android.content.Context
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.profile.domain.Sex
import com.paulfvt.liftapp.progress.domain.BodyDiagram
import com.paulfvt.liftapp.progress.domain.BodyView
import com.paulfvt.liftapp.progress.domain.BodyZone
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BodyMapDto(val diagrams: List<BodyDiagramDto>)

@Serializable
private data class BodyDiagramDto(
    val sex: String,
    val view: BodyView,
    val bounds: List<Float>,
    val outline: List<String>,
    val muscles: List<BodyZoneDto>
)

@Serializable
private data class BodyZoneDto(val muscle: Muscle, val path: String)

// Silhouettes de la carte du corps, préparées par tools/body-map/build_body_map.py. Le fichier (230 Ko) n'est lu
// qu'une fois, à la première demande, puis gardé en mémoire.
@Singleton
class BodyMapRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private var cache: Map<String, List<BodyDiagram>>? = null

    // Silhouette de femme si le profil l'indique, d'homme sinon (sexe non renseigné compris) ; face puis dos.
    suspend fun diagrams(sex: Sex?): List<BodyDiagram> {
        val bySex = mutex.withLock { cache ?: load().also { cache = it } }
        val key = if (sex == Sex.FEMALE) Sex.FEMALE.name else Sex.MALE.name
        return bySex[key].orEmpty().sortedBy { it.view }
    }

    private suspend fun load(): Map<String, List<BodyDiagram>> = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<BodyMapDto>(text).diagrams.groupBy({ it.sex }) { dto ->
            BodyDiagram(dto.view, dto.bounds, dto.outline, dto.muscles.map { BodyZone(it.muscle, it.path) })
        }
    }

    private companion object {
        const val ASSET = "body-map/body_map.json"

        // Le fichier contient aussi sa source (« source ») : ignorée ici.
        val json = Json { ignoreUnknownKeys = true }
    }
}
