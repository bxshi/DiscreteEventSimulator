package edu.nd.bshi.scheduler;

public class Disk {
    private int lastLocation;
    private int seekTime;
    private static final int DISK_TIME = 4100000;

    public Disk(Integer seek_time){
        this.lastLocation = 0;
        this.seekTime = seek_time;
    }

    public void read(Event event){
        int seekDistance = Math.abs(this.lastLocation-event.getBaseAddress());
        if (seekDistance!=0)
            event.addTime(seekTime);
        this.lastLocation = event.getBaseAddress()+event.getAddressLength();
        event.addTime(DISK_TIME);
    }
    public void write(Event event){
        int seekDistance = Math.abs(this.lastLocation-event.getBaseAddress());
        if (seekDistance!=0)
            event.addTime(seekTime);
        this.lastLocation = event.getBaseAddress()+event.getAddressLength();
        event.addTime(DISK_TIME);
    }
}
