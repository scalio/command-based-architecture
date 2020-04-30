package io.scal.commandbasedarchitecture.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

/**
 * Base class for storing pagination result (lists)
 */
@Parcelize
open class PageData<UIItem>(
    open val itemsList: @RawValue List<UIItem>
) : Parcelable {
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
 * Usual simple pagination realization that is based on page number and page size.
 * This class will store this information.
 */
@Parcelize
open class PageDataWithNextPageNumber<UIItem>(
    override val itemsList: @RawValue List<UIItem>,
    open val nextPageNumber: Int?
) : PageData<UIItem>(itemsList) {

    override fun mapItems(newItems: List<UIItem>): PageDataWithNextPageNumber<UIItem> =
        PageDataWithNextPageNumber(
            newItems,
            nextPageNumber
        )

    override fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageDataWithNextPageNumber(
            itemsList.plus(result.itemsList),
            (result as PageDataWithNextPageNumber<*>).nextPageNumber
        )
}

/**
 * Simple pagination realization that is based on latest loaded item (usually its id or date).
 * This class will store this information.
 */
@Parcelize
open class PageDataWithLatestItem<UIItem>(
    override val itemsList: @RawValue List<UIItem>,
    open val latestItem: @RawValue UIItem?
) : PageData<UIItem>(itemsList) {

    override fun mapItems(newItems: List<UIItem>): PageDataWithLatestItem<UIItem> =
        PageDataWithLatestItem(
            newItems,
            latestItem
        )

    @Suppress("UNCHECKED_CAST")
    override fun <Data : PageData<UIItem>> plusNextPage(result: Data): PageData<UIItem> =
        PageDataWithLatestItem(
            itemsList.plus(result.itemsList),
            (result as PageDataWithLatestItem<*>).latestItem as? UIItem
        )
}