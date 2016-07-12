package com.yiting.taskschedule.job;

import com.yiting.taskschedule.meta.ResultCode;
import com.yiting.taskschedule.util.SchedUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by hzyiting on 2016/7/11.
 */
public abstract class MessageJob extends Job {
	private static final Logger logger = LoggerFactory.getLogger(MessageJob.class);

	protected void sendSuccMessage(String to, Map<String, Object> content) {
		if (content == null) {
			content = new HashMap<String, Object>();
		}
		content.put("code", ResultCode.SUCC_RESPONSE_CODE);
		sendMessage(to, content);
	}

	protected void sendSuccMessage(String to, String cmd, Map<String, Object> content) {
		if (content == null) {
			content = new HashMap<String, Object>();
		}
		content.put("code", ResultCode.SUCC_RESPONSE_CODE);
		sendMessage(to, cmd, content);
	}

	protected void sendFailMessage(String to, ResultCode rc, Map<String, Object> content) {
		if (content == null) {
			content = new HashMap<String, Object>();
		}
		content.put("code", rc.getCode());
		content.put("message", rc.getMessage());
		sendMessage(to, content);
	}

	private void sendMessage(String to, Map<String, Object> content) {
		sendMessage(to, init.getCmd(), content);
	}

	private void sendMessage(String to, String cmd, Map<String, Object> content) {
		JSONObject resultMsg = new JSONObject();
		resultMsg.element("uniqueId", init.getUniqueId());
		resultMsg.element("cmd", cmd);
		resultMsg.element("content", content);
		// 优先发送给原消息的生产者，从fromSet中取。
		String from = fromSet.get(to);
		if (StringUtils.isBlank(from)) {
			MQ.sendMessage(to, resultMsg.toString());
		} else {
			MQ.sendMessage(fromSet.get(to), resultMsg.toString());
		}
	}

	protected void send2Redis(String msg, boolean hint) {
		logger.info("uuid: " + init.getUniqueId() + " , msg:" + msg);
		SchedUtils.send2Redis(init.getUniqueId(), msg, hint);
	}

	protected void send2Redis(String format, Object... args) {
		try {
			String msg = String.format(format, args);
			send2Redis(msg);
		} catch (Exception e) {
			logger.info("format string error.", e);
		}
	}
}
