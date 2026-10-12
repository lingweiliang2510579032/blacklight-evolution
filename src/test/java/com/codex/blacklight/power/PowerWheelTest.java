package com.codex.blacklight.power;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PowerWheelTest {
    @Test void centerOutsideAndInvalidCoordinatesCancel(){
        assertEquals(-1,PowerWheel.sector(0,0,30,120));assertEquals(-1,PowerWheel.sector(200,0,30,120));
        assertEquals(-1,PowerWheel.sector(Double.NaN,0,30,120));assertEquals(-1,PowerWheel.sector(Double.POSITIVE_INFINITY,0,30,120));
    }
    @Test void everySectorMatchesItsDisplayedPosition(){
        for(int i=0;i<PowerWheel.size();i++){
            double a=-Math.PI/2+i*Math.PI*2/PowerWheel.size();
            assertEquals(i,PowerWheel.sector(Math.cos(a)*80,Math.sin(a)*80,30,120));
        }
    }
}
