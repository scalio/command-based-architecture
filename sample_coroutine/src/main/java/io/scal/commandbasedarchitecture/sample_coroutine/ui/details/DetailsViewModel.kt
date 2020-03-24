package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

abstract class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var itemUid: String

    abstract val dataState: LiveData<DetailsScreenState>

    abstract fun reload()

    abstract fun addToFavorite(item: UIMainItem)

    abstract fun removeFromFavorite(item: UIMainItem)
}