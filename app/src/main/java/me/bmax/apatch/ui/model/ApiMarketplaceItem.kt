package me.bmax.apatch.ui.model

import java.util.Locale

/**
 * API Marketplace item data model
 * Represents a banner API source from the marketplace
 */
data class ApiMarketplaceItem(
    val name: String,
    val url: String,
    val description: String,
    val descriptionEn: String
) {
    /**
     * Get localized description based on current locale
     */
    fun getLocalizedDescription(): String {
        val language = Locale.getDefault().language
        // mgl（魔法少女）本质是简体中文变体，与 zh 同样使用中文描述
        return if (language == "zh" || language == "mgl") {
            description
        } else {
            descriptionEn
        }
    }
}
