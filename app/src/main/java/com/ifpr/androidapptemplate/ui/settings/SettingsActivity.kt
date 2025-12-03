package com.ifpr.androidapptemplate.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.ThemeManager
import com.ifpr.androidapptemplate.baseclasses.Usuario
import com.ifpr.androidapptemplate.databinding.ActivitySettingsBinding
import java.io.ByteArrayOutputStream

class SettingsActivity : AppCompatActivity() {

    private var selectedTheme: ThemeManager.Theme? = null
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var imageUri: Uri? = null
    private var currentPhotoUrl: String? = null

    // Launcher para selecionar a imagem da galeria
    private val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            Glide.with(this).load(it).into(binding.imageViewPerfilSettings)
        }
    }
    private fun showThemeSelectionDialog() {
        val themes = ThemeManager.Theme.values().map { it.name.capitalize() }.toTypedArray()
        val currentTheme = selectedTheme ?: ThemeManager.getSavedTheme(this)
        val checkedItem = currentTheme.ordinal

        AlertDialog.Builder(this)
            .setTitle("Escolha um Tema")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val theme = ThemeManager.Theme.values()[which]
                selectedTheme = theme // Salva a escolha temporariamente
                binding.textViewCurrentTheme.text = theme.name.capitalize() // Atualiza o texto na tela
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplica o tema antes de inflar o layout
        val selectedTheme = ThemeManager.getSavedTheme(this)
        setTheme(ThemeManager.getThemeStyle(selectedTheme))

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuração da ActionBar
        supportActionBar?.title = "Configurações do Perfil"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicialização do Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Carrega os dados atuais do usuário
        loadUserSettings()

        // Configura os cliques dos botões
        binding.cardFotoPerfilSettings.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.buttonChangePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.buttonSaveSettings.setOnClickListener { saveAllSettings() }
        binding.layoutThemeSelector.setOnClickListener { showThemeSelectionDialog() }
    }

    private fun loadUserSettings() {
        val userId = auth.currentUser?.uid ?: return
                binding.progressBarSettings.isVisible = true

        database.child("usuarios").child(userId).get().addOnSuccessListener { dataSnapshot ->
                val usuario = dataSnapshot.getValue(Usuario::class.java)
            usuario?.let {
                // Carrega nome e foto
                binding.editTextNome.setText(it.nome)
                currentPhotoUrl = it.fotoUrl
                if (!currentPhotoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                            .load(currentPhotoUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(binding.imageViewPerfilSettings)
                }
            }
            binding.progressBarSettings.isVisible = false
        }.addOnFailureListener {
            binding.progressBarSettings.isVisible = false
            Toast.makeText(this, "Falha ao carregar dados.", Toast.LENGTH_SHORT).show()
        }

        // Carrega o tema salvo
        val currentTheme = ThemeManager.getSavedTheme(this)
        selectedTheme = currentTheme
        binding.textViewCurrentTheme.text = currentTheme.name.capitalize()


    }

    private fun saveAllSettings() {
        binding.progressBarSettings.isVisible = true
        val newName = binding.editTextNome.text.toString().trim()
        val userId = auth.currentUser?.uid ?: return

        if (newName.isEmpty()) {
            binding.textInputLayoutNome.error = "O nome não pode ser vazio"
            binding.progressBarSettings.isVisible = false
            return
        } else {
            binding.textInputLayoutNome.error = null
        }

        // 1. Salva o novo nome
        database.child("usuarios").child(userId).child("nome").setValue(newName)
                .addOnFailureListener {
            Log.e("SettingsActivity", "Falha ao salvar nome", it)
        }

        // 2. Salva a nova foto (se uma nova foi escolhida)
        if (imageUri != null) {
            val imageBase64 = uriToBase64(imageUri!!)
            if (imageBase64 != null) {
                database.child("usuarios").child(userId).child("fotoUrl").setValue(imageBase64)
            }
        }

        // 3. Salva o tema
        val themeToSave = selectedTheme ?: ThemeManager.getSavedTheme(this)
        ThemeManager.saveTheme(this, themeToSave)
        ThemeManager.updateIcon(this, themeToSave)

        binding.progressBarSettings.isVisible = false
        Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()

        // Recria a activity para aplicar o tema e notifica que o resultado foi OK
        setResult(RESULT_OK)
        recreate()
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Erro ao converter URI para Base64", e)
            null
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
