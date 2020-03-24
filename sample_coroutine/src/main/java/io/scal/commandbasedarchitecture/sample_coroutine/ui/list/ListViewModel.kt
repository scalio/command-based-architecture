package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.sample_coroutine.model.MainItem

const val PAGE_SIZE = 20

abstract class ListViewModel(application: Application) : AndroidViewModel(application) {

    abstract val dataState: LiveData<ListScreenState>
    abstract val commandManager: CommandManager<ListScreenState>

    abstract fun reload()

    abstract fun loadNextPage()

    abstract fun removeFromFavorite(item: UIMainItem)

    abstract fun addToFavorite(item: UIMainItem)
}

fun MainItem.toUIMainItem(): UIMainItem =
    UIMainItem(
        uid,
        "$firstTitlePart $secondTitlePart".trim(),
        FavoriteState.NoProgress(false)
    )
