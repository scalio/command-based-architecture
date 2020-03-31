package io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.broadcast.ChildViewModel
import io.scal.commandbasedarchitecture.model.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.sample_coroutine.repository.HardCodeRepository
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListScreenState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.PAGE_SIZE
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.commands.ChangeFavoriteStatusCommand
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.toUIMainItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ItemsChildBroadcastViewModel(
    override val key: ItemsBroadcastTypes,
    val childLiveData: LiveData<ListScreenState>,
    val commandManager: CommandManager<ListScreenState>,
    private val context: Context
) : ChildViewModel<ItemsBroadcastTypes> {

    fun reload() {
        commandManager.postCommand(
            ItemsChildRefreshCommand(
                key,
                { executeLoadNextPage(0) },
                { UIProgressErrorItem.Progress },
                { UIProgressErrorItem.Error(it.toString()) { reload() } }
            )
        )
    }

    fun loadNextPage() {
        commandManager.postCommand(
            ItemsChildLoadNextCommand(
                key,
                { executeLoadNextPage(it) },
                { UIProgressErrorItem.Progress },
                { UIProgressErrorItem.Error(it.toString()) { loadNextPage() } }
            )
        )
    }

    private suspend fun executeLoadNextPage(pageNumber: Int): PageDataWithNextPageNumber<UIMainItem> =
        when (key) {
            ItemsBroadcastTypes.AllItems -> {
                val items =
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.loadNextMainPage(pageNumber, PAGE_SIZE)
                    }
                PageDataWithNextPageNumber(
                    withContext(Dispatchers.Default) { items.map { it.toUIMainItem() } },
                    if (items.isEmpty()) null else pageNumber + 1
                )
            }
            is ItemsBroadcastTypes.OneItem -> {
                val item =
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.loadItemDetails(key.uid)
                    }
                PageDataWithNextPageNumber(
                    withContext(Dispatchers.Default) { listOf(item.toUIMainItem()) },
                    null
                )
            }
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
            ChangeFavoriteStatusCommand(
                uid,
                newFavoriteStatus,
                {
                    withContext(Dispatchers.IO) {
                        HardCodeRepository.changeFavoriteStatus(uid, newFavoriteStatus)
                    }
                },
                { Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show() }
            )
        )
    }
}