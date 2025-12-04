package com.gmrsvoiage.togethernotes.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gmrsvoiage.togethernotes.databinding.ActivityMapaTesouroBinding

// Não precisamos mais de OnMapReadyCallback
class MapaTesouroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapaTesouroBinding
    private var tesouroLatitude: Double = 0.0
    private var tesouroLongitude: Double = 0.0
    private var tesouroNome: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaTesouroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Recebe os dados do Intent (isso continua igual)
        tesouroLatitude = intent.getDoubleExtra("TESOURO_LATITUDE", 0.0)
        tesouroLongitude = intent.getDoubleExtra("TESOURO_LONGITUDE", 0.0)
        tesouroNome = intent.getStringExtra("TESOURO_NOME") ?: "Tesouro"
        val tesouroEndereco = intent.getStringExtra("TESOURO_ENDERECO") ?: "Endereço desconhecido"

        // 2. Atualiza os textos na tela
        binding.textTesouroNome.text = tesouroNome
        binding.textTesouroEndereco.text = tesouroEndereco

        // Define o título da ActionBar (barra superior)
        supportActionBar?.title = "Localização do Tesouro"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Adiciona um botão de "voltar"

        // 3. ✅ AÇÃO PRINCIPAL: Configura o clique do botão
        binding.buttonVerNoMapa.setOnClickListener {
            abrirMapaExterno()
        }
    }

    private fun abrirMapaExterno() {
        // Cria uma URI de geolocalização com as coordenadas e nome do tesouro
        val gmmIntentUri = Uri.parse("geo:$tesouroLatitude,$tesouroLongitude?q=$tesouroLatitude,$tesouroLongitude($tesouroNome)")

        // Cria um Intent genérico para visualizar essa URI
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        // Verifica se existe algum aplicativo no celular capaz de abrir este Intent
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Se nenhum app de mapa estiver instalado, avisa o usuário
            Toast.makeText(this, "Nenhum aplicativo de mapa encontrado.", Toast.LENGTH_LONG).show()
        }
    }

    // Função para o botão "voltar" na ActionBar funcionar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
