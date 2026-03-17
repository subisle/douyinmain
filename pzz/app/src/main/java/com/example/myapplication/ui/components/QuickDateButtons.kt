package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.Primary

@Composable
fun QuickDateButtons(
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    onDayBeforeClick: () -> Unit,
    onDatePickerClick: () -> Unit,
    selectedDate: String,
    modifier: Modifier = Modifier
) {
    // 计算今天、昨天、前天的日期
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date())
    val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
    val dayBefore = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000))
    
    val isToday = selectedDate == today
    val isYesterday = selectedDate == yesterday
    val isDayBefore = selectedDate == dayBefore
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 今天按钮
        Button(
            onClick = onTodayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isToday) Primary else Color.White,
                contentColor = if (isToday) Color.White else Primary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
            border = if (!isToday) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
        ) {
            Text(
                text = "今天",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 昨天按钮
        Button(
            onClick = onYesterdayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isYesterday) Primary else Color.White,
                contentColor = if (isYesterday) Color.White else Primary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
            border = if (!isYesterday) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
        ) {
            Text(
                text = "昨天",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 前天按钮
        Button(
            onClick = onDayBeforeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDayBefore) Primary else Color.White,
                contentColor = if (isDayBefore) Color.White else Primary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
            border = if (!isDayBefore) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
        ) {
            Text(
                text = "前天",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 选择日期按钮 - 其他日期时高亮
        val isOtherDate = !isToday && !isYesterday && !isDayBefore
        Button(
            onClick = onDatePickerClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOtherDate) Primary else Color.White,
                contentColor = if (isOtherDate) Color.White else Primary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
            border = if (!isOtherDate) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "选择",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
