package com.xqs.rsocket.conversation.server;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import org.reactivestreams.Publisher;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 使用rsocket-java和audio api实现的网络播放器服务端v2
 * 
 * @author ycr
 *
 */
@SuppressWarnings("deprecation")
public class NetPlayerServerV2 {
	public static final String SERVER_ADDRESS = "192.168.0.101";
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
		AudioFormat audioFormat = 
				new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED,
						48000,
						16,
						2,
						4,
						48000,
						false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
		TargetDataLine td = (TargetDataLine) AudioSystem.getLine(info);
		td.open();
		td.start();
		
		RSocketServer
		.create(
				// 收到client请求连接的报文后，对于Request-Stream通信模式的请求的处理逻辑
				(setup, sendingSocket) -> Mono.just(new AbstractRSocket() {

					@Override
					public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
						Flux<Payload> flux = Flux.generate(sink -> {
							try {
								int bytesRead = 0;
								byte[] buffer = new byte[1024];
								bytesRead = td.read(buffer, 0, buffer.length);
								if(bytesRead <= 0) {
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
