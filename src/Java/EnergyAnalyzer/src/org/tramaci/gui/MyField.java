package org.tramaci.gui;

import java.awt.Color;

public class MyField {

	private int length = 8;
	private String value =null;
	public Color color = Color.WHITE;
	public Color background = Color.BLACK;
	public boolean padLeft = false;
	
	public MyField(MyField o) {
		length=o.length;
		value=o.value;
		color= o.color;
		background = o.background;
		padLeft = o.padLeft;
	}
	
	public MyField(String val,int len) {
		length = len>0 ? len :1;
		setValue(val);
	}
		
	public MyField(String val,int len, Color ink, Color pap, boolean left) {
		length = len;
		color=ink;
		background=pap;
		padLeft=left;
		setValue(val);
	}
	
	public int getLength() { return length; }
	
	public void setValue(String str) {
		int i = str.length();
		if (length==0) length = i;
		
		if (i>length) {
			str=str.substring(0,length);
		} else if (i<length) {
			char[] ch = new char[length];
			if (padLeft) {
				for (int c=0;c<length-i;c++) {
					ch[c]=32;
				}
				System.arraycopy(str.toCharArray(), 0, ch, length-i, i);
			} else {
				for (int c=i;c<length;c++) {
					ch[c]=32;
				}
				System.arraycopy(str.toCharArray(), 0, ch, 0, i);
			}
			str = new String(ch);
		}
		value=str;

	}
	
	public String getValue() { return value; }
	
}
