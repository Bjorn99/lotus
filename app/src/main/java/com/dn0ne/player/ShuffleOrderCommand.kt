package com.dn0ne.player

const val SET_SHUFFLE_ORDER_ACTION = "com.dn0ne.player.SET_SHUFFLE_ORDER"
const val SHUFFLE_ORDER_KEY = "order"

fun validShuffleOrder(order: IntArray?, itemCount: Int): IntArray? {
    if (order == null || order.size != itemCount) return null
    val seen = BooleanArray(itemCount)
    for (index in order) {
        if (index !in 0 until itemCount || seen[index]) return null
        seen[index] = true
    }
    return order
}

enum class ShuffleOrderAction { APPLY, DEFER, REJECT }

fun shuffleOrderAction(order: IntArray?, itemCount: Int): ShuffleOrderAction = when {
    order == null -> ShuffleOrderAction.REJECT
    validShuffleOrder(order, itemCount) != null -> ShuffleOrderAction.APPLY
    validShuffleOrder(order, order.size) != null -> ShuffleOrderAction.DEFER
    else -> ShuffleOrderAction.REJECT
}
