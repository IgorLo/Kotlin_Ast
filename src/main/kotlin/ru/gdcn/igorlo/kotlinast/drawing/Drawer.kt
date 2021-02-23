package ru.gdcn.igorlo.kotlinast.drawing

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.parse.Parser
import ru.gdcn.igorlo.kotlinast.drawing.Constants.ARROW
import ru.gdcn.igorlo.kotlinast.drawing.Constants.ASSIGN
import ru.gdcn.igorlo.kotlinast.drawing.Constants.CBF
import ru.gdcn.igorlo.kotlinast.drawing.Constants.CBS
import ru.gdcn.igorlo.kotlinast.drawing.Constants.DIGRAPH
import ru.gdcn.igorlo.kotlinast.drawing.Constants.LABEL
import ru.gdcn.igorlo.kotlinast.drawing.Constants.NEXT_LINE
import ru.gdcn.igorlo.kotlinast.drawing.Constants.OBF
import ru.gdcn.igorlo.kotlinast.drawing.Constants.OBS
import ru.gdcn.igorlo.kotlinast.drawing.Constants.SEMICOLON
import ru.gdcn.igorlo.kotlinast.drawing.Constants.SPACE
import ru.gdcn.igorlo.kotlinast.drawing.Constants.STRING
import ru.gdcn.igorlo.kotlinast.parser.AST.AstNode
import java.io.IOException
import java.io.File
import java.nio.file.Paths
import java.nio.file.Files


class Drawer(val dots : MutableList<Pair<Int, List<String>>> = ArrayList(), val links: MutableList<Pair<String, String>> = ArrayList()) {

    private val DOT_FILE_LOCATION = "./ast.dot"
    private val OUTPUT_LOCATION = "./ast.svg"

    fun drawAST(ast: AstNode) {
        this.prepareDrawingData(ast)
        this.generateASTDotFile()
        try {
            val graph = Parser().read(File(DOT_FILE_LOCATION))
            Graphviz
                .fromGraph(graph.setDirected(true))
                .width(2500)
                .height(2500)
                .render(Format.SVG)
                .toFile(File(OUTPUT_LOCATION))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun prepareDrawingData(node: AstNode) {
        val dotId = node.id
        val label = mutableListOf(node.type)
        label.add(node.value)
        if (node.children.isNotEmpty()) {
            node.children.forEach { child ->
                links.add(Pair(dotId.toString(), child.id.toString()))
                this.prepareDrawingData(child)
            }
        }
        dots.add(Pair(dotId, label))
    }

    private fun generateASTDotFile() {
        val sb = StringBuilder()
        sb.append(DIGRAPH)
            .append(OBF)
            .append(NEXT_LINE)
            .append(this.generateDeclareString())
            .append(this.generateLinksString())
            .append(NEXT_LINE)
            .append(CBF)
        try {
            Files.write(Paths.get(DOT_FILE_LOCATION), sb.toString().toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun generateDeclareString(): String {
        val sb = StringBuilder()

        dots.forEach { dot ->
            sb.append(dot.first)
                .append(OBS)
                .append(LABEL)
                .append(ASSIGN)
                .append(STRING)
                .append((SPACE + dot.second.joinToString("\n")))
                .append(STRING)
                .append(CBS)
                .append(SEMICOLON)
                .append(NEXT_LINE)
        }

        return sb.toString()
    }

    private fun generateLinksString(): String {
        val sb = StringBuilder()

        links.forEach { link ->
            sb.append(link.first)
                .append(SPACE)
                .append(ARROW)
                .append(SPACE)
                .append(link.second)
                .append(NEXT_LINE)
        }

        return sb.toString()
    }
}