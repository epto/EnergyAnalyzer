package org.tramaci.common;


public class BitStream {
	public static final int MAX_WORD_LENGTH=62;
	public static final int DYNAMIC_SIZE = 0xF000F6C7;
	
	private byte[] data = null;
	private int cursor = 0;
	private int max = 0;
	private boolean isDynamic = false;
	
	private void checkCapacity(int n) {
		if (cursor+n<=max) return;
		int by = n>>3;
		if (0!=(n&7)) by++;
		int j = data.length;
		int k= j+by;
		byte[] nd = new byte[k];
		if (data.length>0) System.arraycopy(data, 0, nd, 0, j);
		data=nd;
		max = (data.length<<3) -1;
	}
	
	public BitStream(int numBits) {
		if (numBits==DYNAMIC_SIZE) {
			isDynamic=true;
			numBits=0;
		} else {
			if (numBits<1) throw new IllegalArgumentException("Invalid BitStream size");
		}
		int h = numBits >> 3;
		if ((numBits&7)!=0) h++;
		data = new byte[h];
		max=numBits-1;
		
	}
	
	public BitStream(byte[] in) {
		data = in.clone();
		max = (data.length<<3) -1;
	}
	
	public void clear() {
		data = new byte[ data.length ];
		cursor = 0;
	}
	
	public int getPosition() { return cursor; }
	
	public int getPositionByte() { 
		int p = cursor>>3;
		if (0!=(cursor&7)) p++;
		return p; 
		}
	
	public int getAvailableBits() {
		return data.length*8 - cursor;
	}
	
	public void rewind() { cursor = 0; }
	
	public void addWord(long word,int wordLength) {
		if (isDynamic) checkCapacity(wordLength);
		
		for (int i=0;i<wordLength;i++) {
			long b = 1L<<i;
			if ((word & b)!=0) {
				int h = cursor>>3;
				int l = cursor&7;
				data[h] |= 1<<l;
			}
			cursor++;
		}
	}
	
	public long readWord(int wordLength) {
		long word = 0;
		long b = 1L<<wordLength;
		for (int i=0;i<wordLength;i++) {
					
			int h = cursor>>3;
			int l = cursor&7;
			if ((data[h] & (1<<l))!=0) word|=b;
			
			word>>=1;
			cursor++;
		}
		return word;
	}
	
	public void addStruct(int[] word,int[] struct) {
		int j = struct.length;
		for (int i=0;i<j;i++) addWord(word[i],struct[i]);
	}
	
	public int[] readStruct(int[] struct) {
		int j = struct.length;
		int[] out = new int[j];
		for (int i=0;i<j;i++) {
			out[i] = (int) readWord(struct[i]);
		}
		return out;
	}
	
	public static int getStructLen(int[] struct,int add) {
		int j = struct.length;
		for (int i=0;i<j;i++) {
			add+=struct[i];
		}
		return add;
	}
	
	public void addWord(long word) {
		int len = 1;
		long test=word;
		for (int i=0;i<=MAX_WORD_LENGTH;i++) {
			if (test==0) {
				len=i;
				break;
			}
			test>>=1;
		}
		addWord(len,6);
		addWord(word,len);
	}
	
	public long readWord() {
		int len = (int) readWord(6);
		return readWord(len);
	}
	
	public void addString(String s) {
		int bitLength = 8;
		char[] ch = s.toCharArray();
		int j = ch.length;
		int ori = 0;
		
		for (int i=0;i<j;i++) {
			ori|=ch[i];
		}
		
		for (int i=16;i>-1;i--) {
			if ((ori&1<<i)!=0) {
				bitLength=i+1;
				break;
			}
		}
		
		addWord(j);
		addWord(bitLength,5);
		if (isDynamic) checkCapacity(bitLength*j);
		for (int i=0;i<j;i++) {
			addWord(ch[i],bitLength);
		}
	}
	
	public String readString() {
		int j = (int) readWord();
		if (j>65535) throw new IllegalArgumentException("Invalid bitstream");
		char[] ch = new char[j];
		int bitLength= (int) readWord(5);
		for (int i=0;i<j;i++) {
			ch[i] = (char) readWord(bitLength);
		}
		return new String(ch);
	}
	
	public void addArray(int[] arr) {
		int j = arr==null ? 0 :  arr.length;
		int bitLength = 1;
		
		addWord(j);
		int ori = 0;
		for (int i=0;i<j;i++) {
			ori|=arr[i];
		}
		
		for (int i=0;i<32;i++) {
			if (ori==0) {
				bitLength=i;
				break;
			}
			ori>>=1;
		}
		
		addWord(bitLength,6);
		if (isDynamic) checkCapacity(bitLength*j);
		for (int i=0;i<j;i++) {
			addWord(arr[i],bitLength);
		}
		
	}
	
	public int[] readArray() {
		int j = (int) readWord();
		int bitLength= (int) readWord(6);
		int[] out = new int[j];
		for (int i=0;i<j;i++) {
			out[i] = (int) readWord(bitLength);
		}
		return out;
	}
	
	public byte[] getBytes() { return data; }
	
	public byte[] getBytesAtCursor() {
		int hi = cursor>>3;
		if ((cursor&7)!=0) hi++;
		byte[] out = new byte[hi];
		System.arraycopy(data, 0, out, 0, hi);
		return out;
	}
	
	public void addBitStream(BitStream bs) {
		int len = bs.cursor;
		addWord(len);
		if (isDynamic) checkCapacity(bs.cursor);
		
		for (int i = 0; i<len; i++) {
			int rh = i >>3;
			int rl =i&7;
			int rb = 1<<rl;
			if (0!=(bs.data[rh] & rb)) {
				int h = cursor>>3;
				int l = cursor&7;
				data[h]|=1<<l;
			}
			cursor++;
		}
	}
	
	public BitStream readBitStream() {
		int len = (int) readWord();
		BitStream bs = new BitStream(len);
		for (int i = 0; i<len;i++) {
		
			int h = cursor>>3;
			int l = cursor&7;
			int wh = i>>3;
			int wl = i&7;
			
			if (0!=(data[h]&(1<<l))) bs.data[wh]|=(1<<wl);
			
		}
		return bs;
	}
		
	public void addFloat(float val) {
		long hi = (long) Math.floor(val);
		float lof = Math.abs(val-hi);
		long lo = (long) (1.0d / lof);
		addWord(hi);
		addWord(lo);
	}
	
	public float readFloat() {
		long hi = readWord();
		long lo = readWord();
		float lof = (1.0f / (float) lo);
		lof+=hi;
		return lof;
	}

	public void addOptString(String str) {
		char[] ch = str.toCharArray();
		char min = 0xFFFF;
		char max = 0;
		int j = ch.length;
		if (j>65535) throw new IllegalArgumentException("String too long");
		
		for (int i = 0;i<j;i++) {
			if (ch[i]<min) min=ch[i];
		}

		for (int i = 0;i<j;i++) {
			ch[i]-=min;
			if (ch[i]>max) max = ch[i];
		}
		
		int stringBits = getWordBits(max);
		int minBits = getWordBits(min);
		int lenBits = getWordBits(j);
		
		addWord(lenBits,4);
		addWord(minBits,4);
		addWord(stringBits,4);
		
		addWord(j,lenBits);
		addWord(min,minBits);

		for (int i=0;i<j;i++) {
			addWord(ch[i],stringBits);
		}
				
	}
	
	public String readOptString() {
		
		int lenBits = (int) (readWord(4));
		int minBits = (int) (readWord(4));
		int stringBits = (int) (readWord(4));
		int len = (int) (readWord(lenBits));
		char min = (char) (readWord(minBits));
		
		char[] ch = new char[len];
		for (int i = 0; i<len;i++) {
			ch[i] = (char) (min + readWord(stringBits));
		}
		return new String(ch);
	}

	public static int getWordBits(long word) {
		int len = 1;
		long test=word;
		for (int i=0;i<=MAX_WORD_LENGTH;i++) {
			if (test==0) {
				len=i;
				break;
			}
			test>>=1;
		}
		return len;
	}
	
	public static void parseTree(byte[] tree, byte[] data) {
		int j = data.length;
		for (int i=0;i<j;i++) {
			int b = data[i]&255;
			int h = b>>3;
			int l = b&7;
			tree[h]|= 1<<l;
		}
	}
	
	public void pakBytes(byte[] ch) {
		int j = ch.length;
		addWord(j);
		byte[] tree = new byte[32];
		parseTree(tree, ch);
		byte[] pal = new byte[256];
		int cb=0;
		
		packTree(tree);
		
		for (int i=0;i<256;i++) {
			if ( BitStream.getBitInArray(i, tree) ) {
				pal[i] = (byte) (255&cb++);
			}
		}
		int wordSize = BitStream.getWordBits(cb);

		for (int i = 0;i<j;i++) {
			addWord( pal[ ch[i] &255 ] , wordSize);
		}
		
	}
	
	public byte[] unPakBytes() {
		int j = (int) readWord();
		byte[] tree = unPackTree();
		byte[] pal = new byte[256];
		int cb=0;
		
		for (int i=0;i<256;i++) {
			if ( BitStream.getBitInArray(i, tree) ) {
				pal[ cb++ ] = (byte) i;
			}
		}
		int wordSize = BitStream.getWordBits(cb);
		
		byte[] msg = new byte[j];
		for (int i=0;i<j;i++) {
			msg[i] = pal[ (int) readWord(wordSize) ];
		}
		return msg;
	}
	
	
	public static boolean[] testWordArray(byte[] buf,int numBlocks,int size) {
		boolean[] out = new boolean[size / numBlocks];
		for (int i = 0 ; i<size; i++) {
			int hi = i>>3;
			int lo = i&7;
			int bl =(int) Math.floor( i / numBlocks) ;
			out[bl]|= 0!=((1<<lo)&buf[hi]);
		}
		return out;
	}

	public void packTree(byte[] buf) {
		boolean[] out = testWordArray(buf,32,256);
		int j = out.length;
		for (int i=0;i<j;i++) {
			addWord(out[i]?1:0,1);
			if (!out[i]) continue;
			int bitPox = 4 * i;
			addByteArray(buf,8,bitPox,4);
		}
		
	}

	public byte[] unPackTree() {
		byte[] out = new byte[32];
		for (int i = 0; i<8;i++) {
			int k = (int) readWord(1);
			if (k==0) continue;
			for (int a =0; a<4;a++) {
				out[ 4*i+a] = (byte) (255&readWord(8));
			}
		}
		return out;
	}

	public void addByteArray(byte[] arr,int wordLen,int from,int len) {
		
		if (isDynamic) checkCapacity(wordLen*len);
		for (int i=0;i<len;i++) {
			addWord(arr[from+i],wordLen);
		}
		
	}
	
	public static boolean getBitInArray(int pox,byte[] data) {
		int hi = pox>>3;
		int lo = pox&7;
		return 0!= (data[hi]&(1<<lo));
	}
	
}
