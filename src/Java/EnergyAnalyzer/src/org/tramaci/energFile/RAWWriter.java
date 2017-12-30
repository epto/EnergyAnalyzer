package org.tramaci.energFile;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.tramaci.common.Metadata;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergData;
import org.tramaci.protocol.EnergPacket;

public class RAWWriter extends EnergWriter {
	public RAWWriter(String fileName, EnergyAnalyzer ea) throws IOException {
		super(fileName, ea);
		openResource();
	}

	public static final String FORMAT_NAME = "Wave RAW";
	public static final String FORMAT_EXT = "raw";
	
	private RandomAccessFile out = null;

	@Override
	public void onPacketArrival(EnergPacket packet) {
		if (packet instanceof EnergData) {
			EnergData e = (EnergData) packet;
			try {
				out.writeShort(e.value1-512);
				out.writeShort(e.value2-512);
			} catch (IOException e1) {
				onError(e1);
			}
		
		}
		
	}

	@Override
	public String getDescription() {
		return "Wave file";
	}

	@Override
	protected void openResource() throws IOException {
		out = new RandomAccessFile(getFileName(),"rw");
		out.setLength(0);
		
	}

	@Override
	protected void closeResource() {
		try { out.close(); } catch(IOException devNull) {}
		
	}

	@Override
	public void setMetadata(Metadata meta) throws IOException {
		
	}
	

}
