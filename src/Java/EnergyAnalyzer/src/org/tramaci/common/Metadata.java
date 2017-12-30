package org.tramaci.common;

import java.io.IOException;

import org.tramaci.common.Util;
import org.tramaci.protocol.EnergProtocolException;
import org.tramaci.protocol.Settings;

public class Metadata {
	
	public static final int TYPE_BOOLEAN 					= 1;
	public static final int TYPE_INTEGER 					= 2;
	public static final int TYPE_LONG 							= 3;
	public static final int TYPE_STRING 						= 4;
	public static final int TYPE_ARRAY_INT 				= 5;
	public static final int TYPE_PROPERTYLIST 			= 6;
	
	public static final int FIELD_USER 							= 0;
	public static final int FIELD_BOARD 						= 1;
	public static final int FIELD_ANTENNA 					= 2;
	public static final int FIELD_DESCRIPTION			= 3;
	public static final int FIELD_THRESHOLD				= 64;
	public static final int FIELD_TCR 							= 65;
	
	public Settings settings = null;
	public String text = null;
	public byte[] setput = null;
	public String setputText = null;
	
	private int[] keyId = new int[0];
	private Object[] value = new Object[0];
	
	public void set(int key,int val) { 				addSet(key,val); 	}
	public void set(int key,long val) { 				addSet(key,val); 	}
	public void set(int key,boolean val) {		addSet(key,val); 	}
	public void set(int key,int[] val) { 				addSet(key,val); 	}
	public void set(int key,String val) { 			addSet(key,val); 	}
	public void set(int key,Metadata val) {	addSet(key,val); 	}
	
	public boolean getBool(int key,boolean def) {							return (boolean) 		get(key,TYPE_BOOLEAN,			def)	;	}
	public int getInt(int key,int def) {												return (int) 				get(key,TYPE_INTEGER,				def)	;	}
	public long getLong(int key,long def) {										return (long) 			get(key,TYPE_LONG,					def)	;	}
	public String getString(int key,String def) {									return (String) 			get(key,TYPE_STRING,				def)	;	}
	public int[] getIntArray(int key,int[] def) {								return (int[]) 			get(key,TYPE_ARRAY_INT,		def)	;	}
	public Metadata getPropertyList(int key,Metadata def) {	return (Metadata)	get(key,TYPE_PROPERTYLIST,	def)	;	}
	
	
	private void addSet(int k,Object val) {
		int j = keyId.length;
		for (int i=0;i<j;i++) {
			if (k == keyId[i]) {
				value[i]=val;
				return;
			}
		}
		
		int[] nk = new int[j+1];
		Object[] nv = new Object[j+1];
		System.arraycopy(keyId, 0, nk, 0, j);
		System.arraycopy(value, 0, nv, 0, j);
		keyId=nk;
		value=nv;
		keyId[j]=k;
		value[j]=val;
		
	}
	
	public Object get(int key,int type, Object defaultValue) {
		Object val = defaultValue;
		for (int i =0 ; i<keyId.length; i++) {
			if ( i == keyId[i]) {
				val = value[i];
				
				switch(type) {
				case TYPE_BOOLEAN:
					if (!(val instanceof Boolean)) return defaultValue;
					break;
					
				case TYPE_INTEGER:
					if (!(val instanceof Integer)) return defaultValue;
					break;
					
				case TYPE_LONG:
					if (!(val instanceof Long)) return defaultValue;
					break;
					
				case TYPE_STRING:
					if (!(val instanceof String)) return defaultValue;
					break;
					
				case TYPE_ARRAY_INT:
					if (!(val instanceof Integer[])) return defaultValue;
					break;
					
				case TYPE_PROPERTYLIST:
					if (!(val instanceof Metadata)) return defaultValue;
					break;
				
				default:
					return defaultValue;
				}
			}
		}
		return val;
	}
	
	public Object get(int key, Object defaultValue) {
		for (int i =0 ; i<keyId.length; i++) {
			if ( key == keyId[i]) {
				if ( value[i].getClass() != defaultValue.getClass()) return defaultValue;
				return value[i];
			}
		}
		return defaultValue;
	}
	
	
	public Metadata() {}


	public Metadata(BitStream bs) throws EnergProtocolException {
		
		int j= (int) bs.readWord();
		keyId = new int[j];
		value = new Object[j];
		
		for (int i = 0; i<j;i++) {
			keyId[i] = (int) bs.readWord();
			int type = (int) bs.readWord(3);
						
			switch(type) {
			
				case TYPE_BOOLEAN:
					value[i] = (bs.readWord(1)!=0 ? true : false);
					break;
					
				case TYPE_INTEGER:
					value[i] = (int) bs.readWord();
					break;
					
				case TYPE_LONG:
					value[i] = bs.readWord();
					break;
					
				case TYPE_STRING:
					value[i] = bs.readString();
					break;
					
				case TYPE_ARRAY_INT:
					value[i] = bs.readArray();
					break;
					
				case TYPE_PROPERTYLIST:
					BitStream sub= bs.readBitStream();
					value[i] = new Metadata(sub);
					break;
					
				default:
					throw new EnergProtocolException("Unknown PropertyList type in BitStream");
			
			}
		}

		if (bs.readWord(1)!=0) settings = new Settings(bs); else settings = null;
		if (bs.readWord(1)!=0) text = bs.readOptString(); else text = null;
		if (bs.readWord(1)!=0) setputText = bs.readOptString(); else setputText = null;
		if (bs.readWord(1)!=0) setput = bs.unPakBytes(); else setput = null;
		
	}
	
	public void toBitStream(BitStream bs) {
		int j = keyId.length;
		bs.addWord(j);
		
		for (int i=0;i<j;i++) {
			bs.addWord(keyId[i]);
			if (value[i] instanceof Metadata) {
				
					bs.addWord(TYPE_PROPERTYLIST,3);
					BitStream sub = new BitStream(BitStream.DYNAMIC_SIZE);
					Metadata p = (Metadata) value[i];
					p.toBitStream(sub);
					bs.addBitStream(sub);
					sub=null;
					System.gc();
					
				} else {
												
					if (value[i] instanceof Boolean) {
						bs.addWord(TYPE_BOOLEAN,3);
						bs.addWord(((boolean)value[i])?1:0,1);
						continue;
					} 
					
					if (value[i] instanceof Integer) {
						bs.addWord(TYPE_INTEGER,3);
						bs.addWord((long) value[i]);
						continue;
					} 
					
					if (value[i] instanceof Long) {
						bs.addWord(TYPE_LONG,3);
						bs.addWord((long) value[i]);
						continue;
					} 
					
					if (value[i] instanceof String) {
						bs.addWord(TYPE_STRING,3);
						bs.addString((String) value[i]);
						continue;
					} 
			
					if (value[i] instanceof Integer[]) {
						bs.addWord(TYPE_ARRAY_INT,3);
						bs.addArray((int[]) value[i]);
						continue;
					} 
					//System.out.print("Not saved data "+value[i].getClass().getName()+" "+value[i].toString()+"\n");
			}
		}
		
		if (settings!=null) {
			bs.addWord(1,1);
			settings.toBitStream(bs);
		} else {
			bs.addWord(0,1);
		}
	
		if (text!=null) {
			bs.addWord(1,1);
			bs.addOptString(text);
		} else {
			bs.addWord(0,1);
		}

		if (setputText!=null) {
			bs.addWord(1,1);
			bs.addOptString(setputText);
		} else {
			bs.addWord(0,1);
		}

		if (setput!=null && setput.length>0) {
			bs.addWord(1,1);
			bs.pakBytes(setput);
		} else {
			bs.addWord(0,1);
		}
	
	}	

	public void saveTo(String file) throws IOException {
		
		if (file.endsWith(".src")) {
			Util.filePutContents(file,this.toString().getBytes());
			return;
		}
		
		if (file.endsWith(".setput")) {
			Util.filePutContents(file, setput!=null ? setput : new byte[0]);
			return;
		}
		
		if (file.endsWith(".setpak")) {
			BitStream bs = new BitStream(BitStream.DYNAMIC_SIZE);
			byte[] b = setput!=null ? setput : new byte[0];
			bs.pakBytes(b);
			Util.filePutContents(file, bs.getBytesAtCursor());
			return;
		}
		
		if (file.endsWith(".txt")) {
			Util.filePutContents(file, text!=null ? text.getBytes() : new byte[0]);
			return;
		}
		
		if (file.endsWith(".pak")) {
			BitStream bs = new BitStream(BitStream.DYNAMIC_SIZE);
			byte[] b = text!=null ? text.getBytes() : new byte[0];
			bs.pakBytes(b);
			Util.filePutContents(file, bs.getBytesAtCursor());
			return;
		}
		
		if (file.endsWith(".septex")) {
			Util.filePutContents(file, setputText!=null ? setputText.getBytes() : new byte[0]);
			return;
		}

		if (file.endsWith(".ebfs")) {
			if (settings == null) settings = new Settings();
			settings.saveTo(file);
			return;
		}
		
		if (file.endsWith(".ebts")) {
			if (settings == null) settings = new Settings();
			settings.saveToText(file);
			return;
		}
		
		BitStream bs  = new BitStream(BitStream.DYNAMIC_SIZE);
		bs.addWord(0xEBF5,16);
		toBitStream(bs);
		byte[] data = bs.getBytesAtCursor();
		Util.filePutContents(file, data);
		
	}
	
	public Metadata loadFrom(String file) throws IOException, EnergProtocolException {
		
		if (file.endsWith(".ebs")) {
			settings = Settings.fromFile(file);
			return this;
		}
		
		if (file.endsWith(".ets")) {
			settings = Settings.fromTextFile(file);
			return this;
		}
		
		byte[] data = Util.fileGetContents(file);
		
		if (file.endsWith(".setput")) {
			setput=data;
			return this;
		}
		
		if (file.endsWith(".setpak")) {
			BitStream bs = new BitStream(data);
			setput = bs.unPakBytes();
			return this;
		}
		
		if (file.endsWith(".txt")) {
			text = new String(data);
			return this;
		}
		
		if (file.endsWith(".pak")) {
			BitStream bs = new BitStream(data);
			text = new String(bs.unPakBytes());
			return this;
		}
		
		if (file.endsWith(".septex")) {
			setputText = new String(data);
			return this;
		}
		
		BitStream bs  = new BitStream(data);
		if (bs.readWord(16)!=0xEBF5) throw new EnergProtocolException("Bed meta file `"+file+"`");
		return new Metadata(bs);		
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(this.getClass().getSimpleName()+" {");
		s.append('\n');
		int j = keyId.length;
		for (int i=0;i<j;i++) {
			s.append("\tItem: ");
			s.append(keyId[i]);
			s.append('\t');
			s.append(value[i].toString());
			s.append('\n');
		}
		s.append("\tSettings: ");
		if (settings==null) s.append("null"); else s.append(settings.toString());
		s.append("\n\tSetputText: {\n\t\t");
		s.append(setputText!=null ? setputText.replace("\n", "\n\t\t") : "null");
		s.append("\n\t}\n\tText: {\n\t\t");
		s.append(text!=null ? text.replace("\n", "\n\t\t") : "null");
		s.append("\n\t}");
		s.append("\n\tSetput: {");
		if (setput!=null) {
			int js = setput.length;
			int md = 0;
			for (int is = 0; is<js ;is++) {
				if (md==0) s.append("\n\t\t");
				s.append(Util.number(255&setput[is],2, 16, '0'));
				md++;
				if (md<16) s.append(' '); else md=0;
			}
			s.append('\n');
		} else {
			s.append("\n\t\tnull\n");
		}
		s.append("\t\t}\n}\n");
		return s.toString();
	}
	
}

