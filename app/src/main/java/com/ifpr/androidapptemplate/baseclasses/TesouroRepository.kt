package com.ifpr.androidapptemplate.baseclasses

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TesouroRepository {

    // Referência principal para o nó "tesouros" no Firebase
    private val databaseReference = FirebaseDatabase.getInstance().getReference("tesouros")

    /**
     * Busca TODOS os tesouros no banco de dados, ordenados pelo mais recente.
     * @param onResult Callback que será chamado com o resultado (sucesso ou falha).
     */
    fun buscarTodosOsTesouros(onResult: (Result<List<Tesouro>>) -> Unit) {
        val query = databaseReference.orderByChild("timestamp")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tesourosList = mutableListOf<Tesouro>()
                if (snapshot.exists()) {
                    for (tesouroSnapshot in snapshot.children) {
                        tesouroSnapshot.getValue(Tesouro::class.java)?.let { tesouro ->
                            tesourosList.add(tesouro)
                        }
                    }
                }
                // Ordena a lista final pela data (mais recentes primeiro)
                onResult(Result.success(tesourosList.sortedByDescending { it.timestamp }))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TesouroRepository", "Erro ao buscar todos os tesouros: ${error.message}")
                onResult(Result.failure(error.toException()))
            }
        })
    }

    /**
     * Busca apenas os tesouros criados por um usuário específico.
     * @param userId O ID do usuário para o qual buscar os tesouros.
     * @param onResult Callback que será chamado com o resultado (sucesso ou falha).
     */
    fun buscarTesourosDoUsuario(userId: String, onResult: (Result<List<Tesouro>>) -> Unit) {
        val query = databaseReference.orderByChild("criadoPorUid").equalTo(userId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tesourosList = mutableListOf<Tesouro>()
                if (snapshot.exists()) {
                    for (tesouroSnapshot in snapshot.children) {
                        tesouroSnapshot.getValue(Tesouro::class.java)?.let { tesouro ->
                            tesourosList.add(tesouro)
                        }
                    }
                }
                // Ordena a lista final pela data (mais recentes primeiro)
                onResult(Result.success(tesourosList.sortedByDescending { it.timestamp }))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TesouroRepository", "Erro ao buscar tesouros do usuário: ${error.message}")
                onResult(Result.failure(error.toException()))
            }
        })
    }
}