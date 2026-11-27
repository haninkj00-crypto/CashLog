package com.example.cashlog.util

import android.content.Context
import android.net.Uri
import com.example.cashlog.model.CashTransaction
import com.example.cashlog.model.Denomination
import com.example.cashlog.model.TransactionType
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    fun generateCsv(transactions: List<CashTransaction>): String {
        val sb = StringBuilder()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("timestamp,type,is_adjustment,")
        for (denom in Denomination.values()) {
            sb.append(denom.name).append(",")
        }
        sb.append("total,memo,lat,lon\n")

        val sorted = transactions.sortedByDescending { it.timestamp }
        for (tx in sorted) {
            sb.append(dateFormatter.format(Date(tx.timestamp))).append(",")
            sb.append(tx.type.name).append(",")
            
            val isAdjustment = tx.type == TransactionType.ADJUSTMENT || tx.type == TransactionType.INITIAL
            sb.append(isAdjustment).append(",")
            
            for (denom in Denomination.values()) {
                sb.append(tx.counts[denom] ?: 0).append(",")
            }
            
            sb.append(tx.total).append(",")
            sb.append("\"").append(tx.memo ?: "").append("\",")
            sb.append(tx.lat ?: "").append(",")
            sb.append(tx.lon ?: "").append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV content to a URI with the specified charset.
     * UTF-8 includes a BOM for Excel compatibility.
     * CP949 uses '?' as a fallback for unencodable characters.
     */
    fun writeToUri(context: Context, uri: Uri, content: String, charset: Charset) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                if (charset.name().equals("UTF-8", ignoreCase = true)) {
                    // Write UTF-8 BOM: EF BB BF
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                }
                // toByteArray(charset) uses a CharsetEncoder which defaults to '?' for unencodable chars
                outputStream.write(content.toByteArray(charset))
            }
        } catch (e: Exception) {
            // Log or handle error appropriately in a real app
        }
    }
}
