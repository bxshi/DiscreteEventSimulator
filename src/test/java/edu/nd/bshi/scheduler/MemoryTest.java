package edu.nd.bshi.scheduler;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;

public class MemoryTest {
    private static final int VM=1000;
    private static final int PM=500;

    @Test
    public void testReadWithLRU() throws Exception {
        Disk disk = new Disk(50);
        Memory memory = new Memory(1000, 500, true, disk);
        Thread t1 = new Thread(2);
        Thread t2 = new Thread(2);
        Event eventLocalP1 = new Event(Event.EVENT_TYPE.READ_RAM, 0, 200, null, t1);
        Event eventLocalP2 = new Event(Event.EVENT_TYPE.READ_RAM, 500, 200, null, t2);
        Event eventRandomP1 = new Event(Event.EVENT_TYPE.READ_RAM, 200, (new Random().nextInt(300))%100, null, t1);
        Event eventRandomP2 = new Event(Event.EVENT_TYPE.READ_RAM, 700, (new Random().nextInt(300))%100, null, t2);
        memory.read(eventLocalP1);
        System.out.println("No PageFault");
        memory.read(eventLocalP2);
        System.out.println("200 PageFault");
        memory.read(eventRandomP1);
        System.out.println("max 100 pageFault");
        memory.read(eventRandomP2);
        System.out.println("max 100 pageFault");
        memory.read(eventLocalP1);
        System.out.println("No pageFault");
        memory.read(eventLocalP2);
        System.out.println("No pageFault");
        memory.read(eventRandomP1);
        System.out.println("max 100 pageFault");
        memory.read(eventRandomP2);
        System.out.println("max 100 pageFault");

        Assert.assertEquals(0, t1.getPageFaultCount());

    }

    @Test
    public void testWrite() throws Exception {
        Disk disk = new Disk(50);
        Memory memory = new Memory(1000, 500, true, disk);
        Thread t1 = new Thread(2);
        Thread t2 = new Thread(2);
        Event eventLocalP1 = new Event(Event.EVENT_TYPE.READ_RAM, 0, 200, null, t1);
        Event eventLocalP2 = new Event(Event.EVENT_TYPE.READ_RAM, 500, 200, null, t2);
        Event eventRandomP1 = new Event(Event.EVENT_TYPE.READ_RAM, 200, (new Random().nextInt(300))%100, null, t1);
        Event eventRandomP2 = new Event(Event.EVENT_TYPE.READ_RAM, 700, (new Random().nextInt(300))%100, null, t2);
        memory.write(eventLocalP1);
        System.out.println("No PageFault");
        memory.write(eventLocalP2);
        System.out.println("200 PageFault");
        memory.write(eventRandomP1);
        System.out.println("max 100 pageFault");
        memory.write(eventRandomP2);
        System.out.println("max 100 pageFault");
        memory.write(eventLocalP1);
        System.out.println("No pageFault");
        memory.write(eventLocalP2);
        System.out.println("No pageFault");
        memory.write(eventRandomP1);
        System.out.println("max 100 pageFault");
        memory.write(eventRandomP2);
        System.out.println("max 100 pageFault");

        Assert.assertEquals(0, t1.getPageFaultCount());
    }
}
