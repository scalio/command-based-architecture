package io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel

import io.scal.commandbasedarchitecture.ActionCommand
import io.scal.commandbasedarchitecture.DataConvertCommandSameResult
import io.scal.commandbasedarchitecture.RemoveOnlyList
import io.scal.commandbasedarchitecture.SingleWithTagStrategy
import io.scal.commandbasedarchitecture.pagination.LoadNextWithPageNumberCommand
import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.pagination.RefreshCommand
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListScreenState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

private data class ItemsChildRefreshStrategy(val key: ItemsBroadcastTypes) :
    SingleWithTagStrategy("Refresh:$key")

class ItemsChildRefreshCommand(
    key: ItemsBroadcastTypes,
    private val refreshAction: suspend () -> PageDataWithNextPageNumber<UIMainItem>,
    progressUiItem: () -> UIProgressErrorItem.Progress,
    errorToUIItem: (Throwable) -> UIProgressErrorItem.Error
) : DataConvertCommandSameResult<PageDataWithNextPageNumber<UIMainItem>, ListScreenState, ListScreenState>(
    RefreshCommand(
        { refreshAction() },
        progressUiItem,
        errorToUIItem,
        ItemsChildRefreshStrategy(key)
    ),
    { _, newInnerData -> newInnerData },
    { this }
)

class ItemsChildLoadNextCommand(
    private val key: ItemsBroadcastTypes,
    loadNextAction: suspend (nextPageNumber: Int) -> PageDataWithNextPageNumber<UIMainItem>,
    progressUiItem: () -> UIProgressErrorItem.Progress,
    errorToUIItem: (Throwable) -> UIProgressErrorItem.Error
) : DataConvertCommandSameResult<PageDataWithNextPageNumber<UIMainItem>, ListScreenState, ListScreenState>(
    LoadNextWithPageNumberCommand(
        loadNextAction,
        progressUiItem,
        errorToUIItem,
        SingleWithTagStrategy("LoadNext:$key")
    ),
    { _, newInnerData -> newInnerData },
    { this }
) {


    override fun shouldAddToPendingActions(
        dataState: ListScreenState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        !pendingActionCommands.any { command ->
            command.strategy.let { it is ItemsChildRefreshStrategy && it.key == key }
        }
                &&
                !runningActionCommands.any { command ->
                    command.strategy.let { it is ItemsChildRefreshStrategy && it.key == key }
                }
                &&
                super.shouldAddToPendingActions(
                    dataState,
                    pendingActionCommands,
                    runningActionCommands
                )
}
