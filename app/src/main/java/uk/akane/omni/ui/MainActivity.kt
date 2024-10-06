package uk.akane.omni.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import uk.akane.omni.R
import uk.akane.omni.logic.enableEdgeToEdgeProperly
import uk.akane.omni.ui.fragments.FlashlightFragment
import uk.akane.omni.ui.fragments.LevelFragment
import uk.akane.omni.ui.fragments.RulerFragment

class MainActivity : AppCompatActivity() {

    private var ready: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Log.d("TAG", "YES:: ${intent.extras}")
        if (intent.hasExtra("targetFragment")) {
            intent.getIntExtra("targetFragment", 0).let {
                when (it) {
                    1 -> {
                        startFragment(LevelFragment())
                        postComplete()
                    }
                    2 -> {
                        startFragment(RulerFragment())
                        postComplete()
                    }
                    3 -> {
                        startFragment(FlashlightFragment())
                        postComplete()
                    }
                }
            }
        }
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