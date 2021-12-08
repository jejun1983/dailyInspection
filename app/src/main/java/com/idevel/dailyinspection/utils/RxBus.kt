package com.idevel.dailyinspection.utils

import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject


object RxBus {
    private val publisher = PublishSubject.create<Any>()

    fun Init() {
        RxJavaPlugins.setErrorHandler {

        }
    }

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class MessageEvent {
    enum class MessageType {
        MT_NONE,
        MT_FLASH_ON,
        MT_FLASH_OFF
    }

    var eventType: MessageType? = MessageType.MT_NONE
    var var0: Int = 0

    constructor()

    constructor(mt: MessageType) {
        eventType = mt
    }

    constructor(mt: MessageType, v: Int) {
        eventType = mt
        var0 = v
    }
}