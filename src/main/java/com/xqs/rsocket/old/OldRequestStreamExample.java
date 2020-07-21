package com.xqs.rsocket.old;

import com.xqs.rsocket.NewRequestStreamExample;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 下面是1.0.0-RC版本下Request-Response示例代码，用到的很多类在1.1.x版本上已经废弃了，新版示例可以看
 * {@link NewRequestStreamExample}
 * 
 * 由于很多操作和{@link OldRequestResponseExample}类似，所以只解释不同的地方
 * 
 * @author ycr
 *
 */
@SuppressWarnings("deprecation")
public class OldRequestStreamExample {
	
	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 8080;

		// ================ Responder ================
		RSocketFactory
		.receive()
		.acceptor(
				(setup, sendingSocket) -> 
				Mono.just(new AbstractRSocket() {
					/*
					 * 仅重写Request-Stream通信模式下的处理方法，可以注意到它与Request-
					 * Response通信模式的区别在于返回的不是Mono而是Flux，
					 * 意味着会吐出多个元素而不是1个元素，但它们吐出的元素类型是一样的，都是Payload
					 */
					@Override
					public Flux<Payload> requestStream(Payload payload) {
						return Flux
								.fromStream(payload.getDataUtf8().codePoints().mapToObj(c -> String.valueOf((char) c))) // 将Requester发来的数据转换为字符串，再拆成单个字符发回
								.map(DefaultPayload::create);
					}

				}))
		.transport(TcpServerTransport.create(serverAddress, serverPort))
		.start()
		.subscribe();
		
		// ================ Requester ================
		RSocket socket = 
				RSocketFactory
				.connect()
				.transport(TcpClientTransport.create(serverAddress, serverPort))
				.start()
				.block();
		
		socket
		.requestStream(DefaultPayload.create("hello")) // 可以看到RSocket让人比较惊喜的地方是，切换通信模式只需要调整这1个api，可以对比OldRequestResponseExample中的Requester
		.map(Payload::getDataUtf8)
		.doOnNext(System.out::println)
		.blockLast();
		
		socket.dispose();
	}
}
