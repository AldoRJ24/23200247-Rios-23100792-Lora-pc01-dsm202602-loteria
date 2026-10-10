package com.example.loteriaapp.data.model

data class Sorteo(
    val id: String = "",
    val nombre: String = "",
    val fecha: String = "",
    val estado: String = ESTADO_ABIERTO
) {
    companion object {
        const val ESTADO_ABIERTO = "abierto"
        const val ESTADO_EN_CURSO = "en_curso"
        const val ESTADO_FINALIZADO = "finalizado"
    }
}
