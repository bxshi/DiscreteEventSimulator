package edu.nd.bshi.scheduler;

import java.util.Comparator;

public class ThreadPriorityComparator implements Comparator<Thread> {

    @Override
    public int compare(Thread p1, Thread p2){
        if(p1.getAccumulatedExecutionTime() < p2.getAccumulatedExecutionTime()){
            return -1;
        }
        if(p1.getAccumulatedExecutionTime() > p2.getAccumulatedExecutionTime()){
            return 1;
        }
        return 0;
    }

}
