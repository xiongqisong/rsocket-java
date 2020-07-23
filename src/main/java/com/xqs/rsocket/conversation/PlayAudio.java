package com.xqs.rsocket.conversation;

import java.io.File;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * 学习audio api
 * 
 * @author ycr
 *
 */
public class PlayAudio {
	public static void main(String[] args) throws Exception {
		new Thread(() -> {
			try {
				// 创建音频输出流，如果这个流可以用Flux代替，那我们就可以实现一个Reactive式的音频播放器
				File file = new File("E:/ycr_learnspace/rsocket-java/src/resources/LYNC_ringback.wav");
				AudioInputStream ais = AudioSystem.getAudioInputStream(file);
//				AudioFormat format = ais.getFormat();
				
				// 根据音频输入流的类型初始化一个DataLine
				DataLine.Info dataLine = new DataLine.Info(SourceDataLine.class, null);
				
				// 通过DataLine初始化一个SourceLine，音频输出源
				SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLine);
				// 打开音频输出源
				line.open();
				line.start();
				int bytesRead = 0;
				byte[] buffer = new byte[1024];
				while (true) {
					// 从音频输入流提取数据
					bytesRead = ais.read(buffer, 0, buffer.length);
					if (bytesRead <= 0) {
						break;
					}
					System.out.println(Arrays.toString(buffer));
					// 将提取出的数据写入到音频输出源，此时可以从扬声器、耳机等音频输出设备听到声音了S
					line.write(buffer, 0, bytesRead);
				}
				// 这一步并不知道具体有什么作用，感觉是和堆外内存ByteBuffer的操作类似，将剩余的数据排空
				line.drain();
				// 关闭流
				line.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

}
