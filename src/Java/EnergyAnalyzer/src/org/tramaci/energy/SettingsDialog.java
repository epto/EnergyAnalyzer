package org.tramaci.energy;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.tramaci.common.Metadata;
import org.tramaci.protocol.Settings;

public class SettingsDialog extends JDialog implements KeyListener, MouseListener, FocusListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6502543834886891631L;

	private EnergyAnalyzer analyzer = null;
	private Settings settings = null;
	private JTextField threshold1 = new JTextField();
	private JTextField threshold2 = new JTextField();
	private JTextField title = new JTextField();
	private JCheckBox autoThreshold = new JCheckBox();
	private JCheckBox analogs = new JCheckBox();
	private JButton cancel = new JButton("Cancel");
	private JButton ok = new JButton("Ok");
	
	public SettingsDialog(EnergyAnalyzer owner) {
		
		super(owner.window,"Settings",true);
		analyzer=owner;
		
		if (analyzer.metadata!=null && analyzer.metadata.settings!=null) {
			settings = analyzer.metadata.settings;
		} else {
			settings = new Settings();
		}
				
		Dimension size = new Dimension(320,200);
		setSize(size);
		setPreferredSize(size);
		setLocationRelativeTo(null);
		setLayout(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setResizable(false);

		setLayout(new GridLayout(6,2));
			
		pack();		
		Container content = getContentPane();
		Rectangle rect = content.getBounds();
		int borderW = 1+Math.abs(size.width - rect.width);
		int borderH = 1+Math.abs(size.height - rect.height);
		Dimension realSize = new Dimension( size.width + borderW , size.height + borderH );
		
		setSize(realSize);
		setPreferredSize(realSize);
		cancel.setMnemonic(1);
		ok.setMnemonic(2);
		
		content.add(new JLabel("Title:"));
		content.add(title);
		content.add(new JLabel("Threshold 1:"));
		content.add(threshold1);
		content.add(new JLabel("Threshold 2:"));
		content.add(threshold2);
		content.add(new JLabel("Enable auto threshold:"));
		content.add(autoThreshold);
		content.add(new JLabel("Enable analogs:"));
		content.add(analogs);
		
		JPanel panel = new JPanel();
		panel.add(cancel);
		content.add(panel);
		cancel.addMouseListener(this);
		
		panel = new JPanel();
		panel.add(ok);
		content.add(panel);
		ok.addMouseListener(this);
		
		threshold1.addKeyListener(this);
		threshold2.addKeyListener(this);
		threshold1.addFocusListener(this);
		threshold2.addFocusListener(this);
		autoThreshold.addMouseListener(this);
		
		readSettings();
		pack();
				
		setVisible(true);
	}
	
	private void readSettings() {
		boolean auto = settings.threshold1 == Settings.AUTO_THRESHOLD || settings.threshold2 == Settings.AUTO_THRESHOLD;
		if (auto) {
			threshold1.setText("(auto)");
			threshold1.setEditable(false);
			threshold2.setText("(auto)");
			threshold2.setEditable(false);
		} else {
			threshold1.setText(Integer.toString(settings.threshold1));
			threshold1.setEditable(true);
			threshold2.setText(Integer.toString(settings.threshold2));
			threshold2.setEditable(true);
		}
		autoThreshold.setSelected(auto);
		analogs.setSelected(settings.enableAnalogs);
		title.setText((settings.title!=null) ? settings.title : "");
	}
	
	private void writeSettings() {
		boolean auto = autoThreshold.isSelected();		
		if (auto) {
			settings.threshold1 = Settings.AUTO_THRESHOLD;
			settings.threshold2 = Settings.AUTO_THRESHOLD;
		} else {
			settings.threshold1 = getNumber(threshold1);
			settings.threshold2 = getNumber(threshold2);
		}
		settings.enableAnalogs = analogs.isSelected();
		settings.title = title.getText();
		
	}

	private int getNumber(JTextField t) {
		String str = t.getText();
		str=str.trim();
		try { 
			int n=	Integer.parseInt(str);
			if (n<0) n=0;
			if (n>1023) n=1023;
			return n;
		} catch(NumberFormatException e) { 
			return 0; 
			}
	}

	@Override
	public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key < 96 && key > 105) e.setKeyChar('0');
        }

	@Override
	public void mouseClicked(MouseEvent e) {
		
		Object obj = e.getSource();
		
		if (obj == autoThreshold) {
			writeSettings();
			readSettings();
			return;			
		}
		
		if (obj == cancel) {
			setVisible(false);
			dispose();
			return;
		}
		
		if (obj == ok) {
			
			writeSettings();
			readSettings();
			
			synchronized(analyzer) {
				if (analyzer.metadata==null) analyzer.metadata = new Metadata();
				analyzer.metadata.settings = settings;
				analyzer.log.echo("Settings: "+settings.toString(), Color.CYAN);
			}
			setVisible(false);
			dispose();
			return;
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		writeSettings();
		readSettings();
	}
		
	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }
	
	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }

	@Override
	public void focusGained(FocusEvent e) { }
	
}
