import boxer.*

fun main() {
    val track = smallTrack()
    val orders = randomOrders(100, track)

    val solver = PackingSolver(track, orders, gamma = 3)
    solver.solve()
    println("\n\nSolved box: ${solver.solution().size}")
}
