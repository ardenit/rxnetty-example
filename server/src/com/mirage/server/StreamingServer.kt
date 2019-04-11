
package com.mirage.server

import io.netty.buffer.ByteBuf
import io.reactivex.netty.channel.Connection
import io.reactivex.netty.protocol.tcp.server.TcpServer
import io.reactivex.netty.util.StringLineDecoder
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.UnicastSubject
import java.util.*


class Player(private val connection: Connection<ByteBuf, ByteBuf>) {

    private var room: Room? = null

    private val inMsgs = UnicastSubject.create<String>()

    private val outMsgs = PublishSubject.create<String>()

    fun init() : Observable<Void> {
        connection
            .addChannelHandlerFirst<String, ByteBuf>("string-decoder", StringLineDecoder())
            .input
            .subscribe(
                {inMsgs.onNext(it)},
                {connection.close()},
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

        pl.observable.subscribe {
            println(it)
        }

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
