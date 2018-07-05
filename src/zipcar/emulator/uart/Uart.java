package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.Peripheral;
import com.microchip.mplab.mdbcore.simulator.SFR;
import com.microchip.mplab.mdbcore.simulator.SFRSet;
import com.microchip.mplab.mdbcore.simulator.scl.SCL;
import com.microchip.mplab.mdbcore.simulator.MessageHandler;
import com.microchip.mplab.mdbcore.simulator.SimulatorDataStore.SimulatorDataStore;
import com.microchip.mplab.mdbcore.simulator.PeripheralSet;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.util.LinkedList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.openide.util.lookup.ServiceProvider;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

@ServiceProvider(path = Peripheral.REGISTRATION_PATH, service = Peripheral.class)
public class Uart implements Peripheral {
    
    String UART_NUM; // UART Name (eg: UART1, UART2, etc...)
    String UART_RX;// Respective UART RX SFR
    String UART_INTERRUPT; // Respective UART Interrupt SFR
    String UART_STA; // Respective UART STA SFR
    String UART_TX; // Respective UART TX SFR
    String REQUEST_FILE; // Request File Path (eg: "~/uartfolder/req"
    String RESPONSE_FILE; // Response File Path (eg: "~/uartfolder/res"
    
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
    LinkedList<Byte> chars = new LinkedList<Byte>();
    BufferedOutputStream request;
    BufferedInputStream response;
    Yaml yaml = new Yaml();
    
    @Override
    public boolean init(SimulatorDataStore DS) {
        
        // Initialize messageHandler
        messageHandler = DS.getMessageHandler();
        
        // Initialize instance variables
        try {
            FileInputStream conf = new FileInputStream(new File("config.yml"));
            Map config = (Map) yaml.load(conf);
            UART_NUM = config.get("uartNum").toString();
            UART_RX = config.get("uartRX").toString();
            UART_INTERRUPT = config.get("uartInterrupt").toString();
            UART_STA = config.get("uartSTA").toString();
            UART_TX = config.get("uartTX").toString();
            REQUEST_FILE = config.get("requestFile").toString();
            RESPONSE_FILE = config.get("responseFile").toString();
        } catch (Exception e) {
            messageHandler.outputError(e);
            // return false;
        }
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
        /* try {
            request = new FileOutputStream(REQUEST_FILE);
            response = new FileInputStream(RESPONSE_FILE);
        } catch (FileNotFoundException e) {
            messageHandler.outputMessage("Exception in init: " + e);
            return false;
        } */
        

        try {
            Socket reqSocket = new Socket("localhost", 5556);
            request = new BufferedOutputStream(reqSocket.getOutputStream());
            response = new BufferedInputStream(reqSocket.getInputStream());
        } catch (Exception e) {
            messageHandler.outputError(e);
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
        try {
            request.close();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if (cycleCount % (267) == 0) {
            try {
                if (response.available() > 0) {
                    messageHandler.outputMessage(response.available() + "");
                    chars.add((byte) response.read());
                }
            } catch (IOException e) {
                messageHandler.outputMessage("Exception reading character from res " + e);
                return;
            }
            if (!chars.isEmpty()) { // Inject anything in chars
                if (sfrSTA.getFieldValue("UTXEN") == 1) { // If STA is ready to receive !! IMPORTANT
                    messageHandler.outputMessage("Injecting: " + chars.peek());
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
            chars.add((byte) str.charAt(i));
        }
    }
    
    // Try to write bytes to the request file
    public void output() {
        try {
            byte b = (byte) sfrTX.read();
            messageHandler.outputMessage(String.format("Writing: 0x%02X" + b));
            request.write(b); 
            request.flush();
        } catch (Exception e) {
            messageHandler.outputMessage("Failed to write request byte " + e);
        }
    }
    
    public static Uart get() {
        return instance;
    }
}
