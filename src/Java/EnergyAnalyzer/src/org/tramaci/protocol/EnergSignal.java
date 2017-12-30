package org.tramaci.protocol;

import org.tramaci.common.BitStream;

public class EnergSignal extends EnergPacket {
	public static final int TYPE_ID =4;
	public static final String FIRST_STRING = "!";
	public static final boolean ADD_TAB = false;
	
	public static final int SIGNAL_PING = 0;
	public static final int SIGNAL_END_TOUCH = 1;
	public static final int SIGNAL_END_STREAM = 2;
	public static final int SIGNAL_GATE_LEVEL = 3;
	public static final int SIGNAL_SPEED_MODE = 4;
	public static final int SIGNAL_NO_PING = 5;
	public static final int SIGNAL_DO_SET = 6;
	public static final int SIGNAL_UNKNOWN = 7;
	public static final int SIGNAL_START = 8;
	public static final int MAX_SIGNAL_ID = 8;
	
	public static final String[] SIGNAL_STRING = new String[MAX_SIGNAL_ID+1];
	
	static {
		
		SIGNAL_STRING[SIGNAL_PING]					=		"Ping from board";
		SIGNAL_STRING[SIGNAL_END_TOUCH]		=		"End of signal";
		SIGNAL_STRING[SIGNAL_END_STREAM]	=		"End of stream";
		SIGNAL_STRING[SIGNAL_GATE_LEVEL]		=		"Gate levels";
		SIGNAL_STRING[SIGNAL_SPEED_MODE]	=		"Change speed mode";
		SIGNAL_STRING[SIGNAL_NO_PING]			=		"No ping from board";
		SIGNAL_STRING[SIGNAL_DO_SET]			=		"Calibration in progress";
		SIGNAL_STRING[SIGNAL_UNKNOWN]		=		"Unknown command";
		SIGNAL_STRING[SIGNAL_START]				=		"Board is running";
		
	}

	public int signal = 0;
	public int[] data = null;
	
	public EnergSignal() {}
	
	public EnergSignal(int event) {
		signal=event;
	}
	
	protected void encodeBitStream(BitStream bs) {
		bs.addWord(signal);
		if (data!=null) bs.addArray(data);
	}
	
	protected void decodeBitStream(BitStream bs) {
		signal = (int) bs.readWord();
		data = bs.readArray();
	}
	
	protected void encodeString(StringBuilder line) {
		line.append(Integer.toString(signal));
		if (data==null) return;
		int j = data.length;
		if (j==0) return;
		for (int i=0;i<j;i++) {
			line.append('\t');
			line.append(Integer.toString(data[i]));
		}
		
	}
	
	protected void decodeString(String line) throws EnergProtocolException {
		String[] tok = line.split("\t");
		int j = tok.length;
		if (j==0) throw new EnergProtocolException("R0012: Bad Signal: "+line);
		signal = val(tok[0]);
		int s = j-1;
		data = new int[s];
		s=0;
		for (int i = 1;i<j;i++) {
			data[s++] = val(tok[i]);
		}
	}
	
}
