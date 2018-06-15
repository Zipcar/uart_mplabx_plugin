/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zipcar.emulator.uart;

import com.microchip.mplab.mdbcore.simulator.SFR.SFRObserver;
import com.microchip.mplab.mdbcore.simulator.SFR;

/**
 *
 * @author cgoldader
 */
public class UartObserver implements SFRObserver {

    @Override
    public void update(SFR generator, SFREvent event, SFREventSource source) {

        if (event == SFREvent.SFR_CHANGED) {
            Uart.get().output();
        }

    }
}
