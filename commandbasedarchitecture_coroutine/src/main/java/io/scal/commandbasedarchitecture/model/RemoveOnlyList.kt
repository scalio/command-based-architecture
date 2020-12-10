package io.scal.commandbasedarchitecture.model

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

fun <E> MutableList<E>.toRemoveOnlyList(): RemoveOnlyList<E> {
    val mutableListClass = javaClass
    val javaClass = RemoveOnlyList::class.java
    val mutableListOpHandler = InvocationHandler { _, method, args: Array<Any>? ->
        val mutableListMethod = mutableListClass.getMethod(method.name, *method.parameterTypes)
        if (null == args) mutableListMethod.invoke(this)
        else mutableListMethod.invoke(this, *args)
    }
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(javaClass), mutableListOpHandler
    ) as RemoveOnlyList<E>
}

interface RemoveOnlyList<out E> : List<E> {

    fun remove(element: @UnsafeVariance E): Boolean

    fun removeAll(elements: Collection<@UnsafeVariance E>): Boolean

    fun clear()

    fun removeAt(index: Int): E
}

fun <T> RemoveOnlyList<T>.removeAll(predicate: (T) -> Boolean): Boolean {
    val itemsToRemove = mutableListOf<T>()
    forEach { if (!predicate(it)) itemsToRemove.add(it) }
    if (itemsToRemove.isEmpty()) return false
    removeAll(itemsToRemove)
    return true
}