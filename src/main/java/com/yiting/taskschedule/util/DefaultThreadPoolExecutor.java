package com.yiting.taskschedule.util;


import com.yiting.taskschedule.common.TaskIdContextHolder;
import com.yiting.taskschedule.job.Job;
import com.yiting.taskschedule.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.*;

/**
 * Created by hzyiting on 2016/7/11.
 */
public class DefaultThreadPoolExecutor extends ThreadPoolExecutor {
	private static Logger logger = LoggerFactory.getLogger(DefaultThreadPoolExecutor.class);

	public DefaultThreadPoolExecutor(int poolSize) {
		super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

	protected void beforeExecute(Thread t, Runnable runnable) {
		super.beforeExecute(t, runnable);
		String uniqueId = null;
		if (runnable instanceof FutureTask<?>) {
			try {
				FutureTask futureTask = (FutureTask) runnable;
				Class<?> futureTaskClz = futureTask.getClass();
				Field callableField = futureTaskClz.getField("callable");
				callableField.setAccessible(true);
				Callable callable = (Callable) callableField.get(futureTask);
				if (callable instanceof Job) {
					Job job = (Job) callable;
					uniqueId = job.getUniqueId();
				} else if (callable instanceof Task) {
					Task task = (Task) callable;
					uniqueId = task.getUniqueId();
				}

			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		TaskIdContextHolder.setTaskId(uniqueId);
		logger.info("Thread " + t.getName() + " set uuid " + uniqueId);

	}

	protected void afterExecute(Runnable r, Throwable t) {
		TaskIdContextHolder.setTaskId(null); // clear
		logger.info("Thread " + Thread.currentThread().getName() + " clear uuid.");
		super.afterExecute(r, t);
	}

}
