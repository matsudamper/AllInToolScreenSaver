package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import net.matsudamper.allintoolscreensaver.compose.MainScreen
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startKoin {
            modules(
                module {
                    single<Application> { application }
                    single<Context> { application }
                    single<SettingsRepository> { SettingsManager(get()) }
                    single<CalendarRepository> { CalendarManager(get()) }
                    viewModel {
                        CalendarDisplayScreenViewModel(get(), get())
                    }
                },
            )
        }
        setContent {
            AllInToolScreenSaverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
