package io.scal.commandbasedarchitecture

import io.scal.commandbasedarchitecture.model.RemoveOnlyList

/**
 * Command that is useful for reuse of existing commands but with different one to one data structures.
 */
open class DataConvertCommand<OuterResult, OuterData : Any?, InnerResult, InnerData : Any?>(
    protected val innerCommand: ActionCommand<InnerResult, InnerData>,
    protected val innerToOuterData: (outerData: OuterData, newInnerData: InnerData) -> OuterData,
    protected val outerToInnerData: OuterData.() -> InnerData,
    protected val innerToOuterResult: InnerResult.() -> OuterResult,
    protected val outerToInnerResult: OuterResult.() -> InnerResult
) : ActionCommand<OuterResult, OuterData>() {

    override val strategy: ExecutionStrategy? = innerCommand.strategy

    override fun onCommandWasAdded(dataState: OuterData): OuterData {
        val innerData = dataState.outerToInnerData()
        val newInnerData = innerCommand.onCommandWasAdded(innerData)
        return returnOldOrNewData(dataState, innerData, newInnerData)
    }

    override fun onExecuteStarting(dataState: OuterData): OuterData {
        val innerData = dataState.outerToInnerData()
        val newInnerData = innerCommand.onExecuteStarting(innerData)
        return returnOldOrNewData(dataState, innerData, newInnerData)
    }

    override suspend fun executeCommand(dataState: OuterData): OuterResult =
        executeCommandImpl(dataState).let { it.innerToOuterResult() }

    protected open suspend fun executeCommandImpl(dataState: OuterData): InnerResult =
        innerCommand.executeCommand(dataState.outerToInnerData())

    override fun onExecuteSuccess(dataState: OuterData, result: OuterResult): OuterData {
        val innerData = dataState.outerToInnerData()
        val newInnerData = innerCommand.onExecuteSuccess(innerData, result.outerToInnerResult())
        return returnOldOrNewData(dataState, innerData, newInnerData)
    }

    override fun onExecuteFail(dataState: OuterData, error: Throwable): OuterData {
        val innerData = dataState.outerToInnerData()
        val newInnerData = innerCommand.onExecuteFail(innerData, error)
        return returnOldOrNewData(dataState, innerData, newInnerData)
    }

    override fun onExecuteFinished(dataState: OuterData): OuterData {
        val innerData = dataState.outerToInnerData()
        val newInnerData = innerCommand.onExecuteFinished(innerData)
        return returnOldOrNewData(dataState, innerData, newInnerData)
    }

    protected open fun returnOldOrNewData(
        oldOuterData: OuterData,
        oldInnerData: InnerData,
        newInnerData: InnerData
    ): OuterData =
        if (oldInnerData == newInnerData) oldOuterData
        else innerToOuterData(oldOuterData, newInnerData)

    override fun shouldAddToPendingActions(
        dataState: OuterData,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        innerCommand.shouldAddToPendingActions(
            dataState.outerToInnerData(),
            pendingActionCommands,
            runningActionCommands
        )

    override fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean =
        innerCommand.shouldBlockOtherTask(pendingActionCommand)

    override fun shouldExecuteAction(
        dataState: OuterData,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        innerCommand.shouldExecuteAction(
            dataState.outerToInnerData(),
            pendingActionCommands,
            runningActionCommands
        )

    override fun toString(): String {
        return "DataConvertCommand. InnerCommand: $innerCommand"
    }
}

/**
 * Command that is useful for reuse of existing commands but with different one to one data structures.
 */
open class DataConvertCommandSameResult<Result, OuterData : Any?, InnerData : Any?>(
    innerCommand: ActionCommand<Result, InnerData>,
    innerToOuterData: (outerData: OuterData, newInnerData: InnerData) -> OuterData,
    outerToInnerData: OuterData.() -> InnerData
) : DataConvertCommand<Result, OuterData, Result, InnerData>(
    innerCommand,
    innerToOuterData,
    outerToInnerData,
    { this },
    { this }
) {

    override fun toString(): String {
        return "DataConvertCommandSameResult. InnerCommand: $innerCommand"
    }
}