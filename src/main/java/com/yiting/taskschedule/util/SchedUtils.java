package com.yiting.taskschedule.util;

import com.google.common.cache.*;
import com.google.gson.Gson;
import com.yiting.taskschedule.common.Const;
import com.yiting.taskschedule.common.StatUtils;
import com.yiting.taskschedule.common.StringTemplateMerger;
import com.yiting.taskschedule.meta.Export;
import com.yiting.taskschedule.meta.Message;
import com.yiting.taskschedule.meta.MsgPayload;
import com.yiting.taskschedule.meta.QueueingTaskMessage;
import com.yiting.taskschedule.service.IRedisService;
import com.yiting.taskschedule.service.ITaskQueueService;
import com.yiting.taskschedule.task.Task;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by hzyiting on 2016/7/11.
 */
@Component
public class SchedUtils {
	private static Logger logger = Logger.getLogger(SchedUtils.class);
	private static Gson gson=new Gson();

	private static ExecutorService jobExecutorService = new DefaultThreadPoolExecutor(128);
	private static ExecutorService taskExecutorService = new DefaultThreadPoolExecutor(128);
	private static ExecutorService logExecutorService = new DefaultThreadPoolExecutor(1);

	private static SchedUtils schedUtils = null;
	private static final String TASK_DIR_PREFIX = "task/";
	private static final long SHUTDOWN_TIMEOUT = 60L;

	private static LinkedBlockingQueue<QueueingTaskMessage> queueingTaskMessageQueue = new LinkedBlockingQueue<QueueingTaskMessage>();
	private static Cache<String, Task> taskPools;
	private static Cache<String, Object> queueKeyCache = null;
	private static Cache<String, String> uuid2QueueKeyCache;
	private static volatile boolean preShutDown=false;
	private static volatile boolean ifCouldShutDown=false;

	@Autowired
	private String jobPackage=null;

	@Autowired
	private IRedisService redisService;

	@Autowired
	private ITaskQueueService taskQueueService;

	@PostConstruct
	private void init(){
		schedUtils = this;
		taskPools = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).removalListener(new RemovalListener<String, Task>() {
			@Override
			public void onRemoval(RemovalNotification<String, Task> notification) {
				String taskId = notification.getKey();
				StatUtils.set(Const.STAT_CMD_SDEL, Const.STAT_KEY_TASK_UUID, taskId);
				StatUtils.set(Const.STAT_CMD_DEL, taskId, null);
				StatUtils.set(Const.STAT_CMD_SDEL, Const.STAT_KEY_TASK_STASHED_SET, taskId);
				if (notification.getCause() == RemovalCause.EXPIRED) {
					logger.warn("[taskPools]task EXPIRED uuid: " + taskId + " = " + notification.getValue() + ", cause: " + notification.getCause());
				} else {
					logger.debug("[taskPools]task uuid: " + taskId + " = " + notification.getValue() + ", cause: " + notification.getCause());
				}
			}
		}).build(); // 1天过期。
		queueKeyCache = CacheBuilder.newBuilder().expireAfterWrite(Const.WAITING_TIMEOUT, TimeUnit.MILLISECONDS)
				.removalListener(new RemovalListener<String, Object>() {
					@Override
					public void onRemoval(RemovalNotification<String, Object> notification) {
						if (notification.getCause() != RemovalCause.EXPLICIT) {
							logger.info("[queueKeyCache]queue key: " + notification.getKey() + ", cause: " + notification.getCause());
						} else { // explicit due to invalid key.
							logger.debug("[queueKeyCache]queue key: " + notification.getKey() + ", cause: " + notification.getCause());
						}
					}
				}).build(); // 过期时间同排队等待时间。
		uuid2QueueKeyCache = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).removalListener(new RemovalListener<String, String>() {
			@Override
			public void onRemoval(RemovalNotification<String, String> notification) {
				if (notification.getCause() != RemovalCause.EXPLICIT) {
					logger.info("[uuid2QueueKeyCache]uuid2Queue key: " + notification.getKey() + " = " + notification.getValue() + ", cause: "
							+ notification.getCause());
				} else { // explicit due to invalid key.
					logger.debug("[uuid2QueueKeyCache]uuid2Queue key: " + notification.getKey() + " = " + notification.getValue() + ", cause: "
							+ notification.getCause());
				}
			}
		}).build(); // 1天过期。
		taskQueueService.process(); // start queueing checker async.

	}

	public void execTask(Callable<String> task) {
		taskExecutorService.submit(task);
	}

	public static Future<Export> execJob(Callable<Export> job) {
		return jobExecutorService.submit(job);
	}

	public static void shutdown() {
		try {
			jobExecutorService.shutdown();
			taskExecutorService.shutdown();
			logExecutorService.shutdown();
			jobExecutorService.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
			taskExecutorService.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
			logExecutorService.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn("sched executor service shutdown error.", e);
		}
	}

	public static Callable<Export> loadJob(String name) throws Exception {
		return (Callable<Export>) Class.forName(schedUtils.jobPackage + "." + name).newInstance();
	}

	public static Task fetchTask(String uniqueId) {
		synchronized (uniqueId.intern()) {
			Task task = taskPools.getIfPresent(uniqueId);
			logger.info("[Sched Platform]fetch Task: uniqueId = " + uniqueId + ", Task = " + task);
			return task;
		}
	}

	public static void storeTask(String uniqueId, Task task) {
		synchronized (uniqueId.intern()) {
			logger.info("[Sched Platform]store Task: uniqueId = " + uniqueId + ", Task = " + task);
			taskPools.put(uniqueId, task);
		}
	}

	public static void clearTask(String uniqueId) {
		synchronized (uniqueId.intern()) {
			logger.info("[Sched Platform]clear Task: uniqueId = " + uniqueId);
			taskPools.invalidate(uniqueId);
		}
	}

	/**
	 * facade ,暴露给外部的接口
	 * @param message
	 */
	public static void execS(Message message) {
		schedUtils.exec(message);
	}

	public static void commit(Message message){
		String uuid=getUniqueId(message);
		QueueingTaskMessage queueingTaskMessage=new QueueingTaskMessage(uuid,message);
		StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_QUEUESIZE, 1);
		offerQueueingTask(queueingTaskMessage);
	}

	public static boolean offerQueueingTask(QueueingTaskMessage queueingTaskMessage) {
		boolean ret=queueingTaskMessageQueue.offer(queueingTaskMessage);
		if(!ret){
			logger.warn("the uuid queueingTaskMessageQueue is full! qt = " + queueingTaskMessageQueue);
			return false;
		}
		return true;
	}

	public static QueueingTaskMessage pollQueueingTaskMessage(){
		try {
			return queueingTaskMessageQueue.take();
		} catch (InterruptedException e) {
			logger.warn("retrieve task queue error.", e);
		}
		return null;
	}

	public static void monitorQueueingTaskQueue() {
		Object[] elements = queueingTaskMessageQueue.toArray();
		for (Object element : elements) {
			QueueingTaskMessage t = (QueueingTaskMessage) element;
			logger.info("[QueueingTask Queue Monitor]" + t);
		}
	}

	/**
	 * 判断当前的queueKey是否已存在，已存在表示该操作对象正在执行，需排队。
	 *
	 * @param queueKey
	 */
	public static boolean isQueueKeyConflicted(String queueKey) {
		synchronized (queueKey.intern()) {
			Object ret = queueKeyCache.getIfPresent(queueKey);
			if (ret == null) {
				return schedUtils.redisService.hasKey(key(Const.REDIS_PREFIX_QUEUEKEY) + queueKey);
			} else {
				return true;
			}
		}
	}

	public static void storeQueueKey(String uuid, String queueKey) {
		if (StringUtils.isNotBlank(queueKey)) {
			synchronized (queueKey.intern()) {
				queueKeyCache.put(queueKey, true);
				schedUtils.redisService.setValue(key(Const.REDIS_PREFIX_QUEUEKEY) + queueKey, true, Const.WAITING_TIMEOUT);
				uuid2QueueKeyCache.put(uuid, queueKey);
				schedUtils.redisService.setValue(key(Const.REDIS_PREFIX_UUID2QUEUEKEY) + uuid, queueKey, Const.WAITING_TIMEOUT);
			}
		}
	}

	public static void evictQueueKey(String uuid) {
		String queueKey = uuid2QueueKeyCache.getIfPresent(uuid);
		if (StringUtils.isNotBlank(queueKey)) {
			synchronized (queueKey.intern()) {
				queueKeyCache.invalidate(queueKey);
				schedUtils.redisService.deleteKey(key(Const.REDIS_PREFIX_QUEUEKEY) + queueKey);
				uuid2QueueKeyCache.invalidate(uuid);
				schedUtils.redisService.deleteKey(key(Const.REDIS_PREFIX_UUID2QUEUEKEY) + uuid);
			}
		} else {
			uuid2QueueKeyCache.invalidate(uuid);
			schedUtils.redisService.deleteKey(key(Const.REDIS_PREFIX_UUID2QUEUEKEY) + uuid);
		}
	}

	public static boolean getPreShutdown() {
		return preShutDown;
	}

	public static boolean couldShutdown() {
		return ifCouldShutDown;
	}

	public static void clear(String uuid) {
		clearTask(uuid);
		evictQueueKey(uuid);
	}


	private void exec(Message message){
		String uniqueId=getUniqueId(message);
		Task task=fetchTask(uniqueId);
		if(task!=null){
			task.storeFromData(message);
			execTask(task);
		}else{
			String taskName=getTaskName(message);
			String jobs=getTaskSchema(taskName);
			execTask(new Task(taskName,jobs,message));
		}
	}

	private String getTaskSchema(String taskName) {
		return StringTemplateMerger.mergeTemplate(new HashMap<String, Object>(), TASK_DIR_PREFIX + taskName);
	}

	private static String getTaskName(Message m) {
		MsgPayload payload = gson.fromJson(m.getMsg(), MsgPayload.class);
		return payload.getCmd();
	}

	public static int getQueueingTaskQueueSize() {
		return queueingTaskMessageQueue.size();
	}

	private static String getUniqueId(Message m) {
		MsgPayload payload = gson.fromJson(m.getMsg(), MsgPayload.class);
		return payload.getUniqueId();
	}


	public static void send2Redis(final String uuid, final String msg, final boolean hint) {
		logExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				logger.info("write to redis uuid:"+uuid+",msg:"+msg);
//				try {
//					IRedisService redisService = (IRedisService) SpringUtils.getBean("redisService");
//					if (hint) {
//					} else {
//					}
//				} catch (Exception e) {
//					logger.info("publish to redis error. reason:" + e.getMessage());
//				}
			}
		});
	}

	private static String key(String key) {
		return key + schedUtils.jobPackage + "_";
	}

}
