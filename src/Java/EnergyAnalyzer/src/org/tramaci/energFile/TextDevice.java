package org.tramaci.energFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergPacket;
import org.tramaci.protocol.EnergProtocolException;
import org.tramaci.protocol.Settings;

public class TextDevice extends VirtualDevice implements BangListener{
	
	private long fileTcr = 0;
	private long lastTcr = 0;
	
	private BangTimer bang = null;
	private BufferedReader file = null;
	
	public TextDevice(String device, EnergyAnalyzer ea) throws IOException {
		super(device, ea);
		if (!new File(device).exists()) throw new IOException("File not found `"+device+"`");
	}

	@Override
	public String getTypeName() {
		return "text file";
	}

	@Override
	public long getFileTcr() {
		return fileTcr;
	}

	@Override
	protected void onOpen() throws IOException {
		file = new BufferedReader(new FileReader(getFileName()));
		bang = new BangTimer(this); 
	}

	@Override
	protected void onClose() throws IOException {
		bang.end();
		file.close();
		
	}

	@Override
	protected void onSeek(int at) throws IOException {
		throw new IOException("Seek operation is not supported");
		
	}

	@Override
	protected void onInit() throws IOException {	}

	@Override
	public int onBang() {
		String line;
		try {
			
			line = file.readLine();
			if (line==null) {
				onEof();
				return 500;
			}
			
			line = line.trim();
			if (line.length()==0) return 20;
			
			EnergPacket packet = EnergPacket.fromString(line);
			if (fileTcr==0) fileTcr = packet.when;
			
			int pause = (int) (packet.when - lastTcr);
			lastTcr = packet.when;
			
			sendPacket(packet);
			
			if (pause<20) pause = 20;
			return pause;
			
		} catch (IOException | EnergProtocolException e) {
			
			onError(e);
			return 500;
			
		}
		
	}

	@Override
	public boolean isDevice() {
		return false;
	}

	@Override
	protected void onSendSettings(Settings settings) throws IOException {
			
	}


}
