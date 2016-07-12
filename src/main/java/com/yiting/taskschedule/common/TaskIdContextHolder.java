package com.yiting.taskschedule.common;

/**
 * Created by hzyiting on 2016/7/11.
 */
public class TaskIdContextHolder {
	private static ThreadLocal<String> uuids = new ThreadLocal<String>();

	public static void setTaskId(String uuid) {
		TaskIdContextHolder.uuids.set(uuid);
	}

	public static String getTaskId() {
		return TaskIdContextHolder.uuids.get();
	}
}
