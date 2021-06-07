package io.scal.commandbasedarchitecture.pagination

import io.scal.commandbasedarchitecture.commands.Command
import io.scal.commandbasedarchitecture.commands.ExecutionStrategy
import io.scal.commandbasedarchitecture.model.PageData
import io.scal.commandbasedarchitecture.model.PaginationState

/**
 * Command that will execute refresh data action with showing refresh progress and SingleTag strategy.
 * That means only one refresh command is able to execute and be added to the queue.
 */
open class RefreshCommand<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        UIProgressItem : UIBaseItem,
        UIErrorItem : UIBaseItem,
        Data : PageData<UIDataItem>>
    (
    private val refreshAction: suspend (PaginationState<UIBaseItem, UIDataItem, Data>) -> Data,
    private val progressUiItem: () -> UIProgressItem,
    private val errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: ExecutionStrategy = RefreshStrategy()
) : Command<Data, PaginationState<UIBaseItem, UIDataItem, Data>>(strategy) {

    override fun onCommandWasAdded(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(refreshStatus = progressUiItem())

    override fun onExecuteStarting(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(refreshStatus = progressUiItem())

    override suspend fun executeCommand(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): Data =
        refreshAction(dataState)

    override fun onExecuteSuccess(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        result: Data
    ): PaginationState<UIBaseItem, UIDataItem, Data> =
        PaginationState(pageData = result)

    override fun onExecuteFail(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        error: Throwable
    ): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(refreshStatus = errorToUIItem(error))
}

/**
 * Command that will execute load next data action while showing page progress and Single strategy.
 * That means only one load next command is able to execute and be added to the queue.
 */
open class LoadNextCommand<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        UIProgressItem : UIBaseItem,
        UIErrorItem : UIBaseItem,
        Data : PageData<UIDataItem>>
    (
    private val loadNextAction: suspend (PaginationState<UIBaseItem, UIDataItem, Data>) -> Data,
    private val progressUiItem: () -> UIProgressItem,
    private val errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: ExecutionStrategy = LoadNextStrategy()
) : Command<Data, PaginationState<UIBaseItem, UIDataItem, Data>>(strategy) {

    override fun onCommandWasAdded(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(nextPageLoadingStatus = progressUiItem())

    override fun onExecuteStarting(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(nextPageLoadingStatus = progressUiItem())

    override suspend fun executeCommand(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): Data =
        loadNextAction(dataState)

    @Suppress("UNCHECKED_CAST")
    override fun onExecuteSuccess(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        result: Data
    ): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(
            pageData = (dataState.pageData?.plusNextPage(result) as? Data) ?: result,
            nextPageLoadingStatus = null
        )

    override fun onExecuteFail(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        error: Throwable
    ): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(nextPageLoadingStatus = errorToUIItem(error))
}