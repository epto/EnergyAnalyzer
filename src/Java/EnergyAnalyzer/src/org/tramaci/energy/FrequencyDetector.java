package org.tramaci.energy;

public class FrequencyDetector {

	private int prevVal = 0;
	private int prevDir = 0;
	private int lastPeak0 = 0;
	private int lastPeak1 = 0;
	private int lastFreq0 = 0;
	private int lastFreq1 =0;
	private int lastIdTrack = 0;
	
	private float currentFreq = 0;
	
	public float getCurrentFrequency() { return currentFreq; }
	
	public float addPeak(int val, int tcr, int idTrack) {
  
		int dir = 0;
		float freq = 0;
		float out = 0;
  
		if (idTrack != lastIdTrack) {
			prevVal = val;
			prevDir = 0;
			lastPeak0=tcr;
			lastPeak1=tcr;
			lastFreq0=0;
			lastFreq1=0;
			lastIdTrack = idTrack;
			currentFreq=0;
			return 0;
		}
    
		if (val>prevVal) {
			dir = 1;
		} else if (val<prevVal) {
			dir = -1;
		} 
		
		prevVal = val;
  
		if (prevDir!=dir && dir!=0) {
    
			prevDir = dir;
    
			if (dir<0) {
				lastFreq0 = Math.abs(tcr - lastPeak0);
				lastPeak0 = tcr; 
			} else {
				lastFreq1 = Math.abs(tcr - lastPeak1);
				lastPeak1=tcr;
			}
   
			freq = lastFreq0 + lastFreq1;
    
		}
  
		if (freq>0) out = (float) (1000.0 / freq);
  
		currentFreq=out;
		return currentFreq;
	}

}
