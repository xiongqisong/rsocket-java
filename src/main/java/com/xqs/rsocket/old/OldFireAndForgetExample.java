package com.xqs.rsocket.old;

import com.xqs.rsocket.NewFireAndForgetExample;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

/**
 * 下面是1.0.0-RC版本下Request-Response示例代码，用到的很多类在1.1.x版本上已经废弃了，新版示例可以看
 * {@link NewFireAndForgetExample}
 * 
 * 还是老样子哈，重复的部分不解释，看{@link OldRequestResponseExample}
 * 就好，FireAndForget通信模式下，Responder需要重写fireAndForget方法，该方法只能返回Mono
 * <Void>，无法返回任何元素给Requester，同样的Requester在fire后只能得到1个Mono<Void>
 * 
 * @author 11085076
 *
 */
@SuppressWarnings("deprecation")
public class OldFireAndForgetExample {
	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 9999;
		
		// ================ Responder ================
		RSocketFactory.receive().acceptor(
				(setup, sendingSocket) -> Mono.just(new AbstractRSocket() {
					@Override
					public Mono<Void> fireAndForget(Payload payload) {
						System.out.println("Receive: " + payload.getDataUtf8());
						return Mono.empty();
					}
				}))
				.transport(TcpServerTransport.create(serverAddress, serverPort))
				.start().subscribe();

		// ================ Requester ================
		RSocket socket = RSocketFactory.connect()
				.transport(TcpClientTransport.create(serverAddress, serverPort))
				.start().block();
		socket.fireAndForget(DefaultPayload.create("hello")).block();
		socket.fireAndForget(DefaultPayload.create("world")).block();
		socket.dispose();
	}
}
