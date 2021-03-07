import boxer.InputData
import boxer.Order
import boxer.PackingSolver
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


fun main(args: Array<String>) {
    val inputData = getInputData("C:\\Users\\yakov\\IdeaProjects\\Boxer\\src\\main\\input.txt")
    val solver = PackingSolver(inputData!!)
    solver.solve(inputData)
    writeOutputData(solver.solution(), "C:\\Users\\yakov\\IdeaProjects\\Boxer\\src\\main\\output.txt")
}

fun writeOutputData(orders: List<Order>, outputFilePath: String) {
    val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper();
    val ordersString: String = objectMapper.writeValueAsString(orders)
    try {
        val fileWriter = FileWriter(File(outputFilePath))
        val bufferedWriter = BufferedWriter(fileWriter)
        bufferedWriter.write("")
        bufferedWriter.append(ordersString)
        bufferedWriter.close()
        fileWriter.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun getInputData(inputFilePath: String): InputData? {
    val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
    var inputData: InputData? = null
    try {
        inputData = objectMapper.readValue<InputData>(File(inputFilePath))
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return inputData
}
