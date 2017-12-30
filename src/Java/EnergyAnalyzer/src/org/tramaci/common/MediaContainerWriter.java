package org.tramaci.common;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.tramaci.common.BitStream;

public class MediaContainerWriter {

	private RandomAccessFile file = null;
	private long startTcr = 0;
	private boolean headerOk = false;
	private byte[] headerData = null;
	private int[] containerStruct = null;
	private int blockSizeBits = 0;
	private int blockTcrBits = 0;
	private int blockHeaderBits = 0;
	//private int blockHeaderSize = 0;
	private int magicNumber = 0;
	private int headerPtr = 0;
	private int headerSize = 0;
	private byte[] metaData = null;
	
	public MediaContainerWriter(String fileIn,int magicNumberIn,int blockTcrBitsIn, int blockSizeBitsIn, int[] struct, byte[] header) throws IOException {
		file = new RandomAccessFile(fileIn,"rw");
		file.setLength(0);
		
		headerData = header.clone();
		containerStruct = struct.clone();
		blockTcrBits = blockTcrBitsIn;
		blockSizeBits = blockSizeBitsIn;
		magicNumber=magicNumberIn;
		blockHeaderBits = blockTcrBits + blockSizeBits;
		
		
		for (int i=0; i< struct.length;i++) {
			blockHeaderBits+=containerStruct[i];
		}
		
		// blockHeaderSize = blockHeaderBits>>3;
		// if (0!=(blockHeaderBits&7)) blockHeaderSize++;
		
	}
	
	public void close() throws IOException {
		
		if (metaData==null) {
			file.close();
			return;
		}
		
		short hi = (short) (magicNumber>>16);
		long ptr = file.getFilePointer();
		
		file.writeInt(metaData.length);
		file.write(metaData);
		metaData=null;
		
		file.seek(0);
		file.writeShort(hi);
		file.writeInt((int) ptr);
		file.close();
	}
	
	public void writeBlock(long tcr,byte[] data, int[] blockData) throws IOException {
		
		if (!headerOk) {
			headerOk=true;
			startTcr = tcr;
			writeHeader();
		}
		
		tcr=tcr-startTcr;
		BitStream bs = new BitStream(blockHeaderBits);
		bs.addWord(tcr,blockTcrBits);
		bs.addWord(data.length,blockSizeBits);
		bs.addStruct(blockData, containerStruct);

		file.write(bs.getBytes());
		file.write(data);
				
	}
	
	public void setMetadata(byte[] meta) {
		metaData=meta.clone();
	}
	
	public void updateHeaderData(byte[] data) throws IOException {
		if (headerData!=null) {
			headerData=data.clone();
			return;
		}
		
		if (data.length>headerSize) throw new IOException("Metadata too big");
		long ptr = file.getFilePointer();
		file.seek(headerPtr);
		file.write(data);
		file.seek(ptr);
		
	}
	
	private void writeHeader() throws IOException {
		short hi = (short) (magicNumber>>16);
		short lo = (short) (magicNumber&65535);
		if (headerData==null) headerData =  new byte[0];
		BitStream bs = new BitStream(8192);

		bs.addWord(blockTcrBits);
		bs.addWord(blockSizeBits);
		bs.addWord(blockHeaderBits);
		bs.addArray(containerStruct);
		
		byte[] head = bs.getBytesAtCursor();
		file.writeShort(hi);
		file.writeInt(0);
		file.writeShort(head.length);
		file.writeShort(headerData.length);
		file.writeLong(startTcr);
		file.write(head);
		headerPtr = (int) file.getFilePointer();
		headerSize = headerData.length;
		file.write(headerData);
		file.writeShort(lo);
		headerData=null;
		
	}

}
