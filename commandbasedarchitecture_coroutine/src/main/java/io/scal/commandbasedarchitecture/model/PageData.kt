package io.scal.commandbasedarchitecture.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.Parcelize

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

    companion object : Parceler<PageData<Any?>> {

        override fun PageData<Any?>.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(itemsList.size)
            itemsList.forEach { it.writeToParcel(parcel) }
        }

        override fun create(parcel: Parcel): PageData<Any?> {
            val list = mutableListOf<Any?>()
            val count = parcel.readInt()
            repeat(count) { list.add(parcel.readNullOrValue()) }

            return PageData(list)
        }
    }
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

    companion object : Parceler<PageDataWithNextPageNumber<Any?>> {

        override fun PageDataWithNextPageNumber<Any?>.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(itemsList.size)
            itemsList.forEach { parcel.writeValue(it) }
            nextPageNumber.writeToParcel(parcel)
        }

        override fun create(parcel: Parcel): PageDataWithNextPageNumber<Any?> {
            val list = mutableListOf<Any?>()
            val count = parcel.readInt()
            repeat(count) { list.add(parcel.readNullOrValue()) }

            return PageDataWithNextPageNumber(
                list,
                parcel.readNullOrValue() as? Int
            )
        }
    }
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

    companion object : Parceler<PageDataWithLatestItem<Any?>> {

        override fun PageDataWithLatestItem<Any?>.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(itemsList.size)
            itemsList.forEach { parcel.writeValue(it) }
            latestItem.writeToParcel(parcel)
        }

        override fun create(parcel: Parcel): PageDataWithLatestItem<Any?> {
            val list = mutableListOf<Any?>()
            val count = parcel.readInt()
            repeat(count) { list.add(parcel.readNullOrValue()) }

            return PageDataWithLatestItem(
                list,
                parcel.readNullOrValue()
            )
        }
    }
}