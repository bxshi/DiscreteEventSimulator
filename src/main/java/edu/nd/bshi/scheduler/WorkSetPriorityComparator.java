package edu.nd.bshi.scheduler;

import java.util.Comparator;

public class WorkSetPriorityComparator implements Comparator<Process> {

    @Override
    public int compare(Process p1, Process p2){
        if(p1.getWorkingSetRatio() > p2.getWorkingSetRatio()){
            return -1;
        }
        if(p1.getWorkingSetRatio() < p2.getWorkingSetRatio()){
            return 1;
        }
        return 0;
    }

}
