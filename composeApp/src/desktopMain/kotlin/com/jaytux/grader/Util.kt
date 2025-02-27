package com.jaytux.grader

fun String.maxN(n: Int): String {
    return if (this.length > n) {
        this.substring(0, n - 3) + "..."
    } else {
        this
    }
}