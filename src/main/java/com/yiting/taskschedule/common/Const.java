/**
 * (C) Copyright Netease.com, Inc. 2015. All Rights Reserved.
 */
package com.yiting.taskschedule.common;


/**
 */
public class Const {

    public static final String TASK_DIR_PREFIX = "task/";

    public static final long ES_SHUTDOWN_TIMEOUT = 60L; // s

    public static final String REDIS_PREFIX_QUEUEKEY = "QUEUEKEY_";

    public static final String REDIS_PREFIX_UUID2QUEUEKEY = "UUID2QUEUEKEY_";

    public static final long RESCHED_WAITING_TIME = 10 * 1000L; // 排队任务等待重新调度的时间。

    public static final long WAITING_TIMEOUT = 5 * 60 * 1000L; // 排队对象超时时间。
    public static final long WAITING_TASK = 1; // 任务在池中缓存的最长时间。1Day。

    public static final int SCHED_RESULT_DENY = 0; // 不允许执行，需排队。
    public static final int SCHED_RESULT_ALLOW = 1; // 允许执行
    public static final int SCHED_RESULT_TOOFREQ = 2; // 调度太频繁，继续排队。
    
    public static final int STAT_CMD_SET = 0; // 设置为当前值。
    public static final int STAT_CMD_DEL = 1; // 设置为当前值。
    public static final int STAT_CMD_ACC = 2; // 累加当前值。
    public static final int STAT_CMD_DSC = 3; // 递减当前值。
    public static final int STAT_CMD_SADD = 4; // 向Set中插值。
    public static final int STAT_CMD_SDEL = 5; // 从Set中删值。
    public static final int STAT_CMD_LADD = 6; // 向List中插值。
    public static final int STAT_CMD_LDEL = 7; // 从List中删值。

    public static final String STAT_KEY_TASK_SUM = "tasksum"; // 历史任务数量。
    public static final String STAT_KEY_TASK_FINISH = "taskfinish"; // 历史正常结束任务数量。
    public static final String STAT_KEY_TASK_INTERRUPT = "taskinterrupt"; // 历史异常结束任务数量。
    public static final String STAT_KEY_TASK_CURRENT = "taskcurrent"; // 当前任务数量。
    public static final String STAT_KEY_TASK_UUID = "tasks"; // 当前任务。
    public static final String STAT_KEY_TASK_QUEUESIZE = "taskqueuesize"; // 当前任务队列容量。
    public static final String STAT_KEY_TASK_STASHED_SET = "taskstashset"; // 当前stash的任务集合。
}
