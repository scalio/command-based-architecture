package io.scal.commandbasedarchitecture.sample_coroutine.ui.root

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListFragment
import io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel.ItemsRootBroadcastViewModel

class RootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ItemsRootBroadcastViewModel.initIfNeeded(application)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_root)

        if (null == savedInstanceState) {
            setupContent()
        }
    }

    private fun setupContent() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, ListFragment(), null)
            .commitAllowingStateLoss()
    }

    fun addNewFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment, null)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    companion object {
        const val userBroadcastViewModels = true
    }
}