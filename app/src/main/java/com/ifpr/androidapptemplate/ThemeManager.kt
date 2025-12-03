package com.ifpr.androidapptemplate

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val KEY_THEME = "selected_theme"

    // Enum para representar os temas
    enum class Theme {
        DEFAULT, // Seu tema padrão
        CLARO,
        ESCURO,
        MAPA
    }

    // Aplica o tema visualmente
    fun applyTheme(theme: Theme) {
        when (theme) {
            // Todos os temas, exceto o Escuro, são baseados no modo "dia" (NO)
            Theme.DEFAULT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.CLARO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.ESCURO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.MAPA -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // Salva a escolha do usuário
    fun saveTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    // Carrega a escolha salva
    fun getSavedTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // CORREÇÃO: O tema padrão agora é DEFAULT
        val themeName = prefs.getString(KEY_THEME, Theme.DEFAULT.name)
        return Theme.valueOf(themeName ?: Theme.DEFAULT.name)
    }

    // Retorna o ID do estilo do tema para aplicar na Activity
    fun getThemeStyle(theme: Theme): Int {
        return when (theme) {
            Theme.DEFAULT -> R.style.Theme_AndroidAppTemplate_Default
            Theme.CLARO -> R.style.Theme_AndroidAppTemplate_Claro
            Theme.ESCURO -> R.style.Theme_AndroidAppTemplate_Escuro
            Theme.MAPA -> R.style.Theme_AndroidAppTemplate_Mapa
        }
    }

    fun updateIcon(context: Context, theme: Theme) {
        val pm = context.packageManager

        // CORREÇÃO: Adicionado o alias para o tema DEFAULT
        val defaultAlias = ComponentName(context, "com.ifpr.androidapptemplate.DefaultAlias")
        val claroAlias = ComponentName(context, "com.ifpr.androidapptemplate.ClaroAlias")
        val escuroAlias = ComponentName(context, "com.ifpr.androidapptemplate.EscuroAlias")
        val mapaAlias = ComponentName(context, "com.ifpr.androidapptemplate.MapaAlias")

        // Desabilita todos primeiro
        pm.setComponentEnabledSetting(defaultAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(claroAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(escuroAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(mapaAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)

        // Habilita apenas o alias do tema selecionado
        when (theme) {
            // CORREÇÃO: Adicionada a lógica para habilitar o ícone do tema DEFAULT
            Theme.DEFAULT -> pm.setComponentEnabledSetting(defaultAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            Theme.CLARO -> pm.setComponentEnabledSetting(claroAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            Theme.ESCURO -> pm.setComponentEnabledSetting(escuroAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            Theme.MAPA -> pm.setComponentEnabledSetting(mapaAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
