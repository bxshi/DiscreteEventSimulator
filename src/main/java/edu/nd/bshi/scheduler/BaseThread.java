package edu.nd.bshi.scheduler;

public class BaseThread {
    private Integer accumulatedExecutionTime =  0;
    private int executionTime = 0;
    private int pageFaultCount = 0;

    public int getPageFaultCount() {
        return pageFaultCount;
    }

    public void addPageFaultCount(int pageFaultCount) {
        this.pageFaultCount += pageFaultCount;
    }

    public Integer getAccumulatedExecutionTime(){
        return this.accumulatedExecutionTime;
    }

    public void addAccumulatedExecutionTime(Integer accumulatedExecutionTime) {
        this.accumulatedExecutionTime += accumulatedExecutionTime;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public void addExecutionTime(int executionTime) {
        this.executionTime += executionTime;
    }
}
