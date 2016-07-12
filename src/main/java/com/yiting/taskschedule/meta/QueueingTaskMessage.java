package com.yiting.taskschedule.meta;

import java.io.Serializable;

/**
 * Created by hzyiting on 2016/5/26.
 * queueingtask 表示的是入队的指令消息
 */

public class QueueingTaskMessage implements Serializable {
    private String uuid;
    private long schedTime;
    private long commitTime;
    Message msg = null;

    public QueueingTaskMessage(String uuid, Message msg) {
        super();
        this.uuid = uuid;
        this.msg = msg;
        this.schedTime = 0L;
        this.commitTime = System.currentTimeMillis();
    }

    public QueueingTaskMessage(String uuid, long schedTime, long commitTime) {
        this.uuid = uuid;
        this.schedTime = schedTime;
        this.commitTime = commitTime;
    }

    public QueueingTaskMessage(String uuid) {
        this.uuid = uuid;
    }

    public long getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(long commitTime) {
        this.commitTime = commitTime;
    }

    public long getSchedTime() {
        return schedTime;
    }

    public void setSchedTime(long schedTime) {
        this.schedTime = schedTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "QueueingTask{" +
                "uuid='" + uuid + '\'' +
                ", schedTime=" + schedTime +
                ", commitTime=" + commitTime +
                ", msg=" + msg +
                '}';
    }
}
