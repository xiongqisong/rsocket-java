package com.xqs.rsocket.conversation;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class RecordAudio {
	public static void main(String[] args) throws Exception {
		File outputFile = new File("E:\\recode.wav");
		
		AudioFormat audioFormat = 
				new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED, 
						44100,
						16, 
						2,
						4,
						44100,
						false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
		TargetDataLine td = (TargetDataLine) AudioSystem.getLine(info);
		td.open();
		td.start();

		new Thread(() -> {
			try {
				AudioInputStream ais = new AudioInputStream(td);
				AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
				System.out.println("over");	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		
		System.out.println("record begin, press enter when you finish");
	}
}