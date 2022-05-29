package org.framework.light.clients.redis.client;

class CommandRuntimeEnv {

    private boolean multi;
    private boolean isSynchronized;
    private boolean pipelined;

    public boolean isMulti() {
        return multi;
    }

    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setSynchronized(boolean aSynchronized) {
        isSynchronized = aSynchronized;
    }

    public boolean isPipelined() {
        return pipelined;
    }

    public void setPipelined(boolean pipelined) {
        this.pipelined = pipelined;
    }
}