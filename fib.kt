package ru.gdcn.igorlo


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
