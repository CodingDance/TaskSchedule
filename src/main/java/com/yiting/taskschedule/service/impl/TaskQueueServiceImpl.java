package com.yiting.taskschedule.service.impl;

import com.yiting.taskschedule.meta.QueueingTaskMessage;
import com.yiting.taskschedule.service.ITaskQueueService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Created by hzyiting on 2016/7/11.
 */
@Component
public class TaskQueueServiceImpl implements ITaskQueueService {
	private static final Logger logger=Logger.getLogger(TaskQueueServiceImpl.class);
	private volatile QueueingTaskMessage currentQueueingTaskMessage =null;

	public void process() {
		while (true){

		}
	}

	public void monitor() {

	}
}
