package com.example.xuper.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.core.content.edit

enum class AppLanguage(val code: String, val displayName: String) {
    ES(code = "es", displayName = "Español"),
    EN(code = "en", displayName = "English"),
    FR(code = "fr", displayName = "Français"),
    DE(code = "de", displayName = "Deutsch"),
    RU(code = "ru", displayName = "Русский"),
}

object LanguageManager {
    private const val PREF_NAME = "xuper_lang_prefs"
    private const val KEY_LANG = "selected_language"

    private var _currentLanguage = mutableStateOf(AppLanguage.ES)
    val currentLanguage: State<AppLanguage> = _currentLanguage

    // UI Strings Map
    private val translations = mapOf(
        AppLanguage.ES to mapOf(
            "tv" to "TV",
            "favorites" to "Favoritos",
            "lists" to "Listas",
            "xuper" to "Xuper",
            "refresh" to "Actualizar",
            "filters" to "Filtros",
            "language" to "Idioma",
            "select_player" to "¿Deseas reproducir este canal o gestionarlo?",
            "internal_player" to "REPRODUCTOR INTERNO",
            "external_player" to "REPRODUCTOR EXTERNO",
            "add_favorite" to "AÑADIR A FAVORITOS",
            "remove_favorite" to "QUITAR DE FAVORITOS",
            "cancel" to "CANCELAR",
            "search_placeholder" to "Buscar canal...",
            "all_lists" to "Todas las listas",
            "all_categories" to "Todos",
            "select_channel_msg" to "Selecciona un canal para reproducir",
            "error_loading" to "Error al cargar canales",
            "retry" to "REINTENTAR",
        ),
        AppLanguage.EN to mapOf(
            "tv" to "TV",
            "favorites" to "Favorites",
            "lists" to "Lists",
            "xuper" to "Xuper",
            "refresh" to "Refresh",
            "filters" to "Filters",
            "language" to "Language",
            "select_player" to "Do you want to play this channel or manage it?",
            "internal_player" to "INTERNAL PLAYER",
            "external_player" to "EXTERNAL PLAYER",
            "add_favorite" to "ADD TO FAVORITES",
            "remove_favorite" to "REMOVE FROM FAVORITES",
            "cancel" to "CANCEL",
            "search_placeholder" to "Search channel...",
            "all_lists" to "All lists",
            "all_categories" to "All",
            "select_channel_msg" to "Select a channel to play",
            "error_loading" to "Error loading channels",
            "retry" to "RETRY",
        ),
        AppLanguage.FR to mapOf(
            "tv" to "TV",
            "favorites" to "Favoris",
            "lists" to "Listes",
            "xuper" to "Xuper",
            "refresh" to "Actualiser",
            "filters" to "Filtres",
            "language" to "Langue",
            "select_player" to "Voulez-vous lire cette chaîne ou la gérer ?",
            "internal_player" to "LECTEUR INTERNE",
            "external_player" to "LECTEUR EXTERNE",
            "add_favorite" to "AJOUTER AUX FAVORIS",
            "remove_favorite" to "RETIRER DES FAVORIS",
            "cancel" to "ANNULER",
            "search_placeholder" to "Rechercher une chaîne...",
            "all_lists" to "Toutes les listes",
            "all_categories" to "Tous",
            "select_channel_msg" to "Sélectionnez une chaîne à lire",
            "error_loading" to "Erreur lors du chargement des chaînes",
            "retry" to "RÉESSAYER",
        ),
        AppLanguage.DE to mapOf(
            "tv" to "TV",
            "favorites" to "Favoriten",
            "lists" to "Listen",
            "xuper" to "Xuper",
            "refresh" to "Aktualisieren",
            "filters" to "Filter",
            "language" to "Sprache",
            "select_player" to "Möchten Sie diesen Kanal abspielen oder verwalten?",
            "internal_player" to "INTERNER PLAYER",
            "external_player" to "EXTERNER PLAYER",
            "add_favorite" to "ZU FAVORITEN HINZUFÜGEN",
            "remove_favorite" to "AUS FAVORITEN ENTFERNEN",
            "cancel" to "ABBRECHEN",
            "search_placeholder" to "Kanal suchen...",
            "all_lists" to "Alle Listen",
            "all_categories" to "Alle",
            "select_channel_msg" to "Wählen Sie einen Kanal zum Abspielen aus",
            "error_loading" to "Fehler beim Laden der Kanäle",
            "retry" to "WIEDERHOLEN",
        ),
        AppLanguage.RU to mapOf(
            "tv" to "ТВ",
            "favorites" to "Избранное",
            "lists" to "Списки",
            "xuper" to "Xuper",
            "refresh" to "Обновить",
            "filters" to "Фильтры",
            "language" to "Язык",
            "select_player" to "Вы хотите воспроизвести этот канал или управлять им?",
            "internal_player" to "ВНУТРЕННИЙ ПЛЕЕР",
            "external_player" to "ВНЕШНИЙ ПЛЕЕР",
            "add_favorite" to "ДОБАВИТЬ В ИЗБРАННОЕ",
            "remove_favorite" to "УДАЛИТЬ ИЗ ИЗБРАННОГО",
            "cancel" to "ОТМЕНА",
            "search_placeholder" to "Поиск канала...",
            "all_lists" to "Все списки",
            "all_categories" to "Все",
            "select_channel_msg" to "Выберите канал для воспроизведения",
            "error_loading" to "Ошибка загрузки каналов",
            "retry" to "ПОВТОРИТЬ",
        ),
    )

    fun getString(key: String): String {
        return translations[_currentLanguage.value]?.get(key) ?: key
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString(KEY_LANG, AppLanguage.ES.code)
        _currentLanguage.value = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ES
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANG, language.code)
        }
    }
}

@Composable
fun stringResourceAI(key: String): String {
    return LanguageManager.getString(key)
}
