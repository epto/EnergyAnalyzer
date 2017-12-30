package org.tramaci.energy;


import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class AboutDialog extends JDialog implements MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2137951582762045694L;
	
	public AboutDialog(EnergyAnalyzer owner) {
		super(owner.window,owner.window.getTitle(),true);
				
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setResizable(false);
		
		Container content = getContentPane();
		setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
		Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		((JComponent) content).setBorder(padding);
				
		
		InputStream s = Main.class.getResourceAsStream("/res/about.txt");
			try {
				
				while(true) {
					int w = s.available();
					if (w==0) break;
					byte[] b = new byte[w];
					s.read(b);
					String st = new String(b,"UTF8");
					st=st.replace("%%VER%%", EnergyAnalyzer.PROG_VERSION);
					String[] test = st.split("\n");
					st=null;
					int j = test.length;

					for (int i=0;i<j;i++) {
						JLabel label = new JLabel(test[i].trim());
						label.setHorizontalAlignment(JLabel.CENTER);
						label.setAlignmentX(Component.CENTER_ALIGNMENT);
						content.add(label);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		
		JButton ok = new JButton("Ok");
		ok.addMouseListener(this);
		
		JPanel panel = new JPanel();
		
		padding = BorderFactory.createEmptyBorder(5, 0, 0, 5);
		panel.setBorder(padding);
		
		panel.add(ok);
		content.add(panel);
		
		setLocationRelativeTo(null);
		pack();
		setVisible(true);
				
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		dispose();
		
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
