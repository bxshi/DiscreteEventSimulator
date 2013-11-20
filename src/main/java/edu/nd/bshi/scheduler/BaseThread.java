package edu.nd.bshi.scheduler;

public class BaseThread {
    private Integer accumulatedExecutionTime =  0;
    private int executionTime = 0;
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
