package ru.gdcn.igorlo


public fun main(args: Array<String>): Int {
    val bananas = 10
    val eggs = 10
    val bread = 0

    if (eggs > 0) {
        bread += 1
    } else  {
        bread -= 1
        bananas -= 0
    }

    bread *= 2
    return bread
}

public fun fib(args: Array<String>): Int {
    val n = 10
    var t1 = 0
    var t2 = 1

    for (i in 1..n) {
        val sum = t1 + t2
        t1 = t2
        t2 = sum
    }

    return t2;
}
