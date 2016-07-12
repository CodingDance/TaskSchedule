package com.yiting.taskschedule.service;

/**
 * Created by hzyiting on 2016/5/26.
 */
public interface ITaskQueueService {
    /**
     * 对队列中的任务进行处理，
     */
    void process();

    /**
     * 对队列任务惊醒监控
     */
    void monitor();
}
