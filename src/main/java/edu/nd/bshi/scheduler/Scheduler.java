package edu.nd.bshi.scheduler;

import java.util.PriorityQueue;

public class Scheduler extends BaseThread {
    private PriorityQueue<Process> priorityQueue = null;
    private int timeSlot = 0;
    private Process currentProcess = null;

    public Scheduler(int memoryBaseAddress, int memorySize, int diskBaseAddress, int diskSize,
                     int processNumber, int threadNumber, int memoryPerProcess, int processTimeSlot,
                     int threadTimeSlot, OperationPattern.TYPE type, int operationPerThread){

        this.timeSlot = processTimeSlot;

        assert memoryPerProcess*processNumber <= memorySize;
        assert processNumber > 0;

        priorityQueue = new PriorityQueue<Process>(processNumber, new ProcessPriorityComparator());
        for (int i = 0; i < processNumber; i++){
            priorityQueue.add(new Process(memoryBaseAddress+i*memoryPerProcess, memoryPerProcess,
                    diskBaseAddress, diskSize, threadNumber, threadTimeSlot, type, operationPerThread));
        }
        this.currentProcess = priorityQueue.poll();
    }

    private boolean switchContext() {
        if(this.currentProcess == null){
            this.currentProcess = priorityQueue.poll();
            return true;
        }
        if(this.currentProcess.getExecutionTime() >= this.timeSlot){
            this.currentProcess.addAccumulatedExecutionTime(
                            this.currentProcess.getExecutionTime()
            );
            this.currentProcess.setExecutionTime(0);
            priorityQueue.add(this.currentProcess);
            this.currentProcess = priorityQueue.poll();
            return true;
        }
        return false;
    }

    public Event getEvent(){
        Event event;
        if(this.switchContext()){
            event = new Event(Event.EVENT_TYPE.SWITCH_PROCESS_CONTEXT, null, null);
        }else{
            event = this.currentProcess.getEvent();
            if(event == null) {
                this.addAccumulatedExecutionTime(this.currentProcess.getAccumulatedExecutionTime());
                this.currentProcess = null;
                if(this.priorityQueue.size()>0){
                    event = this.getEvent();
                }
            }
        }
        return event;
    }

}
