#include <SoftwareSerial.h>
#define SBUF_SIZE 1024
SoftwareSerial bluetooth(4, 7);

float ebimuTest[6] = { 0.00, 10.00, 50.00, 100.00, 150.00, 200.00}; 
char str[6][20];
int j = 0;

void setup()
{
  Serial.begin(115200);
  bluetooth.begin(115200);
}


void loop() {
   if(bluetooth.available())
   {
     Serial.println(bluetooth.read());
   }

   for(int i = 0; i < 6; i++)
   {
      Serial.print(ebimuTest[i]);
      dtostrf(ebimuTest[i],7,2,str[i]);
      bluetooth.write(str[i]);
      if(i <= 4)
      {
        bluetooth.write(",");
      }
      else if(i == 5)
      {
        bluetooth.write('\n');
        Serial.println();
      }
   }

   for(int i = 0; i < 6; i++)
   {
      if(j < 10)
      {
          ebimuTest[i] += 1.00;
      }
      else if(j >= 10 && j < 20)
      {
          ebimuTest[i] -= 1.00;
      }
   }
   j += 1;
   if(j == 20) j = 0;
   //delay(50);
}
