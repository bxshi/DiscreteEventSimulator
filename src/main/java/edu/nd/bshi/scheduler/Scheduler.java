package edu.nd.bshi.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.PriorityQueue;

public class Scheduler extends BaseThread {
    static Logger logger = LogManager.getLogger(Process.class.getName());

    private PriorityQueue<Process> priorityQueue = null;
    private LinkedList<Integer> deadProcess = null;
    private int timeSlot = 0;
    private Process currentProcess = null;
    private int processNumber;

    public Scheduler(int memoryBaseAddress, int memorySize, int diskBaseAddress, int diskSize,
                     int processNumber, int threadNumber, int memoryPerProcess, int processTimeSlot,
                     int threadTimeSlot, OperationPattern.TYPE type, int operationPerThread, int workloadRatio,
                     int physicalMem, MemoryReplacementAlgo.ALGORITHM algo){

        this.timeSlot = processTimeSlot;
        this.processNumber = processNumber;
        deadProcess = new LinkedList<Integer>();

        assert memoryPerProcess*processNumber <= memorySize;
        assert processNumber > 0;

        if(algo == MemoryReplacementAlgo.ALGORITHM.WORKSET){
            priorityQueue = new PriorityQueue<Process>(processNumber, new WorkSetPriorityComparator());
        }else{
            priorityQueue = new PriorityQueue<Process>(processNumber, new ProcessPriorityComparator());
        }
        for (int i = 0; i < processNumber; i++){
            priorityQueue.add(new Process(i, memoryBaseAddress+i*memoryPerProcess, memoryPerProcess,
                    diskBaseAddress, diskSize, threadNumber, threadTimeSlot, type, operationPerThread,
                    workloadRatio, physicalMem));
        }
        this.currentProcess = priorityQueue.poll();
    }

    public LinkedList<Integer> getDeadProcess(){
        return deadProcess;
    }

    public Process getLowestProcess(){
        Process process = this.currentProcess;
        int cnt =0;
        while(this.priorityQueue.iterator().hasNext() && cnt++ < processNumber){
            Process tmp = this.priorityQueue.iterator().next();
            if(tmp == process)
                break;
            if(process==null && tmp.getInMemoryWorkingSet() > 0)
                process = this.priorityQueue.iterator().next();
            else{
                if(tmp.getInMemoryWorkingSet()>0 && process.getInMemoryWorkingSet() > tmp.getInMemoryWorkingSet()){
                    process = tmp;
                }
            }
        }
        logger.trace("return process "+process.getPid());
        return process;
    }

    private boolean switchContext() {
        //no process, get one to start
        if(this.currentProcess == null){
            this.currentProcess = priorityQueue.poll();
            logger.info("Process==null, get new process"+this.currentProcess.printQueue());
            return true;
        }
        //check if exceed its running time
        if(this.currentProcess.getExecutionTime() >= this.timeSlot){
            logger.info("Process "+this.currentProcess.printQueue()+" exceed execution time");
            this.currentProcess.addAccumulatedExecutionTime(
                            this.currentProcess.getExecutionTime()
            );
            this.currentProcess.setExecutionTime(0);
            if(priorityQueue.size() == 0)
                return false;
            priorityQueue.add(this.currentProcess);
            this.currentProcess = priorityQueue.poll();
            logger.info("Get new process "+this.currentProcess.printQueue());
            return true;
        }
        return false;
    }

    public Event getEvent(){
        Event event;
        //Check if we need to switch between process
        if(this.switchContext()){
            event = new Event(Event.EVENT_TYPE.SWITCH_PROCESS_CONTEXT, null, null);
        }else{
            //if no need to switch, try get event from this process
            event = this.currentProcess.getEvent();
            if(event == null) { //can not get event from this process(which means this process is finished)
                deadProcess.add(this.currentProcess.getPid());
                this.addAccumulatedExecutionTime(this.currentProcess.getAccumulatedExecutionTime());
                this.addPageFaultCount(this.currentProcess.getPageFaultCount());
                this.currentProcess = null;
                if(this.priorityQueue.size()>0){//still have other processes to use.
                    event = this.getEvent();
                }
            }
        }
        return event;
    }

}
