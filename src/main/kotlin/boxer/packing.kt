package boxer


class PackingSolver(
    private val track: Track, // грузовик
    private val orders: List<Order>, // все заказы
    alpha: Double = 0.8,  // поддерживающая площадь
    betta: Double = 0.5,  // длина достижимости
    gamma: Int = 0,  // кол-во попыток на груз
) {
    private var bestState = 0
    private var root = PackingNode(0, track, orders, alpha = alpha, betta = betta, gamma = gamma)
    private var solution = root

    fun solution(): List<Order> = root.solution()

    fun solve(seconds: Long = 20) {
        root.logger()
        val workTime = seconds * 1e9
        val start = System.nanoTime()
        var node: PackingNode? = root
        var i = 0
        println()
        println(track)

        while (root.state() != 2 && (System.nanoTime() - start) < workTime) {
            if (node == null || node.level() == orders.size) break

            if (!node.checkStep()) {
                node = node.goBack()  // откатываемся назад
                i--
                println("[main] go back")
                node?.logger()
                println()
            } else {
                node = node.goNext()  // берем следующую
                i++
                if (i > bestState && node != null) {
                    bestState = i
                    solution = node
                }
                println("[main] go next")
                node?.logger()
                println()
            }
        }

        println("Elapsed time: ${(System.nanoTime() - start) / 1e9} sec")
        solution.logger()
    }
}

class PackingNode(
    private val level: Int, // 0 - 0 box in track
    private val track: Track,
    private var freeOrders: List<Order>,
    private var currentSolution: List<Order> = emptyList(),
    private var extremePoints: List<Vec3> = listOf(Vec3(0.0, 0.0, 0.0)),
    private val prevNode: PackingNode? = null,
    private val alpha: Double = 0.8,  // поддерживающая площадь
    private val betta: Double = 0.5,  // длина достижимости
    private val gamma: Int = 0,  // кол-во попыток на груз
) {
    data class Chance(val coords: List<Vec3>, val order: Order) // представление следующего шага

    private var currentState = 0  // states from potential locations
    private var potentialLocations = emptyList<Chance>().toMutableList()
    private var nextNode: PackingNode? = null

    init {
        getLocations()
    }

    fun logger() {
        println("[node] State: $currentState/${potentialLocations.size}")
        println("[node] Current: $level:${freeOrders.size}")
        println("[node] Extreme points: ${extremePoints.size}")
    }

    /** Check next step */
    fun checkStep(): Boolean {
        if (gamma != 0 && currentState + 1 >= gamma) return false // или мы можем перебрать все или не все
        if (potentialLocations.size == 0) return false // там вообще что-то есть
        if (currentState == potentialLocations.size) return false // еще остались состояния
        if (level == freeOrders.size) return false // заказы закончились
        return true
    }

    fun state(): Int = currentState

    fun level(): Int = level

    fun solution(): List<Order> = currentSolution

    /** Берем следующую ноду */
    fun goNext(): PackingNode? {
        if (!checkStep()) return null
        val chance = potentialLocations[currentState]
        val newPoints = newExtremePoints(chance.coords, chance.order.box) // в теории здесь мы докидываем новые точки
        val newSolution = currentSolution + chance.order // мы только здесь кладем след коробку

        val newNode = PackingNode(
            level = level + 1,
            track = track,
            freeOrders = freeOrders,
            alpha = alpha,
            betta = betta,
            gamma = gamma,
            prevNode = this,
            extremePoints = newPoints,
            currentSolution = newSolution,
        )

        currentState++
        nextNode = newNode
        return nextNode
    }

    /** Откатываемся назад, дальше пути закончились */
    fun goBack(): PackingNode? {
        return prevNode
    }

    /** Вычисляем новые уникальные экстремальные точки для следующей ноды */
    private fun newExtremePoints(points: List<Vec3>, box: Box): List<Vec3> {
        val epSet = emptySet<Vec3>().toMutableSet()
        val maxX = box.x + box.length
        val minX = box.x
        val maxY = box.y + box.width
        val minY = box.y
        val maxZ = box.z + box.height
        val minZ = box.z

        for (pt in extremePoints) {
            // за коробкой, не включая верхнуюю и правую грань области
            if (pt.x in 0.0..box.x && (minY <= pt.y) and (pt.y < maxY) && (minZ <= pt.z) and (pt.z < maxZ)) continue
            // на левой грани, не включая верхнее и ближнее ребро
            if (pt.y == box.y && (minX <= pt.x) and (pt.x < maxX) && (minZ <= pt.z) and (pt.z < maxZ)) continue
            // на нижней грани, не включая ближнее и правое ребро
            if (pt.z == box.z && (minX <= pt.x) and (pt.x < maxX) && (minY <= pt.y) and (pt.y < maxY)) continue

            epSet.add(pt)
        }

        epSet.addAll(points)
        return epSet.toList()
    }

    /** Вычисляем новые потенциальные позиции для выставления новых коробок */
    private fun getLocations() {
        if (level == freeOrders.size) return
        val order = freeOrders[level]
        val x = order.box.length
        val y = order.box.width
        val z = order.box.height

        val states = if (order.vertical) {
            listOf(Vec3(x, y, z), Vec3(y, x, z), Vec3(z, y, z), Vec3(x, z, y), Vec3(y, z, x), Vec3(z, x, y))
        } else {
            listOf(Vec3(x, y, z), Vec3(y, z, z))
        }

        if (order.fragility) sortBackLeftUp() else sortBackLeftDown()
        for (point in extremePoints) checkExtremePoint(order, states, point)
    }

    /** Сортируем экстремальные точки - глубже-левее-выше - подходит для хрупкого */
    private fun sortBackLeftUp() {
        extremePoints.sortedWith(compareBy<Vec3> { it.x }.thenBy { it.y }.thenBy { -it.z })
    }

    /** Сортируем экстремальные точки - глубже-левее-ниже - подходит для всего остального */
    private fun sortBackLeftDown() {
        extremePoints.sortedWith(compareBy<Vec3> { it.x }.thenBy { it.y }.thenBy { it.z })
    }

    /** Каждую экстремальную точку проверяем на то, что она может быть потенциальной */
    private fun checkExtremePoint(order: Order, states: List<Vec3>, point: Vec3) {
        for (state in states) {
            val tmpBox = Box(point.x, point.y, point.z, state.x, state.y, state.z)
            val pos = checkPotentialSolution(tmpBox, order.fragility)
            pos?.let { potentialLocations.add(Chance(pos, order.copy(box = tmpBox))) }
        }
    }

    /** Проверяем на возможность установки: есть где, на чем стоять, реально достать */
    private fun checkPotentialSolution(box: Box, fragility: Boolean): List<Vec3>? {
        if (!inTrack(box)) return null

        val points = mutableListOf(
            // начальные точки из алгоритма, проекции на грани
            Vec3(0.0, box.y, box.z + box.height), // 0 x
            Vec3(box.x, 0.0, box.z + box.height),  // 1 y
            Vec3(0.0, box.y + box.width, box.z), // 2 x
            Vec3(box.x, box.y + box.width, 0.0), // 3 z
            Vec3(box.x + box.length, box.y, 0.0), // 4 z
            Vec3(box.x + box.length, 0.0, box.z), // 5 y
        )

        if (!fragility) {
            points.add(Vec3(box.x, box.y, box.z + box.height)) // TODO: добавить еще точек
            if (box.length > betta) points.add(Vec3(box.x + box.length - betta, box.y, box.z + box.height))
        }

        val boxSquare = box.width * box.length  // площадь пов-ти xy
        var supportArea = 0.0  // поддерживающая площадь под коробкой
        var maxX = 0.0  // ближайшая к нам сторона других коробок по проекции от (x, y, z)
        for (solution in currentSolution) {
            if (isOverlapping(box, solution.box)) return null
            maxX = maxProjection(maxX, solution.box, box)

            val area = if (box.z != 0.0) getSupportArea(box, solution.box) else -1.0
            if (area != -1.0) supportArea += area
            if (area != -1.0 && solution.fragility) return null  // заказ под ним хрупкий
            if (maxX - (box.x + box.length) > betta) return null  // не попали в длину достижимости
            if (!checkFrontArea(solution.box, box)) return null // перекрывает доступ для укладки

            updateExtremePoints(points, solution.box, box)  // обновляем экстремальные точки
        }

        if (box.z != 0.0 && supportArea / boxSquare < alpha) return null
        return points
    }

    /** Обновляем экстремальные точки
     * @param points 6 штук, подробнее далее
     * @param a об кого обновляемся (solution box)
     * @param b из-за чего обновляемся (current box)
     * @return
     * */
    private fun updateExtremePoints(points: List<Vec3>, a: Box, b: Box) {
        // 0, 1: (x, y, z + h) проекция на: zy (x++; (x - ?, y, z + h)), zx (y++; (x, y - ?, z + h))
        // 2, 3: (x, y + w, z) проекция на: zy (x++; (x - ?, y + w, z)), xy (z++; (x, y + w, z - ?))
        // 4, 5: (x + l, y, z) проекция на: xy (z++; (x + l, y, z - ?)), xz (y++; (x + l, y - ?, z))

        val maxX = a.x + a.length
        val minX = a.x
        val maxY = a.y + a.width
        val minY = a.y
        val maxZ = a.z + a.height
        val minZ = a.z

        // Рисуйте картинки, иначе не понять это счастье
        if (minX in 0.0..b.x) { // yz
            if (b.y in minY..maxY && b.z + b.height in minZ..maxZ) { // 0
                points[0].x = maxOf(points[0].x, minOf(maxX, b.x))
            }
            if (b.y + b.width in minX..maxX && b.z in minZ..maxZ) { // 2
                points[2].x = maxOf(points[2].x, minOf(maxX, b.x))
            }
        }

        if (minY in 0.0..b.y) {  // xz
            if (b.x in minX..maxX && b.z + b.height in minZ..maxZ) { // 1
                points[1].y = maxOf(points[1].y, minOf(maxY, b.y))
            }
            if (b.x + b.length in minX..maxX && b.z in minZ..maxZ) { // 5
                points[5].y = maxOf(points[5].y, minOf(maxY, b.y))
            }
        }

        if (minZ in 0.0..b.z) { // xy
            if (b.x in minX..maxX && b.y + b.width in minY..maxY) { // 3
                points[3].z = maxOf(points[3].z, minOf(maxZ, b.z))
            }
            if (b.x + b.length in minX..maxX && b.y in minY..maxY) { // 4
                points[4].z = maxOf(points[4].z, minOf(maxZ, b.z))
            }
        }
    }

    /** Check that box in track */
    private fun inTrack(box: Box): Boolean {
        val x = box.x + box.length <= track.length
        val y = box.y + box.width <= track.width
        val z = box.z + box.width <= track.height
        return x and y and z
    }

    /** Support area of box a for box b */
    private fun getSupportArea(a: Box, b: Box): Double {
        if (b.z + b.height != a.z) return 0.0
        val xOverlap = maxOf(0.0, minOf(a.x + a.length, b.x + b.length) - maxOf(a.x, b.x))
        val yOverlap = maxOf(0.0, minOf(a.y + a.width, b.y + b.width) - maxOf(a.y, b.y))
        return xOverlap * yOverlap
    }

    /** Проверяем на пересечение двух отрезков */
    private fun isOverlapping(minA: Double, maxA: Double, minB: Double, maxB: Double): Boolean {
        return (maxA > minB) and (maxB > minA)
    }

    /** Проверяем на пересечение двух кубов */
    private fun isOverlapping(a: Box, b: Box): Boolean {
        val x = isOverlapping(a.x, a.x + a.length, b.x, b.x + b.length)
        val y = isOverlapping(a.y, a.y + a.width, b.y, b.y + b.width)
        val z = isOverlapping(a.z, a.z + a.height, b.z, b.z + b.height)
        return x and y and z
    }

    /** А не перекрывает доступ к b по оси ZY? */
    private fun checkFrontArea(a: Box, b: Box): Boolean {
        if (a.x <= b.x + b.length) return true
        val y = isOverlapping(a.y, a.y + a.width, b.y, b.y + b.width)
        val z = isOverlapping(a.z, a.z + a.height, b.z, b.z + b.height)
        return y and z
    }

    /** Ищем самую длинную проекцию, по ближайшим стенкам по оси их, для проверки доставаемости */
    private fun maxProjection(x: Double, a: Box, b: Box): Double {
        return if (isOverlapping(a.y, a.y + a.width, b.y, b.y + b.width)) maxOf(a.x + a.length, x) else x
    }
}
