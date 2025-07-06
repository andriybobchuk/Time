package com.plcoding.bookpedia.mooney.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bookpedia.core.presentation.Toolbars
import com.plcoding.bookpedia.mooney.presentation.formatWithCommas
import com.recallit.transactions.presentation.TransactionBottomSheet
import com.recallit.transactions.presentation.TransactionsScreenContent
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel(),
    bottomNavbar: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.background(Color(0xFF3E4DBA)),
        topBar = {
            Toolbars.Primary(
                title = "Analytics",
                scrollBehavior = scrollBehavior,
                customContent = {
                    MonthPicker(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                    )
                }
            )
        },
        bottomBar = { bottomNavbar() },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).background(Color(0xFF3E4DBA))) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.metrics) { metric ->
                        MetricCard(metric)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White),
                ) {
                    MonthPicker(
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = viewModel::onMonthSelected,
                    )
                }

            }
        }
    )

}



@Composable
fun MonthPicker(
    selectedMonth: MonthKey,
    onMonthSelected: (MonthKey) -> Unit,
    monthRange: List<MonthKey> = generateRecentMonths(4)
) {
    var expanded by remember { mutableStateOf(false) }

    Button(
        onClick = { expanded = true },
    ) {
       Text(color = Color.White, text = selectedMonth.toDisplayString())
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        monthRange.forEach { month ->
            DropdownMenuItem(
                text = { Text(month.toDisplayString()) },
                onClick = {
                    onMonthSelected(month)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun MetricCard(metric: AnalyticsMetric) {
    Column(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .fillMaxWidth()
            .background(Color.White.copy(0.75f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = metric.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleMedium
        )
        metric.subtitle?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


fun generateRecentMonths(count: Int): List<MonthKey> {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val months = mutableListOf<MonthKey>()
    var year = now.year
    var month = now.monthNumber

    repeat(count) {
        months.add(MonthKey(year, month))
        month--
        if (month == 0) {
            month = 12
            year--
        }
    }

    return months
}
