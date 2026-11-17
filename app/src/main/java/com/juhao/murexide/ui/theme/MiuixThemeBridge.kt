package com.juhao.murexide.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 页面画布（[top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme].background）的项目自定义底色。
 *
 * Miuix 非 Monet 的默认调色板里，浅色的 background 与 surfaceContainer 同为纯白、
 * 深色的两者同为 #242424：页面底色和 Card / 分组行容器糊在一起，看不出层级。
 * 这里显式覆盖页面画布——浅色压成浅灰（白色卡片浮起来），深色压成纯黑。
 *
 * 只改页面底色：surface / surfaceContainer 仍由 Miuix 决定，Card、顶栏、输入框的
 * 容器角色不受影响。要换灰度只改这一个常量。
 */
private val MiuixLightBackground = Color(0xFFF2F2F2)

/** 莫奈调色板模式：关闭 */
const val MIUIX_MONET_OFF = "off"

/** 莫奈调色板模式：以应用主题色为种子 */
const val MIUIX_MONET_THEME = "theme"

/** 莫奈调色板模式：跟随系统壁纸 */
const val MIUIX_MONET_WALLPAPER = "wallpaper"

/** 莫奈模式选项（值 → 显示名），供设置页下拉使用。 */
val MIUIX_MONET_OPTIONS: List<Pair<String, String>> = listOf(
    MIUIX_MONET_THEME to "跟随主题色",
    MIUIX_MONET_WALLPAPER to "跟随系统壁纸",
    MIUIX_MONET_OFF to "关闭（Miuix 默认配色）"
)

/**
 * 把应用的明暗设置映射到 Miuix 的配色模式。
 *
 * 莫奈模式开启时统一走 `Monet*`：只有 Monet 模式 [ThemeController] 才会用 `keyColor`
 * 生成整套配色；关闭时走 `System/Light/Dark`，使用 Miuix 自带的默认调色板。
 *
 * - "system"      跟随系统明暗
 * - "light"       强制浅色
 * - "dark"/"oled" 强制深色（Miuix 暂无纯黑 OLED 配色，按深色处理）
 */
internal fun miuixColorSchemeMode(
    themeMode: String,
    monetEnabled: Boolean
): ColorSchemeMode {
    val light = themeMode == "light"
    val dark = themeMode == "dark" || themeMode == "oled"
    return if (monetEnabled) {
        when {
            light -> ColorSchemeMode.MonetLight
            dark -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
    } else {
        when {
            light -> ColorSchemeMode.Light
            dark -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
    }
}

/**
 * 由应用当前设置构建 Miuix 的 [ThemeController]。
 *
 * @param monetMode [MIUIX_MONET_OFF] / [MIUIX_MONET_THEME] / [MIUIX_MONET_WALLPAPER]
 * @param seedColor 应用当前调色板主色，作为 Monet 种子色
 */
@Composable
internal fun rememberMiuixThemeController(
    themeMode: String,
    monetMode: String,
    seedColor: Color
): ThemeController {
    val monetEnabled = monetMode != MIUIX_MONET_OFF
    val mode = miuixColorSchemeMode(themeMode, monetEnabled)
    // keyColor 仅在 Monet* 模式下生效：null 交给系统壁纸取色
    val keyColor: Color? = when {
        !monetEnabled -> null
        monetMode == MIUIX_MONET_WALLPAPER -> null
        else -> seedColor
    }
    return remember(mode, keyColor) {
        ThemeController(
            colorSchemeMode = mode,
            lightColors = lightColorScheme(background = MiuixLightBackground),
            darkColors = darkColorScheme(background = Color.Black),
            keyColor = keyColor,
        )
    }
}
