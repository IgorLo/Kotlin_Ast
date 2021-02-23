package ru.gdcn.igorlo.kotlinast.parser

import ru.gdcn.igorlo.kotlinast.drawing.Drawer
import java.io.File
import java.util.*


object AST {

    private var idCounter = 0

    data class AstNode(val type: String, val value: String, val children: MutableList<AstNode> = mutableListOf(), val id: Int = idCounter++) {
        override fun toString(): String = "$type\n$value"
    }

    fun parseAndDraw(inputPath: String) {
        val codeLines = File(inputPath).readLines()
            .filter {!it.isBlank()}
            .map {
                it.trim()
                    .replace("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*${'$'})".toRegex(), " ")
            }
        val astTree = buildAst(LinkedList(codeLines))
        Drawer().drawAST(astTree)
    }

    fun buildAst(codeLines: LinkedList<String>): AstNode {
        val root = initPackage(codeLines.removeFirst())
        while (codeLines.isNotEmpty()) {
            val nextLine = codeLines.first()
            root.children.add(when {
                nextLine.isImportLine() -> parseImport(codeLines)
                nextLine.isFunLine() -> parseFun(codeLines)
                else -> throw Exception("Invalid kotlin file")
            })
        }
        return root
    }

    private fun parseImport(codeLines: LinkedList<String>): AstNode =
        AstNode("import", codeLines.removeFirst().substringAfter("import "))

    private fun parseFun(codeLines: LinkedList<String>): AstNode {
        val line = codeLines.first()
        val root = AstNode( "function", line.substringAfter("fun ").substringBefore("(").trim())
        if (line.hasModifier("fun")) {
            root.children.add(parseModifier(line, "fun"))
        }
        val parameters = line
            .substringAfter("(")
            .substringBefore(")")
            .trim()
            .split(",")
        if (parameters.isNotEmpty()) {
            val parametersRootNode = AstNode("parameters", "")
            for (parameter in parameters) {
                parametersRootNode.children.add(parseParameters(parameter))
            }
            root.children.add(parametersRootNode)
        }
        if (line.hasFunType()) {
            root.children.add(AstNode("produced type", line.substringAfter(")").split("[={]".toRegex()).firstOrNull()?.substringAfter(":")?.trim() !!))
        }
        while (codeLines.isNotEmpty()) {
            if (codeLines.first().isBodyStartLine()) {
                root.children.add(parseBody(codeLines))
                break
            }
            codeLines.removeFirst()
        }
        return root
    }

    private fun parseBody(codeLines: LinkedList<String>, blockName: String = "body"): AstNode {
        val bodyRootNode = AstNode(blockName, "")
        if (codeLines.first().substringAfter("{").isBlank()) {
            codeLines.removeFirst()
        }
        while (!codeLines.first().isBodyEndLine()) {
            bodyRootNode.children.add(parseStatement(codeLines))
        }
        if (codeLines.first().substringBefore("}").isNotBlank()) {
            bodyRootNode.children.add(parseStatement(codeLines))
        }
        if (codeLines.first().substringAfter("}").isBlank()) {
            codeLines.removeFirst()
        }
        return bodyRootNode
    }

    private fun parseStatement(codeLines: LinkedList<String>): AstNode {
        return when {
            codeLines.first().isForLoopLine() -> parseForLoop(codeLines)
            codeLines.first().isIfStatementLine() -> parseIfStatement(codeLines)
            codeLines.first().isReturnStatementLine() -> parseReturnStatement(codeLines.removeFirst())
            codeLines.first().isAssignmentLine() -> parseAssignment(codeLines.removeFirst())
            codeLines.first().isWhileStatementLine() -> parseWhileStatement(codeLines)
            codeLines.first().isBreakOrContinueLine() -> parseExpression(codeLines.removeFirst())
            codeLines.first().isDoStatementLine() -> parseDoWhileStatement(codeLines)
            else -> throw Exception("Unsupported statement on line ${codeLines.first()}")
        }
    }

    private fun parseDoWhileStatement(codeLines: LinkedList<String>): AstNode {
        val rootDoWhileNode = AstNode("do while loop", "")
        while (codeLines.isNotEmpty()) {
            if (codeLines.first().isBodyStartLine()) {
                rootDoWhileNode.children.add(parseBody(codeLines))
                if (codeLines.first.isEndWhileStatementLine()) {
                    rootDoWhileNode.children.add(parseExpression(codeLines.removeFirst().substringAfter("(").substringBeforeLast(")")))
                }
                break
            }
            codeLines.removeFirst()
        }
        return rootDoWhileNode
    }

    private fun parseWhileStatement(codeLines: LinkedList<String>): AstNode {
        val rootForLoopNode = AstNode("while loop", "")
        rootForLoopNode.children.add(parseExpression(codeLines.first().substringAfter("(").substringBefore("{").substringBeforeLast(")")))
        while (codeLines.isNotEmpty()) {
            if (codeLines.first().isBodyStartLine()) {
                rootForLoopNode.children.add(parseBody(codeLines))
                break
            }
            codeLines.removeFirst()
        }
        return rootForLoopNode
    }

    private fun parseReturnStatement(line: String): AstNode {
        return AstNode("return statement", "", mutableListOf(
            parseExpression(line.substringAfter("return").trim())
        ))
    }

    private fun parseForLoop(codeLines: LinkedList<String>): AstNode {
        val rootForLoopNode = AstNode("for loop", "")
        rootForLoopNode.children.add(parseExpression(codeLines.first().substringAfter("(").substringBefore("{").substringBeforeLast(")")))
        while (codeLines.isNotEmpty()) {
            if (codeLines.first().isBodyStartLine()) {
                rootForLoopNode.children.add(parseBody(codeLines))
                break
            }
            codeLines.removeFirst()
        }
        return rootForLoopNode
    }

    private fun parseIfStatement(codeLines: LinkedList<String>): AstNode {
        val rootIfStatementNode = AstNode("if statement", "")
        rootIfStatementNode.children.add(parseExpression(codeLines.first().substringAfter("(").substringBefore("{").substringBeforeLast(")")))
        while (codeLines.isNotEmpty()) {
            if (codeLines.first().isBodyStartLine()) {
                rootIfStatementNode.children.add(parseBody(codeLines, "then statement"))
                if (codeLines.first.isElseStatementLine()) {
                    rootIfStatementNode.children.add(parseBody(codeLines, "else statement"))
                }
                break
            }
            codeLines.removeFirst()
        }
        return rootIfStatementNode
    }

    private fun parseAssignment(line: String): AstNode {
        val rootAssignmentNode = AstNode("assignment", "=")
        val expressionParts = line.split("([^<>!=]|)=[^=]".toRegex(), 2)
        val assignmentSign = line.substringAfter(expressionParts[0]).substringBefore(expressionParts[1]).trim()
        val rootVariableNode = AstNode("variable",
            if (expressionParts[0].isNewVariable()) "" else expressionParts[0].trim())
        if (expressionParts[0].isNewVariable()) {
            rootVariableNode.children.add(AstNode("vartype", expressionParts[0].trim().substringBefore(" ")))
            rootVariableNode.children.add(AstNode("name", expressionParts[0].trim().substringAfter(" ").trim().split(" |:|=|\\+=|-=|/=|\\*=".toRegex())[0].trim()))
            if (expressionParts[0].isParameterizedVariable()) {
                rootVariableNode.children.add(AstNode("type", expressionParts[0].substringAfter(":").trim().split(" |=|\\+=|-=|/=|\\*=".toRegex())[0].trim()))
            }
        }
        rootAssignmentNode.children.add(rootVariableNode)
        if (line.isNotAssignExpressionLine()) {
            rootAssignmentNode.children.add(parseExpression(expressionParts[1]))
        } else {
            val rootExpressionNode = AstNode("expression", assignmentSign.substringBefore("="))
            rootExpressionNode.children.add(AstNode("variable", expressionParts[0].trim()))
            rootExpressionNode.children.add(parseExpression(expressionParts[1]))
            rootAssignmentNode.children.add(rootExpressionNode)
        }
        return rootAssignmentNode
    }

    private fun parseExpression(expression: String): AstNode {
        return if (expression.isOperationExpression()) {
            val sign = expression.carveExpressionSign()
            val rootExpressionNode = AstNode("expression", sign)
            rootExpressionNode.children.add(parseVariable(expression.substringBefore(sign).trim()))
            rootExpressionNode.children.add(parseVariable(expression.substringAfter(sign).trim())   )
            rootExpressionNode
        } else {
            AstNode("statement", expression.trim())
        }
    }

    private fun parseVariable(variable: String): AstNode {
        return AstNode(if (variable.isVariableName()) "variable" else "statement", variable.trim())
    }

    private fun parseModifier(line: String, keyword: String): AstNode =
        AstNode("modifier", line.substringBefore(keyword).trim())

    private fun parseParameters(paramLine: String): AstNode =
        AstNode("parameter", "",
            mutableListOf(
                AstNode("name", paramLine.substringBefore(":").trim()),
                AstNode("type", paramLine.substringAfter(":").trim())
            ))

    private fun initPackage(line: String): AstNode =
        if (line.isPackageLine())
            AstNode("package", line.substringAfter("package "))
        else
            AstNode("without", "package")

}