
package com.robotpaw.myhome.signal

/**
 * @param timestampMs монотонное время кадра (~20мс)
 */
data class SignalData(
    val timestampMs: Long,
    val flexor: Int,
    val extensor: Int
)
