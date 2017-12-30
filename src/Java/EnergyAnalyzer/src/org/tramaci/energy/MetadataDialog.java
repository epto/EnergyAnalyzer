package org.tramaci.energy;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.tramaci.common.Metadata;
import org.tramaci.gui.SpringUtilities;

 
public class MetadataDialog extends JDialog implements FocusListener, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1040804300592908515L;
	private EnergyAnalyzer analyzer = null;
	private Metadata metadata = null;
	private JPanel buttonsPanel = new JPanel( new GridLayout(1,2));
	private JButton cancel = new JButton("Cancel");
	private JButton ok = new JButton("Ok");
	private JTextArea textArea = new JTextArea(5,32);
	
	private Object[][] item = new Object[][] {
				{ "User: "	,			new JTextField(32) 		,	Metadata.FIELD_USER } ,
				{ "Board: ", 			new JTextField(32) 		,	Metadata.FIELD_BOARD } ,
				{ "Antenna: ",		new JTextField(32) 		,	Metadata.FIELD_ANTENNA	 } ,
				{ "Description: ",	new JTextField(32) 		,	Metadata.FIELD_DESCRIPTION } ,
				{ "Text:"			 ,  textArea 	} ,
				{ " "					 ,  buttonsPanel }
				}
		;
	
	public MetadataDialog(EnergyAnalyzer owner) {
		
		super(owner.window,"Properties",true);
	
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setResizable(false);
		setLayout(null);
		setLayout(new SpringLayout());
		Container content = getContentPane();
		 
		analyzer=owner;
		metadata = analyzer.metadata;
		
		if (analyzer.metadata==null)  {
			if (analyzer.readerDevice!=null) metadata = analyzer.readerDevice.getMetaData();
			if (analyzer.metadata==null) {
				metadata = new Metadata();
				metadata.set(Metadata.FIELD_DESCRIPTION, new Date().toString());
				metadata.set(Metadata.FIELD_USER, (String) Main.config.get("user", "main" , "user" , "")) ;
				metadata.set(Metadata.FIELD_BOARD, analyzer.getBoard());
			}
		}
		
		JPanel panel = new JPanel();
		cancel.addMouseListener(this);
		panel.add(cancel);
		buttonsPanel.add(panel);
		
		panel = new JPanel();
		ok.addMouseListener(this);
		panel.add(ok);
		buttonsPanel.add(panel);

		JScrollPane scroll = new JScrollPane (textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		int numPairs = item.length;
		
		for (int i = 0; i < numPairs; i++) {
		    JLabel l = new JLabel( (String) item[i][0], JLabel.TRAILING);
		    content.add(l);
		    JComponent textField = (JComponent) item[i][1];
		    if (textField==textArea) textField=scroll;
		    textField.addFocusListener(this);
		    l.setLabelFor(textField);
		    content.add(textField);
		}
		
		//Lay out the panel.
		SpringUtilities.makeCompactGrid(content,
		                                numPairs, 2, //rows, cols
		                                6, 6,        //initX, initY
		                                6, 6);       //xPad, yPad
	
		
		pack();
		readData();
		setVisible(true);
			
	}
	
	private void writeData() {
		int numPairs = item.length;
		for (int i = 0; i<numPairs;i++) {
			JComponent field = (JComponent) item[i][1];
			if (field instanceof JTextField) {
				JTextField text = (JTextField) field;
				int k = (int) item[i][2];
				metadata.set(k,text.getText());
			} else if (field instanceof JTextArea) {
				JTextArea text = (JTextArea) field;
				metadata.text = text.getText();
				break;
			}
		}
	}
	
	private void readData() {
		int numPairs = item.length;
		for (int i = 0; i<numPairs;i++) {
			JComponent field = (JComponent) item[i][1];
			if (field instanceof JTextField) {
				JTextField text = (JTextField) field;
				int k = (int) item[i][2];
				text.setText((String) metadata.get(k,""));
				
			} else if (field instanceof JTextArea) {
				JTextArea text = (JTextArea) field;
				text.setText( metadata.text !=null ? metadata.text : new String() ); 
				break;
			}
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		writeData();
		readData();		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		Object obj=  e.getSource();
		
		if (obj == cancel) {
			setVisible(false);
			dispose();
			return;
		}
		
		if (obj == ok) {
			writeData();
			setVisible(false);
			analyzer.metadata = metadata;
			dispose();
			return;
		}
				
	}
	
	@Override
	public void focusGained(FocusEvent e) { }
	
	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }
	
}
