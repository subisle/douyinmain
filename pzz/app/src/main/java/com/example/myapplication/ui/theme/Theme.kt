package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 自定义颜色数据类
data class CustomColors(
    val surface: Color,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val divider: Color,
    val gradeS: Color,
    val gradeA: Color,
    val gradeB: Color,
    val gradeC: Color,
    val gradeD: Color
)

// 浅色模式自定义颜色
private val LightCustomColors = CustomColors(
    surface = Surface,
    background = Background,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    border = Border,
    divider = Divider,
    gradeS = GradeS,
    gradeA = GradeA,
    gradeB = GradeB,
    gradeC = GradeC,
    gradeD = GradeD
)

// 深色模式自定义颜色
private val DarkCustomColors = CustomColors(
    surface = SurfaceDark,
    background = BackgroundDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    border = BorderDark,
    divider = DividerDark,
    gradeS = GradeSLight,
    gradeA = GradeADark,
    gradeB = GradeBDark,
    gradeC = GradeCDark,
    gradeD = GradeDDark
)

// CompositionLocal for custom colors
val LocalCustomColors = staticCompositionLocalOf { LightCustomColors }

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    tertiary = PrimaryLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    tertiary = PrimaryLight,
    background = Background,
    surface = Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// 扩展属性，方便访问自定义颜色
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = LocalCustomColors.current