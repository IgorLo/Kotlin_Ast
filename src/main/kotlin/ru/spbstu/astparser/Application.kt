package ru.spbstu.astparser

import ru.spbstu.astparser.parser.AstBuilder
import visualization.Drawer
import java.io.File
import java.util.*

const val INPUT_PATH = "./example.kt"

fun main(args: Array<String>) {
    val codeLines = File(INPUT_PATH).readLines()
        .filter {!it.isBlank()}
        .map {
            it.trim()
                .replace("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*${'$'})".toRegex(), " ")
        }
    val astTree = AstBuilder.buildAst(LinkedList(codeLines))
    Drawer().drawAST(astTree)
}
