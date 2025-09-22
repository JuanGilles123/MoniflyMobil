package com.juangilles123.monifly.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object TransactionEventBus {
    private val _refreshRequest = MutableLiveData<Long>()
    val refreshRequest: LiveData<Long> = _refreshRequest

    fun postRefreshRequest() {
        val timestamp = System.currentTimeMillis()
        android.util.Log.d("TransactionEventBus", "Enviando refresh request: $timestamp")
        _refreshRequest.postValue(timestamp) // Usar postValue para llamadas desde hilos de fondo
    }
}