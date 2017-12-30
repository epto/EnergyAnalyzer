package org.tramaci.energFile;

import java.io.IOException;

import org.tramaci.common.BitStream;
import org.tramaci.common.MediaContainerReader;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergPacket;
import org.tramaci.protocol.EnergProtocolException;
import org.tramaci.protocol.Settings;

public class EBFDevice extends VirtualDevice implements BangListener {

	private BangTimer bang = null;
	private MediaContainerReader reader = null;
	private BitStream buffer = null;
	private long curTcr = 0;
	private long lastTcr = 0;
	private int startPox = 0;
	
	public EBFDevice(String device, EnergyAnalyzer ea) throws IOException {
		super(device, ea);
	}

	private EnergPacket onRequestPacket() throws IOException, EnergProtocolException {
		
		if (buffer == null || buffer.getAvailableBits()<10) {
			if (reader.eof()) {
				bang.end();
				onEof();
				return null;
			}
			byte[] block = reader.getBlock();
			curTcr=reader.getCurrentTime();
			buffer = new BitStream(block);
		}
		EnergPacket packet =  EnergPacket.fromBitStream(buffer, curTcr);
		curTcr = packet.when;
		return packet;
		
	}
	
	@Override
	public int onBang() {
		int pause=0;
		
		EnergPacket packet=null;
		try {
			packet = onRequestPacket();
			if (packet==null) return 20;
		} catch (IOException|EnergProtocolException e) {
			onError(e);
			return 500;
		} 
		
		if (lastTcr == 0 ) lastTcr = packet.when;
		
		pause = (int) (packet.when - lastTcr);
		lastTcr = packet.when;
		if (pause<20) pause=20;
		if (pause>2000) pause=2000;
		sendPacket(packet);
		return pause;
	}

	@Override
	public String getTypeName() { return "EBF file"; }

	@Override
	public long getFileTcr() { 
		if (reader==null) return 0;
		return reader.getStartTcr();	
		}

	@Override
	protected void onOpen() throws IOException {
		reader = new MediaContainerReader( getFileName() , EBFWriter.MAGIC_NUMBER ) ;
		if (startPox>0) reader.seek(startPox);
		bang = new BangTimer(this);
		
	}

	@Override
	protected void onClose() throws IOException {
		bang.end();
		reader.close();
		startPox= (int) (reader.eof() ? 0 : Math.floor(reader.getCurrentTime() /1000L));
	}

	@Override
	protected void onSeek(int at) throws IOException {
		startPox = at;
		if (isRunning()) stop();
	}

	@Override
	protected void onInit() throws IOException {
		MediaContainerReader mc = new MediaContainerReader( getFileName() , EBFWriter.MAGIC_NUMBER ) ;
		int len = mc.getIndexLength();
		for (int i= 0 ; i<len; i++) {
			this.addIndex(mc.getIndexStruct(i));
		}
		mc.close();
		
	}

	@Override
	public boolean isDevice() {
		return false;
	}

	@Override
	protected void onSendSettings(Settings settings) throws IOException {
			
	}

	
}
