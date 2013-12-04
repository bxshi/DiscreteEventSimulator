package edu.nd.bshi.scheduler;

import junit.framework.Assert;
import org.junit.Test;

public class ProcessTest {
    @Test
    public void testGetEvent() throws Exception {
        int count=0;
        Process testProcess = new Process(0, 1000, 0, 1000, 2, 1, OperationPattern.TYPE.MEMPAT, 10, 50);
        Event event;
        while(count<20){
            event = testProcess.getEvent();
            Assert.assertNotNull(event);
            count++;
        }
    }
}
