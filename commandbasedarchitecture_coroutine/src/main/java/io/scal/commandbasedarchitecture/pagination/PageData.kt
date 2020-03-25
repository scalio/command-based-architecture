package io.scal.commandbasedarchitecture.pagination

/**
 * Base class for storing pagination result (lists)
 */
open class PageData<UIItem>(
    open val itemsList: List<UIItem>
) {
    /**
     * Should return the same class and same data but with updated items
     */
    open fun mapItems(newItems: List<UIItem>): PageData<UIItem> =
        PageData(newItems)

    /**
     * Should return the same class and add one more page result to existing
     */
    open fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageData(itemsList.plus(result.itemsList))
}

/**
 * Usual simple pagination realization that based on page number and page size.
 * This class will store this information.
 */
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

/**
 * Simple pagination realization that based on latest loaded item (usually its id or date).
 * This class will store this information.
 */
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