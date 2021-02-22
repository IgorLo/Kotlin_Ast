package ru.spbstu.astparser.parser

import java.util.*

object AstBuilder {
    private var idCounter = 0

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
        AstNode(idCounter++, "import", codeLines.removeFirst().substringAfter("import "), mutableListOf())

    private fun parseFun(codeLines: LinkedList<String>): AstNode {
        val line = codeLines.first()
        val root = AstNode(idCounter++, "function", line.substringAfter("fun ").substringBefore("(").trim(), mutableListOf())
        if (line.hasModifier("fun")) {
            root.children.add(parseModifier(line, "fun"))
        }
        val parameters = line
            .substringAfter("(")
            .substringBefore(")")
            .trim()
            .split(",")
        if (parameters.isNotEmpty()) {
            val parametersRootNode = AstNode(idCounter++, "parameters", "", mutableListOf())
            for (parameter in parameters) {
                parametersRootNode.children.add(parseParameters(parameter))
            }
            root.children.add(parametersRootNode)
        }
        if (line.hasFunType()) {
            root.children.add(AstNode(idCounter++, "produced type", line.substringAfter(")").split("[={]".toRegex()).firstOrNull()?.substringAfter(":")?.trim() !!, mutableListOf()))
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
        val bodyRootNode = AstNode(idCounter++, blockName, "", mutableListOf())
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
        val rootDoWhileNode = AstNode(idCounter++, "do while loop", "", mutableListOf())
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
        val rootForLoopNode = AstNode(idCounter++, "while loop", "", mutableListOf())
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
        return AstNode(idCounter++, "return statement", "", mutableListOf(
            parseExpression(line.substringAfter("return").trim())
        ))
    }

    private fun parseForLoop(codeLines: LinkedList<String>): AstNode {
        val rootForLoopNode = AstNode(idCounter++, "for loop", "", mutableListOf())
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
        val rootIfStatementNode = AstNode(idCounter++, "if statement", "", mutableListOf())
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
        val rootAssignmentNode = AstNode(idCounter++, "assignment", "=", mutableListOf())
        val expressionParts = line.split("([^<>!=]|)=[^=]".toRegex(), 2)
        val assignmentSign = line.substringAfter(expressionParts[0]).substringBefore(expressionParts[1]).trim()
        val rootVariableNode = AstNode(idCounter++, "variable",
            if (expressionParts[0].isNewVariable()) "" else expressionParts[0].trim(), mutableListOf())
        if (expressionParts[0].isNewVariable()) {
            rootVariableNode.children.add(AstNode(idCounter++, "vartype", expressionParts[0].trim().substringBefore(" "), mutableListOf()))
            rootVariableNode.children.add(AstNode(idCounter++, "name", expressionParts[0].trim().substringAfter(" ").trim().split(" |:|=|\\+=|-=|/=|\\*=".toRegex())[0].trim(), mutableListOf()))
            if (expressionParts[0].isParameterizedVariable()) {
                rootVariableNode.children.add(AstNode(idCounter++, "type", expressionParts[0].substringAfter(":").trim().split(" |=|\\+=|-=|/=|\\*=".toRegex())[0].trim(), mutableListOf()))
            }
        }
        rootAssignmentNode.children.add(rootVariableNode)
        if (line.isNotAssignExpressionLine()) {
            rootAssignmentNode.children.add(parseExpression(expressionParts[1]))
        } else {
            val rootExpressionNode = AstNode(idCounter++, "expression", assignmentSign.substringBefore("="), mutableListOf())
            rootExpressionNode.children.add(AstNode(idCounter++, "variable", expressionParts[0].trim(), mutableListOf()))
            rootExpressionNode.children.add(parseExpression(expressionParts[1]))
            rootAssignmentNode.children.add(rootExpressionNode)
        }
        return rootAssignmentNode
    }

    private fun parseExpression(expression: String): AstNode {
        return if (expression.isOperationExpression()) {
            val sign = expression.carveExpressionSign()
            val rootExpressionNode = AstNode(idCounter++, "expression", sign, mutableListOf())
            rootExpressionNode.children.add(parseVariable(expression.substringBefore(sign).trim()))
            rootExpressionNode.children.add(parseVariable(expression.substringAfter(sign).trim())   )
            rootExpressionNode
        } else {
            AstNode(idCounter++, "statement", expression.trim(), mutableListOf())
        }
    }

    private fun parseVariable(variable: String): AstNode {
        return AstNode(idCounter++, if (variable.isVariableName()) "variable" else "statement", variable.trim(), mutableListOf())
    }

    private fun parseModifier(line: String, keyword: String): AstNode =
        AstNode(idCounter++, "modifier", line.substringBefore(keyword).trim(), mutableListOf())

    private fun parseParameters(paramLine: String): AstNode =
        AstNode(idCounter++, "parameter", "",
            mutableListOf(AstNode(idCounter++, "name", paramLine.substringBefore(":").trim(), mutableListOf()),
                AstNode(idCounter++, "type", paramLine.substringAfter(":").trim(), mutableListOf())))

    private fun initPackage(line: String): AstNode =
        if (line.isPackageLine())
            AstNode(idCounter++, "package", line.substringAfter("package "), mutableListOf())
        else
            AstNode(idCounter++, "without", "package", mutableListOf())
}