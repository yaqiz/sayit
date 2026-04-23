package com.example.sayit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.sayit.R
import com.example.sayit.data.TaskEntity
import java.time.LocalDate
import java.time.YearMonth

private val AppBackground = Brush.verticalGradient(
    colors = listOf(Color(0xFFF6F2EA), Color(0xFFE9EEF5))
)

private val CompletedBlue = Color(0xFFD8E9FF)
private val CompletedTextBlue = Color(0xFF245FAF)
private val OverdueRed = Color(0xFFFFE2E2)
private val OverdueTextRed = Color(0xFFA14C4C)
private val PendingWhite = Color.White
private val CardCream = Color(0xFFFFFBF5)

@Composable
fun SayitTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun SayitScreen(
    uiState: TaskUiState,
    onStartListening: () -> Unit,
    onToggleTask: (TaskEntity, Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackPressed()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            if (uiState.screenMode == ScreenMode.TASK_LIST) {
                TaskListScreen(
                    uiState = uiState,
                    onStartListening = onStartListening,
                    onToggleTask = onToggleTask,
                    onOpenCalendar = onOpenCalendar
                )
            } else {
                CalendarScreen(
                    uiState = uiState,
                    onOpenDate = onOpenDate
                )
            }
        }
    }
}

@Composable
private fun TaskListScreen(
    uiState: TaskUiState,
    onStartListening: () -> Unit,
    onToggleTask: (TaskEntity, Boolean) -> Unit,
    onOpenCalendar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onStartListening) {
                detectTapGestures(onDoubleTap = { onStartListening() })
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Button(
                    onClick = onStartListening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                ) {
                    Text(
                        text = when {
                            uiState.isListening -> "正在听..."
                            uiState.isProcessingVoice -> "处理中..."
                            else -> "开始语音"
                        }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    HeaderCard(
                        selectedDate = uiState.selectedDate,
                        speechEngineLabel = uiState.speechEngineLabel,
                        isListening = uiState.isListening,
                        isProcessingVoice = uiState.isProcessingVoice,
                        statusMessage = uiState.statusMessage,
                        lastHeardText = uiState.lastHeardText,
                        onOpenCalendar = onOpenCalendar
                    )
                }
                item {
                    TipsCard()
                }
                item {
                    Text(
                        text = buildTaskListTitle(uiState.selectedDate),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (uiState.tasks.isEmpty()) {
                    item {
                        EmptyStateCard(uiState.selectedDate)
                    }
                } else {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            selectedDate = uiState.selectedDate,
                            onToggleTask = onToggleTask
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    uiState: TaskUiState,
    onOpenDate: (LocalDate) -> Unit
) {
    val currentMonthIndex = uiState.visibleMonths.indexOf(YearMonth.now()).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentMonthIndex)

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CalendarHeaderCard(uiState.selectedDate)
            }
            items(uiState.visibleMonths, key = { it.toString() }) { month ->
                MonthCard(
                    month = month,
                    selectedDate = uiState.selectedDate,
                    tasksByDate = uiState.tasksByDate,
                    onOpenDate = onOpenDate
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(
    selectedDate: LocalDate,
    speechEngineLabel: String,
    isListening: Boolean,
    isProcessingVoice: Boolean,
    statusMessage: String,
    lastHeardText: String,
    onOpenCalendar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF133C55))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sayit_header_icon),
                        contentDescription = "SayIt icon",
                        modifier = Modifier.size(52.dp)
                    )
                    Column {
                        Text(
                            text = "SayIt",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = buildDateTitle(selectedDate),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                Text(
                    text = "月历",
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenCalendar)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (speechEngineLabel.isNotBlank()) {
                Text(
                    text = "语音模式：$speechEngineLabel",
                    color = Color(0xFFB9D8FF),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isListening || isProcessingVoice) {
                Text(
                    text = if (isListening) "状态：正在听你说话..." else "状态：正在处理识别结果...",
                    color = Color(0xFFB9D8FF),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = statusMessage,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyLarge
            )
            if (lastHeardText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "识别到：$lastHeardText",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CalendarHeaderCard(selectedDate: LocalDate) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF133C55))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sayit_header_icon),
                    contentDescription = "SayIt icon",
                    modifier = Modifier.size(44.dp)
                )
                Column {
                    Text(
                        text = "Monthly Calendar",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "上下滚动切换月份",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Text(
                text = "点击日期进入当天 task list。当前选中：${buildDateTitle(selectedDate)}",
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TipsCard() {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "可以直接说" else "可以直接说...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (expanded) "↑" else "↓",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF245FAF)
                )
            }
            if (!expanded) {
                return@Column
            }
            Text(text = "记录我今天完成小红书文案")
            Text(text = "小红书文案完成")
            Text(text = "告诉我今天还没完成的任务")
            Text(text = "告诉我今天完成的任务")
            Text(text = "删除当天的所有任务")
            Text(text = "今天下午4点提醒我收衣服")
            Text(text = "5分钟后提醒我去看烤箱")
        }
    }
}

@Composable
private fun EmptyStateCard(selectedDate: LocalDate) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEE4))
    ) {
        Text(
            text = if (selectedDate == LocalDate.now()) {
                "今天还没有任务，先说一句“记录小红书文案”。"
            } else {
                "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日还没有任务。"
            },
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, DayTaskSummary>,
    onOpenDate: (LocalDate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${month.year}年 ${month.monthValue}月",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            )
            WeekdayHeader()
            val cells = buildMonthCells(month)
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).height(68.dp))
                        } else {
                            DayCell(
                                date = date,
                                selectedDate = selectedDate,
                                summary = tasksByDate[date],
                                onOpenDate = onOpenDate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        weekdays.forEach { label ->
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selectedDate: LocalDate,
    summary: DayTaskSummary?,
    onOpenDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isSelected = date == selectedDate
    val isCompleted = summary?.allCompleted == true
    val isOverdue = summary?.hasPending == true && date.isBefore(today)
    val containerColor = when {
        isCompleted -> CompletedBlue
        isOverdue -> OverdueRed
        else -> PendingWhite
    }
    val textColor = when {
        isCompleted -> CompletedTextBlue
        isOverdue -> OverdueTextRed
        else -> Color(0xFF1C1B1F)
    }

    Column(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF133C55) else Color(0xFFE6DDD0),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onOpenDate(date) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                isCompleted -> "完成"
                isOverdue -> "过期"
                else -> "${summary?.totalCount ?: 0}项"
            },
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    selectedDate: LocalDate,
    onToggleTask: (TaskEntity, Boolean) -> Unit
) {
    val today = LocalDate.now()
    val containerColor = when {
        task.isCompleted -> CompletedBlue
        selectedDate.isBefore(today) -> OverdueRed
        else -> PendingWhite
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleTask(task, !task.isCompleted) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { checked -> onToggleTask(task, checked) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    )
                )
                Text(
                    text = when {
                        task.isCompleted -> "已完成"
                        selectedDate.isBefore(today) -> "已过期未完成"
                        else -> "未完成"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4C5B5C)
                )
            }
        }
    }
}

private fun buildDateTitle(date: LocalDate): String {
    return if (date == LocalDate.now()) {
        "今天的任务"
    } else {
        "${date.monthValue}月${date.dayOfMonth}日任务"
    }
}

private fun buildTaskListTitle(date: LocalDate): String {
    return if (date == LocalDate.now()) {
        "今天的 task list"
    } else {
        "${date.monthValue}月${date.dayOfMonth}日的 task list"
    }
}

private fun buildMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmpty = firstDay.dayOfWeek.value % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = MutableList<LocalDate?>(leadingEmpty) { null } + days
    val remainder = cells.size % 7
    return if (remainder == 0) {
        cells
    } else {
        cells + List(7 - remainder) { null }
    }
}
