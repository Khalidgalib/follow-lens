package app.followlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.followlens.data.DatabaseDriverFactory
import app.followlens.ui.App
import app.followlens.ui.DirectoryLister

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(DatabaseDriverFactory(applicationContext), DirectoryLister(applicationContext))
        }
    }
}
