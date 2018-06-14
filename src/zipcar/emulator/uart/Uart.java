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
import java.io.FileNotFoundException;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(path = Peripheral.REGISTRATION_PATH, service = Peripheral.class)
public class Uart implements Peripheral {

    static MessageHandler messageHandler = null;
    static SFR sfrBuff = null;
    static SFR sfrInterrupt = null;
    static SFR sfrSTA = null;
    static SFR sfrTX = null;
    int updateCounter = 0;
    static SFRSet sfrs;
    SCL scl;
    boolean notInitialized = true;
    int cycleCount = 0;
    LinkedList<Character> chars = new LinkedList<Character>();
    MySFRObserver sfrObs = new MySFRObserver();
    static RandomAccessFile request = null;
    static RandomAccessFile response = null;
    
    
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
        
        messageHandler.outputMessage("External Peripheral Initialized: UART");
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
        if (cycleCount == 1000000) {
            try {
                request = new RandomAccessFile("/Users/cgoldader/pic-brain/LMBrain.X/sim/req", "rw");
                response = new RandomAccessFile("/Users/cgoldader/pic-brain/LMBrain.X/sim/res", "rw");
            } catch (FileNotFoundException e) {
                messageHandler.outputMessage("Exception bois: " + e);
            }
        }
        if (cycleCount % 1000000 == 0) {
            try {
                chars.add(response.readChar()); // Doesn't currently work!!!!!!!
            } catch (Exception e) {
                messageHandler.outputMessage("Exception bois: " + e);
            }
            sfrTX.addObserver(sfrObs);
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

    public static void output() {
        try {
            request.write((byte) sfrTX.read()); 
        } catch (Exception e) {
            messageHandler.outputMessage(e + "");
        }
        // messageHandler.outputMessage((char) sfrTX.read() + "");
    }
}

class MySFRObserver implements SFRObserver {

    @Override
    public void update(SFR generator, SFREvent event, SFREventSource source) {
        if (event == SFREvent.SFR_CHANGED) {
            Uart.output();
        }
    }
}
