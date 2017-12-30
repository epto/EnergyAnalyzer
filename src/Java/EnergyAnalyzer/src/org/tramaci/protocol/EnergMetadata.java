package org.tramaci.protocol;

import org.tramaci.common.BitStream;

public class EnergMetadata extends EnergPacket {
	public static final int TYPE_ID =3;
	public static final String FIRST_STRING = "@";
	public static final boolean ADD_TAB = false;
	
	public static final int FIELD_BOARD = 0x42;
	
	public int fieldId = 0;
	public String data = null;
	
	protected void encodeBitStream(BitStream bs) {
		bs.addWord(fieldId,7);
		bs.addString(data);
	}
	
	protected void decodeBitStream(BitStream bs) {
		fieldId = (int) bs.readWord(7);
		data = bs.readString();
	}
	
	protected void encodeString(StringBuilder line) {
		line.append((char)(fieldId&127));
		line.append('\t');
		line.append(data);
	}
	
	protected void decodeString(String line) throws EnergProtocolException {
		if (line.length()<3) throw new EnergProtocolException("R0011: Bad metadata: "+line);
		fieldId = line.codePointAt(0)&127;
		data = line.substring(1);
		data = data.trim();
	}
	
}
