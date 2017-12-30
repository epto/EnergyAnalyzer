package org.tramaci.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

public class FieldArea  extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6511388163884313138L;
	
	public FieldList[] fieldList = new FieldList[0];
	
	public Color borderColor = Color.GREEN;
	public Color backgroundColor = Color.BLACK;
	public Font font = null;
	
	public FieldArea(int width,int height) {
		Rectangle pox = this.getBounds();
		pox.width=width;
		pox.height=height;
		this.setSize(width,height);
		this.setPreferredSize(new Dimension(width,height));
	}
	
	public void add(FieldList f) {
		int j = fieldList.length;
		FieldList[] n = new FieldList[j+1];
		System.arraycopy(fieldList, 0, n, 0, j);
		n[j] = f;
		fieldList=n;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle pox = this.getBounds();
        
        g.setColor(backgroundColor);
        g.fillRect(0, 0, pox.width,pox.height);
       
       int j = fieldList.length;
       if (font!=null) g.setFont(font);
       
       for (int i=0;i<j;i++) {
    	   if (fieldList[i]!=null) fieldList[i].paintComponent((Graphics2D) g);
       }
             
	   g.setColor(borderColor);
	   g.drawRect(0, 0, pox.width, pox.height);
	    
     }

}
