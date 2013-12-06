package edu.nd.bshi.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
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
    private Scheduler scheduler = null;
    int memoryPerProcess;
    int processNumber;
    private MemoryReplacementAlgo.ALGORITHM  algo;
    public Memory(int virtualMemSize, int physicalMemSize, int memoryPerProcess, int processNumber, MemoryReplacementAlgo.ALGORITHM algo, Disk disk, Scheduler scheduler) {
        this.algo = algo;
        this.disk = disk;
        this.memoryPerProcess = memoryPerProcess;
        this.processNumber = processNumber;
        this.scheduler = scheduler;
        //initialize memory
        for(int i = 0; i < physicalMemSize; i++) {
            memorySpace.put(i, IN_MEM);
        }
        for(int i = physicalMemSize; i < virtualMemSize; i++) {
            memorySpace.put(i, OUT_MEM);
        }

        switch (algo){
            case WORKSET:

            case LRU:
                for(int i = 0; i < physicalMemSize; i++){
                    LRU.put(i,0);
                }
                break;

        }

    }

    private int loadMemory(int blk, Event event){
        int time = 0;
        time += this.removeMemory(event);
        //update working set ratio
        if(algo != MemoryReplacementAlgo.ALGORITHM.RANDOM)
            this.LRU.put(blk, 1);
        this.memorySpace.put(blk, IN_MEM);
        if(algo == MemoryReplacementAlgo.ALGORITHM.WORKSET){
            //if load some memory, it must belongs to the current process
            event.addInMemoryWorkSetSize();
        }
        return time + this.disk.loadMemory(blk);
    }

    private int removeMemory(Event event){
        int lowest = 0;
        int hit = 9999;
        int time = 0;

        if(algo == MemoryReplacementAlgo.ALGORITHM.WORKSET){
            LinkedList<Integer> deadProcess = this.scheduler.getDeadProcess();
            Process process = this.scheduler.getLowestProcess();
            int id = process.getPid();
            for(int i : this.LRU.keySet()){
                for(int p : deadProcess){
                    if(i > this.memoryPerProcess * p && i < this.memoryPerProcess *(p+1)){
                        lowest = i;
                        hit = this.LRU.get(i);
                        this.memorySpace.put(lowest, OUT_MEM);
                        this.LRU.remove(lowest);
                        process.setInMemoryWorkingSet(
                                process.getInMemoryWorkingSet()-1
                        );

                        logger.trace("kick out "+lowest);

                        return time;
                    }
                }
                if(i > this.memoryPerProcess * id && i < this.memoryPerProcess *(id+1)){
                    if(this.LRU.get(i)<=hit){
                        lowest = i;
                        hit = this.LRU.get(i);
                    }
                }
            }

            if(this.memorySpace.get(lowest) == DIRTY){
                time += this.disk.writeMemory(lowest);
            }
            this.memorySpace.put(lowest, OUT_MEM);
            this.LRU.remove(lowest);
            process.setInMemoryWorkingSet(
                    process.getInMemoryWorkingSet()-1
            );

            logger.trace("kick out "+lowest);

            return time;

        }else if(algo != MemoryReplacementAlgo.ALGORITHM.RANDOM){
            //update working set ratio
            //remove pages from the one with lowest ratio
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
                    time += this.loadMemory(i, event);
                    pFault++;
                }else{
                    if(algo != MemoryReplacementAlgo.ALGORITHM.RANDOM){
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
                    time += this.loadMemory(i, event);
                    memorySpace.put(i, DIRTY);
                    pFault++;
                }else{
                    if(algo != MemoryReplacementAlgo.ALGORITHM.RANDOM){
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