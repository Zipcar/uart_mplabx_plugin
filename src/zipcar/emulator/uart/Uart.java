package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.Peripheral;
import com.microchip.mplab.mdbcore.simulator.SFR;
import com.microchip.mplab.mdbcore.simulator.SFRSet;
import com.microchip.mplab.mdbcore.simulator.scl.SCL;
import com.microchip.mplab.mdbcore.simulator.MessageHandler;
import com.microchip.mplab.mdbcore.simulator.SimulatorDataStore.SimulatorDataStore;
import com.microchip.mplab.mdbcore.simulator.PeripheralSet;
import com.microchip.mplab.mdbcore.simulator.SFR.SFRObserver;
import java.util.LinkedList;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(path = Peripheral.REGISTRATION_PATH, service = Peripheral.class)
public class Uart implements Peripheral {

    MessageHandler messageHandler = null;
    SFR sfrBuff = null;
    SFR sfrInterrupt = null;
    SFR sfrSTA = null;
    SFR sfrTX = null;
    int updateCounter = 0;
    SFRSet sfrs;
    SCL scl;
    boolean notInitialized = true;
    int cycleCount = 0;
    LinkedList<Character> chars = new LinkedList<Character>();
    FileOutputStream request = null;
    FileInputStream response = null;
    static Uart instance = null;
    
    
    @Override
    public boolean init(SimulatorDataStore DS) {
        // initialize instance variables
        messageHandler = DS.getMessageHandler();
        sfrs = DS.getSFRSet();
        sfrBuff = sfrs.getSFR("U2RXREG");
        sfrInterrupt = sfrs.getSFR("IFS1");
        sfrSTA = sfrs.getSFR("U2STA");
        sfrTX = sfrs.getSFR("U2TXREG");
        
        // remove UART2
        PeripheralSet periphSet = DS.getPeripheralSet();
        Peripheral uartPeriph = periphSet.getPeripheral("UART2");
        if (uartPeriph != null) {
            uartPeriph.deInit();
            periphSet.removePeripheral(uartPeriph);
        }

        // setup pipes
        try {
            request = new FileOutputStream("/Users/cgoldader/pic-brain/LMBrain.X/sim/req");
            response = new FileInputStream("/Users/cgoldader/pic-brain/LMBrain.X/sim/res");
        } catch (FileNotFoundException e) {
            messageHandler.outputMessage("Exception in update: " + e);
            return false;
        }
        
        // add observers
        UartObserver obs = new UartObserver();
        sfrTX.addObserver(obs);
        
        messageHandler.outputMessage("External Peripheral Initialized: UART");
        
        instance = this;
        // add peripheral to list and return true
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
        return "UART2_SIM";
    }

    @Override
    public void removeObserver(PeripheralObserver observer) {

    }

    @Override
    public void reset() {

    }

    @Override
    public void update() {
        if (cycleCount % 1000000 == 0) {
            try {
                if (response.available() != 0) {
                    messageHandler.outputMessage("available bytes: " + response.available());
                    chars.add((char) response.read()); // Doesn't currently work!!!!!!!
                }
            } catch (IOException e) {
                messageHandler.outputMessage("Exception reading character from res " + e);
                return;
            }
            if (!chars.isEmpty()) {
                if (sfrSTA.getFieldValue("UTXEN") == 1) {
                    // messageHandler.outputMessage(chars.peek() + "");
                    sfrBuff.privilegedWrite(chars.pop());   // Inject the next char
                    sfrInterrupt.privilegedSetFieldValue("U2RXIF", 1); // Trigger the interrupt
                }
            }
        }
        cycleCount++;
    }

    public void setString(String str) {
        for (int i = 0; i < str.length(); i++) {
            chars.add(str.charAt(i));
        }
    }

    public void output() {
        try {
            // messageHandler.outputMessage("Should be outputting " + sfrTX.read());
            request.write((byte) sfrTX.read()); 
        } catch (Exception e) {
            messageHandler.outputMessage("failed to write request byte " + e);
        }
        // messageHandler.outputMessage((char) sfrTX.read() + "");
    }
    
    public static Uart get() {
        return instance;
    }
}
