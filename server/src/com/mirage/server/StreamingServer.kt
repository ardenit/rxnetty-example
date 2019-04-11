
package com.mirage.server

import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.DuplexChannel
import io.reactivex.netty.channel.Connection
import io.reactivex.netty.protocol.tcp.server.TcpServer
import io.reactivex.netty.util.StringLineDecoder
import rx.subjects.PublishSubject
import java.net.SocketAddress


class Server(
    private val onNewConnection: (Connection<ByteBuf, ByteBuf>) -> Unit = {println("new connection!")},
    private val onNewClientMessage: (Connection<ByteBuf, ByteBuf>, String) -> Unit = {_, n -> println(n)},
    private val onError: (Connection<ByteBuf, ByteBuf>, Throwable) -> Unit = {c, _ -> c.close()}
) {

    private val server: TcpServer<ByteBuf, ByteBuf> = TcpServer.newServer(55555)

    private val out = PublishSubject.create<String>()

    /**
     * msg should end with '\n'
     */
    fun sendMessage(msg: String) {
        out.onNext(msg)
    }

    fun start() {
        server.start {connection ->
                onNewConnection(connection)
                connection
                    .addChannelHandlerFirst<String, ByteBuf>("string-decoder", StringLineDecoder())
                    .input
                    .subscribe(
                        {onNewClientMessage(connection, it)},
                        {onError(connection, it)},
                        {println("onCompleted"); connection.close()}
                    )
                connection.writeStringAndFlushOnEach(out)
            }
    }

    fun join() {
        server.awaitShutdown()
    }

}

fun main() {

    val s = Server()

    s.start()

    val t = Timer(1500) {
        s.sendMessage(it.toString() + "\n")
    }
    t.start()

    s.join()

}
