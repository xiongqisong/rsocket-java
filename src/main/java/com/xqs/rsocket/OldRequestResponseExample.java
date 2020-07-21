package com.xqs.rsocket;

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
 * {@link NewRequestResponseExample}
 * 
 * 由于在RSocket中使用的更多的概念是Requester（对应传统通信模型中的client）和Responder（对应传统通信模型中的server），
 * 所以我们不再使用client和server的概念 任何一个节点（client/server）可以同时是Requester和Responder
 * 
 * @author ycr
 *
 */
@SuppressWarnings("deprecation")
public class OldRequestResponseExample {
	
	public static void main(String[] args) {
		String serverAddress = "localhost";
		int serverPort = 8080;
		
		// ================ Responder ================
		RSocketFactory
		.receive() // 老版使用receiver来创建Responder
		.acceptor( // 不同通信模式下，Respnoder如何处理消息，这里仅重写了Request-Response模式下的处理方法
				(setup, sendingSocket) -> 
				Mono.just(
						new AbstractRSocket() {
							@Override
							public Mono<Payload> requestResponse(Payload payload) {
								return Mono.just(DefaultPayload.create(
										"Echo >> " + payload.getDataUtf8()));
							}
						}
					)
				)
		.transport(TcpServerTransport.create(serverAddress, serverPort)) // 设置传输层采用Tcp，以及要监听的地址、端口，仔细看会发现还是用了Server关键字，说明Responder和Requester创建的IO线程模型是不一样的
		.start() // 启动Respnoder
		.subscribe(); // ？？？
		
		// ================ Requester ================
		RSocket rsocket =
				RSocketFactory
				.connect() // 老版使用connect来创建Requester
				.transport(TcpClientTransport.create(serverAddress, serverPort)) // 设置传输层采用Tcp，以及要连接的Responder的地址、端口
				.start() // 启动Requester
				.block(); // 阻塞等待Requester启动完成
		
		rsocket
		.requestResponse(DefaultPayload.create("hello")) // 通信模式：Request-Response，发送数据有DefaultPayload辅助创建，返回：响应流
		.map(Payload::getDataUtf8) // 将响应流中的元素映射为字符串
		.doOnNext(System.out::println) // 每当有1个元素产生则执行System.out.println
		.block(); // 阻塞等待调用完成
		
		rsocket.dispose(); // 释放Requester
	}
}
