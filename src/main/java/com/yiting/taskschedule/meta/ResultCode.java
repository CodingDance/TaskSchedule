package com.yiting.taskschedule.meta;

/**
 * Created by hzyiting on 2016/7/11.
 */
public class ResultCode {

	public static final int SUCC_RESPONSE_CODE = 1;

	public static final int ERROR_RESPONSE_CODE = 2;

	private int code;
	private String message;

	public ResultCode() {
	}

	public ResultCode(int code, String message) {
		super();
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
