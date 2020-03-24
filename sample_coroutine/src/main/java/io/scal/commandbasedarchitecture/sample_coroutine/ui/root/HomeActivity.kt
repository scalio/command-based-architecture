package io.scal.commandbasedarchitecture.sample_coroutine.ui.root

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.MainFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        if (null == savedInstanceState) {
            setupContent()
        }
    }

    private fun setupContent() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, MainFragment(), null)
            .commitAllowingStateLoss()
    }
}