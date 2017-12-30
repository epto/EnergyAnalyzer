package org.tramaci.energy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

import javax.swing.JPanel;

import org.tramaci.gui.FieldList;

public class WaveGraph  extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5185239719009303944L;
	
	private float max = 1 ;
	private int count=1;
	private WavePeak[] list = null;
	private int width = 0;
	private int height = 0;
	private int height1 = 0;
	private int mpx = 0;
	private int lastTcr=0;
	
	public Color borderColor = Color.GREEN;
	public Color lineColor = Color.BLUE;
	public Color backgroundColor = Color.BLACK;
	public Color labelColor = Color.YELLOW;
	public Color labelBackground = Color.BLACK;
	public Font font =  null;
	public String label = "Wave";
	public int labelHorizontalPadding = 2;
	public int labelVerticalPadding=1;
	public FieldList fieldList = null;
	
		
	public WaveGraph(int wh,int he,int length,int millisPerPixel) {
		count=length;
		list = new WavePeak[length];
		for (int i=0;i<length;i++) {
			list[i] = new WavePeak(0,i);
		}
		width=wh;
		height=he;
		height1=height-1;
		Rectangle pox = this.getBounds();
		pox.width=width+2;
		pox.height=height+2;
		mpx = millisPerPixel> 0 ? millisPerPixel : 1;
		this.setSize(width,height);
		this.setPreferredSize(new Dimension(width,height));
	}
	
	public void clear() {
		
		list = new WavePeak[count];
		for (int i=0;i<count;i++) {
			list[i] = new WavePeak(0,i);
		}
		
	}
	
	public void addPeak(int val,int tcr, int max) {
		this.max = max>0 ? max : 1;
		System.arraycopy(list, 1, list, 0, count-1);
		list[count-1] = new WavePeak(val,tcr);
		if (val>this.max) this.max=val;
		lastTcr=tcr;
	}
	
	public void addBreak() {
		addPeak(0,lastTcr+1,1);
		max=1;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width,height);
       
        g.setColor(lineColor);
        int x1 = width-1;
        
        for (int i = count-1; i>0;i--) {
        	int x0 = Math.abs(list[i].time - list[i-1].time);
        	if (x0<1) x0=1;
        	if (x0>1000) x0=1000;
        	
        	x0 = x0 / mpx;
        	x0 = x1 - x0;
        	if (x0<0) x0=0;
        	g.drawLine(x0+1, prop( list[i-1].value)+1 , x1+1, prop( list[i].value)+1) ;
        	if (x0==0) break;
        	x1=x0;
        }
        
      if (font!=null) g.setFont(font);
      text((Graphics2D) g,label,1,1,labelHorizontalPadding,labelVerticalPadding,labelColor,labelBackground);
	  
      if (fieldList!=null) fieldList.paintComponent((Graphics2D) g);
      
	  g.setColor(borderColor);
	  g.drawRect(0, 0, width, height);
	    
     }
	
	private void text(Graphics2D g2, String str,float x, float y, int padX,int padY, Color ink, Color paper) {
		
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
        Rectangle pox = gv.getPixelBounds(null, x, y);
              
        g2.setColor(paper);
        g2.fillRect(pox.x, pox.y+pox.height, pox.width+padX*2, pox.height+padY*2);
        g2.setColor(ink);
        g2.drawString(str,x+padX,y+padY+pox.height);
        
    		
	}
	
	private int prop(int v) {
		float c = v / max;
		c = height1 - (c * height1);
		if (c<0) c=0;
		if (c>height1) c=height1;
		return (int)c;
		
	}

}
