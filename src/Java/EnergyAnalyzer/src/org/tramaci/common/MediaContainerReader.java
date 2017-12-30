package org.tramaci.common;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.tramaci.common.BitStream;

public class MediaContainerReader {

	private RandomAccessFile file = null;
	private long startTcr = 0;
	private long currentTcr = 0;
	private long length = 0;
	private byte[] headerData = null;
	private int[] containerStruct = null;
	private int blockSizeBits = 0;
	private int blockTcrBits = 0;
	private int blockHeaderBits = 0;
	private int blockHeaderSize = 0;
	private int startPox = 0;
	private int indexLen=0;
	private int[] index = null;
	private int[][] indexStruct = null;
	private long endPtr = 0;
	private byte[] metadata = null;
	
	public byte[] getMetadata() { return metadata; }
	
	public long getStartTcr() { return startTcr; } 
	
	public int getIndexLength() { return indexLen; }
	
	public int[] getIndexStruct(int pox) {
		if (index==null || pox<0 || pox>=indexLen) throw new IllegalArgumentException("Index not loaded or invalid index");
		return indexStruct[pox];
	}
	
	public void seek(int pox) throws IOException {
		if (index==null)  throw new IllegalArgumentException("No index");
		if (pox<0 || pox>=indexLen) throw new IllegalArgumentException("Invalid index "+pox+" "+indexLen);
		currentTcr = startTcr+pox*1000;
		file.seek(index[pox]);
	}
	
	public int[] getStruct() { return containerStruct.clone(); }
	
	public byte[] getHeader() { return headerData.clone(); }
	
	public long getCurrentTime() { return currentTcr; }
	
	public MediaContainerReader(String fileIn,int magicNumber) throws IOException {
		short hi = (short) (magicNumber>>16);
		short lo = (short) (magicNumber&65535);
		
		file =new RandomAccessFile(fileIn,"r");
		int r = file.readShort();
		if (r!=hi) error("Bad file magic number");
		endPtr = file.readInt();
		
		r = file.readShort();
		byte[] head = new byte[r];
		
		r = file.readShort();
		headerData = new byte[r];
		
		startTcr= file.readLong();
		currentTcr= startTcr;
		
		file.read(head);
		file.read(headerData);
		
		r = file.readShort();
		if (r!=lo) error("Bad file start magic number");
		
		BitStream bs = new BitStream(head);
		blockTcrBits = (int) bs.readWord();
		blockSizeBits = (int) bs.readWord();
		blockHeaderBits = (int) bs.readWord();
		containerStruct = bs.readArray();
		
		blockHeaderSize = blockHeaderBits>>3;
		if (0!=(blockHeaderBits&7)) blockHeaderSize++;
		length= endPtr>0 ? endPtr : file.length();
		startPox = (int) file.getFilePointer();
		if (endPtr==0) endPtr = file.length();
		readIndex();
		
		if (endPtr!=file.length()) {
			file.seek(endPtr);
			int sz = file.readInt();
			metadata = new byte[sz];
			file.read(metadata);
			file.seek(startPox);
		}
		
	}
	
	public boolean eof() {
		try {
			return file.getFilePointer() >= endPtr;
		} catch (IOException e) {
			//e.printStackTrace();
			return true;
		}
	}
	
	public byte[] getBlock() throws IOException {
		if (eof()) throw new IOException("Input past end");
		byte[] head = new byte[blockHeaderSize];

		file.read(head);
		BitStream bs = new BitStream(head);
		long tcr = bs.readWord(blockTcrBits);
		int j = (int) bs.readWord(blockSizeBits);
		currentTcr =  tcr+=startTcr;
		byte[] data = new byte[j];
		file.read(data);
		return data;
	}
	
	private void readIndex() throws IOException {
		indexLen=0;
		index=new int[0];
		indexStruct = new int[0][0];
		System.gc();
		
		file.seek(startPox);
		
		byte[] head = new byte[blockHeaderSize];
		long ptr = file.getFilePointer();
		int mTim = -1;
		while(ptr<length) {
			file.read(head);
			BitStream bs = new BitStream(head);
			long tcr = bs.readWord(blockTcrBits);
			int tim =  (int) Math.floor(tcr/1000);
	
			int len = (int) bs.readWord(blockSizeBits);
			int[] struct = bs.readStruct(containerStruct);
			file.skipBytes(len);
			
			if (mTim!=tim) {
				mTim=tim;
				if (indexLen>=index.length) addCapacity();
				index[indexLen] = (int) ptr;
				indexStruct[indexLen] = struct;
				indexLen++;
			}
			
			ptr = file.getFilePointer();
		}
		file.seek(startPox);
	}
	
	private void addCapacity() {
		int j = index.length;
		int k = j + 30;
		int[] n = new int[k];
		System.arraycopy(index, 0, n, 0,j);
		index=n;
		n=null;
		int[][] s = new int[k][];
		System.arraycopy(indexStruct, 0, s, 0,j);
		indexStruct=s;
		s=null;
		System.gc();
	}

	private void error(String st) throws IOException {
		if (file!=null) try { file.close(); } catch(Exception devNull) {}
		throw new IOException(st);
	}
	
	public void close() throws IOException {
		file.close();
	}
	
}


