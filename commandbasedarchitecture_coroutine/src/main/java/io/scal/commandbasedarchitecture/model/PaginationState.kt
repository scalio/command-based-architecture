package io.scal.commandbasedarchitecture.model

/**
 * Model class that store: pageData, refresh state and next page loading state
 */
data class PaginationState<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        Data : PageData<UIDataItem>>
    (
    val pageData: Data? = null,
    val refreshStatus: UIBaseItem? = null,
    val nextPageLoadingStatus: UIBaseItem? = null
)

/**
 * Combines page data items with next page loading state item. Useful for RecyclerView adapters.
 */
val <UIBaseItem, UIDataItem : UIBaseItem, Data : PageData<UIDataItem>> PaginationState<UIBaseItem, UIDataItem, Data>.dataAndNextPageLoadingStatus: List<UIBaseItem>?
    get() = combineUIItemAndNextPage(
        pageData?.itemsList,
        nextPageLoadingStatus
    )

/**
 * Combines page data items with next page loading state item. Useful for RecyclerView adapters.
 */
fun <UIBaseItem, UIDataItem : UIBaseItem> combineUIItemAndNextPage(
    data: List<UIDataItem>?,
    nextPageLoadingStatus: UIBaseItem?
): List<UIBaseItem>? =
    if (null == nextPageLoadingStatus) data
    else (data ?: emptyList()).plus(nextPageLoadingStatus)