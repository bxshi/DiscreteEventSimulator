package edu.nd.bshi.scheduler;

import java.util.Comparator;

public class ProcessPriorityComparator implements Comparator<Process> {

    @Override
    public int compare(Process p1, Process p2){
        if(p1.getAccumulatedExecutionTime() < p2.getAccumulatedExecutionTime()){
            return -1;
        }
        if(p1.getAccumulatedExecutionTime() > p2.getAccumulatedExecutionTime()){
            return 1;
        }
        return 0;
    }

}
