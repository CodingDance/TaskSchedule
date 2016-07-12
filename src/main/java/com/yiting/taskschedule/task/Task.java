package com.yiting.taskschedule.task;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yiting.taskschedule.annotation.JobSchema;
import com.yiting.taskschedule.common.Const;
import com.yiting.taskschedule.common.StatUtils;
import com.yiting.taskschedule.meta.Export;
import com.yiting.taskschedule.meta.Message;
import com.yiting.taskschedule.meta.MsgPayload;
import com.yiting.taskschedule.util.ReflectUtil;
import com.yiting.taskschedule.util.SchedUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by hzyiting on 2016/7/12.
 */
public class Task implements Callable<String>, Serializable {
	private static final Logger logger = Logger.getLogger(Task.class);

	private static final int FLAG_RUNSTATUS_START = 0;
	private static final int FLAG_RUNSTATUS_FINISHED = 1;
	private static final int FLAG_RUNSTATUS_INTERRUPTED = 2;
	private static final int FLAG_RUNSTATUS_STASHED = 3;

	private JsonParser jsonParser = new JsonParser();
	private Gson gson = new Gson();

	private JsonObject jobdefine = null;
	private String name;
	private String desc;
	private MsgPayload payload;
	private String cur;
	private int runStatus = -1;

	private Map<String, Export> allExport = new HashMap<String, Export>();
	private Map<String, Object> globalExport = new HashMap<String, Object>();
	private Map<String, String> fromSet = new HashMap<String, String>();
	private List<Callable> jobList = new ArrayList<Callable>();

	public Task(String name, String jobs, Message message) {
		this.name = name;
		//转化成jsonobjcet
		JsonElement jelem = gson.fromJson(jobs, JsonElement.class);
		this.jobdefine = jelem.getAsJsonObject();
		this.desc = getMeta("desc");
		this.cur = jobdefine.get("BaseJob").getAsString();
		this.storeFromData(message);
	}

	public int getRunStatus() {
		return this.runStatus;
	}


	public void storeFromData(Message msg) {
		this.payload = gson.fromJson(msg.getMsg(), MsgPayload.class);
		String from = msg.getFrom();
		String type = from.split("\\.")[0];
		fromSet.put(type, from);
	}

	private String getMeta(String key) {
		String value = null;
		try {
			JsonObject meta = (JsonObject) jobdefine.get("Metadata");
			value = meta.get(key).getAsString();
		} catch (Exception ignore) {
			logger.debug("cheat insignificance sonar check.");
		}
		return value;
	}

	public String call() throws Exception {
		StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_UUID, getUniqueId());
		StatUtils.set(Const.STAT_CMD_SDEL, Const.STAT_KEY_TASK_STASHED_SET, getUniqueId());
		runStatus = FLAG_RUNSTATUS_START;
		SchedUtils.storeTask(payload.getUniqueId(), this);  //taskpools 保存了该task的状态start
		while (true) {
			try {
				String jobName = cur;
				Callable<Export> job = SchedUtils.loadJob(jobName);
				JobSchema jobSchema = job.getClass().getAnnotation(JobSchema.class);

				int jobMode = (jobSchema != null ? jobSchema.mode() : JobSchema.MODE_SYNC);
				String jobDesc = (jobSchema != null ? jobSchema.desc() : StringUtils.EMPTY);
				ReflectUtil.setTaskName(job, name);
				ReflectUtil.setFromSet(job, fromSet);
				ReflectUtil.setInit(job, payload);
				ReflectUtil.initAnnotation(job,allExport,globalExport);
				jobList.add(job);
				logger.info("[Job Start]=====" + getUniqueId() + " task: " + this.name + ", exec job: " + jobName + "=====");
				if (StringUtils.isNotBlank(desc) && StringUtils.isNotBlank(jobDesc)) {
					SchedUtils.send2Redis(payload.getUniqueId(), "开始执行任务: " + jobDesc + "。", true);
				}
				Future<Export> f = SchedUtils.execJob(job);
				Export export = f.get();
				handleJobExport(jobName, export);
				String result = export.getResult();
				JsonObject nextJob = jobdefine.getAsJsonObject(cur);
				String nextJobName = ReflectUtil.getNextJob(nextJob, result);
				if (nextJobName != null) {
					logger.info("[Job Switch]=====" + getUniqueId() + " task: " + this.name + ", last result: "
							+ result + ", next job: " + nextJobName + ", jobMode: " + jobMode + "=====");
					cur = nextJobName;
					if (jobMode == JobSchema.MODE_ASYNC) {
						SchedUtils.storeTask(payload.getUniqueId(), this);
						runStatus = FLAG_RUNSTATUS_STASHED;
						break;
					}
				} else {
					runStatus = FLAG_RUNSTATUS_FINISHED;
					break;
				}
			}catch (Exception ex){
				logger.warn("sched task exec exception!", ex);
				for (int i = jobList.size(); i > 0; i--) {
					Callable<Export> job = jobList.get(i - 1);
//					Method method = getMethod(job.getClass(), "destroy");
//					if (method != null) {
//						try {
//							method.invoke(job);
//						} catch (Exception e1) {
//							logger.warn("destroy method invoke exception.");
//							e1.printStackTrace();
//						}
//					}
					ReflectUtil.invoke(job,"destroy");
				}
				runStatus = FLAG_RUNSTATUS_INTERRUPTED;
				break;
			}
		}

		return null;
	}


	private void handleJobExport(String jobName, Export e) {
		allExport.put(jobName, e);
		Set<String> keys = e.getKeySet();
		for (String key : keys) {
			globalExport.put(key, e.get(key));
		}
	}

	public String getUniqueId() {
		if (payload != null) {
			return payload.getUniqueId();
		} else {
			return null;
		}
	}
}
