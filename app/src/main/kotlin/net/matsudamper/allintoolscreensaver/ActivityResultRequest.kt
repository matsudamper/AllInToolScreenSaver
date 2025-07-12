package net.matsudamper.allintoolscreensaver

import androidx.activity.result.contract.ActivityResultContract

class ActivityResultRequest<I, O>(
    val contract: ActivityResultContract<I, O>,
    val input: I,
    val result: (O) -> Unit,
)
