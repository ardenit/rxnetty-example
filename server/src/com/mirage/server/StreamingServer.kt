
package com.mirage.server

import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.DuplexChannel
import io.reactivex.netty.protocol.tcp.server.TcpServer
import io.reactivex.netty.util.StringLineDecoder
import rx.subjects.PublishSubject
import java.net.SocketAddress



fun main() {
    val server: TcpServer<ByteBuf, ByteBuf>

    val out = PublishSubject.create<String>()

    val t = Timer(1500) {
        out.onNext(it.toString() + "\n")
    }
    t.start()

    server = TcpServer.newServer(55555)
        .start {connection ->
            println("new connection!")
            connection
                .addChannelHandlerFirst<String, ByteBuf>("string-decoder", StringLineDecoder())
                .input
                .subscribe(
                    {println("onNext $it")},
                    {println("onError"); connection.close()},
                    {println("onCompleted"); connection.close()}
                )
            connection.writeStringAndFlushOnEach(out)
        }

    server.awaitShutdown()
}
