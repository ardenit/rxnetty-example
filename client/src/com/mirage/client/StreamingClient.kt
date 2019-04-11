
package com.mirage.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.protocol.tcp.client.TcpClient
import io.reactivex.netty.util.StringLineDecoder
import rx.subjects.PublishSubject

fun main() {
    val out = PublishSubject.create<String>()

    val t = Timer(2000) {
        out.onNext(it.toString() + "\n")
    }
    t.start()

    TcpClient.newClient("localhost", 55555)
        .addChannelHandlerLast<ByteBuf, String>("string-decoder") { StringLineDecoder() }
        .createConnectionRequest()
        .flatMap {connection ->
            println("Connection established")
            connection.writeStringAndFlushOnEach(out).subscribe(
                {},
                {connection.close()},
                {connection.close()}
            )
            connection.input
        }
        .subscribe(
            ::println,
            {println("ERROR")},
            {println("COMPLETED")}
        )
        //.forEach(::println)

    println("Client initialization finished")
    Thread.sleep(10000)
}