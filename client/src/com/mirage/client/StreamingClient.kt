
package com.mirage.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.channel.Connection
import io.reactivex.netty.protocol.tcp.client.TcpClient
import io.reactivex.netty.util.StringLineDecoder
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

class Client (
    private val onConnectionEstablished: (Connection<String, ByteBuf>) -> Unit = {println("Connection established")},
    private val onNewServerMessage: (String) -> Unit = {n -> println(n)},
    private val onError: (Connection<String, ByteBuf>, Throwable) -> Unit = {c, _ -> c.close()}
){

    private val out = PublishSubject.create<String>()

    private val client = TcpClient.newClient("localhost", 55555)

    fun start() {
        client.addChannelHandlerLast<ByteBuf, String>("string-decoder") { StringLineDecoder() }
            .createConnectionRequest()
            .flatMap {connection ->
                onConnectionEstablished(connection)
                connection.writeStringAndFlushOnEach(out).subscribe(
                    {},
                    {onError(connection, it)},
                    {connection.close()}
                )
                connection.input
            }
            .subscribe(
                {onNewServerMessage(it)},
                {},
                {}
            )
    }

    /**
     * msg should end with '\n'
     */
    fun sendMessage(msg : String) {
        out.onNext(msg)
    }

}

fun main() {

    val c = Client()

    c.start()

    val t = Timer(2000) {
        c.sendMessage(it.toString() + "\n")
    }

    t.start()

}