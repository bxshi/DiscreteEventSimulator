package edu.nd.bshi.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.PriorityQueue;
import java.util.Random;

public class Process extends BaseThread {
    static Logger logger = LogManager.getLogger(Process.class.getName());

    private PriorityQueue<Thread> priorityQueue = null;

    private static final int READ_RANGE = 10;
    private static final int STRIDE = 50;
    private static final double WORKINGSET_THREADHOLD = 0.8;

    private int memoryBaseAddress = 0;
    private int memorySize = 0;
    private int diskBaseAddress = 0;
    private int diskSize = 0;
    private int timeSlot = 0;
    private int workloadRatio = 50; // only used for patterned memory access
    private int totalWorkingSet = 50; // the pages of total working set
    private int inMemoryWorkingSet = 0; // the pages that in the memory
    private int memPos = 0;

    public int getPid() {
        return this.pid;
    }

    private int pid = 0;
    private Thread currentThread = null;
    private OperationPattern.TYPE type;

    public double getWorkingSetRatio(){
        return this.inMemoryWorkingSet / this.totalWorkingSet;
    }

    public void setInMemoryWorkingSet(int n){
        this.inMemoryWorkingSet = n;
    }
    public int getInMemoryWorkingSet(){
        return this.inMemoryWorkingSet;
    }

    public Process(int pid, int memoryBaseAddress, int memorySize, int diskBaseAddress, int diskSize,
                   int threadNumber, int timeSlot, OperationPattern.TYPE type, int operationPerThread,
                   int workloadRatio, int physicalMemory) {
        this.pid = pid;
        this.memoryBaseAddress = memoryBaseAddress;
        this.memorySize = memorySize;
        this.totalWorkingSet = memorySize;
        this.inMemoryWorkingSet = physicalMemory - memoryBaseAddress > 0 ? physicalMemory - memoryBaseAddress : 0;
        if(this.inMemoryWorkingSet > memorySize){
            this.inMemoryWorkingSet = memorySize;
        }
        this.diskBaseAddress = diskBaseAddress;
        this.diskSize = diskSize;
        this.timeSlot = timeSlot;
        this.type = type;
        this.workloadRatio = workloadRatio;
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
            //no thread, get a new one from pool
            this.currentThread = this.priorityQueue.poll();
            return true;
        }
        if(this.currentThread.getExecutionTime() >= this.timeSlot){
            this.currentThread.addAccumulatedExecutionTime(
                    this.currentThread.getExecutionTime()
            );
            this.currentThread.setExecutionTime(0);
            logger.trace("switch Thread, current "+this.currentThread);
            if(this.currentThread.getOperationCounter() < this.currentThread.getMaxOperations())
                this.priorityQueue.add(this.currentThread);
            logger.trace("Process status "+this.priorityQueue);
            this.currentThread = this.priorityQueue.poll();
            logger.trace("new thread "+this.currentThread);
            if(this.currentThread!=null)
                return true;
            else
                return false;
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

        //check if the thread finish it work
        if(threadFinished()){
            logger.info("thread"+this.currentThread+" FINISHED");
        }

        //check if the process finish its work
        if(processFinished()){
            logger.info("process Finished");
            this.inMemoryWorkingSet = 0;
            return null;
        }
        if (this.switchContext()){
            event = new Event(Event.EVENT_TYPE.SWITCH_THREAD_CONTEXT, this, null);
        }else{
            int baseAddr=0;
            int readSize=0;
            this.currentThread.addCounter();
            switch(this.type){
                //TODO implement other patterns
                case WEBSERVER:
                    baseAddr = random.nextInt(this.memorySize - READ_RANGE);
                    event =new Event(Event.EVENT_TYPE.READ_RAM,
                            this.memoryBaseAddress+baseAddr, READ_RANGE, this, this.currentThread);
                    break;
                case DATABASE:
                    if(eventSelector <= 80){
                        baseAddr = (new Random().nextInt(memorySize/STRIDE - 1)) * STRIDE;
                        event =new Event(Event.EVENT_TYPE.READ_RAM,
                                this.memoryBaseAddress+baseAddr, READ_RANGE, this, this.currentThread);
                    }else{
                        baseAddr = (new Random().nextInt(memorySize/STRIDE - 1)) * STRIDE;
                        event = new Event(Event.EVENT_TYPE.WRITE_RAM,
                                this.memoryBaseAddress+baseAddr, STRIDE, this, this.currentThread);
                    }
                    break;
                case MEMPAT:
                    if(eventSelector <= 50) {
                        if(eventSelector < 50*workloadRatio/100){
                            baseAddr = 0;
                            readSize = this.memorySize * 4 / 10;
                        }else{
                            baseAddr = random.nextInt(this.memorySize);
                            readSize = random.nextInt(new Random().nextInt(this.memorySize - baseAddr) % 99+1);
                        }
                        event = new Event(Event.EVENT_TYPE.READ_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else{
                        if(eventSelector <= 50 + 50*workloadRatio/100){
                            baseAddr = 0;
                            readSize = this.memorySize * 4 / 10;
                        }else{
                            baseAddr = random.nextInt(this.memorySize);
                            readSize = random.nextInt(new Random().nextInt(this.memorySize - baseAddr) % 99+1);
                        }
                        event = new Event(Event.EVENT_TYPE.WRITE_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }
                    break;
                case MEMONLY:

                    if(eventSelector <= 50) {
                        baseAddr = memPos;
                        readSize = memorySize / 10;
                        memPos = (memPos+ memorySize / 10) %(memorySize/10 * 9);

                        event = new Event(Event.EVENT_TYPE.READ_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else{
                        baseAddr = memPos;
                        readSize = memorySize / 10;
                        memPos = (memPos+ memorySize / 10) %(memorySize/10 * 9);
                        event = new Event(Event.EVENT_TYPE.WRITE_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }
                    break;
                case RANDOM:
                    if(eventSelector <= 50) {
                        baseAddr = random.nextInt(this.memorySize);
                        readSize = random.nextInt(this.memorySize - baseAddr+1)+1;
                        event = new Event(Event.EVENT_TYPE.READ_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }else{
                        baseAddr = random.nextInt(this.memorySize);
                        readSize = random.nextInt(this.memorySize - baseAddr+1)+1;
                        event = new Event(Event.EVENT_TYPE.WRITE_RAM,
                                this.memoryBaseAddress+baseAddr, readSize, this, this.currentThread);
                    }
                    break;
            }
        }
        return event;
    }

    @Override
    public String toString(){
        return "\ntotal Exec Time:"+this.getAccumulatedExecutionTime()+
               "\nexecution Time:"+this.getExecutionTime()+
               "\n"+priorityQueue.toString();
    }

    public String printQueue(){
        return priorityQueue.toString();
    }

}
