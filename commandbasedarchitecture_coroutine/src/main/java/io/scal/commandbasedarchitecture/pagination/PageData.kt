package io.scal.commandbasedarchitecture.pagination

open class PageData<UIItem>(
    open val itemsList: List<UIItem>
) {
    open fun mapItems(newItems: List<UIItem>): PageData<UIItem> =
        PageData(newItems)

    open fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageData(itemsList.plus(result.itemsList))
}

open class PageDataWithNextPageNumber<UIItem>(
    data: List<UIItem>,
    val nextPageNumber: Int?
) : PageData<UIItem>(data) {

    override fun mapItems(newItems: List<UIItem>): PageDataWithNextPageNumber<UIItem> =
        PageDataWithNextPageNumber(newItems, nextPageNumber)

    override fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageDataWithNextPageNumber(
            itemsList.plus(result.itemsList),
            (result as PageDataWithNextPageNumber<*>).nextPageNumber
        )
}

open class PageDataWithLatestItem<UIItem>(
    data: List<UIItem>,
    val latestItem: UIItem?
) : PageData<UIItem>(data) {

    override fun mapItems(newItems: List<UIItem>): PageDataWithLatestItem<UIItem> =
        PageDataWithLatestItem(newItems, latestItem)

    @Suppress("UNCHECKED_CAST")
    override fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageDataWithLatestItem(
            itemsList.plus(result.itemsList),
            (result as PageDataWithLatestItem<*>).latestItem as? UIItem
        )
}