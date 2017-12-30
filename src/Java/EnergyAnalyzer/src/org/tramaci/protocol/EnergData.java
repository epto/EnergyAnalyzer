package org.tramaci.protocol;

import org.tramaci.common.BitStream;

public class EnergData extends EnergPacket{
	public static final int TYPE_ID =1;
	public static final String FIRST_STRING = "D2";
	public static final boolean ADD_TAB = true;
	
	public int value1 = 0;
	public int max1 = 0;
	public int value2 = 0;
	public int max2 = 0;
	public int freq = 0;
	public int frameSkip = 0;
	public int idTrack = 0;
	
	protected void encodeBitStream(BitStream bs) {
		bs.addWord(value1,10);
		bs.addWord(max1,10);
		bs.addWord(value2,10);
		bs.addWord(max2,10);
		bs.addWord(freq,13);
		bs.addWord(frameSkip,10);
		bs.addWord(idTrack,4);
	}
	
	protected void decodeBitStream(BitStream bs) {
		value1 = (int) bs.readWord(10);
		max1 = (int) bs.readWord(10);
		value2 = (int) bs.readWord(10);
		max2 = (int) bs.readWord(10);
		freq = (int) bs.readWord(13);
		frameSkip = (int) bs.readWord(10);
		idTrack = (int) bs.readWord(4);
	}
	
	protected void encodeString(StringBuilder line) {
		line.append(Integer.toString(value1));
		line.append('\t');
		line.append(Integer.toString(max1));
		line.append('\t');
		line.append(Integer.toString(value2));
		line.append('\t');
		line.append(Integer.toString(max2));
		line.append('\t');
		line.append(Integer.toString(freq));
		line.append('\t');
		line.append(Integer.toString(frameSkip));
		line.append('\t');
		line.append(Integer.toString(idTrack));
	}
	
	protected void decodeString(String line) throws EnergProtocolException {
		String[] tok = line.split("\t");
		if (tok.length<7) throw new EnergProtocolException("R009: Bad data line: "+line);
		value1 =val(tok[0]);
		max1 = val(tok[1]);
		value2 = val(tok[2]);
		max2 = val(tok[3]);
		freq = val(tok[4]);
		frameSkip = val(tok[5]);
		idTrack = val(tok[6]);
			
	}
	
}
