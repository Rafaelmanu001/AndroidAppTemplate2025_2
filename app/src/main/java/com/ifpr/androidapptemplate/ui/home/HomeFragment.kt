package com.ifpr.androidapptemplate.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplate.databinding.FragmentHomeBinding // Usa o View Binding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aqui você pode definir os textos de boas-vindas e novidades
        binding.textHomeTitle.text = "Bem-vindo ao Together Notes!"

        val novidades = """
            • Versão 1.0 Lançada!
            • Adicione e explore os tesouros.
            • Veja o perfil de outros exploradores.
            • Novas funcionalidades em breve?
        """.trimIndent()

        binding.textHomeContent.text = novidades
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
