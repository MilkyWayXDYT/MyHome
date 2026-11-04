
package com.robotpaw.myhome.signal

data class SignalData(
    val rawFlexor: Int,
    val rawExtensor: Int,
    val flexor: Int,
    val extensor: Int,
    val stateCode: String,
    val noiseEnabled: Boolean
)
