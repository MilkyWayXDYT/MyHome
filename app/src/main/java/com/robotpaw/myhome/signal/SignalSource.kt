package com.robotpaw.myhome.signal

import kotlinx.coroutines.flow.StateFlow

interface SignalSource {
    val frames: StateFlow<SignalData>
}