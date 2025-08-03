import android.app.Application
import com.example.latencychecker.di.viewModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class LatencyCheckerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LatencyCheckerApp)
            modules(
                listOf(
                    appModule,
                    networkModule,
                    repositoryModule,
                    viewModule
                )
            )
        }
    }
}
