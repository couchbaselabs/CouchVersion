package com.github.couchversion.dao;

public class CouchVersionLock {

    public CouchVersionLock(){}

    public CouchVersionLock(Long lockTimestamp) {
        this.lockTimestamp = lockTimestamp;
    }

    private Long lockTimestamp;
    public Long getLockTimestamp() {
        return lockTimestamp;
    }

    public void setLockTimestamp(Long lockTimestamp) {
        this.lockTimestamp = lockTimestamp;
    }
}
