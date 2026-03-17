package com.example.myapplication.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

enum class Gender {
    All,
    Male,
    Female
}

/**
 * 性别筛选器组件
 * 基于 app重构界面.html 的设计
 */
@Composable
fun GenderFilter(
    selectedGender: Gender,
    onGenderChange: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.IconSpacing)
    ) {
        GenderChip(
            text = "全部",
            isSelected = selectedGender == Gender.All,
            onClick = { onGenderChange(Gender.All) }
        )
        GenderChip(
            text = "男",
            isSelected = selectedGender == Gender.Male,
            onClick = { onGenderChange(Gender.Male) }
        )
        GenderChip(
            text = "女",
            isSelected = selectedGender == Gender.Female,
            onClick = { onGenderChange(Gender.Female) }
        )
    }
}

@Composable
fun GenderChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Secondary else Color.White,
        label = "backgroundColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF6B7280),
        label = "textColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Secondary else Color(0xFFE5E7EB),
        label = "borderColor"
    )
    
    Surface(
        onClick = onClick,
        shape = AppShapes.ChipRadius,
        color = backgroundColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
