package me.bmax.apatch.ui.screen.settings

import androidx.annotation.StringRes
import android.content.res.Resources
import me.bmax.apatch.R

enum class SettingCategory(
    val key: String,
    @StringRes val labelResId: Int,
) {
    GENERAL("general", R.string.settings_category_general),
    APPEARANCE("appearance", R.string.settings_category_appearance),
    BEHAVIOR("behavior", R.string.settings_category_behavior),
    SECURITY("security", R.string.settings_category_security),
    BACKUP("backup", R.string.settings_category_backup),
    MODULE("module", R.string.settings_category_module),
    MULTIMEDIA("multimedia", R.string.settings_category_multimedia),
    FUNCTION("function", R.string.settings_category_function),
}

data class SettingEntry(
    val key: String,
    @StringRes val titleResId: Int,
    @StringRes val summaryResId: Int? = null,
    val category: SettingCategory,
)

/** Pre-resolved searchable text for fast filtering without repeated resource lookups. */
data class ResolvedEntry(
    val entry: SettingEntry,
    val title: String,
    val summary: String,
    val categoryName: String,
)

object SettingsRegistry {
    val allSettings: List<SettingEntry> by lazy {
        buildList {
            // === General ===
            add(SettingEntry("general_language", R.string.settings_app_language, category = SettingCategory.GENERAL))
            add(SettingEntry("general_check_update", R.string.settings_check_update, category = SettingCategory.GENERAL))
            add(SettingEntry("general_auto_update", R.string.settings_auto_update_check, R.string.settings_auto_update_check_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_block_kp_update", R.string.settings_block_kernelpatch_update, R.string.settings_block_kernelpatch_update_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_block_ap_update", R.string.settings_block_androidpatch_update, R.string.settings_block_androidpatch_update_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_folkx_engine", R.string.settings_folkx_engine_title, R.string.settings_folkx_engine_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_folkx_animation_type", R.string.settings_folkx_animation_type, category = SettingCategory.GENERAL))
            add(SettingEntry("general_folkx_animation_speed", R.string.settings_folkx_animation_speed, category = SettingCategory.GENERAL))
            add(SettingEntry("general_predictive_back", R.string.settings_predictive_back, R.string.settings_predictive_back_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_new_app_profile", R.string.settings_new_app_profile_mode, category = SettingCategory.GENERAL))
            add(SettingEntry("general_app_list_scheme", R.string.settings_app_list_loading_scheme, category = SettingCategory.GENERAL))
            add(SettingEntry("general_selinux_mode", R.string.settings_selinux_mode, R.string.settings_selinux_mode_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_global_namespace", R.string.settings_global_namespace_mode, R.string.settings_global_namespace_mode_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_magic_mount", R.string.settings_magic_mount, R.string.settings_magic_mount_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_alt_icon", R.string.settings_alt_icon, R.string.alt_icon_summary, SettingCategory.GENERAL))
            add(SettingEntry("general_reset_su_path", R.string.setting_reset_su_path, category = SettingCategory.GENERAL))
            add(SettingEntry("general_app_title", R.string.settings_app_title, category = SettingCategory.GENERAL))
            add(SettingEntry("general_custom_app_title", R.string.settings_custom_app_title, category = SettingCategory.GENERAL))
            add(SettingEntry("general_desktop_app_name", R.string.desktop_app_name, category = SettingCategory.GENERAL))
            add(SettingEntry("general_dpi", R.string.settings_app_dpi, category = SettingCategory.GENERAL))
            add(SettingEntry("general_send_log", R.string.send_log, category = SettingCategory.GENERAL))
            add(SettingEntry("general_clean_storage", R.string.settings_clean_storage, R.string.settings_clean_storage_summary, SettingCategory.GENERAL))

            // === Appearance ===
            add(SettingEntry("appearance_amoled_theme", R.string.settings_amoled_theme, R.string.settings_amoled_theme_desc, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_switch_icon", R.string.settings_switch_icon, R.string.settings_switch_icon_desc, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_discrete_slider", R.string.settings_discrete_slider, R.string.settings_discrete_slider_desc, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_home_layout", R.string.settings_home_layout_style, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_stats_top_layout", R.string.settings_stats_top_layout, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_nav_layout", R.string.settings_nav_layout_title, R.string.settings_nav_layout_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_nav_scheme", R.string.settings_nav_scheme, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass", R.string.settings_navbar_glass_effect, R.string.settings_navbar_glass_effect_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_blur", R.string.settings_navbar_glass_blur_strength, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_transparency", R.string.settings_navbar_glass_transparency, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_highlight", R.string.settings_navbar_glass_highlight_strength, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_specular", R.string.settings_navbar_glass_specular, R.string.settings_navbar_glass_specular_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_glow", R.string.settings_navbar_glass_inner_glow, R.string.settings_navbar_glass_inner_glow_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_navbar_glass_border", R.string.settings_navbar_glass_border, R.string.settings_navbar_glass_border_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_floating_auto_hide", R.string.settings_floating_auto_hide, R.string.settings_floating_auto_hide_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_floating_swipe_hide", R.string.settings_floating_swipe_hide, R.string.settings_floating_swipe_hide_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_list_card_badge", R.string.settings_list_card_hide_status_badge, R.string.settings_list_card_hide_status_badge_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_custom_badge_text", R.string.settings_custom_badge_text, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_list_info_icons", R.string.settings_list_info_show_icons, R.string.settings_list_info_show_icons_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_advanced_title", R.string.settings_advanced_title_style, R.string.settings_advanced_title_style_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_custom_background", R.string.settings_custom_background, R.string.settings_custom_background_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_video_background", R.string.settings_video_background, R.string.settings_video_background_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_multi_background", R.string.settings_multi_background_mode, R.string.settings_multi_background_mode_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_grid_card_bg", R.string.settings_grid_working_card_background, R.string.settings_grid_working_card_background_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_grid_card_check", R.string.settings_grid_working_card_hide_check, R.string.settings_grid_working_card_hide_check_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_grid_card_text", R.string.settings_grid_working_card_hide_text, R.string.settings_grid_working_card_hide_text_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_grid_card_mode", R.string.settings_grid_working_card_hide_mode, R.string.settings_grid_working_card_hide_mode_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_banner", R.string.apm_enable_module_banner, R.string.apm_enable_module_banner_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_folk_banner", R.string.apm_enable_folk_banner, R.string.apm_enable_folk_banner_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_banner_api_mode", R.string.apm_banner_api_mode, R.string.apm_banner_api_mode_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_banner_opacity", R.string.settings_banner_custom_opacity, R.string.settings_banner_custom_opacity_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_custom_font", R.string.settings_custom_font, R.string.settings_custom_font_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_theme_store", R.string.theme_store_title, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_theme_mode", R.string.settings_theme_mode, R.string.settings_theme_mode_summary, SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_save_theme", R.string.settings_save_theme, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_import_theme", R.string.settings_import_theme, category = SettingCategory.APPEARANCE))
            add(SettingEntry("appearance_reset_theme", R.string.settings_reset_theme, category = SettingCategory.APPEARANCE))

            // === Behavior ===
            add(SettingEntry("behavior_web_debugging", R.string.enable_web_debugging, R.string.enable_web_debugging_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_info_copy", R.string.settings_info_copy, R.string.settings_info_copy_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_install_confirm", R.string.settings_apm_install_confirm, R.string.settings_apm_install_confirm_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_module_shortcut", R.string.settings_enable_module_shortcut_add, R.string.settings_enable_module_shortcut_add_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_stay_on_page", R.string.settings_apm_stay_on_page, R.string.settings_apm_stay_on_page_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_apatch_card", R.string.settings_hide_apatch_card, R.string.settings_hide_apatch_card_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_su_path", R.string.home_hide_su_path, R.string.home_hide_su_path_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_kpatch_version", R.string.home_hide_kpatch_version, R.string.home_hide_kpatch_version_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_fingerprint", R.string.home_hide_fingerprint, R.string.home_hide_fingerprint_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_zygisk", R.string.home_hide_zygisk, R.string.home_hide_zygisk_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_hide_mount", R.string.home_hide_mount, R.string.home_hide_mount_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_legacy_su_page", R.string.settings_use_legacy_su_page, R.string.settings_use_legacy_su_page_summary, SettingCategory.BEHAVIOR))
            add(SettingEntry("behavior_badge_count", R.string.enable_badge_count, R.string.enable_badge_count_summary, SettingCategory.BEHAVIOR))

            // === Security ===
            add(SettingEntry("security_biometric_login", R.string.settings_biometric_login, R.string.settings_biometric_login_summary, SettingCategory.SECURITY))
            add(SettingEntry("security_strong_biometric", R.string.settings_strong_biometric, R.string.settings_strong_biometric_summary, SettingCategory.SECURITY))

            // === Backup ===
            add(SettingEntry("backup_local", R.string.settings_enable_local_backup, R.string.settings_enable_local_backup_summary, SettingCategory.BACKUP))
            add(SettingEntry("backup_boot", R.string.settings_auto_backup_boot, R.string.settings_auto_backup_boot_summary, SettingCategory.BACKUP))
            add(SettingEntry("backup_open_dir", R.string.settings_open_backup_dir, category = SettingCategory.BACKUP))
            add(SettingEntry("backup_cloud", R.string.settings_enable_cloud_backup, R.string.settings_enable_cloud_backup_summary, SettingCategory.BACKUP))
            add(SettingEntry("backup_webdav", R.string.settings_configure_webdav, category = SettingCategory.BACKUP))

            // === Module ===
            add(SettingEntry("module_disable_update", R.string.settings_disable_module_update_check, R.string.settings_disable_module_update_check_summary, SettingCategory.MODULE))
            add(SettingEntry("module_more_info", R.string.settings_show_more_module_info, R.string.settings_show_more_module_info_summary, SettingCategory.MODULE))
            add(SettingEntry("module_sort_opt", R.string.settings_module_sort_optimization, R.string.settings_module_sort_optimization_summary, SettingCategory.MODULE))
            add(SettingEntry("module_fold_system", R.string.settings_fold_system_module, R.string.settings_fold_system_module_summary, SettingCategory.MODULE))
            add(SettingEntry("module_batch_install", R.string.apm_batch_install_full_process, R.string.apm_batch_install_full_process_summary, SettingCategory.MODULE))
            add(SettingEntry("module_simple_list", R.string.settings_simple_list_bottom_bar, R.string.settings_simple_list_bottom_bar_summary, SettingCategory.MODULE))
            add(SettingEntry("module_spliced_card", R.string.settings_spliced_card_group, R.string.settings_spliced_card_group_summary, SettingCategory.MODULE))
            add(SettingEntry("module_kpm_status_badge", R.string.settings_show_kpm_status_badge, R.string.settings_show_kpm_status_badge_summary, SettingCategory.MODULE))

            // === Multimedia ===
            add(SettingEntry("multimedia_bg_music", R.string.settings_background_music, R.string.settings_background_music_summary, SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_music_auto_play", R.string.settings_music_auto_play, R.string.settings_music_auto_play_summary, SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_music_looping", R.string.settings_music_looping, R.string.settings_music_looping_summary, SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_music_volume", R.string.settings_music_volume, category = SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_sound_effect", R.string.settings_sound_effect, R.string.settings_sound_effect_summary, SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_startup_sound", R.string.settings_startup_sound, R.string.settings_startup_sound_summary, SettingCategory.MULTIMEDIA))
            add(SettingEntry("multimedia_vibration", R.string.settings_vibration, R.string.settings_vibration_summary, SettingCategory.MULTIMEDIA))

            // === Function ===
            add(SettingEntry("function_hide_service", R.string.settings_hide_service, R.string.settings_hide_service_summary, SettingCategory.FUNCTION))
            add(SettingEntry("function_umount", R.string.settings_umount_service, R.string.settings_umount_service_summary, SettingCategory.FUNCTION))
            add(SettingEntry("function_kernel_spoof", R.string.settings_kernel_spoof, R.string.settings_kernel_spoof_summary, SettingCategory.FUNCTION))
            add(SettingEntry("function_path_hide", R.string.settings_path_hide, R.string.settings_path_hide_summary, SettingCategory.FUNCTION))
            add(SettingEntry("function_net_isolate", R.string.netisolate_title, R.string.netisolate_enable_summary, SettingCategory.FUNCTION))
        }
    }

    /** Resolve all strings once for fast search filtering. Called once per screen composition. */
    fun resolveAll(resources: Resources): List<ResolvedEntry> {
        return allSettings.map { entry ->
            ResolvedEntry(
                entry = entry,
                title = safeGetString(resources, entry.titleResId),
                summary = entry.summaryResId?.let { safeGetString(resources, it) } ?: "",
                categoryName = safeGetString(resources, entry.category.labelResId),
            )
        }
    }

    private fun safeGetString(res: Resources, id: Int): String {
        return try { res.getString(id) } catch (_: Exception) { "" }
    }
}
