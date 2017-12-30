package org.tramaci.energFile;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import org.tramaci.energy.EnergyAnalyzer;
import org.tramaci.protocol.EnergPacket;
import org.tramaci.protocol.EnergProtocolException;
import org.tramaci.protocol.Settings;

public class EnergDevice extends VirtualDevice implements SerialPortEventListener {

	private SerialPort serialPort = null;
	private long fileTcr =0 ;
	private StringBuilder bufferLine = new StringBuilder();

	public EnergDevice(String device, EnergyAnalyzer ea) throws IOException {
		super(device, ea);
	}
	
	@Override
	public String getTypeName() { return "serial port"; }

	@Override
	public long getFileTcr() { return fileTcr; }

	@Override
	protected void onOpen() throws IOException {
		
		if (serialPort!=null) onClose();
		
		try {
			
			serialPort = new SerialPort(getFileName());
			serialPort.openPort(); //Open serial port
	
			serialPort.setParams(SerialPort.BAUDRATE_115200, 
	                     SerialPort.DATABITS_8,
	                     SerialPort.STOPBITS_1,
	                     SerialPort.PARITY_NONE)
	                     ;

			serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
			serialPort.addEventListener(this);

			} catch (SerialPortException exc) {
				throw new IOException(exc.getMessage());
			}
		
		fileTcr = System.currentTimeMillis();
		
	}

	@Override
	protected void onClose() throws IOException {
		
		if (serialPort!=null) {
			synchronized(serialPort) {
				 try {
					 serialPort.removeEventListener();
				 } catch(Exception devNull) {}
				 
				try {
					serialPort.closePort(); 
					} catch(Exception e) {
					throw new IOException(e.getMessage());
				}
			}
		}
		
		serialPort = null;
		
	}

	@Override
	protected void onSeek(int at) throws IOException {
		throw new IOException("Seek operation not supported");
	}

	@Override
	protected void onInit() throws IOException {
		
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		int bytes = event.getEventValue();
		byte[] data = new byte[bytes];
		
		try {
			data = serialPort.readBytes(bytes);
		} catch (SerialPortException e) {
			onError(e);
			return;
		}
		
		for (int i = 0; i<bytes; i++) {
			char c = (char) (255&data[i]);
			if (c == 13 || c == 10) {
				if (c==10 && bufferLine.length() == 0) continue;
				String line = bufferLine.toString();
				bufferLine = new StringBuilder();
				if (line.length()==0) continue;
				
				EnergPacket packet=null;
				
				try {
					packet = EnergPacket.fromString(line);
				} catch (EnergProtocolException e) {
					onError(e);
					return;
				}
			
				sendPacket(packet);
								
			} else {
				bufferLine.append(c);
			}
		}
		
	}

	@Override
	public boolean isDevice() {
		return true;
	}

	public void sendRawCommand(String string) throws IOException {
		try {
			serialPort.writeBytes(string.getBytes());
			serialPort.writeByte((byte) 13);
		} catch (SerialPortException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	protected void onSendSettings(Settings settings) throws IOException {
		boolean auto = settings.threshold1 == Settings.AUTO_THRESHOLD ||  settings.threshold2 == Settings.AUTO_THRESHOLD;
		if (auto) {
		} else {
			sendRawCommand("!S "+Integer.toString(settings.threshold1)+" "+Integer.toString(settings.threshold2));
		}
		sendRawCommand(settings.enableAnalogs ? "A" : "a");
		
	}

	

}
