package edu.nd.bshi.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.PriorityQueue;
import java.util.Random;

public class Process extends BaseThread {
    static Logger logger = LogManager.getLogger(Process.class.getName());

    private PriorityQueue<Thread> priorityQueue = null;

    private int memoryBaseAddress = 0;
    private int memorySize = 0;
    private int diskBaseAddress = 0;
    private int diskSize = 0;
    private int timeSlot = 0;
    private Thread currentThread = null;
    private OperationPattern.TYPE type;

    public Process(int memoryBaseAddress, int memorySize, int diskBaseAddress, int diskSize,
                   int threadNumber, int timeSlot, OperationPattern.TYPE type, int operationPerThread) {
        this.memoryBaseAddress = memoryBaseAddress;
        this.memorySize = memorySize;
        this.diskBaseAddress = diskBaseAddress;
        this.diskSize = diskSize;
        this.timeSlot = timeSlot;
        this.type = type;
        //at least one thread
        threadNumber = threadNumber == 0 ? threadNumber+1 : threadNumber;
        this.priorityQueue = new PriorityQueue<Thread>(threadNumber, new ThreadPriorityComparator());
        for(int i = 0; i < threadNumber; i++){
            this.priorityQueue.add(new Thread(operationPerThread));
        }

        logger.trace("initialize Process "+priorityQueue.toString());

        //get initial thread with zero cost
        this.currentThread = this.priorityQueue.poll();
    }

    private boolean threadFinished(){
        if(this.currentThread.destroy()){
            this.currentThread = null;
            return true;
        }
        return false;
    }

    private boolean processFinished(){
        return this.priorityQueue.size()==0 && this.currentThread==null;
    }

    private boolean switchContext(){
        if(this.priorityQueue.size() == 0){
            //one thread, never switch
            return false;
        }
        if(this.currentThread == null) {
            this.currentThread = this.priorityQueue.poll();
            return true;
        }
        if(this.currentThread.getExecutionTime() >= this.timeSlot){
            this.currentThread.addAccumulatedExecutionTime(
                    this.currentThread.getExecutionTime()
            );
            this.currentThread.setExecutionTime(0);
            logger.trace("switch Thread, current "+this.currentThread);
            this.priorityQueue.add(this.currentThread);
            logger.trace("Process status "+this.priorityQueue);
            this.currentThread = this.priorityQueue.poll();
            logger.trace("new thread "+this.currentThread);
            return true;
        }
        return false;
    }

    public Event getEvent(){
        return eventGenerator();
    }

    private Event eventGenerator(){

        Event event = null;
        Random random = new Random();
        int eventSelector = random.nextInt(100)+1;

        threadFinished();
        if(processFinished()){
            return null;
        }
        if (this.switchContext()){
            event = new Event(Event.EVENT_TYPE.SWITCH_THREAD_CONTEXT, this, null);
        }else{
            switch(this.type){
                //TODO implement other patterns
                case SERVER:
                case DATABASE:
                case RANDOM:
                    if(eventSelector <= 25) {
                        int baseAddr = random.nextInt(this.diskSize);
                        int readSize = random.nextInt(this.diskSize - baseAddr);
                        event = new Event(Event.EVENT_TYPE.READ_DISK,
                                this.diskBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else if(eventSelector <= 50) {
                        int baseAddr = random.nextInt(this.diskSize);
                        int readSize = random.nextInt(this.diskSize - baseAddr);
                        event = new Event(Event.EVENT_TYPE.WRITE_DISK,
                                this.diskBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else if(eventSelector <= 75) {
                        int baseAddr = random.nextInt(this.memorySize);
                        int readSize = random.nextInt(this.memorySize - baseAddr);
                        event = new Event(Event.EVENT_TYPE.READ_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else if(eventSelector <= 100) {
                        int baseAddr = random.nextInt(this.memorySize);
                        int readSize = random.nextInt(this.memorySize - baseAddr);
                        event = new Event(Event.EVENT_TYPE.WRITE_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }
                    break;
            }
            this.currentThread.addCounter();
        }
        return event;
    }

    @Override
    public String toString(){
        return "\ntotal Exec Time:"+this.getAccumulatedExecutionTime()+
               "\nexecution Time:"+this.getExecutionTime()+
               "\n"+priorityQueue.toString();
    }

}
