package org.tramaci.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Util {

	public static byte[] fileGetContents(String name) throws IOException {
		FileInputStream f = new FileInputStream(name);
		int sz = f.available();
		byte[] data = new byte[sz];
		f.read(data);
		f.close();
		return data;
	}
	
	public static void filePutContents(String name,byte[] data) throws IOException {
		FileOutputStream f = new FileOutputStream(name);
		f.write(data);
		f.close();
	}
	
	public static String number(int ni,int l,int base, char pad) {
		char[] o = new char[l];
		char[] n = Integer.toString(ni,base).toCharArray();
		int ln = n.length;
		if (ln<l) {
			int x = l-ln;
			int p = 0;
			
			for (int i=0;i<x;i++) {
				o[p++]=pad;
			}
			
			for (int i=0;i<ln;i++) {
				o[p++] =n[i];
			}
			
		} else if (ln>l) {
			o=n;
			o[l-1] = 0x2026;
		} else {
			o=n;
		}
		return new String(o);
	}


}
