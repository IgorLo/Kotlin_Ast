package ru.spbstu.astparser.parser

data class AstNode(val id: Int, val type: String, val value: String, val children: MutableList<AstNode>) {
    override fun toString(): String = "$type\n$value"
}