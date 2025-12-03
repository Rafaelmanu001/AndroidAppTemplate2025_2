package com.ifpr.androidapptemplate.ui.adapter

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Tesouro
import com.ifpr.androidapptemplate.databinding.ItemHeaderCidadeBinding
import com.ifpr.androidapptemplate.databinding.ItemMeuTesouroBinding

private const val TIPO_CABECALHO = 0
private const val TIPO_TESOURO = 1

class TesouroAdapter(
    private val items: List<ListItem>,
    private val onItemClicked: (Tesouro) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ViewHolder para o Cabeçalho da Cidade
    inner class HeaderViewHolder(private val binding: ItemHeaderCidadeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ListItem.Header) {
            binding.textViewHeaderCidade.text = header.cidade
        }
    }

    // ViewHolder para o Item do Tesouro
    inner class TesouroViewHolder(private val binding: ItemMeuTesouroBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ListItem.TesouroItem) {
            val tesouro = item.tesouro
            binding.textViewNomeTesouroItem.text = tesouro.nome
            binding.textViewEnderecoTesouroItem.text = tesouro.endereco

            tesouro.imageUrl?.let { imageUrl ->
                try {
                    val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                    Glide.with(binding.root.context).load(imageBytes)
                        .placeholder(R.drawable.ic_map).into(binding.imageViewTesouroItem)
                } catch (e: Exception) {
                    Glide.with(binding.root.context).load(imageUrl)
                        .placeholder(R.drawable.ic_map).into(binding.imageViewTesouroItem)
                }
            }
            itemView.setOnClickListener { onItemClicked(tesouro) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TIPO_CABECALHO
            is ListItem.TesouroItem -> TIPO_TESOURO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TIPO_CABECALHO -> {
                val binding = ItemHeaderCidadeBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            TIPO_TESOURO -> {
                val binding = ItemMeuTesouroBinding.inflate(inflater, parent, false)
                TesouroViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Tipo de View inválido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.TesouroItem -> (holder as TesouroViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size
}

