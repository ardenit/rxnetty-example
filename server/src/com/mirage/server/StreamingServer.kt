
package com.mirage.server

import io.netty.buffer.ByteBuf
import io.reactivex.netty.channel.Connection
import io.reactivex.netty.protocol.tcp.server.TcpServer
import io.reactivex.netty.util.StringLineDecoder
import me.khol.reactive.subjects.EventSubject
import rx.Observable
import rx.Subscription
import rx.observers.Subscribers
import rx.subjects.PublishSubject
import java.util.Collections

class EventSubjectAdapter<T>(private val subj: EventSubject<T> = EventSubject.create<T>()) : rx.subjects.Subject<T, T> ({

    it.add(object : Subscription {
        val subs = subj.subscribe(it::onNext, it::onError, it::onCompleted)

        override fun isUnsubscribed(): Boolean = subs.isDisposed
        override fun unsubscribe() = subs.dispose()
    })

}) {

    override fun hasObservers(): Boolean = subj.hasObservers()

    override fun onError(e: Throwable?) = subj.onError(e ?: Exception("WTF"))

    override fun onCompleted() = subj.onComplete()

    override fun onNext(t: T) = subj.onNext(t)
}


class Player(private val connection: Connection<ByteBuf, ByteBuf>) {

    private val inMsgs = EventSubjectAdapter<String>()

    private val outMsgs = PublishSubject.create<String>()

    fun init() : Observable<Void> {
        connection
            .addChannelHandlerFirst<String, ByteBuf>("string-decoder", StringLineDecoder())
            .input
            .subscribe(
                {inMsgs.onNext(it)},
                { e ->
                    println("Connection closed")
                    connection.close()
                    inMsgs.onError(e)
                },
                {connection.close()}
            )
        return connection.writeStringAndFlushOnEach(outMsgs)
    }

    /**
     * msg should end with '\n'
     */
    fun sendMessage(msg: String) {
        outMsgs.onNext(msg)
    }

    val observable: Observable<String>
        get() = inMsgs

}

class Room {

}


class Server {

    private val server: TcpServer<ByteBuf, ByteBuf> = TcpServer.newServer(55555)

    fun start(onNewConnection: (Connection<ByteBuf, ByteBuf>) -> Observable<Void>) {
        server.start(onNewConnection)
    }

    fun join() {
        server.awaitShutdown()
    }

}

fun main() {

    val s = Server()

    val players = Collections.synchronizedList(ArrayList<Player>())

    s.start {
        val pl = Player(it)
        val tmp = pl.init()

        var t : Timer? = null
        val subs = Subscribers.create<String>(::println) {
            t?.stop()
        }
        t = Timer(4000) {
            println("subscribe")
                pl.observable.subscribe(subs)
            subs.unsubscribe()
        }
        t.start()

        players.add(pl)
        tmp
    }


    val t = Timer(1500) {
        for (p in players) {
            p.sendMessage(it.toString() + "\n")
        }
    }

    t.start()

    s.join()

}
