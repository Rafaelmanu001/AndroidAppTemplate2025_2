package com.ifpr.androidapptemplate.ui.usuario


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.ThemeManager
import com.ifpr.androidapptemplate.baseclasses.TesouroRepository
import com.ifpr.androidapptemplate.baseclasses.Usuario
import com.ifpr.androidapptemplate.databinding.FragmentPerfilUsuarioBinding
import com.ifpr.androidapptemplate.ui.adapter.ListItem
import com.ifpr.androidapptemplate.ui.adapter.TesouroAdapter
import com.ifpr.androidapptemplate.ui.details.DetalheTesouroActivity
import java.io.ByteArrayOutputStream

class PerfilUsuarioFragment : Fragment() {

    private var imageUri: Uri? = null
    private var _binding: FragmentPerfilUsuarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var tesouroAdapter: TesouroAdapter
    private lateinit var tesouroRepository: TesouroRepository

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                // Converte e salva a imagem imediatamente após a seleção
                updateProfileImageWithBase64()
            }
        }
    private val listItems = mutableListOf<ListItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Se o usuário salvou as configurações (RESULT_OK), recarregamos os dados
            if (result.resultCode == Activity.RESULT_OK) {
                carregarDadosUsuario()
                activity?.recreate() // Recria a MainActivity para o tema ser aplicado em todo o app
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        tesouroRepository = TesouroRepository()

        setupRecyclerView()
        carregarDadosUsuario()
        buscarMeusTesouros()


        binding.buttonSettings.setOnClickListener {


            // Inicia a nova SettingsActivity

            val intent = Intent(requireContext(), com.ifpr.androidapptemplate.ui.settings.SettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }


        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), com.ifpr.androidapptemplate.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // ✅ AÇÃO: MODIFIQUE ESTE LISTENER
        binding.cardFotoPerfil.setOnClickListener {
            // Abre o seletor de arquivos de imagem
            pickImageLauncher.launch("image/*")
        }
    }

    // ... dentro da classe PerfilUsuarioFragment

    private fun updateProfileImageWithBase64() {
        val userId = auth.currentUser?.uid
        if (userId == null || imageUri == null) {
            return
        }

        binding.progressBarPerfil.isVisible = true
        Toast.makeText(context, "Atualizando foto...", Toast.LENGTH_SHORT).show()

        val imageBase64 = uriToBase64(imageUri!!)
        if (imageBase64 == null) {
            binding.progressBarPerfil.isVisible = false
            Toast.makeText(context, "Falha ao processar a imagem.", Toast.LENGTH_SHORT).show()
            return
        }

        // Salva a string Base64 no Realtime Database
        database.child("usuarios").child(userId).child("fotoUrl").setValue(imageBase64)
            .addOnSuccessListener {
                binding.progressBarPerfil.isVisible = false
                Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()

                // Carrega a nova imagem (que é a string Base64) na ImageView
                // O Glide já sabe como decodificar Base64
                Glide.with(this@PerfilUsuarioFragment)
                    .load(imageBase64)
                    .placeholder(R.drawable.ic_person)
                    .into(binding.imageViewPerfil)
            }
            .addOnFailureListener { e ->
                binding.progressBarPerfil.isVisible = false
                Toast.makeText(context, "Erro ao salvar a nova foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            // Importe se necessário:
            // import android.graphics.Bitmap
            // import android.graphics.BitmapFactory
            // import android.util.Base64
            // import java.io.ByteArrayOutputStream

            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream =
                ByteArrayOutputStream()
            // Comprime a imagem para reduzir o tamanho da string final
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            // Adiciona o prefixo para que o Glide reconheça como uma imagem Base64
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("PerfilUsuarioFragment", "Erro ao converter URI para Base64", e)
            null
        }
    }

// ... resto do seu código


    private fun setupRecyclerView() {
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

    private fun showThemeSelectionDialog() {
        // CORREÇÃO: O comentário foi movido para a linha de cima.
        // Lista de temas para o usuário escolher.
        val themes = arrayOf("Padrão", "Claro", "Escuro", "Mapa Antigo")

        val currentTheme = ThemeManager.getSavedTheme(requireContext())
        val checkedItem = currentTheme.ordinal // Pega o índice do tema atual (ex: DEFAULT é 0, CLARO é 1, etc)

        AlertDialog.Builder(requireContext())
            .setTitle("Escolha um Tema")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                // Ajusta a lógica 'when' para incluir a nova opção
                val selectedTheme = when (which) {
                    0 -> ThemeManager.Theme.DEFAULT
                    1 -> ThemeManager.Theme.CLARO
                    2 -> ThemeManager.Theme.ESCURO
                    else -> ThemeManager.Theme.MAPA
                }

                // O resto do código permanece igual
                ThemeManager.saveTheme(requireContext(), selectedTheme)
                ThemeManager.updateIcon(requireContext(), selectedTheme)

                // CORREÇÃO: Esta chamada agora funciona corretamente
                dialog.dismiss()
                activity?.recreate() // Reinicia a Activity para aplicar o novo tema
            }
            // CORREÇÃO: Este método agora é reconhecido
            .setNegativeButton("Cancelar", null)
            .show()
    }



    private fun buscarMeusTesouros() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBarPerfil.isVisible = true
        binding.textViewNenhumTesouro.isVisible = false

        tesouroRepository.buscarTesourosDoUsuario(userId) { result ->
            binding.progressBarPerfil.isVisible = false

            result.onSuccess { tesourosList ->
                // Sucesso! Atualiza a lista.
                listItems.clear()
                tesourosList.forEach { tesouro ->
                    listItems.add(ListItem.TesouroItem(tesouro))
                }
                tesouroAdapter.notifyDataSetChanged()
                binding.textViewNenhumTesouro.isVisible = listItems.isEmpty()
            }

            result.onFailure { error ->
                // Falha! Tratar o erro.
                Log.e("PerfilUsuarioFragment", "Erro ao buscar tesouros do usuário: ${error.message}")
                Toast.makeText(context, "Falha ao carregar seus tesouros.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
