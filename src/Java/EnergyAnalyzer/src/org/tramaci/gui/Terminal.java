package org.tramaci.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

public class Terminal  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5290867674463786099L;

	private int length = 0;
	private int width = 0;
	private int height = 0;
	
	public Color borderColor = Color.GREEN;
	public Color color = Color.GREEN;
	public Color backgroundColor = Color.BLACK;
	public Font font =  null;
	public FieldList fieldList = null;
	
	private String[] lineString = null;
	private Color[] lineColor = null;
		
	public void echoTab(String st, Color c,int len) {
		st=st.trim();
		String[] tok = st.split("\t");
		int j = tok.length;
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<j;i++) {
			add(sb,tok[i],len);
		}
		echo(sb.toString(),c);
	}
	
	public void echoTab(String st, Color c,int len,int from) {
		st=st.trim();
		String[] tok = st.split("\t");
		int j = tok.length;
		StringBuilder sb = new StringBuilder();
		for (int i=from;i<j;i++) {
			add(sb,tok[i],len);
		}
		echo(sb.toString(),c);
	}
	
	
	private void add(StringBuilder sb, String in, int len) {
		char[] o = new char[len];
		char[] s = in.toCharArray();
		int j = s.length;
		if (j>len) j=len;
		for (int i=0;i<len;i++) {
			if (i<j) o[i]=s[i]; else o[i]=32;
		}
		sb.append(o);
	}
	
	public void echo(String st,Color c) {
		if (c==null) c = color;
		int j = length-1;
		System.arraycopy(lineString, 1, lineString, 0, j);
		System.arraycopy(lineColor, 1, lineColor, 0, j);
		lineString[j] = st;
		lineColor[j]=c;
	}
	
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (font!=null) g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int chh = fm.getHeight();
        int y = 1;
        
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width,height);
        
        for (int i = 0; i<length;i++) {
        	if (lineString[i]==null) continue;
        	g.setColor(lineColor[i]);
        	g.drawString(lineString[i], 1, y);
        	y+= chh;
        	        	
        }
        
        if (fieldList!=null) fieldList.paintComponent((Graphics2D) g);
        
        g.setColor(borderColor);
        g.drawRect(0, 0, width,height);
	}
		
	public Terminal(int width,int height,int lines, Color textColor, Color background) {
		Rectangle pox = this.getBounds();
		pox.width=width+2;
		pox.height=height+2;
		this.setSize(width,height);
		this.setPreferredSize(new Dimension(width,height));
		this.width=width;
		this.height=height;
		length=lines>0 ? lines : 1;
		backgroundColor=background;
		color = textColor;
		lineString = new String[lines];
		lineColor = new Color[lines];
		
		for (int i=0;i<lines;i++) {
			lineString[i] = null;
			lineColor[i] = color;
		}
		
	}
	
	

}
