package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.Peripheral;
import com.microchip.mplab.mdbcore.simulator.SFR;
import com.microchip.mplab.mdbcore.simulator.SFRSet;
import com.microchip.mplab.mdbcore.simulator.scl.SCL;
import com.microchip.mplab.mdbcore.simulator.MessageHandler;
import com.microchip.mplab.mdbcore.simulator.SimulatorDataStore.SimulatorDataStore;
import com.microchip.mplab.mdbcore.simulator.PeripheralSet;
import java.util.LinkedList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(path = Peripheral.REGISTRATION_PATH, service = Peripheral.class)
public class Uart implements Peripheral {

    final String UART_NUM = "UART2"; // UART Name (eg: UART1, UART2, etc...)
    final String UART_RX = "U2RXREG"; // Respective UART RX SFR
    final String UART_INTERRUPT = "IFS1"; // Respective UART Interrupt SFR
    final String UART_STA = "U2STA"; // Respective UART STA SFR
    final String UART_TX = "U2TXREG"; // Respective UART TX SFR
    final String REQUEST_FILE = ""; // Request File Path (eg: "~/uartfolder/req"
    final String RESPONSE_FILE = ""; // Response File Path (eg: "~/uartfolder/res"
    
    static Uart instance;
    MessageHandler messageHandler;
    SFR sfrRX;
    SFR sfrInterrupt;
    SFR sfrSTA;
    SFR sfrTX;
    SFRSet sfrs;
    int updateCounter = 0;
    int cycleCount = 0;
    SCL scl;
    boolean notInitialized = true;
    LinkedList<Character> chars = new LinkedList<Character>();
    FileOutputStream request;
    FileInputStream response;
    
    @Override
    public boolean init(SimulatorDataStore DS) {
        
        // Initialize instance variables
        messageHandler = DS.getMessageHandler();
        sfrs = DS.getSFRSet();
        sfrRX = sfrs.getSFR(UART_RX);
        sfrInterrupt = sfrs.getSFR(UART_INTERRUPT);
        sfrSTA = sfrs.getSFR(UART_STA);
        sfrTX = sfrs.getSFR(UART_TX);
        
        // Remove default UART2
        PeripheralSet periphSet = DS.getPeripheralSet();
        Peripheral uartPeriph = periphSet.getPeripheral(UART_NUM);
        if (uartPeriph != null) {
            uartPeriph.deInit();
            periphSet.removePeripheral(uartPeriph);
        }

        // Setup pipes
        try {
            request = new FileOutputStream(REQUEST_FILE);
            response = new FileInputStream(RESPONSE_FILE);
        } catch (FileNotFoundException e) {
            messageHandler.outputMessage("Exception in init: " + e);
            return false;
        }
        
        // Add observers
        UartObserver obs = new UartObserver();
        sfrTX.addObserver(obs);
        
        messageHandler.outputMessage("External Peripheral Initialized: UART");
        instance = this;
        
        // Add peripheral to list and return true
        DS.getPeripheralSet().addToActivePeripheralList(this);
        return true;
    }

    @Override
    public void deInit() {

    }

    @Override
    public void addObserver(PeripheralObserver observer) {

    }

    @Override
    public String getName() {
        return UART_NUM + "_SIM";
    }

    @Override
    public void removeObserver(PeripheralObserver observer) {

    }

    @Override
    public void reset() {

    }

    @Override
    public void update() {
        if (cycleCount % 267 == 0) {
            try {
                if (response.available() != 0) { // If there are unread bytes, read them and add the chars
                    messageHandler.outputMessage("Available bytes: " + response.available());
                    chars.add((char) response.read());
                }
            } catch (IOException e) {
                messageHandler.outputMessage("Exception reading character from res " + e);
                return;
            }
            if (!chars.isEmpty()) { // Inject anything in chars
                if (sfrSTA.getFieldValue("UTXEN") == 1) { // If STA is ready to receive !! IMPORTANT
                    // messageHandler.outputMessage(chars.peek() + ""); // Returns the next char which will be injected
                    sfrRX.privilegedWrite(chars.pop()); // Inject the next char
                    sfrInterrupt.privilegedSetFieldValue("U2RXIF", 1); // Trigger the interrupt
                }
            }
        }
        cycleCount++;
    }

    // Debugging function for manually adding a string to chars
    public void setString(String str) {
        for (int i = 0; i < str.length(); i++) {
            chars.add(str.charAt(i));
        }
    }
    
    // Try to write bytes to the request file
    public void output() {
        try {
            request.write((byte) sfrTX.read()); 
        } catch (Exception e) {
            messageHandler.outputMessage("failed to write request byte " + e);
        }
    }
    
    public static Uart get() {
        return instance;
    }
}
