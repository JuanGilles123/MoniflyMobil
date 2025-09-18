package com.juangilles123.monifly.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object TransactionEventBus {
    private val _refreshRequest = MutableLiveData<Boolean>()
    val refreshRequest: LiveData<Boolean> = _refreshRequest

    fun postRefreshRequest() {
        _refreshRequest.value = true // O _refreshRequest.postValue(true) si se llama desde un hilo de fondo
    }
}