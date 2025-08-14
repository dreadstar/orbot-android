package org.torproject.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LoggingLevel {
    DISABLED, BASIC, DETAILED, FULL
}

data class LoggingStats(
    val meshEvents: Int = 0,
    val userActions: Int = 0,
    val networkConditions: Int = 0,
    val batteryImpacts: Int = 0,
    val installationSteps: Int = 0,
    val protestMetrics: Int = 0
) {
    fun totalEntries(): Int = meshEvents + userActions + networkConditions + batteryImpacts + installationSteps + protestMetrics
}

class BetaConsentViewModel : ViewModel() {
    private val _consentGiven = MutableLiveData(false)
    val consentGiven: LiveData<Boolean> = _consentGiven

    private val _loggingLevel = MutableLiveData(LoggingLevel.DISABLED)
    val loggingLevel: LiveData<LoggingLevel> = _loggingLevel

    private val _loggingStats = MutableLiveData(LoggingStats())
    val loggingStats: LiveData<LoggingStats> = _loggingStats

    private var statsJob: Job? = null

    fun setConsent(consent: Boolean, level: LoggingLevel) {
        _consentGiven.value = consent
        _loggingLevel.value = level
        if (consent) {
            startStatsUpdate()
        } else {
            stopStatsUpdate()
            _loggingStats.value = LoggingStats()
        }
    }

    fun revokeConsent() {
        setConsent(false, LoggingLevel.DISABLED)
    }

    fun exportData(onExport: (LoggingStats, Boolean, LoggingLevel) -> Unit) {
        onExport(_loggingStats.value ?: LoggingStats(), _consentGiven.value ?: false, _loggingLevel.value ?: LoggingLevel.DISABLED)
    }

    private fun startStatsUpdate() {
        stopStatsUpdate()
        statsJob = viewModelScope.launch {
            while (_consentGiven.value == true) {
                delay(5000)
                val prev = _loggingStats.value ?: LoggingStats()
                val level = _loggingLevel.value ?: LoggingLevel.DISABLED
                _loggingStats.value = prev.copy(
                    meshEvents = prev.meshEvents + (0..2).random(),
                    userActions = prev.userActions + (0..1).random(),
                    networkConditions = prev.networkConditions + (0..3).random(),
                    batteryImpacts = prev.batteryImpacts + (0..0).random(),
                    installationSteps = prev.installationSteps,
                    protestMetrics = prev.protestMetrics + if (level == LoggingLevel.FULL) (0..0).random() else 0
                )
            }
        }
    }

    private fun stopStatsUpdate() {
        statsJob?.cancel()
        statsJob = null
    }
}
