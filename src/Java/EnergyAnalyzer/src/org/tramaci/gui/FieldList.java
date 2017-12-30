package org.tramaci.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class FieldList {
		
	public int left = 0;
	public int top = 0;
	public int horizontalPadding = 2;
	public int verticalPadding = 1;
	public boolean pseudoMonoSpace = true;
	private int gravity = 0;
	private int gravityX0 = 0;
	private int gravityY0 = 0;
	private int gravityLineNumber = 0;
	private JPanel gravityObject= null;
		
	public MyField[] field = new MyField[0];
	private int[] index = new int[0];
	
	public static MyField getFromArray(FieldList[] list,int k) {
		if (k<0) throw new IllegalArgumentException("Invalid field index "+k);
		
		int j = list.length;
		for (int i=0;i<j;i++) {
			if (k<list[i].index.length) {
				if (list[i].index[k]!=-1) return list[i].field[ list[i].index[k] ] ;
			}
		}
		throw new IllegalArgumentException("Invalid field index "+k);
	} 
	
	public static void set(FieldList[] list,int k, String str, Color color) {
		MyField field = FieldList.getFromArray(list, k);
		field.color = color;
		field.setValue(str);
	}
		
	public static void setValue(FieldList[] list,int k, String str) {
		FieldList.getFromArray(list, k).setValue(str);
	}
	
	public static void setColor(FieldList[] list,int k, Color color) {
		FieldList.getFromArray(list, k).color = color;
	}
	
	
	public int getGravity() { return gravity; };
	
	public void setGravity(int currentGravity,JPanel inObject,int lineNumber) {
		gravity = currentGravity;
		gravityObject = inObject;
		gravityLineNumber=lineNumber;
	}
	
	private void calcGravity() {
		boolean mono = 0==(gravity & Gravity.KERNING);
		
		Graphics g2 = gravityObject.getGraphics();
		Rectangle rect= gravityObject.getBounds();
		int j = field.length;
		double x = 0;
		Rectangle2D[] pox = new Rectangle2D[j];
		
		FontMetrics fm = g2.getFontMetrics();
		Rectangle2D chs = fm.getStringBounds("_", g2);
		
		double height = 1;
		double width =  1;
		
		for (int i=0;i<j;i++) {
			pox[i] = fm.getStringBounds(field[i].getValue(), g2);
			double he = chs.getHeight(); 
			double wh = chs.getWidth(); 
			if (he>height) height=he;
			if (wh>width) width=wh;
			}
		
		for (int i=0;i<j;i++) {
			int x0 = (int) (x - pox[i].getX());
			int wh =  (int) ( mono ? (width*field[i].getLength() + pox[i].getX()) : (pox[i].getWidth() + pox[i].getX()));
			int x1 = (int) (x0 +wh);
			x=x1;
		}
		
		width= x;
		gravityX0=left;
		gravityY0=top;
		int mGravity = gravity & Gravity.MASK;
		if (0!=(mGravity & Gravity.BOTTOM)) gravityY0 = (int) (rect.height - (height * gravityLineNumber) - top - height); else gravityY0 = (int) (top + gravityLineNumber* height); 
		if (0!=(mGravity & Gravity.RIGHT)) gravityX0 = (int) (rect.width-( width + 1 + left)); else gravityX0 = left;
				
	} 
	
	private void addIndex(int k,int v) {
		int j = index.length;
		if (k>=j) {
			int[] n = new int[k+1];
			for (int i=j;i<k;i++) {
				n[i]=-1;
			}
			System.arraycopy(index, 0, n, 0, j);
			index=n;
		} else {
			if (index[k]!=-1) throw new IllegalArgumentException("Field index already used "+k);
		}
		index[k] =v;
	}
	
	public MyField get(int k) {
		if (k<0 || k>=index.length||  index[k] <0) throw new IllegalArgumentException("Invalid field index "+k);
		return field[ index[k] ];
	}
	
	public void setValue(int k,String str) {
		if (k<0 || k>=index.length||  index[k] <0) throw new IllegalArgumentException("Invalid field index "+k);
		field[ index[k] ].setValue(str);
	}
	
	public void setColor(int k,Color c) {
		if (k<0 || k>=index.length||  index[k] <0) throw new IllegalArgumentException("Invalid field index "+k);
		field[ index[k] ].color = c;
	}
	
	public void set(int k,String str,Color c) {
		if (k<0 || k>=index.length||  index[k] <0) throw new IllegalArgumentException("Invalid field index "+k);
		field[ index[k] ].setValue(str);
		field[ index[k] ].color = c;
	}
	
	public int add(MyField f) { return add(-1,f); }
	
	public int add(int k,MyField f) {
		int j = field.length;
		MyField[] n = new MyField[j+1];
		System.arraycopy(field, 0, n, 0, j);
		n[j] = f;
		field=n;
		if (k>-1) addIndex(k,j);
		return j;
	}
	
	public void paintComponent(Graphics2D g2) {
		int curX0 = 0;
		int curY0 = 0;
		
		if (gravity!=0) {
			calcGravity();
			curX0 = gravityX0;
			curY0 = gravityY0;
		} else {
			curX0 = left;
			curY0 = top;
		}
		
		int j = field.length;
		double x = curX0;
		Rectangle2D[] pox = new Rectangle2D[j];
		
		FontMetrics fm = g2.getFontMetrics();
		Rectangle2D chs = fm.getStringBounds("_", g2);
		
		double height = 1;
		double width =  1;
		
		for (int i=0;i<j;i++) {
			pox[i] = fm.getStringBounds(field[i].getValue(), g2);
			double he = chs.getHeight(); 
			double wh = chs.getWidth(); 
			if (he>height) height=he;
			if (wh>width) width=wh;
			}
		
		for (int i=0;i<j;i++) {

			int x0 = (int) (x - pox[i].getX());
			int wh =  (int) ( pseudoMonoSpace ? (width*field[i].getLength() + pox[i].getX()) : (pox[i].getWidth() + pox[i].getX()) );
			int x1 = (int) (x0 +wh);
			x=x1;
			
			g2.setColor(field[i].background);			
			g2.fillRect(x0, curY0, wh, (int)height);
			
			g2.setColor(field[i].color);
			g2.drawString(field[i].getValue(),(int) (x0 - pox[i].getX()),(int)(curY0 - pox[i].getY()));
			
		}
			 
		
	}
		
}
