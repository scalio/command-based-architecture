package io.scal.commandbasedarchitecture.sample_coroutine.ui.list.commands

import io.scal.commandbasedarchitecture.ActionCommandWithStrategy
import io.scal.commandbasedarchitecture.ConcurrentStrategyWithTag
import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.FavoriteState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListScreenState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

/**
 * Command that handle all the logic for changing favorite status including:
 * 1. Only one command per item
 * 2. Only latest update will be pushed to server
 * 3. Immediate user response for the change
 * 4. Good revert logic if request fails
 */
internal class ChangeFavoriteStatusCommand(
    private val mainItemUid: String,
    private val changeToFavorite: Boolean,
    private val changeFavoriteAction: suspend () -> Unit,
    private val onFavoriteChangeFailed: (Throwable) -> Unit
) : ActionCommandWithStrategy<Unit, ListScreenState>(ConcurrentStrategyWithTag(mainItemUid)) {

    override fun onCommandWasAdded(dataState: ListScreenState): ListScreenState =
        // we update item state only if the list contains this item
        if (dataState.pageData?.itemsList?.any { it.key == mainItemUid } == true) {
            dataState.copy(
                pageData = dataState.pageData?.updateItemFavoriteState {
                    it.newSelection(changeToFavorite)
                }
            )
        } else {
            dataState
        }

    override suspend fun executeCommand(dataState: ListScreenState) {
        val uiMainItem = dataState.pageData?.itemsList?.firstOrNull { it.key == mainItemUid }
        if (uiMainItem?.favoriteState is FavoriteState.PreSelectProgress && uiMainItem.favoriteState.hasChange()) {
            // we should run this command only if there was a change. otherwise we should skip it
            changeFavoriteAction()
        }
    }

    override fun onExecuteSuccess(dataState: ListScreenState, result: Unit): ListScreenState =
        if (dataState.pageData?.itemsList?.any { it.key == mainItemUid } == true) {
            dataState.copy(
                pageData = dataState.pageData?.updateItemFavoriteState {
                    it.newFinalState(changeToFavorite)
                }
            )
        } else {
            dataState
        }

    override fun onExecuteFail(dataState: ListScreenState, error: Throwable): ListScreenState =
        if (dataState.pageData?.itemsList?.any { it.key == mainItemUid } == true) {
            var hasChange = false
            val newData = dataState.copy(
                pageData = dataState.pageData?.updateItemFavoriteState {
                    val newFavoriteState = it.revertState(!changeToFavorite)
                    if (it.favorite != newFavoriteState.favorite) {
                        hasChange = true
                    }
                    newFavoriteState
                }
            )
            if (hasChange) onFavoriteChangeFailed(error)
            newData
        } else {
            dataState
        }

    private fun PageDataWithNextPageNumber<UIMainItem>.updateItemFavoriteState(
        newStateGenerator: (FavoriteState) -> FavoriteState
    ): PageDataWithNextPageNumber<UIMainItem> =
        PageDataWithNextPageNumber(
            itemsList.map {
                if (it.key == mainItemUid)
                    it.copy(favoriteState = newStateGenerator(it.favoriteState))
                else
                    it
            },
            nextPageNumber
        )
}