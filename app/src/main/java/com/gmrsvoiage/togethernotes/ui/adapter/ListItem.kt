package com.gmrsvoiage.togethernotes.ui.adapter

import com.gmrsvoiage.togethernotes.baseclasses.Tesouro

sealed class ListItem {    data class Header(val cidade: String) : ListItem()
    data class TesouroItem(val tesouro: Tesouro) : ListItem()
}
