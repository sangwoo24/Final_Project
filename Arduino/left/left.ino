#include <SoftwareSerial.h>
#define SBUF_SIZE 64
SoftwareSerial bluetooth(4, 7);
int EBimuAsciiParser(float *item, int number_of_item);



//flex 1번이 엄지--------------
int flexData[5];
char leftHandFlex[5][40];
//---------------------------


//Ebimu----------------------
int flag = 0;
char leftHandEbimu[6][40];
float axis_6[6];
char sbuf[SBUF_SIZE];
signed int sbuf_cnt=0;
//---------------------------

void setup(){
  Serial.begin(115200);
  bluetooth.begin(115200);
}

void loop(){
    //Flex Sensor-------------------------------
    Serial.print("Flex sensor : ");
    for(int i = 0; i < 5; i++){
        flexData[i] = analogRead(i+1);
        sprintf(leftHandFlex[i],"%d",flexData[i]);
        Serial.print(leftHandFlex[i]); Serial.print(" ");
    }
    Serial.println(" ");
    //------------------------------------------

    
    //Ebimu-------------------------------------
    EBimuAsciiParser(axis_6,6);
//    Serial.print("  6 axis : ");
    for(int i = 0; i < 6; i++){
        dtostrf(axis_6[i],7,2,leftHandEbimu[i]);
        Serial.print(leftHandEbimu[i]); Serial.print(" ");
    }
    Serial.println(" ");
    //------------------------------------------

   if(axis_6[3] < -5 || axis_6[3] > 5 || axis_6[4] > 5 || axis_6[4] < -5 || axis_6[5] > 5 || axis_6[5] < -5){
      flag = 0;
   }
   else{
      flag = 1; 
   }  
   
    if(flag == 1){    
      //Bluetooth Send----------------------------
      for(int i = 0; i < 6; i++){
        bluetooth.write(leftHandEbimu[i]);
        bluetooth.write(",");
      }
      
      bluetooth.write(leftHandFlex[0]);
      bluetooth.write(",");
      bluetooth.write(leftHandFlex[1]);
      bluetooth.write(",");
      bluetooth.write(leftHandFlex[2]);
      bluetooth.write(",");
      bluetooth.write(leftHandFlex[3]);
      bluetooth.write(",");
      bluetooth.write(leftHandFlex[4]);
      bluetooth.write(",");   
      bluetooth.write("5.00");
      bluetooth.write("\n");

      delay(10);
    }else{
      Serial.println("Error");
    }
}


int EBimuAsciiParser(float *item, int number_of_item)
{
    int n,i;
    int rbytes;
    char *addr; 
    int result = 0;
    
    rbytes = Serial.available();
    for(n=0;n<rbytes;n++)
    {
        sbuf[sbuf_cnt] = Serial.read();
        if(sbuf[sbuf_cnt]==0x0a)
        {
            addr = strtok(sbuf,",");
            for(i=0;i<number_of_item;i++)
            {
                item[i] = atof(addr);
                addr = strtok(NULL,",");
            }
            result = 1;
        }
        else if(sbuf[sbuf_cnt]=='*')
        {     
            sbuf_cnt=-1;
        }
 
        sbuf_cnt++;
        if(sbuf_cnt>=SBUF_SIZE) sbuf_cnt=0;
    }
    return result;
}
