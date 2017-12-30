package org.tramaci.energFile;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.tramaci.common.Metadata;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergMetadata;
import org.tramaci.protocol.EnergPacket;
import org.tramaci.protocol.EnergSignal;
import org.tramaci.protocol.Settings;

public abstract class VirtualDevice {

	protected String fileName = null;
	protected int[][] index = null;
	protected boolean fOpen = false;
	protected int indexLen = 0;
	protected EnergyAnalyzer analyzer = null;
	private long tcr = 0;
	private boolean hasTcr = false;
	private Metadata metadata = new Metadata();
	
	public VirtualDevice(String device, EnergyAnalyzer ea) throws IOException {
		fileName = device;
		analyzer=ea;
		clearIndex();
		onInit();
		analyzer.setReaderDevice(this);
		if (analyzer.metadata!=null) metadata=analyzer.metadata;
	}
	
	public void setMetadata(Metadata set) {
		metadata = set;
		if (metadata==null) metadata = new Metadata();
		
	}
	
	public Metadata getMetaData() { 
		return metadata; 
		} 
	
	public final boolean isRunning() {
		return fOpen;
	}
	
	public final void start() throws IOException {
		if (fOpen) throw new IOException(getTypeName()+" `"+getDeviceName()+"` already open");
		onOpen();
		fOpen=true;
		synchronized(analyzer) {
			analyzer.onVirtualDeviceStart(this);
		}
		if (metadata!=null && metadata.settings!=null) {
			try {
				onSendSettings(metadata.settings);
				String st;
				if (metadata.settings.title!=null && metadata.settings.title.length()>0) st = "`"+metadata.settings.title+"`"; else st = metadata.settings.toString();
				analyzer.log.echo("DeviceSettings: "+st, Color.CYAN);
			} catch(IOException e) {
				analyzer.log.echo("DeviceSettings: "+e.getMessage(), Color.RED);
			}
		}
				
	}
	
	public final void stop() throws IOException {
		if (!fOpen) throw new IOException(getTypeName()+" `"+getDeviceName()+"` not open");
		onClose();
		fOpen=false;
		synchronized(analyzer) {
			analyzer.onVirtualDeviceStop(this);
		}
		
	}
	
	public final void seekAt(int pox) throws IOException {
		if (fOpen) stop();
		if (index==null) throw new IOException("Index of "+getTypeName()+" `"+getDeviceName()+"` not loaded");
		onSeek(pox);
		synchronized(analyzer) {
			analyzer.onVirtualDeviceSeek(this,pox);
		}
	}
	
	public final int[] getIndex(int pox) throws IOException {
		if (index==null) throw new IOException("Index of "+getTypeName()+" `"+getDeviceName()+"` not loaded");
		return index[pox].clone();
	}
	
	public final int[][] getIndex() {
		int l = getIndexLength();
		int[][] out = new int[ l ][];
		System.arraycopy(index, 0, out, 0, l);
		return out;
	}
	
	public final boolean hasIndex() { 
		return index!=null; 
	}
	
	public final int getIndexLength() { 
		return indexLen; 
	}
	
	protected final void clearIndex()  {
		index = new int[30][];
		indexLen=0;
	}
	
	protected final void addIndex(int[] dta) {
		index[indexLen++] = dta;
		if (indexLen>=index.length) {
			int[][] n = new int[index.length+15][];
			System.arraycopy(index, 0, n, 0, index.length);
			index=null;
			index=n;
			n=null;
			System.gc();
		}
	}
	
	protected final void sendPacket(EnergPacket packet) {
		tcr=packet.when;
		
		if (packet instanceof EnergSignal) {
			
			EnergSignal signal = (EnergSignal) packet;
			if (signal.signal == EnergSignal.SIGNAL_GATE_LEVEL) metadata.set(Metadata.FIELD_THRESHOLD, signal.data);
			
		} else if (packet instanceof EnergMetadata) {
			
			EnergMetadata meta = (EnergMetadata) packet;
			
			if (meta.fieldId == EnergMetadata.FIELD_BOARD) {
				metadata.set(Metadata.FIELD_BOARD, meta.data);
			}
			
		}
		
		if (!hasTcr) {
			metadata.set(Metadata.FIELD_TCR,packet.when);
			hasTcr=true;;
		}
		
		synchronized(analyzer) {
			analyzer.onPacketArrival(packet);
		}
	}
	
	protected final void onEof() {
		
		if (fOpen) {
			try { onClose(); } catch (IOException devNull) {	}
			fOpen=false;
		}
		
		synchronized(analyzer) {
			analyzer.onEndOfFile(this);
		}
	}
	
	protected final void onError(Exception err) {
		
		if (fOpen) {
			try { onClose(); } catch (IOException devNull) {	}
			fOpen=false;
		}
		
		synchronized(analyzer) {
			analyzer.onError(this, err);
		}
	}
	
	public final long getCurrentTime() { return tcr; }
	public final String getDeviceName() { return new File(fileName).getName(); }
	public final String getFileName() { return fileName; }
	
	public abstract String getTypeName();
	public abstract long getFileTcr();
	public abstract boolean isDevice();
	
	protected abstract void onOpen() throws IOException;
	protected abstract void onClose() throws IOException;
	protected abstract void onSeek(int at) throws IOException;
	protected abstract void onInit() throws IOException;
	protected abstract void onSendSettings(Settings settings) throws IOException;

	
}
