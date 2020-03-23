package io.scal.commandbasedarchitecture.sample_coroutine.ui.main

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.CommandManagerImpl
import io.scal.commandbasedarchitecture.pagination.LoadNextWithPageNumberCommand
import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.pagination.RefreshCommand
import io.scal.commandbasedarchitecture.sample_coroutine.model.MainItem
import io.scal.commandbasedarchitecture.sample_coroutine.repository.HardCodeRepository
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.commands.MainChangeFavoriteStatusCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 20

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val mutableDataState = MutableLiveData(
        MainScreenState(null, null, null)
    )
    val dataState: LiveData<MainScreenState> = mutableDataState

    private val commandManager: CommandManager<MainScreenState> by lazy {
        CommandManagerImpl(mutableDataState, viewModelScope)
    }

    init {
        reload()
    }

    fun reload() {
        commandManager.postCommand(
            RefreshCommand(
                { executeLoadNextPage(0) },
                { UIProgressErrorItem.Progress },
                { UIProgressErrorItem.Error(it.toString()) { reload() } }
            )
        )
    }

    fun loadNextPage() {
        commandManager.postCommand(
            LoadNextWithPageNumberCommand(
                { executeLoadNextPage(it) },
                { UIProgressErrorItem.Progress },
                { UIProgressErrorItem.Error(it.toString()) { loadNextPage() } }
            )
        )
    }

    private suspend fun executeLoadNextPage(pageNumber: Int): PageDataWithNextPageNumber<UIMainItem> {
        val items =
            withContext(Dispatchers.IO) {
                HardCodeRepository.loadNextMainPage(pageNumber, PAGE_SIZE)
            }
        return PageDataWithNextPageNumber(
            withContext(Dispatchers.Default) { items.map { it.toUIMainItem() } },
            if (items.isEmpty()) null else pageNumber + 1
        )
    }

    fun removeFromFavorite(item: UIMainItem) {
        if (item.favoriteState.favorite) {
            executeFavoriteChangeAction(item.uid, false)
        }
    }

    fun addToFavorite(item: UIMainItem) {
        if (!item.favoriteState.favorite) {
            executeFavoriteChangeAction(item.uid, true)
        }
    }

    private fun executeFavoriteChangeAction(uid: String, newFavoriteStatus: Boolean) {
        commandManager.postCommand(
            MainChangeFavoriteStatusCommand(
                uid,
                newFavoriteStatus,
                {
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.changeFavoriteStatus(uid, newFavoriteStatus)
                    }
                },
                { Toast.makeText(getApplication(), it.toString(), Toast.LENGTH_SHORT).show() }
            )
        )
    }
}

private fun MainItem.toUIMainItem(): UIMainItem {
    return UIMainItem(
        uid,
        "$firstTitlePart $secondTitlePart".trim(),
        FavoriteState.NoProgress(false)
    )
}
