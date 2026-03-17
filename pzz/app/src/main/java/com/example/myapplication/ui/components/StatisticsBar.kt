package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Statistics
import com.example.myapplication.ui.theme.Primary
import com.example.myapplication.ui.theme.customColors

@Composable
fun StatisticsBar(
    statistics: Statistics,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "总计",
                value = "${statistics.totalCount}条"
            )
            
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.customColors.divider
            )
            
            StatItem(
                label = "有音浪",
                value = "${statistics.activeCount}位"
            )
            
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.customColors.divider
            )
            
            StatItem(
                label = "未开播",
                value = "${statistics.inactiveCount}位"
            )
            
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.customColors.divider
            )
            
            StatItem(
                label = "总音浪",
                value = formatSoundWave(statistics.totalSoundWave)
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.customColors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}
