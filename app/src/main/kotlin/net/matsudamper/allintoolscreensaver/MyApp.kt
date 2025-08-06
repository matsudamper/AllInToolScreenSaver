package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.content.Context
import java.time.Clock
import net.matsudamper.allintoolscreensaver.feature.InMemoryCache
import net.matsudamper.allintoolscreensaver.feature.alert.AlertManager
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepositoryImpl
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepositoryImpl
import net.matsudamper.allintoolscreensaver.lib.PermissionChecker
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
                    single<PermissionChecker> { PermissionChecker(get()) }
                    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
                    single<CalendarRepository> { CalendarRepositoryImpl(get()) }
                    single<NotificationRepository> { NotificationRepository() }
                    single<AlertManager> {
                        AlertManager(
                            calendarRepository = get(),
                            settingsRepository = get(),
                            application = get(),
                            clock = get(),
                        )
                    }
                    single<InMemoryCache> { InMemoryCache() }
                    viewModel {
                        CalendarDisplayScreenViewModel(
                            application = get(),
                            settingsRepository = get(),
                            calendarRepository = get(),
                            clock = get(),
                        )
                    }
                    single<Clock> { Clock.systemDefaultZone() }
                },
            )
        }
    }
}
