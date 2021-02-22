package ru.spbstu.astparser

import ru.spbstu.astparser.parser.AstBuilder

public fun main (args: Array<String>) : String {
    val scanner: Int = 5 + 5
    val scan = scanner + 1

    while (scan < 15) {
        scan += 1
        break
    }

    do {
        scan += 1
    } while (true)

    if (scan > 10) {
        for (line in lines) {
            var str = 'Hello World'
        }
        val str = '123'
    }
    else {
        val str = '123'
        var str = 'Hello World'
    }
    scanner += 24   
    
    return scan * 2

}
