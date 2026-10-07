package com.paulfvt.liftapp.core.domain

// Déplace l'élément d'indice from à l'indice to ; les éléments entre les deux glissent d'un cran.
// Sert au glisser-déposer comme aux actions « Monter » et « Descendre ».
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    require(from in indices && to in indices) { "Indices hors de la liste : $from -> $to" }
    return toMutableList().apply { add(to, removeAt(from)) }
}

// Insère item juste après anchor, ou à la fin si anchor est absent.
fun <T> List<T>.insertedAfter(anchor: T, item: T): List<T> {
    val position = indexOf(anchor).let { if (it == -1) size else it + 1 }
    return toMutableList().apply { add(position, item) }
}
