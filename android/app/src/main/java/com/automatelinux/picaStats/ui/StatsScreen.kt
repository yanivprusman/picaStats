package com.automatelinux.picaStats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.automatelinux.picaStats.data.DayPoint
import com.automatelinux.picaStats.data.StatsResponse
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    isProd: Boolean,
    onReportIssue: () -> Unit,
    vm: StatsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("picawish", fontWeight = FontWeight.Bold)
                        Text(
                            "visitor analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            if (!isProd) {
                FloatingActionButton(
                    onClick = onReportIssue,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = "Report Issue",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
        ) {
            when (val s = state) {
                is StatsViewModel.UiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is StatsViewModel.UiState.Error -> ErrorView(s.message) { vm.refresh() }

                is StatsViewModel.UiState.Success -> Dashboard(s.data, refreshing)
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Couldn't load stats", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun Dashboard(data: StatsResponse, refreshing: Boolean) {
    val s = data.summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        HeroCard(today = s.today, yesterday = s.yesterday, site = data.site)

        // Stat tiles
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Last 7 days", s.last7d.toString(), Modifier.weight(1f))
            StatTile("Last 30 days", s.last30d.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("All-time", s.allTime.toString(), Modifier.weight(1f))
            StatTile(
                "Cards made",
                data.wishes?.toString() ?: "—",
                Modifier.weight(1f),
            )
        }

        SectionCard("Last 14 days") {
            BarChart(data.byDay.takeLast(14))
        }

        if (data.topReferrers.isNotEmpty()) {
            SectionCard("Top referrers") {
                data.topReferrers.take(6).forEach {
                    LabeledRow(prettyReferrer(it.referer), it.visits.toString())
                }
            }
        }

        if (data.countries.isNotEmpty()) {
            SectionCard("Countries") {
                data.countries.take(6).forEach {
                    LabeledRow(
                        if (it.name.isNotBlank()) it.name else it.code,
                        it.visits.toString(),
                    )
                }
            }
        }

        if (data.allSites.isNotEmpty()) {
            SectionCard("All tracked sites (7d)") {
                data.allSites.take(8).forEach {
                    LabeledRow(it.site, it.visits.toString())
                }
            }
        }

        Text(
            "Updated ${formatTime(data.generatedAt)}${if (refreshing) " · refreshing…" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun HeroCard(today: Int, yesterday: Int, site: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "VISITS TODAY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                today.toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            val delta = today - yesterday
            val (icon, color, label) = when {
                delta > 0 -> Triple(Icons.Filled.ArrowUpward, Color(0xFF22C55E), "+$delta")
                delta < 0 -> Triple(Icons.Filled.ArrowDownward, Color(0xFFEF4444), delta.toString())
                else -> Triple(null, MaterialTheme.colorScheme.onSurfaceVariant, "no change")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text("$label vs yesterday", color = color, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                site,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BarChart(points: List<DayPoint>) {
    val barColor = MaterialTheme.colorScheme.primary
    if (points.isEmpty()) {
        Text(
            "No data yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val max = (points.maxOfOrNull { it.visits } ?: 1).coerceAtLeast(1)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(MaterialTheme.shapes.small),
    ) {
        val n = points.size
        val gap = 6.dp.toPx()
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        points.forEachIndexed { i, p ->
            val h = (p.visits.toFloat() / max) * size.height
            val x = i * (barW + gap)
            drawRect(
                color = barColor,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
            )
        }
    }
}

private fun prettyReferrer(raw: String): String {
    if (raw.isBlank() || raw == "-") return "direct / none"
    return raw.removePrefix("https://").removePrefix("http://").removeSuffix("/")
}

private fun formatTime(iso: String): String = try {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) {
    "just now"
}
