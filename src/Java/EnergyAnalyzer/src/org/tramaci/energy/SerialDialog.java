package org.tramaci.energy;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jssc.SerialPortList;

public class SerialDialog implements MouseListener, WindowListener {

	public JDialog window = null;
	private String[] ports = null;
	private JComboBox<String> combo = null;
	private JButton buttonOk = null;
	private JButton buttonCancel = null;
	private volatile boolean done = false;
	private volatile boolean asListener = false;
	private JFrame owner = null;
	private EnergyAnalyzer analyzer = null;
	private Method method = null;
	
	public SerialDialog(JFrame ownerIn) {
		owner = ownerIn;
		asListener=false;
		init();
	}
	
	public SerialDialog() {
		asListener=false;
		init();
	}
	
	@SuppressWarnings("unchecked")
	public SerialDialog(EnergyAnalyzer ownerIn, String methodToInvoke) throws NoSuchMethodException, SecurityException {
		owner = ownerIn.window;
		analyzer=ownerIn;
		asListener=true;
		Class<SerialDialog>[] argv = new Class[1];
		argv[0] = (Class<SerialDialog>) this.getClass();
		method = analyzer.getClass().getMethod(methodToInvoke, argv);
		init();
	}
	
	private void init() {
		
		ports = SerialPortList.getPortNames();
		
		window = new JDialog(owner,"Select serial port",true);
		
		Dimension size = new Dimension(250,100);
		window.setSize(size);
		window.setPreferredSize(size);
		window.setLocationRelativeTo(null);
		window.setLayout(null);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setLayout(new GridLayout(2,2));
		window.setResizable(false);
		window.addWindowListener(this);
		Container content = window.getContentPane();
		
		content.add(new JLabel("Serial port:"));
		
		combo = new JComboBox<String>(ports);
		content.add(combo);
		
		buttonOk = new JButton("Ok");
		buttonCancel = new JButton("Cancel");
		buttonOk.addMouseListener(this);
		buttonCancel.addMouseListener(this);
		
		JPanel panel = new JPanel();
		panel.add(buttonCancel);
		content.add(panel);
		panel = new JPanel();
		panel.add(buttonOk);
		content.add(panel);
		panel=null;
		
		if (ports.length>0) {
			combo.setSelectedItem(ports[ports.length-1]);
			String def = (String) Main.config.get("main", "user", "port", ports[ports.length-1]);
			
			for (int i=0;i<ports.length;i++) {
				if (def.compareTo(ports[i])==0) {
					combo.setSelectedItem(ports[i]);
					break;
				}
			}
		}
		
	}
	
	public String[] getPortList() { 
		return ports.clone(); 
		}
	
	public void setPort(String device) throws IOException {
		int j = ports.length;
		for (int i=0;i<j;i++) {
			
			if (ports[i].compareTo(device)==0) {
				setPortImpl(i);
				return;
			}
			
			if (ports[i].compareToIgnoreCase(device)==0) {
				setPortImpl(i);
				return;
			}
			
			File f = new File(ports[i]);
			if (f.getName().compareToIgnoreCase(device)==0) {
				setPortImpl(i);
				return;
			}
			
		}
		
		throw new IOException("Unknown port `"+device+"`");
	}

	private void setPortImpl(int i) {
		combo.setSelectedItem(ports[i]);
	}
	
	public String getSelectedPort() {
		return (String) combo.getSelectedItem();
	}
	
	public String doDialog() throws InterruptedException {
		asListener=false;
		done=false;
		if (ports.length>0) combo.setSelectedItem(ports[ports.length-1]);
		
		window.pack();
		window.setVisible(true);
		
		synchronized(window) {
			window.wait();
		}
		
		String str = getSelectedPort();
		
		window.setVisible(false);
		window.dispose();
		if (!done) throw new InterruptedException("Dialog cancelled");
		Main.config.set("user", "port", str);
		return str;
	}
	
	public void open() {
		asListener=true;
		done=false;
		if (ports.length>0) combo.setSelectedItem(ports[ports.length-1]);
		
		window.pack();
		window.setVisible(true);
		
	}
	
	public boolean isDone() { 
		return done; 
		}
	
	private void onSelect() {
		window.setVisible(false);
		if (done) Main.config.set("user", "port", getSelectedPort());
		try {
			method.invoke(analyzer, this);
		} catch (IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
			analyzer.log.echo(e.getMessage(),Color.RED);
		} 	
		window.dispose();

	}

	@Override
	public void mouseClicked(MouseEvent e) {

		done =  (e.getSource() == buttonOk) ;
		
		if (asListener) {
			onSelect();
			window.removeWindowListener(this);
			return;
		}
		
		synchronized(window) {
			window.notify();
		}
			
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

	@Override
	public void windowOpened(WindowEvent e) {
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		
		done = false;
		window.removeWindowListener(this);
		
		if (asListener) {
			onSelect();
			return;
		}
		
		synchronized(window) {
			window.notify();
		}
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		
	}

}
