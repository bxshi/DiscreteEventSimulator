package edu.nd.bshi.scheduler;

import java.util.HashMap;
import java.util.Random;

public class Memory {
    private int blockLoadTime = 4100000;
    private int blockWriteTime = 4100000;
    private int blockInMemoryRead = 25;
    private int blockInMemoryWrite = 25;
    private HashMap<Integer, Integer> memorySpace = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer> LRU = new HashMap<Integer, Integer>();
    private static final int IN_MEM = 1;
    private static final int OUT_MEM = 0;
    private static final int DIRTY = 2;
    private boolean hasLRU = true;

    public Memory(int virtualMemSize, int physicalMemSize, boolean hasLRU) {
        this.hasLRU = hasLRU;
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
            this.LRU.put(blk, 0);
        this.memorySpace.put(blk, IN_MEM);
        return time;
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
                time += this.blockWriteTime;
            }
            this.memorySpace.put(lowest, OUT_MEM);
            this.LRU.remove(lowest);

            return time + this.blockLoadTime;
        }else{
            int rdm = (new Random().nextInt() % this.memorySpace.keySet().size());
            while(this.memorySpace.get(rdm)==null || this.memorySpace.get(rdm)==OUT_MEM)
                rdm = (new Random().nextInt() % this.memorySpace.keySet().size());
            if(this.memorySpace.get(rdm) == DIRTY)
                time += this.blockWriteTime;
            this.memorySpace.put(rdm, OUT_MEM);
            return time + this.blockLoadTime;
        }
    }

    public void read(Event event) {
        int time = 0;
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    int t = this.loadMemory(i);
                    if(t > time) time = t;
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
        if(time!=0)
            event.addPageFault(1);
        event.addTime(time+blockInMemoryRead);

    }

    public void write(Event event) {
        int time = 0;
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    int t = this.loadMemory(i);
                    memorySpace.put(i, DIRTY);
                    if(t > time) time = t;
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
        if(time!=0)
            event.addPageFault(1);
        event.addTime(time+blockInMemoryWrite);
    }

}
