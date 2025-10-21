package net.matsudamper.allintoolscreensaver

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import net.matsudamper.allintoolscreensaver.adapter.ScreenSaverScreenAdapter
import net.matsudamper.allintoolscreensaver.feature.alert.AlertManager
import net.matsudamper.allintoolscreensaver.ui.theme.AllInToolScreenSaverTheme
import org.koin.core.context.GlobalContext

class ClockDreamService :
    DreamService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.Companion.create(this).apply {
        performAttach()
    }

    private val koin get() = GlobalContext.get()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isFullscreen = true
        isScreenBright = true
        isInteractive = true

        val composeView = createComposeView()
        setContentView(composeView)
    }

    private fun createComposeView(): ComposeView {
        return ComposeView(this).apply {
            setupViewTreeOwners()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AllInToolScreenSaverTheme(
                    clock = koin.get(),
                ) {
                    ScreenSaverScreenAdapter()
                }
            }
        }
    }

    private fun ComposeView.setupViewTreeOwners() {
        setViewTreeLifecycleOwner(this@ClockDreamService)
        setViewTreeViewModelStoreOwner(this@ClockDreamService)
        setViewTreeSavedStateRegistryOwner(this@ClockDreamService)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
