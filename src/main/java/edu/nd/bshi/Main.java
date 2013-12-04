package edu.nd.bshi;

import edu.nd.bshi.scheduler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Main {

    static Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) {

        HashMap<String, Object> options = new HashMap<String, Object>();

        for(String arg : args) {
            if(arg.startsWith("-VM")){
                options.put("virtual_memory", Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-PM")){
                options.put("physical_memory",Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-PS")){
                options.put("process_time",Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-TS")){
                options.put("thread_time",Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-TP")){
                options.put("thread",Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-D")){
                options.put("disk",Integer.parseInt(arg.substring(2)));
            }else if(arg.startsWith("-P")){
                options.put("process", Integer.parseInt(arg.substring(2)));
            }else if(arg.startsWith("-M")){
                options.put("process_memory", Integer.parseInt(arg.substring(2)));
            }else if(arg.startsWith("-O")){
                options.put("operation", Integer.parseInt(arg.substring(2)));
            }else if(arg.startsWith("-T")){
                options.put("type", OperationPattern.TYPE.valueOf(arg.substring(2)));
            }else if(arg.startsWith("-S")){
                options.put("seek_time", Integer.parseInt(arg.substring(2)));
            }else if(arg.startsWith("-WR")){
                options.put("workload_ratio", Integer.parseInt(arg.substring(3)));
            }else if(arg.startsWith("-W")){
                options.put("work_load", true);
            }

        }

        if(options.keySet().size()<11){
            logger.error("\ndes\n" +
                    "    [-VM VirtualMemory Size] [-PM Physical Memory Size]\n" +
                    "    [-D Disk Size] [-P Process Number]\n" +
                    "    [-TP Thread Per Process] [-M Memory Per Process]\n" +
                    "    [-PS Process Time Slot] [-TS Thread Time Slot]\n" +
                    "    [-T Thread Operation Pattern (RANDOM|SERVER|DATABASE)]\n" +
                    "    [-O Operations per Thread] [-S Seek Time]\n" +
                    "    [-W withWorkload (Optional)]");
            return;
        }
        if(!options.containsKey("workload_ratio")){
            options.put("workload_ratio", 50);
        }
        Scheduler scheduler = new Scheduler(
                0,
                (Integer)options.get("virtual_memory"),
                0,
                (Integer)options.get("disk"),
                (Integer)options.get("process"),
                (Integer)options.get("thread"),
                (Integer)options.get("process_memory"),
                (Integer)options.get("process_time"),
                (Integer)options.get("thread_time"),
                (OperationPattern.TYPE)options.get("type"),
                (Integer)options.get("operation"),
                (Integer)options.get("workload_ratio")
        );
        Disk disk = new Disk(
                (Integer)options.get("seek_time")
        );

        Memory memory = new Memory(
                (Integer)options.get("virtual_memory"),
                (Integer)options.get("physical_memory"),
                options.keySet().contains("work_load") ? (Boolean)options.get("work_load") : false,
                disk
        );

        while(true) {
            Event event = scheduler.getEvent();
            if(event == null){
                System.out.println(scheduler.getAccumulatedExecutionTime()+","+scheduler.getPageFaultCount());
                System.exit(0);
            }else{
                logger.trace(event.toString());
                switch(event.getEventType()){
                    case SWITCH_PROCESS_CONTEXT:
                        event.addTime(1);
                        //dirty trick
                        scheduler.addAccumulatedExecutionTime(1);
                        break;
                    case SWITCH_THREAD_CONTEXT:
                        event.addTime(1);
                        break;
                    case READ_DISK:
                        disk.read(event);
                        break;
                    case WRITE_DISK:
                        disk.write(event);
                        break;
                    case READ_RAM:
                        memory.read(event);
                        break;
                    case WRITE_RAM:
                        memory.write(event);
                        break;
                    case DATA_COMPUTE:
                        event.addTime(1);
                }
            }
        }

    }
}
