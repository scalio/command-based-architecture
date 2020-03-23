package io.scal.commandbasedarchitecture.sample_coroutine.ui.main.commands

import io.scal.commandbasedarchitecture.ActionCommandWithStrategy
import io.scal.commandbasedarchitecture.ConcurrentStrategy
import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.FavoriteState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.MainScreenState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.UIMainItem

internal class MainChangeFavoriteStatusCommand(
    private val mainItemUid: String,
    private val changeToFavorite: Boolean,
    private val changeFavoriteAction: suspend () -> Unit,
    private val onFavoriteChangeFailed: (Throwable) -> Unit
) : ActionCommandWithStrategy<Unit, MainScreenState>(ConcurrentStrategy(mainItemUid)) {

    override fun onCommandWasAdded(dataState: MainScreenState): MainScreenState =
        if (dataState.pageData?.itemsList?.any { it.key == mainItemUid } == true) {
            dataState.copy(
                pageData = dataState.pageData?.updateItemFavoriteState {
                    it.newSelection(
                        changeToFavorite
                    )
                }
            )
        } else {
            dataState
        }

    override suspend fun executeCommand(dataState: MainScreenState) {
        val uiMainItem = dataState.pageData?.itemsList?.firstOrNull { it.key == mainItemUid }
        if (uiMainItem?.favoriteState is FavoriteState.PreSelectProgress && uiMainItem.favoriteState.hasChange()) {
            changeFavoriteAction()
        }
    }

    override fun onExecuteSuccess(dataState: MainScreenState, result: Unit): MainScreenState =
        if (dataState.pageData?.itemsList?.any { it.key == mainItemUid } == true) {
            dataState.copy(
                pageData = dataState.pageData?.updateItemFavoriteState {
                    it.newFinalState(
                        changeToFavorite
                    )
                }
            )
        } else {
            dataState
        }

    override fun onExecuteFail(dataState: MainScreenState, error: Throwable): MainScreenState =
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