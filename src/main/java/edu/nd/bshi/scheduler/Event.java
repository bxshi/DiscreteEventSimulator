package edu.nd.bshi.scheduler;

public class Event {
    public static enum EVENT_TYPE {
        DATA_COMPUTE,
        READ_DISK,
        WRITE_DISK,
        READ_RAM,
        WRITE_RAM,
        SWITCH_THREAD_CONTEXT,
        SWITCH_PROCESS_CONTEXT
    }

    private EVENT_TYPE eventType;
    private int baseAddress;
    private int addressLength;

    public Thread getThread() {
        return thread;
    }

    Thread thread;
    Process process;

    Event(EVENT_TYPE eventType, int baseAddress, int addressLength, Process process, Thread thread){
        this.eventType = eventType;
        this.baseAddress = baseAddress;
        this.addressLength = addressLength;
        this.thread = thread;
        this.process = process;
    }

    Event(EVENT_TYPE eventType, Process process, Thread thread){
        this.eventType = eventType;
        this.thread = thread;
        this.process = process;
    }

    public int getBaseAddress() {
        return baseAddress;
    }
    public int getAddressLength() {
        return addressLength;
    }
    public EVENT_TYPE getEventType() {
        return eventType;
    }

    public void addPageFault(int time){
        if(this.process!=null){
            this.process.addPageFaultCount(time);
        }
        if(this.thread!=null){
            this.thread.addPageFaultCount(time);
        }
    }

    public void addTime(int time) {
        if(this.process!=null)
            this.process.addExecutionTime(time);
        if(this.thread!=null)
            this.thread.addExecutionTime(time);
    }

    @Override
    public String toString() {
        return this.eventType.toString()+" "+this.baseAddress+":"+this.addressLength+" "+this.thread;
    }

}
