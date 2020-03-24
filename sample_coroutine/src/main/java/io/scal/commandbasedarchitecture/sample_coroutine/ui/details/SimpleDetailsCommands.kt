package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import io.scal.commandbasedarchitecture.ActionCommandWithStrategy
import io.scal.commandbasedarchitecture.ConcurrentStrategy
import io.scal.commandbasedarchitecture.SingleStrategy
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

class ReloadDetailsCommand(
    private val refreshAction: suspend () -> UIMainItem,
    private val errorToUIItem: (Throwable) -> UIItem
) :
    ActionCommandWithStrategy<UIMainItem, DetailsScreenState>(SingleStrategy()) {

    override fun onExecuteStarting(dataState: DetailsScreenState): DetailsScreenState =
        dataState.copy(refreshStatus = UIProgressErrorItem.Progress)

    override suspend fun executeCommand(dataState: DetailsScreenState): UIMainItem =
        refreshAction()

    override fun onExecuteSuccess(
        dataState: DetailsScreenState,
        result: UIMainItem
    ): DetailsScreenState =
        dataState.copy(
            refreshStatus = UIProgressErrorItem.Nothing,
            item = result
        )

    override fun onExecuteFail(
        dataState: DetailsScreenState,
        error: Throwable
    ): DetailsScreenState =
        dataState.copy(refreshStatus = errorToUIItem(error))
}

class ChangeFavoriteStatusCommand(
    private val changeToFavorite: Boolean,
    private val changeFavoriteAction: suspend () -> Unit,
    private val onFavoriteChangeFailed: (Throwable) -> Unit
) :
    ActionCommandWithStrategy<Unit, DetailsScreenState>(ConcurrentStrategy()) {

    override fun onExecuteStarting(dataState: DetailsScreenState): DetailsScreenState =
        dataState.item
            ?.let {
                dataState.copy(
                    item = it.copy(
                        favoriteState = it.favoriteState.newSelection(changeToFavorite)
                    )
                )
            }
            ?: dataState

    override suspend fun executeCommand(dataState: DetailsScreenState): Unit =
        changeFavoriteAction()

    override fun onExecuteSuccess(
        dataState: DetailsScreenState,
        result: Unit
    ): DetailsScreenState =
        dataState.item
            ?.let {
                dataState.copy(
                    item = it.copy(
                        favoriteState = it.favoriteState.newFinalState(changeToFavorite)
                    )
                )
            }
            ?: dataState

    override fun onExecuteFail(
        dataState: DetailsScreenState,
        error: Throwable
    ): DetailsScreenState =
        dataState.item
            ?.let {
                dataState.copy(
                    item = it.copy(
                        favoriteState = run {
                            val newFavoriteState = it.favoriteState.revertState(!changeToFavorite)
                            if (it.favoriteState != newFavoriteState) {
                                onFavoriteChangeFailed(error)
                            }
                            newFavoriteState
                        }
                    )
                )
            }
            ?: dataState
}