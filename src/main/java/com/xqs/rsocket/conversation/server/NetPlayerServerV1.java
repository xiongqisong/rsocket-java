package com.xqs.rsocket.conversation.server;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 使用rsocket-java和audio api实现的网络播放器服务端v1
 * 
 * @author ycr
 *
 */
@SuppressWarnings("deprecation")
public class NetPlayerServerV1 {
	public static final String SERVER_ADDRESS = "172.25.44.161";
	public static final int SERVER_PORT = 11111;
	public static void main(String[] args) throws Exception {
		new Thread(() -> {
			try {
				server();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
	private static void server() throws Exception {
		// 服务器：收到客户端建连请求后，从音频文件中获取字节流，发送给客户端
		// TODO: 改成相对地址，资源目前放在src/main/resources目录下
		File file = new File("E:/ycr_learnspace/rsocket-java/src/resources/LYNC_ringback.wav");
		AudioInputStream ais = AudioSystem.getAudioInputStream(file);
		RSocketServer
		.create(
				// 收到client请求连接的报文后，对于Request-Stream通信模式的请求的处理逻辑
				(setup, sendingSocket) -> Mono.just(new AbstractRSocket() {

					@Override
					public Flux<Payload> requestStream(Payload payload) {
						// 打印客户端的请求报文
						System.out.println(payload.getDataUtf8());
						
						// 输出一个音频字节流
						Flux<Payload> flux = Flux.generate(sink -> {
							try {
								int bytesRead = 0;
								byte[] buffer = new byte[1024];
								bytesRead = ais.read(buffer, 0, buffer.length);
								if (bytesRead <= 0) {
									sink.complete();
								}
								sink.next(DefaultPayload.create(buffer));
							} catch (Exception e) {
								System.out.println("error");
								e.printStackTrace();
								sink.complete();
							}
						});
						return flux;
					}

				}))
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.bind(TcpServerTransport.create(SERVER_ADDRESS, SERVER_PORT))
				.block()
				.onClose()
				.block();
	}
}
