package uk.akane.omni.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import uk.akane.omni.R
import uk.akane.omni.logic.enableEdgeToEdgeProperly

class MainActivity : AppCompatActivity() {

    private var ready: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
        setContentView(R.layout.activity_main)
    }

    fun postComplete() = run { ready = true }

    fun isInflationStarted() = ready

    fun startFragment(frag: Fragment, args: (Bundle.() -> Unit)? = null) {
        supportFragmentManager.commit {
            addToBackStack(System.currentTimeMillis().toString())
            hide(supportFragmentManager.fragments.last())
            replace(R.id.container, frag.apply { args?.let { arguments = Bundle().apply(it) } })
        }
    }

}