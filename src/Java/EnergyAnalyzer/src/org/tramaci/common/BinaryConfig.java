package org.tramaci.common;

import java.util.HashMap;

import org.tramaci.protocol.EnergProtocolException;

public class BinaryConfig {

	private HashMap<String,HashMap<String,Object>> ini = null;
	
	public HashMap<String,Object> getSection(String name) {
		HashMap<String,Object> out = ini.get(name);
		if (out==null) {
			out = new HashMap<String,Object>();
			ini.put(name, out);
			}
		return out;
	}
	
	public void setSection(String name,HashMap<String,Object> data) {
		HashMap<String,Object> out = ini.get(name);
		if (out==null) {
			ini.put(name, data);
		} else {
			for (String k:data.keySet()) {
				out.put(k, data.get(k));
			}
		}
	}
	
	public Object get(String section,String param, Object defaultValue) {
		HashMap<String,Object> out = getSection(section);
		Object value = out.get(param);
		if (value==null) {
			value=defaultValue;
		} else if (value.getClass() != defaultValue.getClass() ) {
			System.err.print("Config: bad var type `"+section+"."+param+"`\n");
			return value;
		}
		return value;
	}
	
	public void set(String section, String param, Object value) {
		HashMap<String,Object> out = getSection(section);
		out.put(param, value);
	}
	
	public Object get(String main,String section, String param, Object value) {
		Object v = get(main,param,value);
		return get(section,param,v);
	}
	
	public BinaryConfig() {
		
		 ini = new HashMap<String,HashMap<String,Object>>();
		
	}
	
	public BinaryConfig(BitStream bs) throws EnergProtocolException {
		
		ini = new HashMap<String,HashMap<String,Object>>();
		
		int js = (int) bs.readWord();
		for (int is = 0; is<js; is++) {
			String section = bs.readString();
			HashMap<String,Object> data = getSection(section);
			int j = (int) bs.readWord();
			for (int i =0 ; i<j; i++) {
				String key = bs.readString();
				int type = (int) bs.readWord(4);
				Object value  = null;
				
				switch(type) {
				
					case 2:
						value = (int) bs.readWord();
						break;
				
					case 4:
						value = (long) bs.readWord();
						break;
						
					case 1:
						value = bs.readWord(1)  !=0 ;
						break;
						
					case 5:
						value = (int[]) bs.readArray();
						break;
						
					case 6:
						value = (float) bs.readFloat();
						break;
						
					case 7:
						value = (String) bs.readString();
						break;
						
					case 0:
						break;
						
					default:
							throw new EnergProtocolException("Invalid bits sequence");
					
				}
				
				data.put(key, value);
				
			}
		}
	}
	
	public BitStream toBitStream(BitStream bs) {
		int j = ini.size();
		if (bs==null) bs = new BitStream(BitStream.DYNAMIC_SIZE);
		bs.addWord(j);
		
		for (String section:ini.keySet()) {
			bs.addString(section);
			HashMap<String,Object> data = getSection(section);
			bs.addWord(data.size());
			
			for (String key:data.keySet()) {
				bs.addString(key);
				Object val = data.get(key);
				
				if (val instanceof Integer) {
					bs.addWord(2,4);
					bs.addWord((int) val);
					
				} else 	if (val instanceof Long) {
					bs.addWord(4,4);
					bs.addWord((long) val);
					
				} else if (val instanceof Boolean) {
					bs.addWord(1,4);
					bs.addWord( (boolean) val ? 1 : 0, 1 );
					
				} else if (val instanceof Integer[]) {
					bs.addWord(5,4);
					bs.addArray((int[]) val);
					
				} else if (val instanceof Float) {
					bs.addWord(6,4);
					bs.addFloat((float) val);
					
				}  else if (val instanceof String) {
					bs.addWord(7,4);
					bs.addString((String) val);
					
				} else {
					bs.addWord(0,4);
				}
						
			}
			
		}
		
		return bs;
	}
	
}
