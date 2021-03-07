package boxer

import kotlin.random.Random

fun randomOrder(track: Track, order_id: Int = 0, fragility: Boolean = false): Order {
    fun randSize(size: Double): Double = Random.nextDouble(10.0, size * 1e3) / 1e3

    val width = track.width / 2
    val length = track.length / 2
    val height = track.height / 2

    val boxWidth = randSize(width)
    val boxHeight = randSize(height)
    val boxLength = randSize(length)
    val boxValue = boxWidth * boxHeight * boxLength  // m3

    val boxDensity = Random.nextInt(500, 1200) // kg/m3
    val boxWeight = boxDensity * boxValue  // kg

    return Order(
        order_id,
        Box(width = boxWidth, height = boxHeight, length = boxLength),
        boxWeight,
        boxValue,
        fragility,
        false
    )
}

fun randomOrders(n: Int, track: Track): List<Order> {
    return List(n) { i -> randomOrder(track, i) }
}
