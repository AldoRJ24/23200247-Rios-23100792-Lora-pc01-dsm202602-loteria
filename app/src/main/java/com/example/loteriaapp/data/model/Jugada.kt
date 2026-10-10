package com.example.loteriaapp.data.model

data class Jugada(
    val sorteoId: String = "",
    val userId: String = "",
    val numeros: List<Int> = emptyList()
)
