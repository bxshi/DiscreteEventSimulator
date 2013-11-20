package edu.nd.bshi.scheduler;

public class Thread extends BaseThread {

    private int maxOperations = 0;
    private int operationCounter = 0;

    Thread(int maxOperations){
        this.maxOperations = maxOperations;
    }

    public boolean destroy(){
        return this.operationCounter >= maxOperations;
    }

    public void addCounter(){
        operationCounter++;
    }

    @Override
    public String toString() {
        return "{id:"+this.hashCode()+",accumulate:"+this.getAccumulatedExecutionTime()+",execution:"+this.getExecutionTime()+",operation:"+this.operationCounter+"}";
    }
}
