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
                    factory<Application> { this@MyApp }
                    factory<Context> { this@MyApp }
                    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
                    single<CalendarRepository> { CalendarRepositoryImpl(get()) }
                    factory<AlertManager> {
                        AlertManager(
                            calendarRepository = get(),
                            settingsRepository = get(),
                            inMemoryCache = get(),
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
    }
}
