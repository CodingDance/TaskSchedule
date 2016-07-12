package com.yiting.taskschedule.service;

import java.util.List;

/**
 * Created by hzyiting on 2016/7/11.
 */
public interface IRedisService {

	public void publish(Object message);

	public Object getValue(String key);

	public void setValue(String key, Object value);

	public void setValue(String key, Object value, long expire);

	public boolean setValueIfAbsent(String key, Object value) throws Exception;

	public void deleteKey(String key);

	public boolean hasKey(String key);

	public boolean expire(String key, long expire);

	public List<String> getFromList(String key, int start, int end);

	public long incr(String key, int step, long expire);

	public long getTtl(String key);
}
