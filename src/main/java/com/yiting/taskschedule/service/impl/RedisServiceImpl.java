package com.yiting.taskschedule.service.impl;

import com.yiting.taskschedule.service.IRedisService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by hzyiting on 2016/7/11.
 */
@Component
public class RedisServiceImpl implements IRedisService {
	public void publish(Object message) {

	}

	public Object getValue(String key) {
		return null;
	}

	public void setValue(String key, Object value) {

	}

	public void setValue(String key, Object value, long expire) {

	}

	public boolean setValueIfAbsent(String key, Object value) throws Exception {
		return false;
	}

	public void deleteKey(String key) {

	}

	public boolean hasKey(String key) {
		return false;
	}

	public boolean expire(String key, long expire) {
		return false;
	}

	public List<String> getFromList(String key, int start, int end) {
		return null;
	}

	public long incr(String key, int step, long expire) {
		return 0;
	}

	public long getTtl(String key) {
		return 0;
	}
}
