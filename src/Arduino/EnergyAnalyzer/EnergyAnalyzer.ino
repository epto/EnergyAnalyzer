/**
 * Energy Analyzer 1.3 (C) 2017 by EPTO (A)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */
#define VERSION                   "RivEnerg/1.3"
#define PAUSE0                    5     //  Wave length on mode 0 [milliseconds].
#define PAUSE1                    10    //  Wave length on mode 1 [milliseconds].
#define PIN_MODE                  12    //  Mode pin.
#define TIM_THRESHOLD             500   //  Timeout touch [milliseconds].
#define SIGNAL_TIMEOUT            1000  //  Signal timeout [milliseconds].
#define MIN_SERIAL_TX_AVAILABLE   32    //  Requested bytes number to send data [bytes].
#define MIN_FREQ                  50    //  Minimum sound frequency [hz].
#define MAX_FREQ                  1900  //  Maximum sound frequency [hz].
#define FREQ_MULT                 3     //  Multipiler.
#define OUT_PIN                   9     //  Output pin.
#define SET_LED                   13    //  Led pin.
#define SET_TIME                  1000  //  Setup time [milliseconds]
#define ANALOGS                   2     //  Number of analogic inputs.
#define FREQ_RANGE                ( MAX_FREQ - MIN_FREQ )
#define FREQ_STEP                 ( 1023.0 / FREQ_RANGE )

boolean setDone =false;
boolean touch = false;
boolean mode = false;
boolean prevMode = false;
boolean resetTrack = false;
boolean resetSignal = false;
boolean enableAnalogs = false;

int idTrack = 0;
int frameSkip = 0;
int curTmp = 0;
int analogCnt = 0;

unsigned long lastTouch = 0;
unsigned long tim = 0;
unsigned long mTim = 0;
unsigned long freq=0;
unsigned long lastPeak = 0;
unsigned long analogTim = 0;

int threshold[ANALOGS];
int val[ANALOGS];
int valMax[ANALOGS];

void setup() {
  analogReference(INTERNAL);
  pinMode(OUT_PIN,OUTPUT);
  pinMode(PIN_MODE,INPUT_PULLUP);
  pinMode(SET_LED,OUTPUT);
  prevMode = true ^ digitalRead(PIN_MODE);
  
  Serial.begin(115200);  
  Serial.println();
  Serial.flush();
  
  Serial.println();
  Serial.flush();
  
  Serial.print("@B\t");
  Serial.println(VERSION);
  
  Serial.print("@A\t");
  Serial.println(ANALOGS);
  Serial.flush();

  Serial.println("!8");
  Serial.flush();
  
  doSet(); 
  
}

void doSet() {
    digitalWrite(SET_LED,HIGH);
    Serial.println("!6");
    Serial.flush();
    
    curTmp=0;
    mTim = millis();
    
    for (int i = 0 ; i<ANALOGS; i++) {
      threshold[i]=1;
      val[i]=0;
      valMax[i]=0;
    }
    
    tone(OUT_PIN,440);
    delay(250);
    tone(OUT_PIN,880);
    delay(250);
    noTone(OUT_PIN);
    tone(OUT_PIN,110);
    setDone=false;
  }

void loop() {
  
  tim = millis();

  touch = false;
  mode = digitalRead(PIN_MODE);
  if (mode!=prevMode) {
    prevMode=mode;
    Serial.print("!4\t");
    Serial.println(mode&1);
    Serial.flush();
    }
  
  for (int i=0;i<ANALOGS;i++) {
  
    val[i] = analogRead(i);
    
    if (val[i]>threshold[i]) {
      if (val[i]>valMax[i]) valMax[i]=val[i];
      touch=true;
      lastTouch=tim;
      lastPeak = tim;
    } 
    
    if (!setDone) {
      
      if (val[i]>threshold[i]) threshold[i]=val[i];
            
      if (tim!=mTim) {
        mTim=tim;
        if (curTmp++>SET_TIME) {
          setDone=true;
          tone(OUT_PIN,220);
          delay(250);
          tone(OUT_PIN,880);
          digitalWrite(SET_LED,LOW);
          delay(250);
          noTone(OUT_PIN);
          Serial.print("!3");
          for (int i =0;i<ANALOGS;i++) {
            Serial.print('\t');
            Serial.print(threshold[i]);
          }
          Serial.println();
          Serial.flush();
        }
      }
    } 
  }

  if (setDone) {
    for (int i=0;i<ANALOGS;i++) {
        if (val[i]>threshold[i]) {
          
          freq = MIN_FREQ + (abs(val[i]-threshold[i]) / FREQ_STEP);
          freq *= FREQ_MULT;
          if (freq > MAX_FREQ) freq=MAX_FREQ;
          if (freq < MIN_FREQ) freq=MIN_FREQ;
          tone(OUT_PIN,freq);
          if (mode) delay(PAUSE1); else delay(PAUSE0);
        } else {
          noTone(OUT_PIN);
        }
      }
  }
    
  if (resetTrack && setDone && abs(tim-lastTouch)>TIM_THRESHOLD) {
    idTrack=(idTrack+1)%10;
    resetTrack=false;
    for (int i=0;i<ANALOGS;i++) {
        valMax[i]=0;
      }
   }

  if (resetSignal && setDone && abs(tim-lastTouch)>SIGNAL_TIMEOUT) {
    resetSignal=false;
    Serial.println("!1");
    Serial.flush();
    }
  
  if (touch) {
    
    resetTrack=true;
    resetSignal=true;
    
    if (Serial.availableForWrite()>=MIN_SERIAL_TX_AVAILABLE) {
        freq = lastPeak%8000;
        if (setDone) Serial.print('D'); else Serial.print('S');
        Serial.print(ANALOGS);
        Serial.print('\t');
        
        for (int i=0;i<ANALOGS;i++) {
          Serial.print(val[i]);
          Serial.print('\t');
          Serial.print(valMax[i]);
          Serial.print('\t');
        }
        
        Serial.print(freq);
        Serial.print('\t');
        Serial.print(frameSkip);
        Serial.print('\t');
        Serial.println(idTrack);
                                      
        Serial.flush();                                      
        frameSkip=0;
      } else {
        if (frameSkip<1000) frameSkip++;
      }

      if (!setDone) delay(PAUSE0);
    }

  if (enableAnalogs) {
    if (tim!=analogTim) {
      analogTim=tim;
      if (analogCnt++>=500) {
        analogCnt=0;
        Serial.print("!12\t");
        for (int ii=2; ii<=5;ii++) {
          Serial.print(analogRead(ii));
          if (ii<5) Serial.write('\t');
          }
        Serial.println();
        Serial.flush();
        }
      }
    }

  if (Serial.available()) {
    int i = Serial.read();
    
    switch(i) {
      case 'P':
        Serial.println("!0");
        break;

      case 'A':
        enableAnalogs = true;
        Serial.println("!11");
        break;

      case 'a':
        enableAnalogs = false;
        Serial.println("!10");
        break;

      case 'S':
        setDone=true;
        for (int i=0;i<ANALOGS;i++) {
          threshold[i] = Serial.parseInt();
          }
        Serial.println("!13");
        break;
        
      case 'V':
        Serial.print("@B\t");
        Serial.println(VERSION);
        break;

      case 'R':
        doSet();
        Serial.print("@B\t");
        Serial.println(VERSION);
        break;

      case 13:
      case 10:
        break;
        
      default:
        Serial.println("!7");   
      }
      
    Serial.flush();
    }
}

