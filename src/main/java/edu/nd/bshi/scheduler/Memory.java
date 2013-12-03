package edu.nd.bshi.scheduler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class Memory {
    private int virtualMemSize, physicalMemSize;
    private int blockLoadTime = 10;
    private int blockWriteTime = 10;
    private int blockInMemoryRead = 1;
    private int blockInMemoryWrite = 1;
    private HashMap<Integer, Integer> memorySpace = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer> LRU = new HashMap<Integer, Integer>();
    private static final int IN_MEM = 1;
    private static final int OUT_MEM = 0;
    private static final int DIRTY = 2;

    public Memory(int virtualMemSize, int physicalMemSize) {
        this.virtualMemSize = virtualMemSize;
        this.physicalMemSize = physicalMemSize;

        //initialize memory
        for(int i = 0; i < physicalMemSize; i++) {
            memorySpace.put(i, IN_MEM);
            HashMap<Integer, Integer> memoryBlk = new HashMap<Integer, Integer>();
            LRU.put(i, 0);
        }
        for(int i = physicalMemSize; i < virtualMemSize; i++) {
            memorySpace.put(i, OUT_MEM);
        }
    }

    private int loadMemory(int blk){
        int time = 0;
        time += this.removeMemory();
        this.LRU.put(blk, 0);
        return time + this.memorySpace.put(blk, IN_MEM);
    }

    private int removeMemory(){
        int lowest = 0;
        int hit = 9999;
        int time = 0;

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
    }

    public void read(Event event) {
        int time = 0;
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    //TODO not in memory, need to do a disk load
                    time += this.loadMemory(i);
                }else{
                    int hit = LRU.get(i);
                    LRU.put(i, hit+1);
                    time += blockInMemoryRead;
                }
            }
        }catch (Exception err){
            err.printStackTrace();
        }

        event.addTime(time);

    }

    public void write(Event event) {
        int time = 0;
        try{
            for(int i = event.getBaseAddress(); i < event.getBaseAddress()+event.getAddressLength(); i++) {
                if(memorySpace.get(i) == OUT_MEM){
                    //TODO not in memory, need to do a disk load
                    time += this.loadMemory(i);
                    memorySpace.put(i, DIRTY);
                }else{
                    int hit = LRU.get(i);
                    LRU.put(i, hit+1);
                    time += blockInMemoryWrite;
                    memorySpace.put(i, DIRTY);
                }
            }
        }catch (Exception err){
            err.printStackTrace();
        }

        event.addTime(time);
    }

}
