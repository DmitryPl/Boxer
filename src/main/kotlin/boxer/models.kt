package boxer

import com.fasterxml.jackson.annotation.JsonProperty

data class Box(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    val width: Double,
    val height: Double,
    val length: Double,
    )

data class InputData(val seconds: Long,
                     val alpha: Double,
                     val betta: Double,
                     val gamma: Int,
                     val track: Track,
                     val orders: List<Order>) {
}


data class Track(
    val value_capacity: Double,
    val lifting_capacity: Double,
    val width: Double,
    val height: Double,
    val length: Double,
    )

data class Order(
    val order_id: Int = 0,
    val box: Box,
    val weight: Double = 0.0,
    val value: Double = 0.0,
    val fragility: Boolean = false,
    val vertical: Boolean = false
    )

data class Vec3(var x: Double, var y: Double, var z: Double) : Comparable<Vec3> {
    override fun compareTo(other: Vec3): Int = when {
        x != other.x -> (other.x - x).toInt()
        y != other.y -> (other.y - y).toInt()
        z != other.z -> (other.z - z).toInt()
        else -> 0
    }
}
