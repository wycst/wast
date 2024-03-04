package io.github.wycst.wast.jdbc.executer;

class CacheableEntity {

    CacheableEntity(Object entity) {
        this.entity = entity;
    }

    private Object entity;
    private long lastHitAt;

    Object get() {
        lastHitAt = System.currentTimeMillis();
        return getEntity();
    }

    Object getEntity() {
        return entity;
    }

    void set(Object entity) {
        lastHitAt = System.currentTimeMillis();
        this.entity = entity;
    }

    long getLastHitAt() {
        return lastHitAt;
    }

}
