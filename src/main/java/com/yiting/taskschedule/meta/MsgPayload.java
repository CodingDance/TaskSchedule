package com.yiting.taskschedule.meta;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by hzyiting on 2016/7/11.
 */
public class MsgPayload {

	private static final long serialVersionUID = 1L;

	private String uniqueId;
	private Map<String, Object> content;
	private String cmd;

	public MsgPayload() {
	}

	/**
	 * @param uniqueId
	 * @param content
	 * @param cmd
	 */
	public MsgPayload(String uniqueId, Map<String, Object> content, String cmd) {
		super();
		this.uniqueId = uniqueId;
		this.content = content;
		this.cmd = cmd;
	}

	/**
	 * @return the uniqueId
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	/**
	 * @return the content
	 */
	public Map<String, Object> getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

	/**
	 * @return the cmd
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public Long getContentLong(String key) {
		String sVal = String.valueOf(content.get(key));
		return Long.valueOf(sVal);
	}

	public String getContentString(String key) {
		if (content.get(key) != null) {
			return String.valueOf(content.get(key));
		}
		return null;
	}

	public Integer getContentInteger(String key) {
		String sVal = String.valueOf(content.get(key));
		return Integer.valueOf(sVal);
	}

	@Override
	public String toString() {
		return "MsgPayload{" +
				"uniqueId='" + uniqueId + '\'' +
				", content=" + content +
				", cmd='" + cmd + '\'' +
				'}';
	}
}
