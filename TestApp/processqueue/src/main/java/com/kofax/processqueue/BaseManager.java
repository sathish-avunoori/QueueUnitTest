package com.kofax.processqueue;


public class BaseManager {

public enum State{
    STARTED,
    STOPPED
}

    public State status = State.STOPPED;

    public State getStatus() {
        return status;
    }

    public void setStatus(State status) {
        this.status = status;
    }
}
