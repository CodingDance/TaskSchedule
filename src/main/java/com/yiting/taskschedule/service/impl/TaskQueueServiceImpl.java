package com.yiting.taskschedule.service.impl;

import com.yiting.taskschedule.common.Const;
import com.yiting.taskschedule.common.StatUtils;
import com.yiting.taskschedule.meta.Message;
import com.yiting.taskschedule.meta.QueueingTaskMessage;
import com.yiting.taskschedule.service.ITaskQueueService;
import com.yiting.taskschedule.task.Task;
import com.yiting.taskschedule.util.SchedUtils;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
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
			try {
				QueueingTaskMessage queueingTaskMessage=SchedUtils.pollQueueingTaskMessage(); //会阻塞
				currentQueueingTaskMessage=queueingTaskMessage;
				if(queueingTaskMessage==null){
					continue;
				}
				int checkResult=sched(queueingTaskMessage);
				if(checkResult== Const.SCHED_RESULT_ALLOW){
					String uuid=queueingTaskMessage.getUuid();
					Message msg=queueingTaskMessage.getMsg();
					String queueKey=getQueueKey(queueingTaskMessage);
					SchedUtils.storeQueueKey(uuid,queueKey);
					SchedUtils.execS(msg);
					StatUtils.set(Const.STAT_CMD_DSC,Const.STAT_KEY_TASK_QUEUESIZE,1);
				}else{
					if(checkResult!=Const.SCHED_RESULT_TOOFREQ){
						queueingTaskMessage.setSchedTime(System.currentTimeMillis());
					}
					SchedUtils.offerQueueingTask(queueingTaskMessage);
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}catch (Exception e){
				logger.warn("process task error!", e);
			}finally {
				currentQueueingTaskMessage=null;
			}
		}
	}

	private int sched(QueueingTaskMessage queueingTaskMessage) {
		if(System.currentTimeMillis()-queueingTaskMessage.getCommitTime()>Const.WAITING_TIMEOUT){
			logger.warn("任务排队超时，允许执行。qt:" + queueingTaskMessage);
			return Const.SCHED_RESULT_ALLOW;
		}
		long schedTime=queueingTaskMessage.getSchedTime();
		if(System.currentTimeMillis()-schedTime<Const.RESCHED_WAITING_TIME){
			return Const.SCHED_RESULT_TOOFREQ;
		}
		String uuid=queueingTaskMessage.getUuid();
		Task task=SchedUtils.fetchTask(uuid);
		if(task!=null){
			int runStatus=task.getRunStatus();
			if(runStatus==Task.FLAG_RUNSTATUS_START){
				logger.warn("当前uuid的任务还在执行中，需排队: " + task);
				return Const.SCHED_RESULT_DENY;
			} else if (runStatus == Task.FLAG_RUNSTATUS_STASHED) {
				logger.info("此任务允许执行。");
				return Const.SCHED_RESULT_ALLOW;
			}else {
				logger.warn("出现了未考虑到的情况，需要人工介入排查一下!!!");
				return Const.SCHED_RESULT_ALLOW;
			}
		}
		return Const.SCHED_RESULT_ALLOW;
	}

	private String getQueueKey(QueueingTaskMessage msg) {
		return msg.getUuid();
	}

	/**
	 * 对排队的任务进行监控。 cron format: Seconds Minutes Hours Day-of-Month Month Day-of-Week
	 */
	@Scheduled(cron = "0 */2 * * * *")
	public void monitor() {
		logger.info("[Queue Monitor]current queue size: " + SchedUtils.getQueueingTaskQueueSize());
		logger.info("[Queue Monitor]current QueueingTask: " + currentQueueingTaskMessage);
		SchedUtils.monitorQueueingTaskQueue();
	}
}
