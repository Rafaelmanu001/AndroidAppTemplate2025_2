package com.ifpr.androidapptemplate.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.baseclasses.Tesouro
import com.ifpr.androidapptemplate.databinding.FragmentDashboardBinding
import java.io.ByteArrayOutputStream
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Variáveis de Localização
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var cidade: String = ""

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                // Se a permissão foi negada e não devemos mais pedir, guie para as configurações.
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(context, "Permissão de localização é necessária.", Toast.LENGTH_LONG).show()
                }
            }
        }


    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("tesouros")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.buttonSelectImage.setOnClickListener { openFileChooser() }
        binding.buttonGetLocation.setOnClickListener { checkLocationPermission() }
        binding.salvarItemButton.setOnClickListener { salvarTesouro() }

        return binding.root
    }

    private fun checkLocationPermission() {
        when {
            // 1. Permissão já foi concedida?
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }

            // 2. ✅ Devemos mostrar uma explicação? (O usuário já negou uma vez)
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionRationaleDialog()
            }

            // 3. Permissão ainda não foi pedida ou foi negada permanentemente
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissão Necessária")
            .setMessage("Para adicionar um tesouro, precisamos da sua localização para saber onde ele está no mundo. Por favor, conceda a permissão.")
            .setPositiveButton("Ok, Entendi") { _, _ ->
                // Pede a permissão novamente
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissão Negada Permanentemente")
            .setMessage("A permissão de localização foi negada permanentemente. Para usar esta funcionalidade, você precisa habilitá-la manualmente nas configurações do aplicativo.")
            .setPositiveButton("Ir para Configurações") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        binding.progressBar.visibility = View.VISIBLE
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                binding.progressBar.visibility = View.GONE
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    cidade = getCityName(latitude, longitude)
                    binding.textViewLocation.text = "Localização obtida: $cidade"
                } else {
                    Toast.makeText(context, "Não foi possível obter a localização. Verifique se o GPS está ativado e tente novamente.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Erro ao obter localização: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].locality ?: addresses[0].subAdminArea ?: "Local Desconhecido"
            } else {
                "Cidade não encontrada"
            }
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Erro no Geocoder", e)
            "Erro ao obter cidade"
        }
    }

    private fun salvarTesouro() {
        val nome = binding.nomeItemEditText.text.toString().trim()
        val descricao = binding.descricaoItemEditText.text.toString().trim()

        if (nome.isEmpty() || descricao.isEmpty() || imageUri == null || cidade.isEmpty()) {
            Toast.makeText(context, "Preencha todos os campos, selecione uma imagem e obtenha a localização.", Toast.LENGTH_LONG).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.salvarItemButton.isEnabled = false

        val imagemEmBase64 = uriToBase64(imageUri!!)
        if (imagemEmBase64 == null) {
            Toast.makeText(context, "Falha ao processar a imagem.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.salvarItemButton.isEnabled = true
            return
        }

        criarTesouroNoDatabase(nome, descricao, imagemEmBase64, latitude, longitude, cidade)
    }

    private fun criarTesouroNoDatabase(nome: String, descricao: String, imagemBase64: String, lat: Double, lon: Double, novaCidade: String) {
        val tesouroId = databaseReference.push().key ?: ""
        val currentUser = auth.currentUser

        val tesouro = Tesouro(
            id = tesouroId,
            nome = nome,
            descricao = descricao,
            imageUrl = imagemBase64,
            latitude = lat,
            longitude = lon,
            endereco = novaCidade,
            criadoPorUid = currentUser?.uid ?: "",
            criadoPorNome = currentUser?.displayName ?: "Anônimo",
            timestamp = Date().time
        )

        databaseReference.child(tesouroId).setValue(tesouro)
            .addOnSuccessListener {
                Toast.makeText(context, "Tesouro cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                limparCampos()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Falha ao cadastrar o tesouro: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                binding.salvarItemButton.isEnabled = true
            }
    }

    private fun limparCampos() {
        binding.nomeItemEditText.setText("")
        binding.descricaoItemEditText.setText("")
        binding.textViewLocation.text = "Nenhuma localização selecionada"
        binding.imageItem.setImageResource(android.R.drawable.ic_menu_gallery)
        imageUri = null
        cidade = ""
        latitude = 0.0
        longitude = 0.0
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                Glide.with(this).load(imageUri).into(binding.imageItem)
            }
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
