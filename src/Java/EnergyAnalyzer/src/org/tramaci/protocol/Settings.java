package org.tramaci.protocol;

import java.io.IOException;

import org.tramaci.common.BitStream;
import org.tramaci.common.Util;

public class Settings {

	public static final int 	AUTO_THRESHOLD = 65535;
	public int threshold1	=	AUTO_THRESHOLD;
	public int threshold2 =	AUTO_THRESHOLD;
	public boolean enableAnalogs = false;
	public String title = null;

	public void toBitStream(BitStream bs) {
		boolean auto = threshold1 == AUTO_THRESHOLD || threshold2 == AUTO_THRESHOLD;
		bs.addWord(auto?1:0,1);
		bs.addWord(enableAnalogs ? 1:0,1);
		if (!auto) {
			bs.addWord(threshold1,10);
			bs.addWord(threshold2,10);
		}
		if (title!=null && title.length()>0) {
			bs.addWord(1,1);
			bs.addOptString(title);
		} else {
			bs.addWord(0,1);
		}
		
	}
	
	public Settings(BitStream bs) {
		boolean auto =  bs.readWord(1) !=0;
		enableAnalogs =  bs.readWord(1) !=0;
		
		if (auto) {
			threshold1 = AUTO_THRESHOLD;
			threshold2 = AUTO_THRESHOLD;
		} else {
			threshold1 = (int) bs.readWord(10);
			threshold2 = (int) bs.readWord(10);
		}
		
		if (bs.readWord(1)!=0) title = bs.readOptString();
		
	}
		
	public Settings(int thr1,int thr2, boolean analogs) {
		
		threshold1=thr1;
		threshold2=thr2;
		enableAnalogs=analogs;
		
	}
	
	public Settings(String str) throws EnergProtocolException {
		String[] tok = str.split("\t");
		int l = tok.length;
		if (l<3) throw new EnergProtocolException("Invalid settings string: "+str);
		try {
			if (tok[0].compareTo("auto")==0) {
				threshold1 = AUTO_THRESHOLD;
			} else {
				threshold1 = Integer.parseInt(tok[0]);
			}
			
			if (tok[1].compareTo("auto")==0) {
				threshold2 = AUTO_THRESHOLD;
			} else {
				threshold2 = Integer.parseInt(tok[1]);
			}
		} catch(NumberFormatException err) {
			throw new EnergProtocolException("Invalid settings string: "+str);
		}
		
		enableAnalogs = tok[2].contains("analog");
		if (l>3) title = tok[3];
				
	} 
	
	public Settings() {	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		if (threshold1 != AUTO_THRESHOLD) str.append(threshold1); else str.append("auto");
		str.append('\t');
		if (threshold2 != AUTO_THRESHOLD) str.append(threshold2); else str.append("auto");
		str.append('\t');
		if (enableAnalogs) str.append("analog"); else str.append("digital");
		if (title!=null) {
			str.append('\t');
			str.append(title);
		}
		return str.toString();
	}

	public void saveTo(String file) throws IOException {
		BitStream bs = new BitStream(BitStream.DYNAMIC_SIZE);
		bs.addWord(0xEBF4, 16);
		toBitStream(bs);
		byte[] data = bs.getBytesAtCursor();
		Util.filePutContents(file, data);
	}
	
	public void saveToText(String file) throws IOException {
		byte[] data = this.toString().getBytes();
		Util.filePutContents(file, data);
	}
	
	public static Settings fromFile(String file) throws IOException, EnergProtocolException {
		byte[] data = Util.fileGetContents(file);
		BitStream bs = new BitStream(data);
		if (bs.readWord(16)!=0xEBF4) throw new EnergProtocolException("Invalid file `"+file+"`");
		return new Settings(bs);
	}
	
	public static Settings fromTextFile(String file) throws IOException, EnergProtocolException {
		byte[] data = Util.fileGetContents(file);
		return new Settings( new String(data).trim() ) ;
	}
}
