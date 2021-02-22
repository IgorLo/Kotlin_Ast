package ru.gdcn.igorlo.kotlinast

import ru.gdcn.igorlo.kotlinast.parser.AstBuilder
import ru.gdcn.igorlo.kotlinast.drawing.Drawer
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
