package org.tramaci.energFile;


public class BangTimer extends Thread {

	private volatile boolean running = false;
	private BangListener listener = null;
	
	public BangTimer(BangListener listenerIn) {
		listener=listenerIn;
		this.setDaemon(true);
		this.setName(this.getClass().getSimpleName());
		start();
	}
	
	public void end() {
		running=false;
		this.interrupt();
	}
	
	public boolean isRunning() { return running; }
	
	public void run() {
		running=true;
		int pause = 0;
		synchronized(listener) {
			pause = listener.onBang();
		}
		
		while(running) {
			if (this.isInterrupted()) break;
			if (pause<20) pause=20;
			if (pause>2000) pause=2000;
			
			try {
				Thread.sleep(pause);
			} catch (InterruptedException e) {
				break;
			}
			
			synchronized(listener) {
				pause = listener.onBang();
			}
		}
		
		running=false;
	}
		
}
