package com.portalhost.uinotify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Toast(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.Info,
    val duration: Long = 3000,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class ToastType {
    Info,
    Success,
    Warning,
    Error
}

class ToastManager {
    private val _toasts = MutableStateFlow<List<Toast>>(emptyList())
    val toasts: StateFlow<List<Toast>> = _toasts

    private var nextId = System.currentTimeMillis()

    fun show(
        message: String,
        type: ToastType = ToastType.Info,
        duration: Long = 3000,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        val toast = Toast(
            id = nextId++,
            message = message,
            type = type,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
        _toasts.value = _toasts.value + toast

        CoroutineScope(Dispatchers.Default).launch {
            delay(duration)
            dismiss(toast.id)
        }
    }

    fun dismiss(id: Long) {
        _toasts.value = _toasts.value.filter { it.id != id }
    }

    fun info(message: String, duration: Long = 3000) = show(message, ToastType.Info, duration)
    fun success(message: String, duration: Long = 3000) = show(message, ToastType.Success, duration)
    fun warning(message: String, duration: Long = 3000) = show(message, ToastType.Warning, duration)
    fun error(message: String, duration: Long = 5000) = show(message, ToastType.Error, duration)
    
    fun warningWithAction(
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
        duration: Long = 10000
    ) = show(message, ToastType.Warning, duration, actionLabel, onAction)
}