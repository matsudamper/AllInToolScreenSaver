package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MyApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(
                module {
                    factory<Application> { this@MyApp }
                    factory<Context> { this@MyApp }
                    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
                    single<CalendarRepository> { CalendarRepositoryImpl(get()) }
                    single<AlertManager> {
                        AlertManager(
                            calendarRepository = get(),
                            settingsRepository = get(),
                            application = get(),
                        )
                    }
                    single<InMemoryCache> { InMemoryCache() }
                    viewModel {
                        CalendarDisplayScreenViewModel(get(), get(), get())
                    }
                },
            )
        }

        setupAlertService()
    }

    private fun setupAlertService() {
        applicationScope.launch {
            val settingsRepository = SettingsRepositoryImpl(this@MyApp)
            settingsRepository.getAlertEnabledFlow().collectLatest { isEnabled ->
                if (isEnabled && Settings.canDrawOverlays(this@MyApp)) {
                    AlertService.startService(this@MyApp)
                } else {
                    AlertService.stopService(this@MyApp)
                }
            }
        }
    }
}
