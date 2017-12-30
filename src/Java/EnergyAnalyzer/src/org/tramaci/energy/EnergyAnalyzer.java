package org.tramaci.energy;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.tramaci.common.Metadata;
import org.tramaci.energFile.EBFDevice;
import org.tramaci.energFile.EBFWriter;
import org.tramaci.energFile.EnergDevice;
import org.tramaci.energFile.EnergWriter;
import org.tramaci.energFile.RAWWriter;
import org.tramaci.energFile.TextDevice;
import org.tramaci.energFile.TextWriter;
import org.tramaci.energFile.VirtualDevice;
import org.tramaci.gui.ButtonEventsReceiver;
import org.tramaci.gui.FieldArea;
import org.tramaci.gui.FieldList;
import org.tramaci.gui.MyField;
import org.tramaci.gui.Gravity;
import org.tramaci.gui.MyButton;
import org.tramaci.gui.Terminal;
import org.tramaci.protocol.EnergData;
import org.tramaci.protocol.EnergMetadata;
import org.tramaci.protocol.EnergPacket;
import org.tramaci.protocol.EnergProtocolException;
import org.tramaci.protocol.EnergSetting;
import org.tramaci.protocol.EnergSignal;
import org.tramaci.protocol.Settings;

public class EnergyAnalyzer extends Thread implements ButtonEventsReceiver, ActionListener, WindowListener, MouseListener {
	
	public static final String PROG_VERSION 	= 		"1.3 B";
			
	private static final int POX_LED_LEFT = 3;
	
	private static final int FIELD_FREQ = 1;
	private static final int FIELD_MAX = 2;
	private static final int FIELD_AVG = 3;
	private static final int FIELD_DELTA = 4;
	private static final int FIELD_PEAK = 5;
	private static final int FIELD_TRHESHOLD = 6;
	
	private static final int LED_BOARD = 1;
	private static final int LED_DATA = 2;
	private static final int LED_SETUP = 3;
	private static final int LED_SKIP = 4;
	private static final int LED_BOARD_NAME = 5;
	
	private static final int PARAM_ID_TRACK = 6;
	private static final int PARAM_FRAME_SKIP = 7;
	private static final int PARAM_LAG = 8;
	private static final int PARAM_DEVICE = 9;
	
	private static final int PARAM_MODE = 10;
	private static final int PARAM_DAY = 11;
	private static final int PARAM_MONTH = 12;
	private static final int PARAM_YEAR=13;
	private static final int PARAM_HOUR = 14;
	private static final int PARAM_MINUTE = 15;
	private static final int PARAM_SECONDS=16;
					
	public static final int BUTTON_PLAY = 1;
	public static final int BUTTON_PAUSE = 2;
	public static final int BUTTON_RECORD = 3;
	
	public static final int ACTION_NONE = 				0x0000;
	public static final int ACTION_READ =				0x0201;
	public static final int ACTION_RECORD = 		0x0102;
	public static final int ACTION_PLAY = 				0x0203;
	public static final int ACTION_SAME =				0xFFFF;

	public static final int FLAG_CAN_RECORD =		0x0100;
	public static final int FLAG_CAN_PLAY =			0x0200;
	
	public JFrame window = null;
	public WaveGraph sensor1 = null;
	public WaveGraph sensor2 = null;
	public WaveGraph entVal = null;
	public EntanglementGraf entang = null;
	public FieldArea leds = null;
	public Terminal log = null;
	public Player player = null;
	public MyButton pauseButton = null;
	public MyButton playButton = null;
	public MyButton recordButton = null;
	
	private FrequencyDetector freq1 = null;
	private FrequencyDetector freq2 = null;
	private FrequencyDetector entFreq = null;
	
	public EnergWriter writer = null;
	public VirtualDevice readerDevice = null;
	
	//public PropertyList meta = new PropertyList();

	private int action = ACTION_NONE;

	private int lastTcr = 0;
	private int distTcr = 0;
	private int countTcr = 0;
	
	private int entangCnt = 0;
	private int entangSum = 0;
	private int entangAvg = 0;
	private int entangDelta = 0;
	private int entangDeltaSum = 0;
	
	private int maxVal1 = 0;
	private int maxVal2 = 0;
	private int maxCurVal1=0;
	private int maxCurVal2=0;
	private int mFskip = 0;
	private int mIdTrack=-1;
	
	private volatile long helpNextColorTime = 0;
	private long prevSetTime = -1;
	
	private volatile long lastRepaint = 0;
	public Metadata metadata = null;
	
	public EnergyAnalyzer() {
		
		setName(this.getClass().getSimpleName());
		setDaemon(true);
		
		window = new JFrame("Energy analyzer");
		
		Dimension size = new Dimension(756,384);
		window.setSize(size);
		window.setPreferredSize(size);
		window.setLocationRelativeTo(null);
		window.setLayout(null);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addWindowListener(this);
		window.setResizable(false);
		createMenu();
		
		window.pack(); 
		
		Container content = window.getContentPane();
		Rectangle rect = content.getBounds();
		int borderW = 1+Math.abs(size.width - rect.width);
		int borderH = 1+Math.abs(size.height - rect.height);
		Dimension realSize = new Dimension( size.width + borderW , size.height + borderH );
		
		window.setSize(realSize);
		window.setPreferredSize(realSize);
		window.pack();
		
		FontMetrics fm = window.getFontMetrics(window.getFont());
		int lines = (int)(128 / fm.getHeight());
		log = new Terminal(756,128, lines,Color.GRAY,Color.BLACK);
		log.font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
		log.fieldList = new FieldList();
		log.fieldList.add(new MyField( "Energy Analyzer Ver. "+PROG_VERSION+" (C) 2017 by EPTO(A)",0,Color.GREEN,Color.BLACK,false));
		log.fieldList.setGravity(Gravity.RIGHT | Gravity.BOTTOM , log,0);
				
		entang = new  EntanglementGraf(256,256,16);
		entang.label = "Entanglement phase";
		entang.labelCenter = true;
	
		sensor1 = new WaveGraph(500,64,100,2);
		sensor1.label="Left";
		
		sensor2 = new WaveGraph(500,64,100,2);
		sensor2.label="Right";
		
		entVal = new WaveGraph(500,64,100,2);
		entVal.label="Entanglement";
		
		//	Leds - line0 ------------------------------------------------
		leds = new FieldArea(500,64-20);
		
		FieldList fl = new FieldList();
		
		fl.add(new MyField("Status:",11,Color.YELLOW,Color.BLACK,false)) ;
		
		fl.add( LED_BOARD , new MyField("\u25CF",2,Color.RED,Color.BLACK,false)) ;
		fl.add(new MyField("Hw.",5,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add( LED_DATA, new MyField("\u25CF",2,Color.RED,Color.BLACK,false)) ;
		fl.add( new MyField("Data",5,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add( LED_SETUP, new MyField("\u25CF",2,Color.RED,Color.BLACK,false)) ;
		fl.add( new MyField("Set",5,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add( LED_SKIP, new MyField("\u25CF",2,Color.RED,Color.BLACK,false)) ;
		fl.add( new MyField("Skip",10,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Board:",6,Color.CYAN,Color.BLACK,false)) ;
		fl.add( LED_BOARD_NAME, new MyField("N/A",20,Color.RED,Color.BLACK,false)) ;
		fl.left=POX_LED_LEFT;
		fl.setGravity(Gravity.TOP, leds, 0);
		leds.add(fl);
		
		//	Leds - line1 ------------------------------------------------
		fl = new FieldList();
		fl.add(new MyField("Parameters:",11,Color.YELLOW,Color.BLACK,false)) ;
		
		fl.add(new MyField("Touch:",9,Color.CYAN,Color.BLACK,false)) ;
		fl.add( PARAM_ID_TRACK, new MyField("-",2,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("F.Skip:",8,Color.CYAN,Color.BLACK,false)) ;
		fl.add( PARAM_FRAME_SKIP,	new MyField("-",4,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Lag:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( PARAM_LAG,new MyField("-",5,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Dev:",6,Color.CYAN,Color.BLACK,false)) ;
		fl.add( PARAM_DEVICE,	new MyField("(None)",20,Color.GREEN,Color.BLACK,false)) ;
		
		fl.left = POX_LED_LEFT;
		fl.setGravity(Gravity.TOP, leds, 1);		
		leds.add(fl);
		
		//	Leds - line2 ------------------------------------------------
		fl = new FieldList();
		fl.add(new MyField("Mode:",11,Color.YELLOW,Color.BLACK,false)) ;
		fl.add(PARAM_MODE,new MyField("(none)",9,Color.GRAY,Color.BLACK,false)) ;
		
		fl.add(new MyField("Date:",6,Color.YELLOW,Color.BLACK,false)) ;
		fl.add(PARAM_DAY, new MyField("00",3,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("/",1,Color.CYAN,Color.BLACK,false)) ;
		fl.add(PARAM_MONTH,new MyField("00",3,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("/",1,Color.CYAN,Color.BLACK,false)) ;
		fl.add(PARAM_YEAR,new MyField("0000",5,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("H",2,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add(PARAM_HOUR,new MyField("00",3,Color.GREEN,Color.BLACK,false)) ;
		fl.add(new MyField(":",1,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add(PARAM_MINUTE,new MyField("00",3,Color.GREEN,Color.BLACK,false)) ;
		fl.add(new MyField(":",1,Color.CYAN,Color.BLACK,false)) ;
		
		fl.add(PARAM_SECONDS,new MyField("00",2,Color.GREEN,Color.BLACK,false)) ;
		
		fl.left = POX_LED_LEFT;
		fl.setGravity(Gravity.TOP, leds, 2);		
		leds.add(fl);
		
		//	Sensor1	------------------------------------------------
		fl = new FieldList();
		
		fl.add(new MyField("Thr.:",6,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_TRHESHOLD,new MyField("?",5,Color.GRAY,Color.BLACK,false)) ;
		
		fl.add(new MyField("Freq:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_FREQ,new MyField("0 Hz",9,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Max:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_MAX,	new MyField("-",5,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Peak:",7,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_PEAK,new MyField("-",5,Color.GREEN,Color.BLACK,false)) ;
	
		fl.setGravity(Gravity.RIGHT , sensor1, 0);
		sensor1.fieldList = fl;
				
		//	Sensor2	------------------------------------------------
		fl = new FieldList();
		
		fl.add( new MyField("Thr.:",6,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_TRHESHOLD,new MyField("?",5,Color.GRAY,Color.BLACK,false)) ;
				
		fl.add( new MyField("Freq:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_FREQ,new MyField("0 Hz",9,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add( new MyField("Max:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_MAX,	new MyField("-",5,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add( new MyField("Peak:",7,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_PEAK,new MyField("-",5,Color.GREEN,Color.BLACK,false)) ;
	
		//	Entanglement	------------------------------------------------
		fl.setGravity(Gravity.RIGHT , sensor2, 0);
		sensor2.fieldList = fl;
		
		fl = new FieldList();
		fl.add(new MyField("Freq:",5,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_FREQ,new MyField("0 Hz",9,Color.GREEN,Color.BLACK,false)) ;
		
		fl.add(new MyField("Avg:",4,Color.CYAN,Color.BLACK,false)) ;
		fl.add( FIELD_AVG,	new MyField("-",4,Color.GREEN,Color.BLACK,true)) ;
		
		fl.add(new MyField(" ∆ ",3,Color.CYAN,Color.BLACK,true)) ;
		fl.add( FIELD_DELTA,new MyField("0",4,Color.GREEN,Color.BLACK,false)) ;
			
		fl.setGravity(Gravity.RIGHT , entVal, 0);
		entVal.fieldList = fl;
		
		//	Player	------------------------------------------------
		
		pauseButton = new MyButton(this,"\u25a0",BUTTON_PAUSE,32,20);
		playButton = new MyButton(this,"\u25ba",BUTTON_PLAY,32,20);
		recordButton = new MyButton(this,"\u25cf",BUTTON_RECORD,32,20);
		
		player = new Player(500-pauseButton.getWidth() - playButton.getWidth() - recordButton.getWidth() ,20,this);
		setPlayerMode(ACTION_NONE,false);
		
		//	Layout	------------------------------------------------
		content.add(entang);
		content.add(sensor1);
		content.add(sensor2);
		content.add(entVal);
		content.add(leds);
		content.add(player);
		content.add(pauseButton);
		content.add(playButton);
		content.add(recordButton);
		content.add(log);
		
		sensor1.setLocation(256, 0);
		sensor2.setLocation(256, 64);
		entVal.setLocation(256, 128);
		leds.setLocation(256, 192);
		player.setLocation(256,leds.getY() + leds.getHeight());
		pauseButton.setLocation(player.getX()+player.getWidth(), player.getY());
		playButton.setLocation(pauseButton.getX()+pauseButton.getWidth(), pauseButton.getY());
		recordButton.setLocation(playButton.getX()+playButton.getWidth(), playButton.getY());
		
		log.setLocation(0, 256);
		log.echo("Load Ok", Color.GREEN);				
		window.setVisible(true);
		
		//	Calc. objects	------------------------------------------------
		freq1 = new FrequencyDetector();
		freq2 = new FrequencyDetector();
		entFreq = new FrequencyDetector();
		
		try {
			window.setIconImage(ImageIO.read(EnergyAnalyzer.class.getResource("/res/icon.png")));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		start();
		reset(true);
	}

	// ██ Events:
	
	public void onPacketArrival(EnergPacket packet) {
	
		setLed(EnergyAnalyzer.LED_BOARD,Color.GREEN);
		Color lineColor = Color.GRAY;
		boolean doLog = true;
		
		if (packet instanceof EnergData) {
			
			EnergData data = (EnergData) packet;
			setLed(EnergyAnalyzer.LED_SETUP,Color.GREEN);
			setLed(EnergyAnalyzer.LED_DATA,Color.GREEN);
			onDataArrival(data.when, data.value1, data.max1, data.value2, data.max2, data.freq, data.frameSkip, data.idTrack,false);
			lineColor = Color.GREEN;
			
		} else if (packet instanceof EnergSetting) {
			
			setLed(EnergyAnalyzer.LED_SETUP,Color.RED);
			setLed(EnergyAnalyzer.LED_DATA,Color.ORANGE);
			EnergSetting data = (EnergSetting) packet;
			onDataArrival(data.when,data.value1, data.max1, data.value2, data.max2, data.freq, data.frameSkip, data.idTrack,false);
			lineColor = Color.GRAY;
			
		} else if (packet instanceof EnergMetadata) {
			
			EnergMetadata o = (EnergMetadata) packet;
			
			if (o.fieldId == EnergMetadata.FIELD_BOARD) {
				
				setBoard(o.data);
				log.echo("Board: "+o.data, Color.GREEN);
				doLog=false;
				
			} else {
				
				lineColor = Color.ORANGE;
				
			}
			
		} else if (packet instanceof EnergSignal) {
			
			EnergSignal o = (EnergSignal) packet;
		
			if (o.signal>0 && o.signal<=EnergSignal.MAX_SIGNAL_ID) {
			
				doLog=false;
				log.echo(EnergSignal.SIGNAL_STRING[o.signal], Color.CYAN);
			
				switch(o.signal) {
					
				case EnergSignal.SIGNAL_DO_SET:
					setLed(EnergyAnalyzer.LED_SETUP,Color.ORANGE);
					break;
					
				case EnergSignal.SIGNAL_GATE_LEVEL:
					setLed(EnergyAnalyzer.LED_SETUP,Color.GREEN);
					if (o.data.length>1) {
						setThreshold(o.data[0],o.data[1]);
					} 
					break;
					
				}
				
			} else {
				
				lineColor = Color.BLUE;
				
			}
						
		}
		
		if (doLog) {
			String logLine = packet.toString();
			log.echoTab(packet.toString(), lineColor, 8, logLine.contains("\t") ? 1 : 0);
		}
		
		setTime(packet.when);
		repaint();
		
		if (writer!=null) writer.onPacketArrival(packet);
		
	}

	public void onProtocolError(EnergProtocolException e) {
		
		setLed(EnergyAnalyzer.LED_DATA,Color.RED);
		log.echo("Error: "+e.getMessage(), Color.RED);
		repaint();
		
	}

	public void onDataArrival(long when,int value1, int valueMax1, int value2, int valueMax2,int tcr, int frameSkip, int idTrack,boolean repaint) {
		boolean isNewTouch = mIdTrack != idTrack;
		mIdTrack=idTrack;
		
		int v1 = relVal(value1,valueMax1, maxCurVal1);
		int v2 = relVal(value2,valueMax2, maxCurVal2);
		v2 = Math.abs(v1 - v2);
		float v2f = v2>0 ? v2 : 0.00098f;
		int entangVal = (int) ( 1023.0f* (1.0f / v2f) );
		
		entangSum+=entangVal;
		entangDeltaSum += Math.abs(value1 - value2);
		
		if (entangCnt++>10) {
			entangAvg = entangSum / entangCnt;
			entangDelta = entangDeltaSum / entangCnt;
			entangCnt=0;
			entangSum=0;
			entangDeltaSum=0;
			}
					
		distTcr = Math.abs(tcr-lastTcr);
		lastTcr=tcr;
		countTcr+=distTcr;
		
		if (countTcr<1000) {
			if (value1>maxVal1) maxVal1=value1;
			if (value2>maxVal2) maxVal2=value2;
		} else {
			countTcr=0;
			maxCurVal1 = maxVal1;
			maxCurVal2= maxVal2;
			maxVal1=0;
			maxVal2=0;
		}
		
		v1 = calcEntang(value1,maxCurVal1,valueMax1);
		v2 = calcEntang(value2,maxCurVal2,valueMax2);
		
		entang.addLine(v1,v2);
			
		sensor1.addPeak(value1, tcr, 1023);
		sensor2.addPeak(value2, tcr, 1023);
		
		String strFreq = null;
		float freq = freq1.addPeak(value1, tcr, 1);
				
		if (freq>0 || isNewTouch) {
			strFreq = floatString(freq)+ " Hz";
			sensor1.fieldList.setValue(FIELD_FREQ,strFreq);
		}
		sensor1.fieldList.setValue(FIELD_MAX,Integer.toString(valueMax1));
		sensor1.fieldList.setValue(FIELD_PEAK,Integer.toString(maxCurVal1));
		
		freq = freq2.addPeak(value1, tcr, 1);
		
		if (freq>0 || isNewTouch) {
			strFreq = floatString(freq)+ " Hz";
			sensor2.fieldList.setValue(FIELD_FREQ,strFreq);
		}
		
		sensor2.fieldList.setValue(FIELD_MAX,Integer.toString(valueMax2));
		sensor2.fieldList.setValue(FIELD_PEAK,Integer.toString(maxCurVal2));
		
		freq = entFreq.addPeak(entangVal, tcr, idTrack);
		
		if (freq>0 || isNewTouch) {
			strFreq = floatString(freq)+ " Hz";
			entVal.fieldList.setValue(FIELD_FREQ,strFreq);
		}
		
		entVal.fieldList.setValue(FIELD_AVG,Integer.toString(entangAvg));
		entVal.fieldList.setValue(FIELD_DELTA,Integer.toString(entangDelta)) ;
					
		entVal.addPeak(entangVal,tcr,1023);
		
		setParam(EnergyAnalyzer.PARAM_LAG,Integer.toString(distTcr));
		setParam(EnergyAnalyzer.PARAM_ID_TRACK,Integer.toString(idTrack));
		setParam(EnergyAnalyzer.PARAM_FRAME_SKIP,Integer.toString(frameSkip));
		
		if (mFskip != frameSkip) {
			mFskip = frameSkip;
			setLed(EnergyAnalyzer.LED_SKIP,Color.RED);
		} else {
			setLed(EnergyAnalyzer.LED_SKIP,frameSkip==0 ?  Color.GREEN : Color.ORANGE);
		}
		
		if (repaint) window.repaint();
	}

	public void onInitOutput(EnergWriter energWriter) {
		
		log.echo("Save output to "+energWriter.getFromatName()+" `"+energWriter.getFileName()+"`", Color.GREEN);
		
	}

	public void onOutputError(EnergWriter energWriter,Exception e) {
		log.echo("I/O Error on "+energWriter.getFromatName()+": "+e.getMessage(),Color.RED);
		writer = null;
	}

	public void onOutputClose(EnergWriter energWriter) {
		log.echo("Close "+energWriter.getFromatName()+" `"+energWriter.getDevice()+"`", Color.GREEN);
		writer = null;
	}

	public synchronized void onVirtualDeviceStart(VirtualDevice virtualDevice) {
		log.echo("Start", Color.GREEN);
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, true);
		
	}

	public synchronized void onVirtualDeviceStop(VirtualDevice virtualDevice) {
		log.echo("Stop", Color.CYAN);
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, false);
	}

	public synchronized void onVirtualDeviceSeek(VirtualDevice virtualDevice, int pox) {
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, virtualDevice.isRunning());
	}

	public void onEndOfFile(VirtualDevice virtualDevice) {
		log.echo("End of "+virtualDevice.getTypeName()+" `"+virtualDevice.getDeviceName()+"` stream" , Color.CYAN);
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, false);
	}

	public void onError(VirtualDevice virtualDevice, Exception err) {
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, false);
		log.echo("Error on "+virtualDevice.getTypeName()+" `"+virtualDevice.getDeviceName()+"` : "+err.getMessage(),Color.RED);
				
	}

	public void onProgramClose() {
		if (readerDevice!=null) try { readerDevice.stop(); } catch(Exception devNull) {}
		if (writer!=null) writer.close();
		
		try {
			Main.saveConfig();
		} catch (IOException e) {
			System.err.print("Config error: "+e.getMessage()+"\n");
		}
		
		System.exit(0);
	}

	public void onSelectSerialRead(SerialDialog serialDialog) {
		if (!serialDialog.isDone()) return;
		reset(false);
		
		String port = serialDialog.getSelectedPort();
		
		try {
						
			EnergDevice reader = new EnergDevice(port,this);
			if (metadata!=null) reader.setMetadata(metadata);
			setHelp("Press play button to "+(reader.isDevice() ? "read signals" : "start replay"));
			
		} catch (IOException e) {
			log.echo(e.getMessage(), Color.RED);
	
		}
		
	}

	public void onSelectSerialSave(SerialDialog serialDialog) {
		if (!serialDialog.isDone()) return;
		
		try {
					
			EnergDevice reader = new EnergDevice(serialDialog.getSelectedPort(),this);
			if (metadata!=null) reader.setMetadata(metadata);
			
		} catch (IOException e) {
			log.echo(e.getMessage(), Color.RED);
			return;
		}
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Energy binary file", "ebf"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text reader logs", "log"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("RAW PCM file", "raw"));
		
		File dir = new File( (String) Main.config.get("main", "user", "documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
		if (dir.exists() && dir.isDirectory() ) {
			chooser.setCurrentDirectory(dir);
		} else {
			 dir = new File( (String) Main.config.get("main","documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
			 if (dir.exists() && dir.isDirectory() ) chooser.setCurrentDirectory(dir);
		}
		
		int returnVal = chooser.showSaveDialog(window);
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			dir =  chooser.getSelectedFile();
			Main.config.set("user", "documents", dir.getParent()) ;
			
			String fileName = dir.toString();
		
			FileFilter filter =  chooser.getFileFilter();
			
			if (filter instanceof FileNameExtensionFilter) {
				
				FileNameExtensionFilter filterExt = (FileNameExtensionFilter)filter; 
				String[] ext = filterExt.getExtensions();
				if (ext.length>0 && !fileName.endsWith("."+ext[0])) fileName+="."+ext[0];
			
			}
							
			try {
				if (fileName.endsWith(".ebf")) {
					reset(false);
					writer = new EBFWriter(fileName,this);
				} else if (fileName.endsWith(".log")) {
					writer = new TextWriter(fileName,this);
				} else if (fileName.endsWith(".raw")) {
					writer = new RAWWriter(fileName,this);
				} else {
					log.echo("Invalid file type",Color.RED);
				}
			} catch(IOException e) {
				log.echo(e.getMessage(), Color.RED);
			}
		}
		
		if (writer!=null) {
			setHelp("Press record button to start");
			if (metadata!=null) {
					try {
						writer.setMetadata(metadata);
					} catch (IOException e) {
						log.echo("setMetadata: "+e.getMessage(), Color.RED);
					}
			}
			
		}
		
	}
	
	public void onSelectSerialDefault(SerialDialog serialDialog) {
		String str = serialDialog.getSelectedPort();
		Main.config.set("user","port",str);
		log.echo("Default device: "+str, Color.CYAN);
	
	}
	
	private void onSaveGraphics() {
		if (readerDevice==null) {
			log.echo("Nothing to save", Color.GRAY);
			return;
		}
		
		Rectangle rec = entang.getBounds();
		BufferedImage img = new BufferedImage(rec.width, rec.height,  BufferedImage.TYPE_INT_RGB);
		entang.paint(img.getGraphics());
		
		onButton(0, BUTTON_PAUSE);
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Portable Network Graphics (PNG)", "png"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Joint Photographic Experts Group (JPEG)", "jpeg"));
		
		File dir = new File( (String) Main.config.get("main", "user", "documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
		if (dir.exists() && dir.isDirectory() ) {
			chooser.setCurrentDirectory(dir);
		} else {
			 dir = new File( (String) Main.config.get("main","documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
			 if (dir.exists() && dir.isDirectory() ) chooser.setCurrentDirectory(dir);
		}
				
		int returnVal = chooser.showSaveDialog(window);
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			dir =  chooser.getSelectedFile();
			Main.config.set("user", "documents", dir.getParent()) ;
			
			String fileName = dir.toString();
		
			FileFilter filter =  chooser.getFileFilter();
			String imgExt = "png";
			
			if (filter instanceof FileNameExtensionFilter) {
				FileNameExtensionFilter filterExt = (FileNameExtensionFilter)filter; 
				String[] ext = filterExt.getExtensions();
				if (ext.length>0 && !fileName.endsWith("."+ext[0])) fileName+="."+ext[0];
				imgExt = ext[0].toLowerCase();
				}
			
			 try {
				ImageIO.write(img,imgExt, new File(fileName));
			 	} catch (IOException e) {
			 		log.echo("Save Graphics: "+e.getMessage(),Color.RED);
			 	}
			 
			}
		
	}

	private void onOpenFile() {
		VirtualDevice reader = null;
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Energy binary file", "ebf"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text reader logs", "log"));
		
		File dir = new File( (String) Main.config.get("main", "user", "documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
		if (dir.exists() && dir.isDirectory() ) {
			chooser.setCurrentDirectory(dir);
		} else {
			 dir = new File( (String) Main.config.get("main","documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
			 if (dir.exists() && dir.isDirectory() ) chooser.setCurrentDirectory(dir);
		}
				
		int returnVal = chooser.showOpenDialog(window);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			dir =  chooser.getSelectedFile();
			Main.config.set("user", "documents", dir.getParent()) ;
			
			String fileName = dir.toString();
		
			
			try {
				if (fileName.endsWith(".ebf")) {
					reset(true);
					reader = new EBFDevice(fileName,this);
					setPlayerMode(ACTION_PLAY,false);
				} else if (fileName.endsWith(".log")) {
					reader = new TextDevice(fileName,this);
					setPlayerMode(ACTION_PLAY,false);
				} else {
					log.echo("Invalid file type",Color.RED);
					reset(true);
				}
			} catch(IOException e) {
				log.echo(e.getMessage(), Color.RED);
			}
		}
		
		if (reader!=null) {
			setHelp("Press play button to "+(reader.isDevice() ? "read signals" : "start replay"));
			metadata = reader.getMetaData();
			if (metadata!=null) onMetadata();
		
		}
		
	}

	public void onMetadata() {
		log.echo("Metadata:", Color.CYAN);
		setBoard((String) metadata.getString(Metadata.FIELD_BOARD, "N/A."));
		setThreshold(
				(int) metadata.get(Metadata.FIELD_BOARD,	0) ,
				(int) metadata.get(Metadata.FIELD_BOARD, 	0))
				;
		
		if (metadata.text!=null) {
			String[] lin = metadata.text.split("\n");
			for (int i=0;i<lin.length;i++) {
				log.echo(lin[i], Color.WHITE);
			}
		}
		
	}
	
	// ██ Gui events:
	
	@Override
	public void onButton(int eventId, int buttons) {
	
		try {
			synchronized(this) {
				
				if (eventId == BUTTON_PAUSE && readerDevice!=null) {
					if (!readerDevice.isRunning()) {
						log.echo("Already stopped", Color.GRAY);
						return;
					}
					
					log.echo("Pause", Color.CYAN);
					
					Metadata m = readerDevice.getMetaData();
					if (m!=null) metadata = m;
										
					readerDevice.stop();
					setPlayerMode( writer!=null ? ACTION_RECORD : ACTION_PLAY, false);
					
				}
				
				if (eventId == BUTTON_PLAY && readerDevice!=null && writer == null) {
					if (readerDevice.isRunning()) {
						log.echo("Already playing", Color.GRAY);
						return;
					}
					
					log.echo("Play", Color.CYAN);
					if (metadata!=null) readerDevice.setMetadata(metadata);

					readerDevice.start();	
					
					setPlayerMode( ACTION_PLAY, true);
					
				}
				
				if (eventId == BUTTON_RECORD && readerDevice!=null && writer != null) {
					
					if (readerDevice.isRunning()) {
						log.echo("Already recording", Color.GRAY);
						return;
					}
					
					log.echo("Record", Color.CYAN);
					if (metadata!=null) {
						readerDevice.setMetadata(metadata);
						log.echo("Settings sent\n",Color.CYAN);
					}
					readerDevice.start();	
					setPlayerMode( ACTION_RECORD, true);
					
				}
			}
			
		} catch(IOException err) {
			log.echo("Error "+err.getMessage(), Color.RED);
			setPlayerMode( (writer != null) ? ACTION_RECORD  : ACTION_PLAY, false);
		}
		repaint();
				
	}
	
	private JFileChooser getSettingsFileChooser(boolean save) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Settings", "ebfs"));
		
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text settings", "ebts"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text / Comments", "txt"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Analyzer metadata", "meta"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Setput text (septex)", "septex"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Binary data packet (pak)", "pak"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Extended settings data (Setput)", "setput"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Raw settings packet (Setpak)", "setpak"));
		if (save) chooser.addChoosableFileFilter(new FileNameExtensionFilter("Settings source code", "src"));
		
		
		File dir = new File( (String) Main.config.get("user", "settings" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
		if (dir.exists() && dir.isDirectory() ) {
			chooser.setCurrentDirectory(dir);
		} else {
			 dir = new File( (String) Main.config.get("main","documents" , chooser.getFileSystemView().getDefaultDirectory().toString() ) );
			 if (dir.exists() && dir.isDirectory() ) chooser.setCurrentDirectory(dir);
		}
		
		return chooser;
	}
	
	private void onLoadSettings() {
				
		JFileChooser chooser = getSettingsFileChooser(false);
				
		int returnVal = chooser.showOpenDialog(window);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File dir =  chooser.getSelectedFile();
			Main.config.set("user", "settings", dir.getParent()) ;
			String fileName = dir.toString();
			
			try {
				
				if (metadata==null) metadata= new Metadata();
				metadata = metadata.loadFrom(fileName);

				if (readerDevice !=null) readerDevice.setMetadata(metadata);
			} catch(IOException | EnergProtocolException e) {
				log.echo(e.getMessage(), Color.RED);
			}
		}
		
	}

	private void onSaveSettings() {
		
		if (metadata == null) {
			log.echo("No metadata or settings available", Color.RED);
			return;
		}
		
		JFileChooser chooser = getSettingsFileChooser(true);
		
		int returnVal = chooser.showSaveDialog(window);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File dir =  chooser.getSelectedFile();
			Main.config.set("user", "settings", dir.getParent()) ;
			String fileName = dir.toString();
			
			FileFilter filter =  chooser.getFileFilter();
			
			if (filter instanceof FileNameExtensionFilter) {
				
				FileNameExtensionFilter filterExt = (FileNameExtensionFilter)filter; 
				String[] ext = filterExt.getExtensions();
				if (ext.length>0 && !fileName.endsWith("."+ext[0])) fileName+="."+ext[0];
			
			}
			
			
			
			try {
				
				metadata.saveTo(fileName);
				
			} catch(IOException e) {
				log.echo(e.getMessage(), Color.RED);
			}
		}
		
	}

	@SuppressWarnings("unused")
	@Override
	public void actionPerformed(ActionEvent e) {
		
		JMenuItem item = (JMenuItem) e.getSource();
		int cmd = item.getMnemonic();
		
		switch(cmd) {
		
		case 0x101:
			reset(false);
			SerialDialog sd;
			try {
				sd = new SerialDialog(this,"onSelectSerialRead");
				sd.open();
			} catch (NoSuchMethodException | SecurityException e1) {
				log.echo(e1.getMessage(),Color.RED);
			}
			
			break;
			
		case 0x102:
			reset(false);
			
			try {
				sd = new SerialDialog(this,"onSelectSerialSave");
				sd.open();
			} catch (NoSuchMethodException | SecurityException e1) {
				log.echo(e1.getMessage(),Color.RED);
			}
				
			break;
			
		case 0x103:
			reset(true);
			onOpenFile();
			break;
		
		case 0x104:
			reset(true);
			log.echo("Close", Color.CYAN);
			break;
					
		case 0x105:
			onProgramClose();
			break;
				
		case 0x201:
			try {
				sd = new SerialDialog(this,"onSelectSerialDefault");
				sd.open();
			} catch (NoSuchMethodException | SecurityException e1) {
				log.echo(e1.getMessage(),Color.RED);
			}
			break;
			
		case 0x202:
			Main.resetConfig();
			reset(true);
			setHelp("Configuration rebuilded");
			break;
					
		case 0x203:
			try {
				Main.saveConfig();
				setHelp("Configuration saved");
			} catch (IOException e1) {
				log.echo("Config error: "+e1.getMessage(),Color.RED);
			}
			break;
		
		case 0x301:	//	Settings...
			if (metadata==null) metadata = new Metadata();
			if (metadata.settings==null) metadata.settings = new Settings();
			SettingsDialog set = new SettingsDialog(this);
			break;
			
		case 0x302: // Properties...
			MetadataDialog meta = new MetadataDialog(this);
			break;
			
		case 0x303: // Save settings
			onSaveSettings();
			break;
			
		case 0x304: // Open settings
			onLoadSettings();
			break;
			
			
		case 0x305:	//	reset settings.
			if (metadata!=null) metadata.settings = null;
			setHelp("Settings removed");
			break;
			
		case 0x401:	// Save ghaphics
			onSaveGraphics();
			break;
				
		}
		
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
				
	}

	@Override
	public void windowClosing(WindowEvent e) {
				
	}

	@Override
	public void windowClosed(WindowEvent e) {
		window.removeWindowListener(this);
		onProgramClose();
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
	
	// ██ Setters:

	public void setOutput(EnergWriter energWriter) {
		writer = energWriter;
		log.echo("Connected to "+energWriter.getFromatName()+" `"+energWriter.getDevice()+"`", Color.GREEN);
		setPlayerMode(ACTION_RECORD,false);
	}

	public void setPlayerMode(int mode, boolean run) {
	
		if (mode!=ACTION_SAME) {
			action=mode;
		} else {
			mode=action;
		}
		
		if (0!= (mode & FLAG_CAN_RECORD)) {
			recordButton.buttonColor = run ? Color.ORANGE : Color.RED;
		
		} else {
			recordButton.buttonColor = Color.GRAY;
		}
		
		if (0!= (mode & FLAG_CAN_PLAY)) {
			playButton.buttonColor = run ? Color.BLUE : Color.GREEN;
	
		} else {
			playButton.buttonColor = Color.GRAY;
		}
		
		pauseButton.buttonColor = run ? Color.GREEN : Color.BLUE;
		
		if (readerDevice==null) {
			if (writer==null) {
				setMode("Stop");
			} else {
				setMode("N/A");
			}
			setDevice(null);
			
		} else {
			
			if (writer == null) {
				setDevice(readerDevice.getDeviceName());
				
				if (readerDevice.isDevice()) {
					setMode( run ? "Reading" : "Read" ) ;
					
				} else {
					setMode( run ? "Playing" : "Play" ) ;
				}
				
			} else {
				
				if (readerDevice.isDevice()) {
					setMode( run ? "Recording" : "Record" ) ;
				} else {
					setMode( run ? "Converting" : "Convert" ) ;
				}
				
			}
			
		}
		
	}

	public void setLed(int id,Color color) {
		leds.fieldList[0].setColor(id, color);
		
	}

	public void setLed(int id, boolean bit) {
		leds.fieldList[0].setColor(id, bit ? Color.GREEN : Color.RED);
	}
	
	public void setParam(int id, String val) {
		leds.fieldList[1].setValue(id, val);
	}
	
	public void setBoard(String str) {
		leds.fieldList[0].set(LED_BOARD_NAME, str, Color.GREEN);		
	}
	
	public void setReaderDevice(VirtualDevice virtualDevice) {
		readerDevice = virtualDevice;
		log.echo("Linked to "+virtualDevice.getTypeName()+" `"+virtualDevice.getDeviceName()+"`", Color.CYAN);
		setPlayerMode(writer!=null ? ACTION_RECORD : ACTION_PLAY, false);
		player.setIndex(readerDevice.getIndex(), readerDevice);
	
	}

	public void setTime(long tcr) {
		boolean isReset = (tcr==0);
		long x = (long) Math.floor(tcr/1000);
		
		if (isReset) {
			prevSetTime=-1;
		} else {
			if (x == prevSetTime) return;
			prevSetTime=x;
		}
		
		String str = null;
		Calendar when = Calendar.getInstance();
		when.setTimeInMillis(tcr);
		str = isReset ? "0000"  : number(when.get(Calendar.YEAR),4);
		leds.fieldList[2].setValue(PARAM_YEAR, str);
		
		str = isReset ? "00" : number(1+when.get(Calendar.MONTH),2);
		leds.fieldList[2].setValue(PARAM_MONTH, str);
		
		str = isReset ? "00" : number(when.get(Calendar.DAY_OF_MONTH),2);
		leds.fieldList[2].setValue(PARAM_DAY, str);
		
		str = isReset ? "00" : number(when.get(Calendar.HOUR_OF_DAY),2);
		leds.fieldList[2].setValue(PARAM_HOUR, str);
		
		str = isReset ? "00" : number(when.get(Calendar.MINUTE),2);
		leds.fieldList[2].setValue(PARAM_MINUTE, str);
		
		str = isReset ? "00" : number(when.get(Calendar.SECOND),2);
		leds.fieldList[2].setValue(PARAM_SECONDS, str);
		
		if (readerDevice!=null) player.setSecond((int) Math.floor((tcr - readerDevice.getFileTcr())/1000L));
		
	}

	public void setThreshold(int t1, int t2) {
		sensor1.fieldList.set(FIELD_TRHESHOLD, Integer.toString(t1), Color.GREEN);
		sensor2.fieldList.set(FIELD_TRHESHOLD, Integer.toString(t2), Color.GREEN);
	}

	public void setDevice(String dev) {
		if (dev==null) leds.fieldList[1].set(PARAM_DEVICE, "N/A", Color.RED); else leds.fieldList[1].set(PARAM_DEVICE, dev, Color.GREEN);
	}

	public void setHelp(String hlp) {
		Color c = null;
		if (hlp==null) {
			hlp="Energy Analyzer Ver. "+PROG_VERSION+" (C) 2017 by EPTO(A)";
			c = Color.GREEN;
			helpNextColorTime = 0;
		} else {
			c = Color.CYAN;
			helpNextColorTime = System.currentTimeMillis()+2000;
		}
		log.fieldList.field[0].setValue(hlp);
		log.fieldList.field[0].color = c;
			
	}
	
	// ██ Getters:
	
	public VirtualDevice getReaderDevice() {
		return readerDevice;
	}

	public String getBoard() {
		return leds.fieldList[0].get(LED_BOARD_NAME).getValue();
	}
	
	// ██ Generic methods
	
	public void clear() {
			
		lastTcr = 0;
		distTcr = 0;
		countTcr = 0;
		entangCnt = 0;
		entangSum = 0;
		entangAvg = 0;
		entangDelta=0;
		entangDeltaSum=0;
		
		maxVal1 = 0;
		maxVal2 = 0;
		maxCurVal1=0;
		maxCurVal2=0;
		mFskip = 0;
		mIdTrack=-1;
		prevSetTime = -1;
		
		entang.clear();
		sensor1.clear();
		sensor2.clear();
		entVal.clear();
				
		sensor1.fieldList.setValue(FIELD_MAX,"0");
		sensor1.fieldList.setValue(FIELD_PEAK,"0");
		sensor1.fieldList.setValue(FIELD_FREQ,"0 Hz");
		sensor1.fieldList.setValue(FIELD_TRHESHOLD,"?");
		sensor1.fieldList.setColor(FIELD_TRHESHOLD,Color.GRAY);
		
		sensor2.fieldList.setValue(FIELD_MAX,"0");
		sensor2.fieldList.setValue(FIELD_PEAK,"0");
		sensor2.fieldList.setValue(FIELD_FREQ,"0 Hz");
		sensor2.fieldList.setValue(FIELD_TRHESHOLD,"?");
		sensor2.fieldList.setColor(FIELD_TRHESHOLD,Color.GRAY);
		
		entVal.fieldList.setValue(FIELD_AVG,"0");
		entVal.fieldList.setValue(FIELD_DELTA,"0");
		setBoard("N/A");
		setDevice("N/A");
		
		setParam(EnergyAnalyzer.PARAM_LAG,"0");
		setParam(EnergyAnalyzer.PARAM_ID_TRACK,"?");
		setParam(EnergyAnalyzer.PARAM_FRAME_SKIP,"0");
		setTime(0);
		window.repaint();
	
	}

	public void repaint() {
		long when = System.currentTimeMillis();
		if (when - lastRepaint<40) return;
		lastRepaint = when;
		
		if (helpNextColorTime>0 && when>=helpNextColorTime) {
			helpNextColorTime=0;
			log.fieldList.field[0].color = Color.green;
			setHelp(null);
		}

		synchronized(this) {
			window.repaint();
		}
	}

	public void reset(boolean delSettings) {
		if (readerDevice!=null) try { readerDevice.stop(); } catch(Exception devNull) {}
		if (writer!=null) {
			if (metadata!=null) {
				try {
					writer.setMetadata(metadata);
				} catch (IOException e) {
					log.echo("setMetadata: "+e.getMessage(), Color.RED);
				}
			writer.close();
			}
		}
		if (delSettings) metadata=null;
		clear();
		player.clear();
		setPlayerMode(ACTION_NONE, false);
		setHelp(null);
	}

	public void run() {
		while(window!=null) {
			try {
				Thread.sleep(250);
				if (action == ACTION_RECORD && readerDevice!=null && readerDevice.isRunning()) {
					setTime(System.currentTimeMillis());
				}
			} catch (InterruptedException e) {
				break;
			}
			repaint();
		}
	}

	// ██ Private methods	
	
	private int relVal(int val, int max, int peak) {
		float p = max>0 ? max : ( peak>0 ? peak : 1) ;
		p = (float) val / p;
		p = p * 1023.0f;
		return (int) p;
	}

 private int calcEntang(int val, int max, int max2) {
		double v = val;
		double m = max>0 ? max : ( max2 >0 ? max2 : 1);
		v = v / m;
		v = 32.0 + v * 800.0;
		return (int) v;
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
	
	private void setMode(String str) {
		FieldList.set(leds.fieldList, PARAM_MODE, str, Color.GREEN);
	}
	
	private static String floatString(float f) {
		f*=100;
		f = (float) (Math.floor(f)/100);
		return Float.toString(f);
	}

	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		
		menu = arr2Menu("File",
				new Object[][] {
				{ "Read"		,	0x0101 , KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK)},
				{ "Record\u2026"	,	0x0102 , KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK) },
				{ "Play\u2026"		,	0x0103 , KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK) },
				{ null },
				{ "Close"		,	0x0104 , KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK) },
				{ "Properties",	0x0302 , KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK) },
				{ null },
				{ "Exit"			,	0x0105, KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK) }
				})
		;
		
		menuBar.add(menu);
		
		menu = arr2Menu("Analyzer",
				new Object[][] {
				{ "Default port"				,	0x0201 },
				{ "Reset configuration"	,	0x0202 },
				{ "Save configuration"		,	0x0203 },
				{ null },
				{ "Save graphics\u2026"				,	0x0401,	 KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK) }
				})
		;
		
		menuBar.add(menu);
		
		menu = arr2Menu("Board",
				new Object[][] {
				{ "Open settings\u2026"		,	0x0304 },
				{ "Save settings\u2026"		,	0x0303 },
				{ null },
				{ "Settings"				,	0x0301,	KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK)  },
				{ null },
				{ "Reset"					,	0x0305 }
				})
		;

		menuBar.add(menu);
		
		menu = new JMenu("About");
		menu.setMnemonic(0xff00);
		menu.addMouseListener(this);
		menuBar.add(menu);
		
		window.setJMenuBar(menuBar);
		
	}
	
	private JMenu arr2Menu(String text,Object[][] item) {
		JMenu menu = new JMenu(text);
		int j= item.length;
		for (int i = 0; i<j;i++) {
			
			if (item[i][0]==null) {
				menu.addSeparator();
				continue;
			}
			text = (String) item[i][0];
			int key = (Integer) item[i][1];
			JMenuItem menuItem = new JMenuItem(text);
			menuItem.setMnemonic(key);
			menuItem.addActionListener(this);
			if (item[i].length>2) menuItem.setAccelerator((KeyStroke) item[i][2]);
			menu.add(menuItem);
		}
		return menu;
	}

	@SuppressWarnings("unused")
	@Override
	public void mouseClicked(MouseEvent e) {
		Object obj = e.getSource();
		
		if (obj instanceof JMenu) {
			
			JMenu menu = (JMenu) obj;
			if (menu.getMnemonic() == 0xff00) {
					AboutDialog about = new AboutDialog(this);
				}
			
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

	
}
