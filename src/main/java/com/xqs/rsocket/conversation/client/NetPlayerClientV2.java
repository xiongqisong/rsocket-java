package com.xqs.rsocket.conversation.client;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;

/**
 * 使用rsocket-java和audio api实现的网络播放器客户端v1
 * 
 * @author ycr
 *
 */
public class NetPlayerClientV2 {
	public static final String SERVER_ADDRESS = "192.168.0.101";
	public static final int SERVER_PORT = 11111;
	public static void main(String[] args) {
		new Thread(() -> {
			try {
				client();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private static void client() throws Exception {
		// 客户端：请求连接服务器后，接收服务器发来的音频字节流，播放出来
		RSocket clientRSocket = RSocketConnector.create()
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.connect(TcpClientTransport.create(SERVER_ADDRESS, SERVER_PORT))
				.block();

		// 根据音频输入流的类型初始化一个DataLine，实际上我测试了不传入流类型也可以，下面就没传
		DataLine.Info dataLine = new DataLine.Info(SourceDataLine.class, null);

		// 通过DataLine初始化一个SourceLine，音频输出源
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLine);
		// 打开音频输出源并启动它
		line.open();
		line.start();

		// 创建一个rsocket，使用Request-Channel通信模式，因为我们要持续发送从话筒收到的音频流
		clientRSocket
		.requestChannel(Flux.just("hello","world").map(DefaultPayload::create))
		.take(Integer.MAX_VALUE)
		.doOnNext(
				// 每当有输入的音频字节流进来就播放
				p -> {
					try {
						// 注意：不是所有Payload使用的ByteBuffer都支持直接转字节数组的
						if (p.getData().hasArray()) {
							System.out.println("has array");
						} else {
							// 碰到不能直接转字节数组的ByteBuffer，手动转，有性能损耗
							byte[] data = new byte[p.getData().remaining()];
							p.getData().get(data, 0, data.length);
							// 将提取出的数据写入到音频输出源，此时可以从扬声器、耳机等音频输出设备听到声音了
							line.write(data, 0, data.length);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				})
		.blockLast();
		
		// 关闭rsocket
		clientRSocket.dispose();
	}
}
