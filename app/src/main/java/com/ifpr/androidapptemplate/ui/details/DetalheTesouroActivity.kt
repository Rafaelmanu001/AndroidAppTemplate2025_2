package com.ifpr.androidapptemplate.ui.details

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Tesouro

class DetalheTesouroActivity : AppCompatActivity() {

    private lateinit var tesouroId: String
    private lateinit var buttonCurtir: ImageButton
    private lateinit var contagemCurtidasTextView: TextView
    private var tesouroAtual: Tesouro? = null
    private val usuarioAtualUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_tesouro)

        // Pega o ID do tesouro
        val idRecebido = intent.getStringExtra("TESOURO_ID")
        if (idRecebido == null) {
            Toast.makeText(this, "Erro: Tesouro não encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tesouroId = idRecebido

        // Vincula os componentes da UI
        buttonCurtir = findViewById(R.id.button_curtir)
        contagemCurtidasTextView = findViewById(R.id.text_view_contagem_curtidas)

        // Configura o clique do botão de curtir
        buttonCurtir.setOnClickListener {
            alternarCurtida()
        }

        // Inicia o carregamento dos dados do Firebase
        carregarDadosDoTesouro()
    }

    private fun carregarDadosDoTesouro() {
        // Usa addValueEventListener para ouvir mudanças em tempo real (como novas curtidas)
        val databaseRef = FirebaseDatabase.getInstance().getReference("tesouros").child(tesouroId)
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tesouroAtual = snapshot.getValue(Tesouro::class.java)
                tesouroAtual?.let { preencherTela(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "Falha ao carregar dados.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun preencherTela(tesouro: Tesouro) {
        val imageView: ImageView = findViewById(R.id.imageView_detalhe)
        val nomeView: TextView = findViewById(R.id.textView_nome_detalhe)
        val descricaoView: TextView = findViewById(R.id.textView_descricao_detalhe)
        val enderecoView: TextView = findViewById(R.id.textView_endereco_detalhe)

        nomeView.text = tesouro.nome
        descricaoView.text = tesouro.descricao
        enderecoView.text = tesouro.endereco

        Glide.with(this).load(tesouro.imageUrl).into(imageView)

        // ✅ Lógica para atualizar a UI de curtidas
        atualizarUICurtida(tesouro)
    }

    private fun atualizarUICurtida(tesouro: Tesouro) {
        // Atualiza a contagem de curtidas
        contagemCurtidasTextView.text = tesouro.curtidas.size.toString()

        // Verifica se o usuário atual curtiu e atualiza o ícone do coração
        if (tesouro.curtidas.containsKey(usuarioAtualUid)) {
            buttonCurtir.setImageResource(R.drawable.ic_heart_filled) // Coração preenchido
        } else {
            buttonCurtir.setImageResource(R.drawable.ic_heart_outline) // Coração vazio
        }
    }

    private fun alternarCurtida() {
        if (usuarioAtualUid == null) {
            Toast.makeText(this, "Você precisa estar logado para curtir.", Toast.LENGTH_SHORT).show()
            return
        }

        // Referência para o nó de curtidas do tesouro
        val curtidasRef = FirebaseDatabase.getInstance().getReference("tesouros")
            .child(tesouroId).child("curtidas").child(usuarioAtualUid)

        tesouroAtual?.let { tesouro ->
            if (tesouro.curtidas.containsKey(usuarioAtualUid)) {
                // Se já curtiu, remove a curtida (descurtir)
                curtidasRef.removeValue()
            } else {
                // Se não curtiu, adiciona a curtida
                curtidasRef.setValue(true)
            }
        }
    }
}
