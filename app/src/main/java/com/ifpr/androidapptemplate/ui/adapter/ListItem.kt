package com.ifpr.androidapptemplate.ui.adapter

import com.ifpr.androidapptemplate.baseclasses.Tesouro

sealed class ListItem {    data class Header(val cidade: String) : ListItem()
    data class TesouroItem(val tesouro: Tesouro) : ListItem()
}
