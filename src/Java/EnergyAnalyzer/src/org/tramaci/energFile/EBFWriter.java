package org.tramaci.energFile;

import java.io.IOException;

import org.tramaci.common.BitStream;
import org.tramaci.common.MediaContainerWriter;
import org.tramaci.common.Metadata;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergData;
import org.tramaci.protocol.EnergPacket;

public class EBFWriter extends EnergWriter {
	public static final String FORMAT_NAME = "EBFFile";
	public static final String FORMAT_EXT = "ebf";
	
	public static final int MAGIC_NUMBER = 0xEBF1E002;
	public static final int[] INDEX_STRUCT = new int[] { 5, 10 };
	public static final int MEDIA_PACKET_BYTES = 3072;
	public static final int MEDIA_PACKET_BOUND = MEDIA_PACKET_BYTES + 1024;
	public static final int MEDIA_PACKET_BITS = MEDIA_PACKET_BOUND<<3;
	public static final int MEDIA_TCR_BITS = 27;
	public static final int MEDIA_SIZE_BITS = 12;
	
	private MediaContainerWriter wr = null;
	private long fileTcr = 0;
	private int cIdTrack = 0;
	private int cTouch = 0;
	private int cMax = 0;
	private int cSecond = 0 ;
	private long prevTcr = 0;
	private long blockTcr = 0;
	private BitStream buffer = null;
	
	public EBFWriter(String fileName, EnergyAnalyzer ea) throws IOException {
		super(fileName, ea);
		byte[] metaData = new byte[0];
		
		wr =new MediaContainerWriter(fileName, MAGIC_NUMBER, MEDIA_TCR_BITS, MEDIA_SIZE_BITS, INDEX_STRUCT, metaData); 
		buffer = new BitStream(MEDIA_PACKET_BITS);
	}
	
	private void flush(boolean resetCounters) throws IOException {
		
		int[] index = new int[] {
				cTouch	,
				cMax	}
		;
		
		if (resetCounters) {
			cTouch=0;
			cMax=0;
		}
		
		wr.writeBlock(blockTcr, buffer.getBytesAtCursor() , index);
		
		buffer = new BitStream(MEDIA_PACKET_BITS);
		blockTcr=0;
		prevTcr=0;
	}
	
	@Override
	public void onPacketArrival(EnergPacket packet) {
		
		try {
			if (fileTcr == 0) fileTcr = packet.when;
			if (prevTcr==0) prevTcr = packet.when;
			if (blockTcr == 0 ) blockTcr = packet.when;
			
			int relTcr = (int) (packet.when - fileTcr);
			int packetSecond = (int) Math.floor(relTcr/1000);
			if (packetSecond!=cSecond) flush(true);
			cSecond=packetSecond;
			
			if (packet instanceof EnergData) {
				EnergData o = (EnergData) packet;
				if (o.max1>cMax) cMax=o.max1;
				if (o.max2>cMax) cMax=o.max2;
			
				if (o.idTrack!=cIdTrack) {
					cIdTrack = o.idTrack;
					if (cTouch<15) cTouch++;
				}
			}
			
			packet.toBitStream(buffer, prevTcr);
			if (buffer.getPositionByte()>MEDIA_PACKET_BYTES) flush(false);
			prevTcr = packet.when;
			
		} catch(IOException err) {
			onError(err);
		}
		
	}
	
	@Override
	protected void openResource() throws IOException {
			
	}
	
	@Override
	protected void closeResource() {
		try {
			if (buffer.getPosition()>0) flush(true);
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public String getDescription() {
		return "binary file";
	}

	@Override
	public void setMetadata(Metadata meta) throws IOException {
		BitStream bs = new BitStream(BitStream.DYNAMIC_SIZE);
		meta.toBitStream(bs);
		wr.setMetadata(bs.getBytesAtCursor());
	}

}
