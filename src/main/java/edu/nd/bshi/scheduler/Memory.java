package edu.nd.bshi.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Random;

public class Memory {
    static Logger logger = LogManager.getLogger(Process.class.getName());
    private Disk disk = null;
    private int blockLoadTime = 164000;
    private int blockWriteTime = 164000;
    private int blockInMemoryRead = 1;
    private int blockInMemoryWrite = 1;
    private HashMap<Integer, Integer> memorySpace = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer> LRU = new HashMap<Integer, Integer>();
    private static final int IN_MEM = 1;
    private static final int OUT_MEM = 0;
    private static final int DIRTY = 2;
    private boolean hasLRU = true;
    public Memory(int virtualMemSize, int physicalMemSize, boolean hasLRU, Disk disk) {
        this.hasLRU = hasLRU;
        this.disk = disk;
        //initialize memory
        for(int i = 0; i < physicalMemSize; i++) {
            memorySpace.put(i, IN_MEM);
            if(this.hasLRU)
                LRU.put(i, 0);
        }
        for(int i = physicalMemSize; i < virtualMemSize; i++) {
            memorySpace.put(i, OUT_MEM);
        }
    }

    private int loadMemory(int blk){
        int time = 0;
        time += this.removeMemory();
        if(this.hasLRU)
            this.LRU.put(blk, 1);
        this.memorySpace.put(blk, IN_MEM);
        return time + this.disk.loadMemory(blk);
    }

    private int removeMemory(){
        int lowest = 0;
        int hit = 9999;
        int time = 0;
        if(this.hasLRU){
            for(int i : this.LRU.keySet()){
                if(this.LRU.get(i)<=hit){
                    lowest = i;
                    hit = this.LRU.get(i);
                }
            }

            if(this.memorySpace.get(lowest) == DIRTY){
                time += this.disk.writeMemory(lowest);
            }
            this.memorySpace.put(lowest, OUT_MEM);
            this.LRU.remove(lowest);

            logger.trace("kick out "+lowest);

            return time;
        }else{
            int rdm = (new Random().nextInt() % this.memorySpace.keySet().size());
            while(this.memorySpace.get(rdm)==null || this.memorySpace.get(rdm)==OUT_MEM)
                rdm = (new Random().nextInt() % this.memorySpace.keySet().size());
            if(this.memorySpace.get(rdm) == DIRTY)
                time += this.disk.writeMemory(rdm);
            this.memorySpace.put(rdm, OUT_MEM);
            return time;
        }
    }

    public void read(Event event) {
        int time = 0;
        int pFault = 0;
//        logger.trace("start "+LRU.toString());
//        logger.trace(memorySpace.toString());
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    logger.trace("Block "+i+" out of memory");
                    time += this.loadMemory(i);
                    pFault++;
                }else{
                    if(this.hasLRU){
                        int hit = LRU.get(i);
                        LRU.put(i, hit+1);
                    }

                }
            }
        }catch (Exception err){
            err.printStackTrace();
        }
        event.addPageFault(pFault);
        event.addTime(time+blockInMemoryRead);

//        logger.trace("end"+LRU.toString());
//        logger.trace(memorySpace.toString());


    }

    public void write(Event event) {
        int time = 0;
        int pFault = 0;
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    logger.trace("Block "+i+" out of memory");
                    time += this.loadMemory(i);
                    memorySpace.put(i, DIRTY);
                    pFault++;
                }else{
                    if(this.hasLRU){
                        int hit = LRU.get(i);
                        LRU.put(i, hit+1);
                    }
                    memorySpace.put(i, DIRTY);
                }
            }
        }catch (Exception err){
            err.printStackTrace();
        }
        event.addPageFault(pFault);
        event.addTime(time+blockInMemoryWrite);
    }

}
