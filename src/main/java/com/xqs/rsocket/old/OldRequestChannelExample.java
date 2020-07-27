package com.xqs.rsocket.old;

import org.reactivestreams.Publisher;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("deprecation")
public class OldRequestChannelExample {

	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 9999;
		
		// ================ Responder ================
		RSocketFactory
		.receive()
		.acceptor(
				(setup, sendingSocket) -> Mono.just(new AbstractRSocket() {

					@Override
					public Flux<Payload> requestChannel(
							Publisher<Payload> payloads) {
						return Flux.from(payloads).flatMap(payload->Flux.fromStream(payload.getDataUtf8().codePoints().mapToObj(c->String.valueOf((char)c)).map(DefaultPayload::create)));
					}
					
				}))
		.transport(TcpServerTransport.create(serverAddress, serverPort))
		.start()
		.subscribe();
		
		// ================ Requester ================
		RSocket socket =RSocketFactory.connect().transport(TcpClientTransport.create(serverAddress, serverPort)).start().block();
		socket
		.requestChannel(Flux.just("hello", "world", "goodbye").map(DefaultPayload::create)) // 构建一个Flux用于请求
		.map(Payload::getDataUtf8)
		.doOnNext(System.out::println)
		.blockLast();
		socket.dispose();
	}
}