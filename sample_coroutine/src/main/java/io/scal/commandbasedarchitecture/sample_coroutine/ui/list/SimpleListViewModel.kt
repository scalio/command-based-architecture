package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.scal.commandbasedarchitecture.managers.ExecutionController
import io.scal.commandbasedarchitecture.managers.FlowCommandManager
import io.scal.commandbasedarchitecture.managers.ICommandManager
import io.scal.commandbasedarchitecture.model.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.pagination.LoadNextCommand
import io.scal.commandbasedarchitecture.pagination.RefreshCommand
import io.scal.commandbasedarchitecture.sample_coroutine.repository.HardCodeRepository
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.commands.ChangeFavoriteStatusCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleListViewModel(application: Application) : ListViewModel(application) {

    private val mutableScreenState = MutableStateFlow(
        ListScreenState(null, null, null)
    )
    override val screenState: LiveData<ListScreenState> by lazy {
        mutableScreenState
            .asLiveData(viewModelScope.coroutineContext)
    }

    override val commandManager: ICommandManager<ListScreenState> by lazy {
        FlowCommandManager(
            mutableScreenState,
            ExecutionController(viewModelScope),
            { Log.w("SimpleViewModel", it) },
            { message, error -> Log.w("SimpleViewModel", message, error) }
        )
    }

    init {
        viewModelScope.launch {
            mutableScreenState
                .map { if(!it.pageData?.itemsList.isNullOrEmpty()) it.refreshStatus else null }
                .distinctUntilChanged()
                .collect {
                    if (it is UIProgressErrorItem.Error) {
                        Toast.makeText(getApplication(), it.error, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        reload()
    }

    override fun reload() {
        commandManager.postCommand(
            RefreshCommand(
                { executeLoadNextPage(0) },
                { UIProgressErrorItem.Progress },
                { UIProgressErrorItem.Error(it.toString()) { reload() } }
            )
        )
    }

    override fun loadNextPage() {
        if (null != mutableScreenState.value.pageData?.nextPageNumber) {
            commandManager.postCommand(
                LoadNextCommand(
                    {
                        val nextPageNumber = it.pageData?.nextPageNumber
                        if (null == nextPageNumber)
                            PageDataWithNextPageNumber(emptyList(), null)
                        else
                            executeLoadNextPage(nextPageNumber)
                    },
                    { UIProgressErrorItem.Progress },
                    { UIProgressErrorItem.Error(it.toString()) { loadNextPage() } }
                )
            )
        }
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

    override fun removeFromFavorite(item: UIMainItem) {
        if (item.favoriteState.favorite) {
            executeFavoriteChangeAction(item.uid, false)
        }
    }

    override fun addToFavorite(item: UIMainItem) {
        if (!item.favoriteState.favorite) {
            executeFavoriteChangeAction(item.uid, true)
        }
    }

    private fun executeFavoriteChangeAction(uid: String, newFavoriteStatus: Boolean) {
        commandManager.postCommand(
            ChangeFavoriteStatusCommand(
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
