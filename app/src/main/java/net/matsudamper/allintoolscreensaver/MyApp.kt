package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.content.Context
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(
                module {
                    single<Application> { this@MyApp }
                    single<Context> { this@MyApp }
                    single<SettingsRepository> { SettingsManager(get()) }
                    single<CalendarRepository> { CalendarManager(get()) }
                    single { AlertManager(get()) }
                    viewModel {
                        CalendarDisplayScreenViewModel(get(), get())
                    }
                },
            )
        }
    }
}
