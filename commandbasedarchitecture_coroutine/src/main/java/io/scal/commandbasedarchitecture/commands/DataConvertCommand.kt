package io.scal.commandbasedarchitecture.commands

import io.scal.commandbasedarchitecture.model.RemoveOnlyList

/**
 * Command that is useful for reuse of existing commands but with different one to one data structures.
 */
open class DataConvertCommand<OuterResult, OuterData : Any?, InnerResult, InnerData : Any?>(
    protected val innerCommand: Command<InnerResult, InnerData>,
    protected val innerToOuterData: (outerData: OuterData, newInnerData: InnerData) -> OuterData,
    protected val outerToInnerData: OuterData.() -> InnerData,
    protected val innerToOuterResult: InnerResult.() -> OuterResult,
    protected val outerToInnerResult: OuterResult.() -> InnerResult
) : Command<OuterResult, OuterData>(innerCommand.strategy) {

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
        executeCommandImpl(dataState).innerToOuterResult()

    override suspend fun executeCommandWithSideEffects(
        getCurrentDataState: () -> OuterData,
        updateCurrentDataState: (OuterData) -> Unit
    ): OuterResult =
        executeCommandWithSideEffectsImpl(getCurrentDataState, updateCurrentDataState)
            .innerToOuterResult()

    protected open suspend fun executeCommandImpl(dataState: OuterData): InnerResult =
        innerCommand.executeCommand(dataState.outerToInnerData())

    protected open suspend fun executeCommandWithSideEffectsImpl(
        getCurrentDataState: () -> OuterData,
        updateCurrentDataState: (OuterData) -> Unit
    ): InnerResult =
        innerCommand.executeCommandWithSideEffects(
            { getCurrentDataState().outerToInnerData() },
            { updateCurrentDataState(innerToOuterData(getCurrentDataState(), it)) }
        )

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

    override fun shouldAddToPendingCommands(
        pendingActionCommands: List<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        innerCommand.shouldAddToPendingCommands(
            pendingActionCommands,
            runningActionCommands
        )

    override fun shouldBlockOtherCommand(pendingActionCommand: Command<*, *>): Boolean =
        innerCommand.shouldBlockOtherCommand(pendingActionCommand)

    override fun shouldExecute(
        pendingActionCommands: List<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        innerCommand.shouldExecute(
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
    innerCommand: Command<Result, InnerData>,
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