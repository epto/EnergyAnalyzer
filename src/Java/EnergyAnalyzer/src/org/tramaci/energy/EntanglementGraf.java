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

public class EntanglementGraf extends JPanel{
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1453637199762163272L;

	protected int width = 0;
	private int height = 0;
	private int mx=0;
	private int my = 0;
	private int count = 0;
	private int[][] lines = null;
	
	public Font font = null;
	public Color borderColor = Color.GREEN;
	public Color crossColor = Color.BLUE;
	public Color backgroundColor = Color.BLACK;
	public Color labelColor = Color.YELLOW;
	public Color labelBackground = Color.BLACK;
	public String label = null;
	public int horizontalPadding = 1;
	public int verticalPadding = 2;
	public boolean labelCenter = false;
	
	public EntanglementGraf(int wh,int he, int z) {
		lines = new int[z][4];
		count=z;
		width=wh;
		height=he;
		Rectangle pox = this.getBounds();
		pox.width=width+2;
		pox.height=height+2;
		this.setBounds(pox.x,pox.y,pox.width,pox.height);
		this.setSize(width,height);
		this.setPreferredSize(new Dimension(width,height));
	}
	
	public void clear() {
		lines = new int[count][4];
	}
	
	public void addLine(int x0,int y0) {
		x0 = (int) ((x0/1023.0)*width) ;
		y0 = (int) ((y0/1023.0)*height) ;
		y0 = height-y0;
		x0++;
		y0++;
		System.arraycopy(lines, 1, lines, 0, count-1);
		lines[count-1]=new int[] { mx,my, x0, y0 };
		mx=x0;
		my=y0;
	}
	
	private void text(Graphics2D g2, String str,float x, float y, int padX,int padY, Color ink, Color paper) {
		
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
        Rectangle pox = gv.getPixelBounds(null, x, y);
        if (labelCenter) x = (width/2) - (pox.width/2)- padX;
          
        g2.setColor(paper);
        g2.fillRect(pox.x, pox.y+pox.height, pox.width+padX*2, pox.height+padY*2);
        g2.setColor(ink);
        g2.drawString(str,x+padX,y+padY+pox.height);
            		
	}
	
	@Override
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width,height);
                
        g.setColor(crossColor);
        int i = height>>1;
        g.drawLine(0,i,width,i);
        i=width>>1;
        g.drawLine(i,0,i,height);
        float fCount = count;
        for ( i = 0 ; i< count; i++) {
        	int h = (int) Math.floor((i/fCount)*255.0);
        	Color c = new Color(0,64+ (h>>1),h);
        	g.setColor(c);
        	g.drawLine(lines[i][0], lines[i][1], lines[i][2], lines[i][3] ) ;
        	}
        
        if (font!=null) g.setFont(font);
        if (label!=null) text((Graphics2D) g,label,1,1,horizontalPadding,verticalPadding,labelColor,labelBackground);
        
        g.setColor(borderColor);
        g.drawRect(0, 0, width, height);
        
        }
	
		
	
	
}
