package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.Peripheral;
import com.microchip.mplab.mdbcore.simulator.SFR;
import com.microchip.mplab.mdbcore.simulator.SFRSet;
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
    String UART_RX; // Respective UART RX SFR
    String UART_INTERRUPT; // Respective UART Interrupt SFR
    String UART_STA; // Respective UART STA SFR
    String UART_TX; // Respective UART TX SFR
    
    MessageHandler messageHandler;
    SFR sfrRX;
    SFR sfrInterrupt;
    SFR sfrSTA;
    SFR sfrTX;
    SFRObserver sfrTXObserver;
    int cycleCount = 0;
    boolean notInitialized = true;
    LinkedList<Byte> chars;
    FileInputStream conf;
    Map config;
    BufferedOutputStream request;
    BufferedInputStream response;
    Yaml yaml;
    
    public Uart() {
        sfrTXObserver = new UartObserver();
        chars = new LinkedList<Byte>();
        yaml = new Yaml();
    }

    @Override
    public boolean init(SimulatorDataStore DS) {
        SFRSet sfrs;

        messageHandler = DS.getMessageHandler();
        
        // Initialize instance variables
        try {
            conf = new FileInputStream(new File("config.yml"));
            config = (Map) yaml.load(conf);]
            UART_NUM = config.get("uartNum").toString();
            UART_RX = config.get("uartRX").toString();
            UART_INTERRUPT = config.get("uartInterrupt").toString();
            UART_STA = config.get("uartSTA").toString();
            UART_TX = config.get("uartTX").toString();
        } catch (FileNotFoundException e) {
            messageHandler.outputError(e);
            messageHandler.outputMessage("Are you sure you placed config.yml in the correct folder?");
            return false;
        } catch (SecurityException e) {
            messageHandler.outputErorr(e);
            return false;
        } catch (NullPointerException e) {
            messageHandler.outputError(e);
            messageHandler.outputMessage("Are you sure you have all of the necessary config fields?");
            return false;
        } catch (ClassCastException e) {
            messageHandler.outputError(e);
            return false;
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

        openSockets();

        sfrTX.addObserver(sfrTXObserver);
        
        messageHandler.outputMessage("External Peripheral Initialized: UART");   
        periphSet.addToActivePeripheralList(this);
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

    // Unimplemented method of interface. Most PeripheralObserver applications that'd be of use are covered by deInit().
    @Override
    public void addObserver(PeripheralObserver observer) {

    }

    @Override
    public String getName() {
        return UART_NUM + "_SIM";
    }

    // Unimplemented method of interface. Most PeripheralObserver applications that'd be of use are covered by deInit().
    @Override
    public void removeObserver(PeripheralObserver observer) {

    }

    @Override
    public void reset() {

    }

    @Override
    public void update() {
        if (sfrTXObserver.changed()) {
            output();
        }
        try {
            if (response.available() > 0) {
                messageHandler.outputMessage(response.available() + "");
                chars.add((byte) response.read());
            }
        } catch (IOException e) {
            messageHandler.outputMessage("Exception reading character from res " + e);
            return;
        }
        if (!chars.isEmpty()) {
            if cycleCount == 267 {
                if (sfrSTA.getFieldValue("UTXEN") == 1) {
                    messageHandler.outputMessage("Injecting: " + chars.peek());
                    sfrRX.privilegedWrite(chars.pop());
                    sfrInterrupt.privilegedSetFieldValue("U2RXIF", 1);
                }
                cycleCount = 0;
            } else {
                cycleCount++;
            }
        }
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
            request.write(b); 
            request.flush();
        } catch (IOException e) {
            messageHandler.outputMessage("Failed to write request byte... Attempting to reopen sockets.\n" + e);
            openSockets();
        }
    }

    public void openSockets() {
        try {
            Socket reqSocket = new Socket("localhost", 5556);
        } catch (IOException e) {
            messageHandler.outputError(e);
            messageHandler.outputMessage("Failed to open socket. Is there an external listener running?");
            return false;
        } catch (SecurityException e) {
            messageHandler.outputError(e);
            return false;
        } catch (IllegalArgumentException e) {
            messageHandler.outputError(e);
            messageHandler.outputMessage("Provided port is outside of valid values (0-65535).");
            return false;
        } catch (NullPointerException e) {
            messageHandler.outputError(e);
            return false;
        }

        try {
            request = new BufferedOutputStream(reqSocket.getOutputStream());
            response = new BufferedInputStream(reqSocket.getInputStream());
        } catch (IOException e) {
            messageHandler.outputError(e);
            messageHandler.outputMessage("Failed to open Req/Res streams.");
        }
    }
}
