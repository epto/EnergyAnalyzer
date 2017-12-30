package org.tramaci.energy;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.swing.JFileChooser;

import jssc.SerialPortList;

import org.tramaci.common.BitStream;
import org.tramaci.common.BinaryConfig;
import org.tramaci.energFile.EBFDevice;
import org.tramaci.energFile.EBFWriter;
import org.tramaci.energFile.EnergDevice;
import org.tramaci.energFile.EnergWriter;
import org.tramaci.energFile.RAWWriter;
import org.tramaci.energFile.TextDevice;
import org.tramaci.energFile.TextWriter;
import org.tramaci.energFile.VirtualDevice;
import org.tramaci.protocol.EnergProtocolException;

/**
 * @Podorico
 * */
public class Main {

	public static final String PATH_NAME = "EnergyAnalyzer";
	public static final String CONFIG_FILE_NAME = "config.dat";
	public static final int CONFIG_MAGIC_NUMBER = 0xEBFE0103;
	
	public static BinaryConfig config = null;
	
	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
		String[] portNames = new String[0];
		String device = null;
		int devNum = -1;
		boolean isFile = false;
		boolean chkDev = true;
		String saveTo = null;
		
		try {
			loadConfig();
			
			int j = args.length-1;
			portNames = SerialPortList.getPortNames();
			
			for (int i =0; i<=j;i++) {
				
				String cmd = args[i];
				if (!cmd.startsWith("-")) throw new IllegalArgumentException("Invalid argument `"+cmd+"`");
	
				if (cmd.compareTo("-?")==0) {
					help();
					System.exit(0);
				}
				
				if (cmd.compareTo("-w") ==0 || cmd.compareTo("--dialog")==0) {
					SerialDialog sd = new SerialDialog();
					try  {
						device = sd.doDialog();
					} catch(InterruptedException err) {
						System.exit(1);
					}
					continue;
				}
				
				if (cmd.compareTo("-l")==0 || cmd.compareTo("--list")==0) {
					  for(int ii = 0; ii < portNames.length; ii++){
						  System.out.print(ii+1);
						  System.out.print('\t');
						  System.out.println(portNames[ii]);
					  }
					System.exit(0);
				}
				
				if (cmd.compareTo("-n")==0 ) {
					chkDev=false;
					continue;
				}
				
				if (cmd.compareTo("-a")==0 || cmd.compareTo("--auto")==0) {
					device=null;
					
					for (int ii=0;ii<portNames.length;ii++) {
						String s = portNames[ii].toLowerCase();
						if (s.contains("ttyusb")) device = portNames[ii];
					}
					
					if (device==null) {
							for (int ii=0;ii<portNames.length;ii++) {
							String s = portNames[ii].toLowerCase();
							if (s.contains("com")) device = portNames[ii];
						}
					}
					
					if (device==null) {
							for (int ii=0;ii<portNames.length;ii++) {
							String s = portNames[ii].toLowerCase();
							if (s.contains("ttyama")) device = portNames[ii];
						}
					}
				
					if (device==null)  throw new IllegalArgumentException("Can't autodetect device.");
					continue;
				}
				
				if (i == j) throw new IllegalArgumentException("Invalid last argument `"+cmd+"`");
				
				if (cmd.compareTo("-p")==0 || cmd.compareTo("--port")==0 ) {
					String t = args[++i].toLowerCase();
					
					if (t.compareTo("first")==0) {
						devNum=-2;
						
					} else if (t.compareTo("last")==0) {
						devNum=-3;
						
					} else {
						if (!t.matches("/^[0-9]{1,2}$/")) throw new IllegalArgumentException("Invalid argument `"+cmd+" "+t+"`");
						devNum = Integer.parseInt(t)-1;
						if (devNum>=portNames.length) throw new IllegalArgumentException("Port number not found `"+cmd+" "+t+"`");
						
					}
					
					isFile=false;
					continue;
				}
				
				if (cmd.compareTo("-d")==0 || cmd.compareTo("--device")==0) {
					device = args[++i];
					if (chkDev) {
						boolean ok=false;
						for (int ii=0; ii<portNames.length; ii++) {
							if (device.compareToIgnoreCase(portNames[i])==0) {
								device = portNames[i];
								ok=true;
								break;
							}
						}
						if (!ok) throw new IllegalArgumentException("Unknown port name `"+device+"`");
					}
					isFile=false;
					devNum=-1;
					continue;
				}
				
				if (cmd.compareTo("-f")==0 || cmd.compareTo("--file")==0) {
					device = args[++i];
					devNum=-1;
					isFile=true;
					continue;
				}
				
				if (cmd.compareTo("-s")==0 || cmd.compareTo("--save")==0) {
					saveTo = args[++i];
					continue;
				}
			
				throw new IllegalArgumentException("Invalid argument `"+cmd+"`");
				
			}
					
			if (!isFile) {
				if (devNum<-1 && portNames.length ==0) throw new IllegalArgumentException("Empty port list..");
				
				if (devNum==-2) {
					device = portNames[0];
					
				} else if (devNum==-3) {
					device = portNames[ portNames.length-1];
					
				}
				
				if (device==null && saveTo!=null) throw new IllegalArgumentException("Device specification required.");
			}
		
		} catch(IllegalArgumentException err) {
			System.err.println("Error: "+err.getMessage());
			System.err.println();
			help();
			System.exit(1);
		}
					
		EnergyAnalyzer analyzer = new EnergyAnalyzer();
		
		VirtualDevice reader=null;
		EnergWriter writer = null;
		try {
			if (isFile) {
				if (device.endsWith(".txt") || device.endsWith(".log")) {
					reader = new TextDevice(device,analyzer);
				} else {
					reader = new EBFDevice(device,analyzer);
				}
			} else {
				if (device!=null) reader = new EnergDevice(device,analyzer);
			}
			
			if (saveTo!=null) {
				if (saveTo.endsWith(".txt") || saveTo.endsWith(".log")) {
					writer = new TextWriter(saveTo,analyzer);
				} else {
					if (saveTo.endsWith(".raw")) {
						writer = new RAWWriter(saveTo,analyzer);
					} else {
						writer = new EBFWriter(saveTo,analyzer);	
					}
					
				}
			}
			
		} catch(IOException e) {
			System.err.print("Error: "+e.getMessage()+"\n");
			System.exit(1);
		}
	}

	public static String getConfigFileName() {
		String userHome = System.getProperty("user.home");
		String appData = null;
		
		if (userHome==null) userHome = ".";
		
		try {
			appData = System.getenv("APPDATA");
		} catch(Exception devNull) {}
		
		if (appData == null) appData = userHome + "/." + PATH_NAME;  else appData+= '/'+ PATH_NAME;
		return appData + '/' + CONFIG_FILE_NAME;
	}
	
	public static void resetConfig() {
		
		String userHome = System.getProperty("user.home");
		String user = System.getProperty("user.name");
		String userDoc = null;
		String appData = null;
		
		if (userHome==null) userHome = ".";
		
		try {
			File doc = new JFileChooser().getFileSystemView().getDefaultDirectory();
			if (doc.exists() && doc.isDirectory()) userDoc = doc.toString();
		} catch(Exception devNull) {}
		
		try {
			appData = System.getenv("APPDATA");
		} catch(Exception devNull) {}
		
		if (userDoc == null) userDoc = userHome + '/' + PATH_NAME; else userDoc += '/' + PATH_NAME;
		if (appData == null) appData = userHome + "/." + PATH_NAME;  else appData+= '/'+ PATH_NAME;
		
		String[] ports = SerialPortList.getPortNames();
		
		int j = ports.length;
		String selPort = null;
		
		if (j>0) {
		
			selPort = ports[ports.length-1];
			
			for (int i = 0; i<j; i++) {
				File p = new File(ports[i]);
				String name = p.getName();
				name = name.toLowerCase();
				if (name.startsWith("tty")) selPort = ports[i];
			}
			
			for (int i = 0; i<j; i++) {
				File p = new File(ports[i]);
				String name = p.getName();
				name = name.toLowerCase();
				if (name.startsWith("com")) selPort = ports[i];
				if (name.contains("tty.usbserial-")) selPort = ports[i];
				if (name.startsWith("ttyama")) selPort = ports[i];
				if (name.startsWith("ttyusb")) selPort = ports[i];
			}
			
		} else {
			selPort = "";
		}
		
		if (user==null) user="User";
		
		Main.config = new BinaryConfig();
		Main.config.set("main", "documents", userDoc);
		Main.config.set("main", "home", appData);
		Main.config.set("main", "port", selPort);
		Main.config.set("main", "user", user);
		
		Main.config.set("user", "documents", userDoc);
		Main.config.set("user", "settings", userDoc);
		Main.config.set("user", "home", appData);
		Main.config.set("user", "port", selPort);
		Main.config.set("user", "user", user);
						
		File f = new File(userDoc);
		f.mkdirs();
		f.mkdir();
		f.setWritable(true,false);
		f.setReadable(true,false);
		f.setExecutable(true,false);
		
		f = new File(appData);
		f.mkdirs();
		f.mkdir();
		f.setWritable(true,false);
		f.setReadable(true,false);
		f.setExecutable(true,false);
		
		try {
			saveConfig();
		} catch (IOException e) {
			System.err.print("Config error: "+e.getMessage()+"\n");
		}
		
	}
	
	public static void saveConfig() throws IOException {
		RandomAccessFile fo = null;
		try {
			fo = new RandomAccessFile( Main.getConfigFileName() , "rw" );
			fo.setLength(0);
			fo.writeInt(CONFIG_MAGIC_NUMBER);
			BitStream bs = Main.config.toBitStream(null);
			byte[] data = bs.getBytesAtCursor();
			fo.writeShort(data.length);
			fo.write(data);
			fo.close();
			
		} catch(IOException e) {
			if (fo!=null) try { fo.close(); } catch(Exception devNull) {}
			throw e;
		}
	}
	
	@SuppressWarnings("resource")
	public static void loadConfig() {
		
		String conf = Main.getConfigFileName();
		if (!new File(conf).exists()) {
			resetConfig();
			return;
		}
		
		RandomAccessFile fo = null;
		try {
			fo = new RandomAccessFile( conf , "rw" );
			fo.seek(0);
			int test = fo.readInt();
			if (test!=CONFIG_MAGIC_NUMBER) throw new IOException("Invalid config file");
			test = fo.readShort();
			byte[] data =  new byte[test];
			fo.read(data);
			fo.close();
			fo=null;
			
			BitStream bs = new BitStream(data);
			
			config = new BinaryConfig(bs);
			
		} catch(IOException | EnergProtocolException e) {
			if (fo!=null) try { fo.close(); } catch(Exception devNull) {}
			resetConfig();
			System.err.print("Config error: "+e.getMessage()+"\n");
		}
		
	}
	
	public static void help() {
			InputStream s = Main.class.getResourceAsStream("/res/help.txt");
			try {
				
				while(true) {
					int w = s.available();
					if (w==0) break;
					byte[] b = new byte[w];
					s.read(b);
					String st = new String(b,"UTF8");
					st=st.replace("%%VER%%", EnergyAnalyzer.PROG_VERSION);
					System.out.print(st);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.out.println("");
	}

}
