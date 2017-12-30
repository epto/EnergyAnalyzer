package org.tramaci.energy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JPanel;

import org.tramaci.energFile.VirtualDevice;

public class Player extends JPanel implements MouseListener {
	
	private EnergyAnalyzer analyzer = null;
	private VirtualDevice reader = null;
	private int width = 0 ;
	private int height = 0;
	private boolean seekable = false;
	private int[][] index = null;
	private int indexLen = 0;
	private int basePox = 0;
	private int curPox = 0;
	public Font font = null;
	
	public Color color = Color.GREEN;
	public Color backColor = Color.BLUE;
	public Color maxColor = Color.CYAN;
	public Color dataColor = Color.GREEN;
	public Color touchColor = Color.GREEN;
	public Color borderColor = Color.GREEN;
	public Color cursorColor = Color.BLUE;
	public Color disabledColor = Color.DARK_GRAY;
	public Color grafColor = Color.BLACK;
	public Color dataBorderColor = Color.GREEN;
	public int secondWidth = 4;
	private boolean showData = false;
	private long endShowData = 0;
	private int dataX = 0;
	
	private BufferedImage indexImg = null;

	/**
	 * 
	 */
	private static final long serialVersionUID = -8654648563768735457L;

	public Player(int widthIn, int heightIn, EnergyAnalyzer ea) {
		
		width=widthIn;
		height=heightIn;
		analyzer = ea;
		Rectangle pox = this.getBounds();
		pox.width=width+2;
		pox.height=height+2;
		setBounds(pox.x,pox.y,pox.width,pox.height);
		setSize(width,height);
		setSecond(0);
		setPreferredSize(new Dimension(width,height));
		addMouseListener(this);
		
	}
		
	public void clear() {
		index= null;
		seekable=false;
		indexLen=0;
		basePox=0;
		curPox=0;
		dataX=0;
		showData=false;
		endShowData=0;
		indexImg=null;
	}
	
	public void setIndex(int[][] idx,VirtualDevice readerIn) {
		reader=readerIn;
		index = idx;
		indexLen=index.length;
		seekable=indexLen>0;
		indexImg=null;
		if (seekable) updateGraph();
		basePox=0;
		curPox=0;
	}
		
	public void setSecond(int pox) {
		if (pox<0 || pox>=indexLen) return;
		curPox = pox;
		int wh = (width-40)/secondWidth;
		
		if (pox>indexLen-wh) {
			basePox = indexLen-wh;
		} else {
			basePox = pox - wh/2;
		}
		
		if (basePox<0) basePox = 0;
		if (basePox>indexLen-wh) basePox = indexLen-wh;
		if (seekable) updateGraph();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (font!=null) g.setFont(font);
        
        if (indexImg!=null)  {
        	g.setColor(backColor);
        	g.fillRect(0, 0, width, height);
        	g.drawImage(indexImg, 20, 1, null); 

        	} else {
        	g.setColor(disabledColor);
        	g.fillRect(0, 0, width, height);
        	}
        
        g.setColor((indexImg!=null) ? cursorColor : disabledColor);
                
        int max = width-56;
        int x = relVal(curPox,indexLen,max);
               
        g.fillRect(x+20, height-5, 16, 6);
        g.setColor(borderColor);
        g.drawRect(x+20, height-5, 16, 6);
        g.drawLine(20,height-5,width-20,height-5);
        g.drawRect(0, 0, 20, height);
        g.drawRect(width-20, 0, 20, height);
        g.drawRect(0, 0, width, height);
        g.setColor(dataColor);
        g.drawLine(5,10,15,5);
        g.drawLine(5,10,15,15);
        g.drawLine(width-5,10,width-15,5);
        g.drawLine(width-5,10,width-15,15);
        
        if (showData&&indexLen>0) {
        	int mx = basePox + (dataX / secondWidth);
        	if (mx<0) mx=0;
        	if (mx>=indexLen) mx = indexLen-1;
        	String str = 
        			number((int) Math.floor(mx/3600),2)
        			+":"+
        			number((int) Math.floor((mx%3600) / 60 ),2) 
        			+":"+
        			number((int) Math.floor(mx%60),2)
        			+" "+
        			number(index[mx][0] ,2)
        			+" "+  
        			number(index[mx][1], 4)
        			;
        	
        	FontMetrics fm = g.getFontMetrics();
        	Rectangle2D rect = fm.getStringBounds(str, g);
        	int x0 = (int) ((width/2) - (rect.getWidth()/2));
        	int y0 = (int) ((height/2) - (rect.getHeight()/2));
        	g.setColor(backColor);
        	g.fillRect(x0-9, y0-1, (int)(rect.getWidth()+18),(int)(rect.getHeight()+2));
        	g.setColor(dataBorderColor);
        	g.drawRect(x0-9, y0-1, (int)(rect.getWidth()+18),(int)(rect.getHeight()+2));
        	g.setColor(dataColor);
        	g.drawString(str, x0, y0+fm.getHeight()-2);
        	if (System.currentTimeMillis()>endShowData) showData=false;
        	}
       
        }
	
	private static String number(int ni,int l) {
		char[] o = new char[l];
		char[] n = Integer.toString(ni).toCharArray();
		int ln = n.length;
		if (ln<l) {
			int x = l-ln;
			int p = 0;
			
			for (int i=0;i<x;i++) {
				o[p++]=48;
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
	
	private int relVal(int val, int max, int toMax) {
		float p = max>0 ? max : 1;
		float mm = toMax;
		p = (float) val / p;
		p = p * mm;
		return (int) p;
	}
	
	private void updateGraph() {
		if (basePox<0) basePox=0;
		int pw = width-40;
		int ph = height-7;
		indexImg = null;
		System.gc();
		indexImg = new BufferedImage(pw,ph,BufferedImage.TYPE_USHORT_555_RGB);
		Graphics g = indexImg.getGraphics();
		g.setColor(backColor);
		g.fillRect(0, 0, pw, ph);
		int maxSeconds = pw / secondWidth;
		int toSeconds = basePox + maxSeconds;
		if (toSeconds>indexLen) toSeconds = indexLen;
		int x = 0 ;
		
		for (int csec = basePox; csec<toSeconds; csec++) {
						
			if (index[csec][0]>0 || index[csec][1]>0) {
				g.setColor(grafColor);
				g.fillRect(x, 0, secondWidth, ph);
			}
			
			int h1 = relVal(index[csec][0],15,ph-1);
			int h2 = relVal(index[csec][1],1023,ph-1);
			
			if (h1>h2) {
				g.setColor(touchColor);
				g.fillRect(x, ph-h1,secondWidth, h1);
				
				g.setColor(maxColor);
				g.fillRect(x, ph-h2,secondWidth, h2);
				
			} else {
				g.setColor(maxColor);
				g.fillRect(x, ph-h2,secondWidth, h2);
				
				g.setColor(touchColor);
				g.fillRect(x, ph-h1,secondWidth, h1);
								
			}
			x+=secondWidth;
		} 
		
		g.dispose();
				
	}
		
	private void showData(int x) {
		dataX = x;
		showData=true;
		endShowData = System.currentTimeMillis()+1000;
		this.repaint();
		int pox = (int) Math.floor(((float)x/(float)(width-40)) * (float)(indexLen-1));
		if (pox<0 || !seekable) return;
		
		try {
			curPox=pox;
			reader.seekAt(pox);
			
			analyzer.setTime(reader.getFileTcr() + pox*1000L);
			repaint();
			analyzer.clear();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void decPos() {
		
		if (!seekable) return;
		curPox--;
		if (curPox<0) curPox=0;
		try {
			reader.seekAt(curPox);
			
			analyzer.setTime(reader.getFileTcr() + curPox*1000L);
			repaint();
			analyzer.clear();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void incPos() {
		if (!seekable) return;
		curPox++;
		if (curPox>=indexLen) curPox=indexLen-1;
		try {
			reader.seekAt(curPox);
			analyzer.setTime(reader.getFileTcr() + curPox*1000L);
			repaint();
			analyzer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (!seekable) return;
		int x = e.getX();
		int f = width-20;
		if (x<20) decPos();
		if (x>20 && x<f) showData(x-20);
		if (x>f) incPos();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	
}
