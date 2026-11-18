package id.ac.ub.todoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import id.ac.ub.todoapp.ui.TodoScreen
import id.ac.ub.todoapp.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {

    // Android 17 (API 37) silently drops local-network traffic such as
    // 10.0.2.2 unless this runtime permission is granted. Wait for the
    // result before composing the screen so the first fetch can succeed.
    private val localNetworkPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { showTodoScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (needsLocalNetworkPermission()) {
            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            showTodoScreen()
        }
    }

    private fun needsLocalNetworkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 37) return false
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_LOCAL_NETWORK
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun showTodoScreen() {
        setContent {
            TodoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TodoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
