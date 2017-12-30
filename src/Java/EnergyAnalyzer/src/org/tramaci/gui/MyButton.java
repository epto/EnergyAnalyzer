package org.tramaci.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class MyButton extends JPanel implements MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6738905337887003081L;
	
	public static final int RIGHT_BUTTON = 1;
	public static final int LEFT_BUTTON = 0;
	
	public String label = "";
	private int width=0;
	private int height= 0;
	
	public Color color = Color.BLACK;
	public Color backColor = Color.BLACK;
	public Color borderColor = Color.GREEN;
	public Color disabledColor = Color.DARK_GRAY;
	public Color marginColor = Color.GREEN;
	public Color buttonColor = Color.GREEN;
	public int margin = 2;
	public int arc= 8;
	public boolean enabled =true;
	public Font font = null;
	private int eventId = 0;
	private ButtonEventsReceiver obj = null;
	
	public MyButton(ButtonEventsReceiver inObj, String str,int event, int widthIn, int heightIn) {
		
		width=widthIn;
		height=heightIn;
		label=str;
		obj=inObj;
		Rectangle pox = this.getBounds();
		pox.width=width+2;
		pox.height=height+2;
		setBounds(pox.x,pox.y,pox.width,pox.height);
		setSize(width,height);
		eventId = event;
		setPreferredSize(new Dimension(width,height));
		addMouseListener(this);
		
	}
	
	@Override
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (font!=null) g.setFont(font);
        g.setColor(backColor);
        g.fillRect(0, 0, width, height);
		g.setColor(buttonColor);
        g.fillRoundRect(margin, margin, width-margin*2, height-margin*2, arc, arc);
        FontMetrics fm = g.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(label, g);
		int x0 = (int) ((width/2) - (rect.getWidth()/2));
		int y0 = (int) ((height/2) - (rect.getHeight()/2));
		//g.setColor(backColor);
		//g.fillRect(x0-1, y0-1, (int)(rect.getWidth()+2),(int)(rect.getHeight()+2));
		g.setColor(enabled ? color: disabledColor);
		g.drawString(label, x0, y0+fm.getHeight()-margin-1);
		g.setColor(borderColor);
        g.drawRect(0, 0, width, height);
        g.setColor(marginColor);
        g.drawRoundRect(margin, margin, width-margin*2, height-margin*2, arc, arc);
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		obj.onButton(eventId, e.getButton());
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

}
