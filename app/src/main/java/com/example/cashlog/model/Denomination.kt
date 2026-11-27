package com.example.cashlog.model

enum class Denomination(val faceValue: Int, val label: String) {
    W50000(50000, "오만원"),
    W10000(10000, "일만원"),
    W5000(5000, "오천원"),
    W1000(1000, "천원"),
    C500(500, "오백원"),
    C100(100, "백원"),
    C10_OLD(10, "구십"),
    C50(50, "오십원"),
    C10_NEW(10, "신십")
}
