package com.ifpr.androidapptemplate.baseclasses

data class Tesouro(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val imageUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val endereco: String = "",
    val criadoPorUid: String = "",
    val criadoPorNome: String = "",
    val timestamp: Long = 0L,
    val curtidas: Map<String, Boolean> = emptyMap()
)
