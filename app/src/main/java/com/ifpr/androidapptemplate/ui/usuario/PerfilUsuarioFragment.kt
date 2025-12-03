package com.ifpr.androidapptemplate.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Tesouro
import com.ifpr.androidapptemplate.baseclasses.Usuario
import com.ifpr.androidapptemplate.databinding.FragmentPerfilUsuarioBinding
// ✅ CORREÇÃO 1: Importar as classes necessárias
import com.ifpr.androidapptemplate.ui.adapter.ListItem
import com.ifpr.androidapptemplate.ui.adapter.TesouroAdapter
import com.ifpr.androidapptemplate.ui.details.DetalheTesouroActivity
import com.ifpr.androidapptemplate.ui.login.LoginActivity

class PerfilUsuarioFragment : Fragment() {

    private var _binding: FragmentPerfilUsuarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var tesouroAdapter: TesouroAdapter
    // ✅ CORREÇÃO 2: A lista agora armazena 'ListItem'
    private val listItems = mutableListOf<ListItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupRecyclerView()
        carregarDadosUsuario()
        buscarMeusTesouros()

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun setupRecyclerView() {
        // ✅ CORREÇÃO 3: O adapter agora recebe a 'listItems'
        tesouroAdapter = TesouroAdapter(listItems) { tesouro ->
            val intent = Intent(requireContext(), DetalheTesouroActivity::class.java).apply {
                putExtra("TESOURO_ID", tesouro.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewMeusTesouros.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMeusTesouros.adapter = tesouroAdapter
        binding.recyclerViewMeusTesouros.isNestedScrollingEnabled = false
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("usuarios").child(userId).get().addOnSuccessListener { dataSnapshot ->
                val usuario = dataSnapshot.getValue(Usuario::class.java)
                usuario?.let {
                    binding.textViewNomePerfil.text = it.nome
                    binding.textViewEmailPerfil.text = it.email
                    if (!it.fotoUrl.isNullOrEmpty()) {
                        Glide.with(this@PerfilUsuarioFragment)
                            .load(it.fotoUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(binding.imageViewPerfil)
                    }
                }
            }.addOnFailureListener {
                Log.e("PerfilUsuarioFragment", "Erro ao buscar dados do usuário", it)
            }
        }
    }

    private fun buscarMeusTesouros() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBarPerfil.isVisible = true
        binding.textViewNenhumTesouro.isVisible = false

        val query = database.child("tesouros").orderByChild("criadoPorUid").equalTo(userId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // ✅ CORREÇÃO 4: Limpa a lista correta
                listItems.clear()
                val tempList = mutableListOf<Tesouro>()

                if (snapshot.exists()) {
                    for (tesouroSnapshot in snapshot.children) {
                        tesouroSnapshot.getValue(Tesouro::class.java)?.let {
                            tempList.add(it)
                        }
                    }
                }

                // Ordena a lista de tesouros
                tempList.sortByDescending { it.timestamp }

                // ✅ CORREÇÃO 5: Converte cada Tesouro em um ListItem.TesouroItem
                tempList.forEach { tesouro ->
                    listItems.add(ListItem.TesouroItem(tesouro))
                }

                tesouroAdapter.notifyDataSetChanged()
                binding.progressBarPerfil.isVisible = false
                // Usa a nova lista para verificar se está vazia
                binding.textViewNenhumTesouro.isVisible = listItems.isEmpty()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarPerfil.isVisible = false
                Log.e("PerfilUsuarioFragment", "Erro ao buscar tesouros do usuário: ${error.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
