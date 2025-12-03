package com.ifpr.androidapptemplate.ui.mapa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ifpr.androidapptemplate.baseclasses.Tesouro
import com.ifpr.androidapptemplate.baseclasses.TesouroRepository
import com.ifpr.androidapptemplate.databinding.FragmentMapaBinding
import com.ifpr.androidapptemplate.ui.adapter.ListItem
import com.ifpr.androidapptemplate.ui.adapter.TesouroAdapter
import com.ifpr.androidapptemplate.ui.details.MapaTesouroActivity

class MapaFragment : Fragment() {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    // Variáveis do Repositório e do Adapter
    private lateinit var tesouroRepository: TesouroRepository
    private lateinit var tesouroAdapter: TesouroAdapter
    private val listItems = mutableListOf<ListItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Apenas UMA declaração de onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o repositório aqui
        tesouroRepository = TesouroRepository()

        setupRecyclerView()
        buscarTodosOsTesouros()
    }

    private fun setupRecyclerView() {
        tesouroAdapter = TesouroAdapter(listItems) { tesouro ->
            val intent = Intent(requireContext(), MapaTesouroActivity::class.java).apply {
                putExtra("TESOURO_ID", tesouro.id)
                putExtra("TESOURO_NOME", tesouro.nome)
                putExtra("TESOURO_LATITUDE", tesouro.latitude)
                putExtra("TESOURO_LONGITUDE", tesouro.longitude)
                putExtra("TESOURO_ENDERECO", tesouro.endereco)
            }
            startActivity(intent)
        }
        binding.recyclerViewExplorarTesouros.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewExplorarTesouros.adapter = tesouroAdapter
    }

    private fun buscarTodosOsTesouros() {
        binding.progressBarExplorar.isVisible = true
        binding.textViewNenhumTesouroExplorar.isVisible = false

        tesouroRepository.buscarTodosOsTesouros { result ->
            // Este bloco é executado quando o repositório retorna os dados
            binding.progressBarExplorar.isVisible = false

            result.onSuccess { tesourosList ->
                // Sucesso! A lista de tesouros foi recebida.
                processarListaAgrupada(tesourosList)
                tesouroAdapter.notifyDataSetChanged()
                binding.textViewNenhumTesouroExplorar.isVisible = listItems.isEmpty()
            }

            result.onFailure { error ->
                // Falha! Tratar o erro.
                Log.e("MapaFragment", "Erro ao buscar tesouros: ${error.message}")
                Toast.makeText(context, "Falha ao carregar os tesouros.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processarListaAgrupada(tesouros: List<Tesouro>) {
        listItems.clear()

        val tesourosAgrupados = tesouros
            // .sortedByDescending { it.timestamp } // O repositório já faz isso
            .groupBy { it.endereco }

        tesourosAgrupados.forEach { (cidade, tesourosDaCidade) ->
            listItems.add(ListItem.Header(cidade))
            tesourosDaCidade.forEach { tesouro ->
                listItems.add(ListItem.TesouroItem(tesouro))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
