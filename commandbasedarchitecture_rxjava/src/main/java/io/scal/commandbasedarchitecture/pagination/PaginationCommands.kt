package io.scal.commandbasedarchitecture.pagination

import io.reactivex.Single
import io.scal.commandbasedarchitecture.*
import io.scal.commandbasedarchitecture.model.PageData
import io.scal.commandbasedarchitecture.model.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.model.PaginationState
import io.scal.commandbasedarchitecture.model.RemoveOnlyList

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
    private val refreshAction: (PaginationState<UIBaseItem, UIDataItem, Data>) -> Single<Data>,
    private val progressUiItem: () -> UIProgressItem,
    private val errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: ExecutionStrategy = RefreshStrategy()
) : ActionCommandWithStrategy<Data, PaginationState<UIBaseItem, UIDataItem, Data>>(
    strategy
) {

    override fun onCommandWasAdded(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(refreshStatus = progressUiItem())

    override fun onExecuteStarting(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(refreshStatus = progressUiItem())

    override fun executeCommand(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): Single<Data> =
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
    private val loadNextAction: (PaginationState<UIBaseItem, UIDataItem, Data>) -> Single<Data>,
    private val progressUiItem: () -> UIProgressItem,
    private val errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: ExecutionStrategy = LoadNextStrategy()
) : ActionCommandWithStrategy<Data, PaginationState<UIBaseItem, UIDataItem, Data>>(
    strategy
) {

    override fun onCommandWasAdded(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(nextPageLoadingStatus = progressUiItem())

    override fun onExecuteStarting(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): PaginationState<UIBaseItem, UIDataItem, Data> =
        dataState.copy(nextPageLoadingStatus = progressUiItem())

    override fun executeCommand(dataState: PaginationState<UIBaseItem, UIDataItem, Data>): Single<Data> =
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

/**
 * Command that will execute load next data action while showing page progress and Single strategy.
 * That means only one load next command is able to executed and added to the queue.
 */
open class LoadNextWithPageNumberCommand<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        UIProgressItem : UIBaseItem,
        UIErrorItem : UIBaseItem,
        Data : PageDataWithNextPageNumber<UIDataItem>>
    (
    private val loadNextAction: (nextPageNumber: Int) -> Single<Data>,
    progressUiItem: () -> UIProgressItem,
    errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: ExecutionStrategy = LoadNextStrategy()
) : DataConvertCommandSameResult<Data, PaginationState<UIBaseItem, UIDataItem, Data>, PaginationState<UIBaseItem, UIDataItem, Data>>(
    LoadNextCommand(
        { loadNextAction(it.pageData!!.nextPageNumber!!) },
        progressUiItem,
        errorToUIItem,
        strategy
    ),
    { _, result -> result },
    { this }
) {

    override fun shouldAddToPendingActions(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        null != dataState.pageData?.nextPageNumber &&
                super.shouldAddToPendingActions(
                    dataState,
                    pendingActionCommands,
                    runningActionCommands
                )

    override fun shouldExecuteAction(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        if (null == dataState.pageData?.nextPageNumber) {
            pendingActionCommands.remove(this)
            false
        } else {
            super.shouldExecuteAction(dataState, pendingActionCommands, runningActionCommands)
        }
}

/**
 * Command that will execute load next data action while showing page progress and Single strategy.
 * That means only one load next command is able to be executed and added to the queue.
 */
open class LoadNextWithLatestItemCommand<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        UIProgressItem : UIBaseItem,
        UIErrorItem : UIBaseItem,
        Data : io.scal.commandbasedarchitecture.model.PageDataWithLatestItem<UIDataItem>>
    (
    private val loadNextAction: (latestItem: UIDataItem) -> Single<Data>,
    progressUiItem: () -> UIProgressItem,
    errorToUIItem: (Throwable) -> UIErrorItem,
    strategy: SingleStrategy = LoadNextStrategy()
) : DataConvertCommandSameResult<Data, PaginationState<UIBaseItem, UIDataItem, Data>, PaginationState<UIBaseItem, UIDataItem, Data>>(
    LoadNextCommand(
        { loadNextAction(it.pageData!!.latestItem!!) },
        progressUiItem,
        errorToUIItem,
        strategy
    ),
    { _, result -> result },
    { this }
) {

    override fun shouldAddToPendingActions(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        null != dataState.pageData?.latestItem &&
                super.shouldAddToPendingActions(
                    dataState,
                    pendingActionCommands,
                    runningActionCommands
                )

    override fun shouldExecuteAction(
        dataState: PaginationState<UIBaseItem, UIDataItem, Data>,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        if (null == dataState.pageData?.latestItem) {
            pendingActionCommands.remove(this)
            false
        } else {
            super.shouldExecuteAction(dataState, pendingActionCommands, runningActionCommands)
        }
}