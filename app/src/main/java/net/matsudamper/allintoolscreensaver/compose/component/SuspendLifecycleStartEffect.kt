package net.matsudamper.allintoolscreensaver.compose.component

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Composable
fun SuspendLifecycleStartEffect(
    key: Any,
    onStart: suspend () -> Unit,
) {
    LifecycleStartEffect(key) {
        val coroutineScope = CoroutineScope(Job())
        coroutineScope.launch { onStart() }
        onStopOrDispose { coroutineScope.cancel() }
    }
}
