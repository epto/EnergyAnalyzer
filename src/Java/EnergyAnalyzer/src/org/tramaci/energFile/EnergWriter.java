package org.tramaci.energFile;

import java.io.File;
import java.io.IOException;

import org.tramaci.common.Metadata;
import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergPacket;


public abstract class EnergWriter {
	
	public static final int STATUS_CLOSED = 0;
	public static final int STATUS_OPEN = 1;
	public static final int STATUS_ERROR = 2;
	
	public static final String FORMAT_NAME = "none";
	public static final String FORMAT_EXT = "";
	
	protected EnergyAnalyzer analyzer= null;
	protected String file = null;
	protected int status = STATUS_CLOSED;
	
	public int getStatus() { return status; }
	
	public String getFileName() { return file; }
	
	public String getDevice() {
		File f = new File(file);
		return f.getName();
	}
	
	public String getFromatName() { 

		try {
				
			return (String) this.getClass().getDeclaredField("FORMAT_NAME").get(this);
				
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
		
		}		
	}
	
	public String getFromatExt() { 

		try {
				
			return (String) this.getClass().getDeclaredField("FORMAT_EXT").get(this);
				
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
		
		}		
	}
	
	
	public EnergWriter(String fileName, EnergyAnalyzer ea) throws IOException {
		
		analyzer = ea;
		file = fileName;
		analyzer.onInitOutput(this);
		status=STATUS_OPEN;	
		analyzer.setOutput(this);
		
	}
	
	protected void onError(Exception e) {
		e.printStackTrace();
		closeResource();
		status =STATUS_ERROR; 
		analyzer.onOutputError(this,e);
	}
			
	public void close() {
		closeResource();
		status=STATUS_CLOSED;
		analyzer.onOutputClose(this);
	}
	
	public abstract void onPacketArrival(EnergPacket packet);
	public abstract String getDescription();
	public abstract void setMetadata(Metadata meta) throws IOException;
	protected abstract void openResource() throws IOException;
	protected abstract void closeResource();

}
