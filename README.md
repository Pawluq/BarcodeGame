The game, that was played at the presentation day. To play you have to scan alternately a random EAN-13 barcode and a barcode from the shelf. 
If you have repeated this 15 times, the timer and game will stop.

The general App is divided in the MainActivity.kt for the init and logic and the ArduinoCommunication.java 
for the communication with the arduino.

The barcodeRegistry map keeps the list of barcodes on the shelf. 
Everytime a barcode is read by the API the processBarcode() is called, 
which looks up in the registry if the barcode is existing on the shelf.
If the barcode is registered, the sendColorCode() is called. 
Here is the ArduinoCommunication.java for the bluetooth communication called. 
It will proceed the mapped codes from  the barcodeRegistry to the arduino.

The use of  the Barcode API refers to this tutorial: https://www.youtube.com/watch?v=KBlQt7IsvM4
