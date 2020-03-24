package io.scal.commandbasedarchitecture.pagination

data class PaginationState<
        UIBaseItem,
        UIDataItem : UIBaseItem,
        Data : PageData<UIDataItem>>
    (
    val pageData: Data? = null,
    val refreshStatus: UIBaseItem? = null,
    val nextPageLoadingStatus: UIBaseItem? = null
)

val <UIBaseItem, UIDataItem : UIBaseItem, Data : PageData<UIDataItem>> PaginationState<UIBaseItem, UIDataItem, Data>.dataAndNextPageLoadingStatus: List<UIBaseItem>?
    get() = combineUIItemAndNextPage(pageData?.itemsList, nextPageLoadingStatus)

fun <UIBaseItem, UIDataItem : UIBaseItem> combineUIItemAndNextPage(
    data: List<UIDataItem>?,
    nextPageLoadingStatus: UIBaseItem?
): List<UIBaseItem>? =
    if (null == nextPageLoadingStatus) data
    else (data ?: emptyList()).plus(nextPageLoadingStatus)