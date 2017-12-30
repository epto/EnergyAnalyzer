package org.tramaci.protocol;

import org.tramaci.common.BitStream;

public class EnergPacket {
	public static final int TYPE_ID =0;
	public static final String FIRST_STRING = ".";
	public static final boolean ADD_TAB = true;
	
	public long when = 0;
	
	public static EnergPacket fromBitStream(BitStream bs, long timeBase) throws EnergProtocolException {
		int type = (int) bs.readWord(3);
		EnergPacket o = null;
		
		switch(type) {
		
			case EnergPacket.TYPE_ID:
				o = new EnergPacket();
				break;
				
			case EnergData.TYPE_ID:
				o = new EnergData();
				break;
				
			case EnergSetting.TYPE_ID:
				o = new EnergSetting();
				break;
				
			case EnergMetadata.TYPE_ID:
				o = new EnergMetadata();
				break;
				
			case EnergSignal.TYPE_ID:
				o = new EnergSignal();
				break;
				
			default:
				
				throw new EnergProtocolException("Unknown packet type "+type);					
				
		}
		
		o.when = timeBase+bs.readWord(10);
		o.decodeBitStream(bs);
		return o;
		
	}
	
	public void toBitStream(BitStream bs, long timeBase) {
		long tcr = when - timeBase;
		int type = getTypeId();
		bs.addWord(type,3);
		bs.addWord(tcr,10);
		encodeBitStream(bs);
	}
	
	public int getTypeId() { 

		try {
				
			return this.getClass().getDeclaredField("TYPE_ID").getInt(this);
				
		} catch(Exception e) {
			
			e.printStackTrace();
			return -1;
		
		}		
	}
	
	public String getFirstString() { 

		try {
				
			return (String) this.getClass().getDeclaredField("FIRST_STRING").get(this);
				
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
		
		}		
	}
	
	public boolean getAddTab() { 

		try {
				
			return  this.getClass().getDeclaredField("ADD_TAB").getBoolean(this);
				
		} catch(Exception e) {
			
			e.printStackTrace();
			return true;
		
		}		
	}
	
	public static EnergPacket fromString(String line) throws EnergProtocolException {
		line=line.trim();

		if (line.length()<2) throw new EnergProtocolException("R005: Bad line: "+line);
		EnergPacket o = null;
		
		long when = 0;
		if (line.startsWith(EnergPacket.FIRST_STRING)) {
			int i = line.indexOf(9);
			
			if (i<4) throw new EnergProtocolException("R001: Bad timecode");
			
			String t = line.substring(1,i);
			
			try {
				when = Long.parseLong(t.trim());
			} catch(NumberFormatException err) {
				throw new EnergProtocolException("R002: Bad timecode");
			}
			
			line=line.substring(i+1);
			line=line.trim();
			if (line.length()<2) throw new EnergProtocolException("R003: Bad timecode");
			
		} else {
			
			when = System.currentTimeMillis();
			
		}
		
		if (line.startsWith(EnergData.FIRST_STRING)) {
			o = new EnergData();
			
		} else if (line.startsWith(EnergSetting.FIRST_STRING)) {
			o = new EnergSetting();
			
		} else if (line.startsWith(EnergMetadata.FIRST_STRING)) {
			o = new EnergMetadata();
			
		} else if (line.startsWith(EnergSignal.FIRST_STRING)) {
			o = new EnergSignal();
			
		} else {
			throw new EnergProtocolException("R004: Bad line: "+line);
			
		}
		int t = o.getFirstString().length();
		if (line.length()<t) throw new EnergProtocolException("R0016: Bad line length: "+line);
		line = line.substring(t);
		line = line.trim();
		
		o.when = when;
		o.decodeString(line);
		return o;
	}
	
	public String toString() {
		StringBuilder line = new StringBuilder();
		line.append(EnergPacket.FIRST_STRING);
		line.append(Long.toString(when));
		line.append('\t');
		line.append(getFirstString());
		if (getAddTab()) line.append('\t');
		encodeString(line);
		line.append('\n');
		return line.toString();
	}
	
	protected int val(String v) throws EnergProtocolException {
		try { return Integer.parseInt(v); } catch( NumberFormatException err) {
			throw new EnergProtocolException("R008: Bad number: "+v);
		}
	}
	
	protected void encodeString(StringBuilder line) {
		
	}
	
	protected void decodeString(String line) throws EnergProtocolException {
		
	}
	
	protected void decodeBitStream(BitStream bs) throws EnergProtocolException {
		
	}
	
	protected void encodeBitStream(BitStream bs) {
		
	}
	
	
}
