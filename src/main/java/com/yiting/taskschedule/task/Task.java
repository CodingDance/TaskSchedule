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

	public static final int FLAG_RUNSTATUS_START = 0;
	public static final int FLAG_RUNSTATUS_FINISHED = 1;
	public static final int FLAG_RUNSTATUS_INTERRUPTED = 2;
	public static final int FLAG_RUNSTATUS_STASHED = 3;

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
		StatUtils.set(Const.STAT_CMD_SADD, Const.STAT_KEY_TASK_UUID, getUniqueId());
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
				if(export==null){
					return null;
				}
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
					ReflectUtil.invoke(job,"destroy");
				}
				runStatus = FLAG_RUNSTATUS_INTERRUPTED;
				break;
			}
		}

		logger.info("Msg Payload: " + payload + " FromSet: " + fromSet);
		switch (runStatus) {
			case FLAG_RUNSTATUS_FINISHED:
				SchedUtils.clear(payload.getUniqueId());
				logger.info("[Task Finished] " + getUniqueId() + " JobList: " + formatExecedJobList());
				StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_SUM, 1);
				StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_FINISH, 1);
				break;
			case FLAG_RUNSTATUS_INTERRUPTED:
				SchedUtils.clear(payload.getUniqueId());
				logger.info("[Task Interrupt] " + getUniqueId() + " JobList: " + formatExecedJobList());
				logger.info("[Task Interrupt] " + getUniqueId() + " allExport: " + allExport);
				logger.info("[Task Interrupt] " + getUniqueId() + " globalExport: " + globalExport);
				StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_SUM, 1);
				StatUtils.set(Const.STAT_CMD_ACC, Const.STAT_KEY_TASK_INTERRUPT, 1);
				break;
			case FLAG_RUNSTATUS_STASHED:
				logger.info("[Task Stashed, Wait Message Event Trigger] " + getUniqueId() + " JobList: "
						+ formatExecedJobList());
				StatUtils.set(Const.STAT_CMD_SADD, Const.STAT_KEY_TASK_STASHED_SET, getUniqueId());
				break;
			case FLAG_RUNSTATUS_START:
			default:
				logger.info("Attention! No Job Execed!");
				break;
		}

		return null;
	}


	public Map<String, Object> toSimpleMap() {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("name", this.name);
		content.put("cur", this.cur);
		List<String> jobs = new ArrayList<String>();
		for (Callable c : jobList) {
			jobs.add(c.toString());
		}
		content.put("jobs", jobs);
		return content;
	}

	private String formatExecedJobList() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < jobList.size(); i++) {
			Callable job = jobList.get(i);
			if (i > 0) {
				sb.append(" ==> ");
			}
			sb.append(job.getClass().getSimpleName());
		}
		sb.append("]");
		return sb.toString();
	}

	private void handleJobExport(String jobName, Export e) {
		allExport.put(jobName, e);
		if(e!=null) {
			Set<String> keys = e.getKeySet();
			for (String key : keys) {
				globalExport.put(key, e.get(key));
			}
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
