package org.tramaci.energFile;

import java.io.FileOutputStream;
import java.io.IOException;

import org.tramaci.common.Metadata;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergPacket;

public class TextWriter extends EnergWriter {
	
	public static final String FORMAT_NAME = "text file";
	public static final String FORMAT_EXT = "txt";
	private static final byte[] CRLF = new byte[] {13,10};
	
	public TextWriter(String fileName, EnergyAnalyzer ea) throws IOException {
		super(fileName, ea);
		openResource();
		
	}

	private FileOutputStream fOut = null;
	
	protected void openResource() throws IOException {
		fOut = new FileOutputStream(file);
	}
	
	protected void closeResource() {
		if (fOut!=null) try { fOut.close(); } catch(Exception devNull) {}
		fOut=null;
	}
	
	public void onPacketArrival(EnergPacket packet) {
		if (status!=STATUS_OPEN) return;
				
		String text = packet.toString();
	
		try {
						
			text=text.trim();
			fOut.write(text.getBytes());
			fOut.write(CRLF);
		
		} catch (IOException e) {
			onError(e);
			
		}
		
	}
	
	@Override
	public String getDescription() {
		return "text file";
	}

	@Override
	public void setMetadata(Metadata meta) throws IOException {
	
	}
	
	
}
