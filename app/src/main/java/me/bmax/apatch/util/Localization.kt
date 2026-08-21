package me.bmax.apatch.util

import java.util.Locale

/**
 * 从多语言映射中选择与系统 locale 最匹配的文本。
 *
 * 回退链（参考 Android Locale 解析规则）：
 * 1. 精确完整标签（如 "zh-CN"、"zh-Hans-CN"、"zh_TW"、"mgl"）
 * 2. 精确语言代码（如 "zh"）
 * 3. 同一语言族的变体键（如 "zh-Hans"、"zh_CN"、"zh-rCN"）：
 *    - 优先匹配与系统地区(region)一致的键
 *    - 简体系统(zh-*)优先简体变体键，繁体系统优先繁体变体键
 *    - 最后回退到任意同语言族变体
 * 4. 全部未命中返回 null，由调用方回退到默认文本
 *
 * 解决简体中文变体（zh-CN）环境下，插件只提供 "zh-CN"/"zh-Hans" 等
 * 变体键或只有普通 "zh" 键时，直接显示默认语言（英文）的问题。
 *
 * mgl（魔法少女）本质是简体中文主题变体，但其语言代码不是 "zh"，
 * 精确命中 "mgl" 键后，其余回退一律按 "zh" 参与匹配，避免插件
 * 只提供 "zh"/"zh-CN" 等中文键时在插件页面直接显示英文。
 */
fun pickLocalizedString(map: Map<String, String>, locale: Locale?): String? {
    if (map.isEmpty()) return null
    val loc = locale ?: Locale.getDefault()

    fun valueOf(key: String): String? = map[key]?.takeIf { it.isNotBlank() }

    // 1) 精确完整标签，如 "zh-CN" / "zh-Hans-CN" / "zh_TW"
    valueOf(loc.toLanguageTag())?.let { return it }

    // 2) 精确语言代码，如 "zh"
    valueOf(loc.language)?.let { return it }

    val rawLang = loc.language
    if (rawLang.isEmpty()) return null

    // mgl（魔法少女）本质是中文变体："mgl" 键未命中后按 "zh" 回退，
    // 使插件只提供 "zh"/"zh-CN" 等中文键时也能显示中文而非英文。
    val lang = if (rawLang == "mgl") "zh" else rawLang
    if (lang != rawLang) {
        valueOf(lang)?.let { return it }
    }

    // 3) 同语言族变体键：如 "zh-Hans"、"zh_CN"、"zh-rCN"
    val variants = map.entries.filter { (k, v) ->
        v.isNotBlank() && (k.startsWith("$lang-") || k.startsWith("${lang}_"))
    }
    if (variants.isEmpty()) return null

    // 3a) 与系统地区一致的键优先，如 zh-TW 系统优先 "zh-TW" / "zh_TW"
    val region = loc.country
    if (region.isNotEmpty()) {
        variants.firstOrNull { (k, _) ->
            k.equals("$lang-$region", ignoreCase = true) ||
                k.equals("${lang}_$region", ignoreCase = true)
        }?.let { return it.value }
    }

    // 3b) zh 族简繁偏好：繁体系统优先繁体变体，简体系统优先简体变体
    if (lang == "zh") {
        val isTraditional = region in setOf("TW", "HK", "MO")
        val preferred = if (isTraditional) {
            variants.firstOrNull { (k, _) ->
                k.contains("Hant", ignoreCase = true) ||
                    listOf("TW", "HK", "MO").any { k.contains(it, ignoreCase = true) }
            }
        } else {
            variants.firstOrNull { (k, _) ->
                k.contains("Hans", ignoreCase = true) ||
                    listOf("CN", "SG").any { k.contains(it, ignoreCase = true) }
            }
        }
        preferred?.let { return it.value }
    }

    // 3c) 任意同语言族变体
    return variants.first().value
}
