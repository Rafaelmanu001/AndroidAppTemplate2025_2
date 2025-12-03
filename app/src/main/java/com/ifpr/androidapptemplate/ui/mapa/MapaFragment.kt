package com.ifpr.androidapptemplate.ui.mapa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.baseclasses.Tesouro
import com.ifpr.androidapptemplate.databinding.FragmentMapaBinding
import com.ifpr.androidapptemplate.ui.adapter.ListItem
import com.ifpr.androidapptemplate.ui.adapter.TesouroAdapter
import com.ifpr.androidapptemplate.ui.details.DetalheTesouroActivity

class MapaFragment : Fragment() {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private lateinit var tesouroAdapter: TesouroAdapter
    private val listItems = mutableListOf<ListItem>()

    private val databaseReference = FirebaseDatabase.getInstance().getReference("tesouros")
        .orderByChild("timestamp")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        buscarTodosOsTesouros()
    }

    private fun setupRecyclerView() {
        tesouroAdapter = TesouroAdapter(listItems) { tesouro ->
            val intent = Intent(requireContext(), DetalheTesouroActivity::class.java).apply {
                putExtra("TESOURO_ID", tesouro.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewExplorarTesouros.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewExplorarTesouros.adapter = tesouroAdapter
    }

    private fun buscarTodosOsTesouros() {
        binding.progressBarExplorar.isVisible = true
        binding.textViewNenhumTesouroExplorar.isVisible = false

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tesourosList = mutableListOf<Tesouro>()
                if (snapshot.exists()) {
                    for (tesouroSnapshot in snapshot.children) {
                        tesouroSnapshot.getValue(Tesouro::class.java)?.let { tesouro ->
                            tesourosList.add(tesouro)
                        }
                    }
                }

                // Processa a lista para agrupar por cidade
                processarListaAgrupada(tesourosList)

                tesouroAdapter.notifyDataSetChanged()
                binding.progressBarExplorar.isVisible = false
                binding.textViewNenhumTesouroExplorar.isVisible = listItems.isEmpty()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarExplorar.isVisible = false
                Log.e("FragmentMapa", "Erro ao buscar tesouros: ${error.message}")
            }
        })
    }

    private fun processarListaAgrupada(tesouros: List<Tesouro>) {
        listItems.clear()

        // Agrupa os tesouros por cidade e depois ordena por timestamp dentro de cada cidade
        val tesourosAgrupados = tesouros
            .sortedByDescending { it.timestamp }
            .groupBy { it.endereco } // 'endereco' agora contém a cidade

        // Adiciona os itens agrupados à lista final que será exibida
        tesourosAgrupados.forEach { (cidade, tesourosDaCidade) ->
            listItems.add(ListItem.Header(cidade)) // Adiciona o cabeçalho da cidade
            tesourosDaCidade.forEach { tesouro ->
                listItems.add(ListItem.TesouroItem(tesouro)) // Adiciona cada tesouro
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
