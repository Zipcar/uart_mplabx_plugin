package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.Peripheral;
import com.microchip.mplab.mdbcore.simulator.SFR;
import com.microchip.mplab.mdbcore.simulator.SFRSet;
import com.microchip.mplab.mdbcore.simulator.scl.SCL;
import com.microchip.mplab.mdbcore.simulator.MessageHandler;
import com.microchip.mplab.mdbcore.simulator.SimulatorDataStore.SimulatorDataStore;
import com.microchip.mplab.mdbcore.simulator.PeripheralSet;
import com.microchip.mplab.mdbcore.simulator.SFR.SFRObserver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.LinkedList;
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

    // declare pipe vars
    String reqPipePath = "/Users/cgoldader/pic-brain/LMBrain.X/sim/u2req"; // TEMPORARY PATHS -- CHANGE
    String resPipePath = "/Users/cgoldader/pic-brain/LMBrain.X/sim/u2res"; // >>>>>>>>>>>>>>>>>>>>>>>>>
    File in = new File(reqPipePath);
    File out = new File(reqPipePath);
    static FileOutputStream requestStream = null;
    static FileInputStream responseStream = null;

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
            sfrTX.addObserver(sfrObs);

                // prepare pipes (need to catch exceptions)
            try {
                // Runtime.getRuntime().exec("mkfifo " + reqPipePath); // Create pipes if they don't exist
                // Runtime.getRuntime().exec("mkfifo " + resPipePath);
                messageHandler.outputMessage("I am trying to init! I promise!");
                requestStream = new FileOutputStream(in);
                responseStream = new FileInputStream(out);
            } catch (Exception e) {
                messageHandler.outputMessage("Yikes. Hit an error: " + e.toString());
                messageHandler.outputError(e);
            }
            try {
                if (responseStream.available() != 0) {
                    chars.add((char) responseStream.read());
                }
            } catch (Exception e) {
                messageHandler.outputMessage("Yikes. Hit an error: " + e.toString());
                messageHandler.outputError(e);
            }
        }
        
        if (cycleCount % 1000000 == 0) {
            if (!chars.isEmpty()) {
                if (sfrSTA.getFieldValue("UTXEN") == 1) {
                    // messageHandler.outputMessage(chars.peek() + "");
                    sfrBuff.privilegedWrite(chars.pop());   // Inject the next char
                    sfrInterrupt.privilegedSetFieldValue("U2RXIF", 1); // Trigger the interrupt
                    // sfrSTA.privilegedSetFieldValue("URXDA", 1); // Trigger the STA
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
        char currentChar = (char) sfrTX.read();
        messageHandler.outputMessage(currentChar + "");
        try {
            requestStream.write((byte) currentChar);
        } catch (Exception e) {
            messageHandler.outputMessage("Yikes, hit an exception in output");
            messageHandler.outputError(e);
        }
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
