package com.menacefit.habitude.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menacefit.habitude.data.repository.StatsRepository
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.util.TimeProvider
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarDayCell(val date: LocalDate?, val performance: DayPerformance?)
data class HeatmapCell(val date: LocalDate, val performance: DayPerformance)
data class DayDetail(val date: LocalDate, val completedCount: Int, val scheduledCount: Int, val xp: Int, val performance: DayPerformance)

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val daysInMonth: List<CalendarDayCell> = emptyList(),
    val selectedDayDetail: DayDetail? = null,
    val heatmapWeeks: List<List<HeatmapCell?>> = emptyList(),
)

class CalendarViewModel(
    private val statsRepository: StatsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(displayedMonth = YearMonth.from(timeProvider.todayLocalDate())))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(_uiState.value.displayedMonth)
        loadHeatmap()
    }

    fun nextMonth() {
        val next = _uiState.value.displayedMonth.plusMonths(1)
        _uiState.update { it.copy(displayedMonth = next) }
        loadMonth(next)
    }

    fun previousMonth() {
        val previous = _uiState.value.displayedMonth.minusMonths(1)
        _uiState.update { it.copy(displayedMonth = previous) }
        loadMonth(previous)
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            val snapshot = statsRepository.getSnapshot(date)
            _uiState.update {
                it.copy(
                    selectedDayDetail = DayDetail(
                        date = date,
                        completedCount = snapshot?.completedCount ?: 0,
                        scheduledCount = snapshot?.scheduledCount ?: 0,
                        xp = snapshot?.xpEarned ?: 0,
                        performance = snapshot?.performance ?: DayPerformance.NONE,
                    ),
                )
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedDayDetail = null) }
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            val snapshotsByDate = statsRepository.getRange(start, end).associateBy { it.date }
            val leadingBlanks = start.dayOfWeek.value - 1 // Monday = 1 -> 0 leading blanks

            val cells = buildList {
                repeat(leadingBlanks) { add(CalendarDayCell(null, null)) }
                var d = start
                while (!d.isAfter(end)) {
                    add(CalendarDayCell(d, snapshotsByDate[d]?.performance))
                    d = d.plusDays(1)
                }
            }
            _uiState.update { it.copy(daysInMonth = cells) }
        }
    }

    private fun loadHeatmap() {
        viewModelScope.launch {
            val today = timeProvider.todayLocalDate()
            val start = today.minusMonths(12).plusDays(1)
            val snapshotsByDate = statsRepository.getRange(start, today).associateBy { it.date }

            val leadingBlanks = start.dayOfWeek.value - 1
            val cells = buildList<HeatmapCell?> {
                repeat(leadingBlanks) { add(null) }
                var d = start
                while (!d.isAfter(today)) {
                    add(HeatmapCell(d, snapshotsByDate[d]?.performance ?: DayPerformance.NONE))
                    d = d.plusDays(1)
                }
            }
            val weeks = cells.chunked(7)
            _uiState.update { it.copy(heatmapWeeks = weeks) }
        }
    }
}
