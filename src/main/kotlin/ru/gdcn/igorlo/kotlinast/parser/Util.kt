package ru.gdcn.igorlo.kotlinast.parser


fun String.isPackageLine(): Boolean = contains("^package\\s".toRegex())
fun String.isFunLine(): Boolean = contains("(^|[A-Za-z]+ )fun [A-Za-z]+([ (])".toRegex())
fun String.isImportLine(): Boolean = contains("^import [A-Za-z]+".toRegex())
fun String.hasModifier(keyword: String): Boolean = trim().contains("^[A-Za-z]+ $keyword".toRegex())
fun String.hasFunType(): Boolean = substringAfter(")").split("[={]".toRegex()).firstOrNull()?.contains(":") ?: false
fun String.isBodyStartLine(): Boolean = contains("\\{".toRegex())
fun String.isBodyEndLine(): Boolean = contains("}".toRegex())
fun String.isForLoopLine(): Boolean = contains("for".toRegex())
fun String.isIfStatementLine(): Boolean = contains("if".toRegex())
fun String.isElseStatementLine(): Boolean = contains("else".toRegex())
fun String.isWhileStatementLine(): Boolean = contains("while ".toRegex())
fun String.isEndWhileStatementLine(): Boolean = contains("}.*while".toRegex())
fun String.isDoStatementLine(): Boolean = contains("do".toRegex())
fun String.isAssignmentLine(): Boolean = contains(" = |\\+=|-=|\\*=|/=".toRegex())
fun String.isReturnStatementLine(): Boolean = contains("return".toRegex())
fun String.isNotAssignExpressionLine(): Boolean = contains(".*([^<>!=*/+\\-%])=([^=]|)".toRegex())
fun String.isParameterizedVariable(): Boolean = contains("(val|var).* .+.*:.*[A-Za-z]+".toRegex())
fun String.isNewVariable(): Boolean =  trim().contains("(val|var).* .+.*".toRegex())
fun String.isOperationExpression(): Boolean = contains("\\+\\+|--|\\+|-|\\*|\\\\|%|\\.\\.|!in|in|==|!=|>=|<=|>|<".toRegex())
fun String.isVariableName(): Boolean = trim().contains("^[a-zA-Z_\$][a-zA-Z_\$0-9]*\$".toRegex())
fun String.isBreakOrContinueLine(): Boolean = contains("(continue|break)".toRegex())


fun String.carveExpressionSign(): String = "\\+\\+|\\-\\-|\\+|\\-|\\*|\\\\|\\%|\\.\\.|\\!in|in|\\=\\=|\\!\\=|\\>\\=|\\<\\=|\\>|\\<".toRegex().find(this)?.value !!