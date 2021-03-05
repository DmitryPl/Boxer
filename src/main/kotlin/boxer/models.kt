package boxer

data class Box(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    val width: Double,
    val height: Double,
    val length: Double,
)

data class Track(
    val valueCapacity: Double,
    val liftingCapacity: Double,
    val width: Double,
    val height: Double,
    val length: Double,
)

data class Order(
    val order_id: Int,
    val box: Box,
    val weight: Double,
    val value: Double,
    val fragility: Boolean,
    val vertical: Boolean,
)

data class Vec3(var x: Double, var y: Double, var z: Double) : Comparable<Vec3> {
    override fun compareTo(other: Vec3): Int = when {
        x != other.x -> (other.x - x).toInt()
        y != other.y -> (other.y - y).toInt()
        z != other.z -> (other.z - z).toInt()
        else -> 0
    }
}
