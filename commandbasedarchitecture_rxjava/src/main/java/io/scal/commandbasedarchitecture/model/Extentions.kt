package io.scal.commandbasedarchitecture.model


fun <UIBaseItem, UIDataItem : UIBaseItem, Data : PageData<UIDataItem>> applyNewDataToOtherState(
    stateToUpdate: PaginationState<UIBaseItem, UIDataItem, Data>,
    newState: PaginationState<UIBaseItem, UIDataItem, Data>,
    fullUpdate: Boolean,
    itemsAreSame: (newItem: UIDataItem, oldItem: UIDataItem) -> Boolean
): PaginationState<UIBaseItem, UIDataItem, Data> {
    val newStatePageData = newState.pageData
    return when {
        fullUpdate -> newState
        null == newStatePageData -> stateToUpdate
        else -> {
            val stateToUpdatePageData = stateToUpdate.pageData
            if (stateToUpdatePageData?.itemsList == null) {
                stateToUpdate
            } else {
                val updatedItems = stateToUpdatePageData
                    .itemsList
                    .map { oldItem ->
                        newStatePageData.itemsList
                            .firstOrNull { itemsAreSame(it, oldItem) }
                            ?: oldItem
                    }
                @Suppress("UNCHECKED_CAST")
                if (updatedItems == stateToUpdatePageData.itemsList) stateToUpdate
                else stateToUpdate.copy(
                    pageData = stateToUpdatePageData.mapItems(updatedItems) as Data
                )
            }
        }
    }
}