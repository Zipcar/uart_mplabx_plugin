# MPLabX Simulator UART I/O Plug-in
This is a plug-in for MPLabX v4.20, which support basic UART reading and injection.
The plug-in reads and writes to pre-configured request and response paths that are 
external to the simulator, and allow interaction with the developer's platform of choice
for simulating peripherals (Modem, GPS, etc.) The possibilities are endless! 

The majority of the code is under src/Uart.java, but if you wish to implement your own
SFRObservers, then you'll want to look under src/UartObserver.java

## Setup & Build
### Pre-requisits
1. MPLab X v4.20
2. MPLab X SDK
3. Netbeans (any recent version should work, submit an issue if it doesn't)
4. JDK 1.8

### Config & Build
1. Clone repo `git clone https://github.com/Zipcar/uart_mplabx_plugin.git`
2. Open Netbeans
3. `File -> Open Project` Open the `uart_mplabx_plugin` project folder
4. Open `uart_mplabx_plugin -> Source Packages -> zipcar.emulator.uart -> Uart.java` from the projects tab
5. Change the Strings at the top of the class to your respective register values & file paths
6. Right click `uart_mplabx_plugin` and click `Create NBM`

### Install
1. Open MPLab X
2. `Tools -> Plugins -> Downloaded` and click `Add Plugins...`
3. Navigate to the `uart_mplabx_plugin/build` folder and open `zipcar-emulator-uart.nbm`
4. Congrats, your plugin has been loaded!  