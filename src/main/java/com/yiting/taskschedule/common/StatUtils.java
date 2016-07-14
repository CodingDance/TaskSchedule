package com.yiting.taskschedule.common;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hzyiting on 2016/7/11.
 */
public class StatUtils {
	private static final Logger logger = LoggerFactory.getLogger(StatUtils.class);

	private static final Map<String, Object> stats = new ConcurrentHashMap<String, Object>();

	private static StatUtils statUtils = null;

	@Autowired
	private String jobPackage = null;

	@PostConstruct
	private void init() {
		statUtils = this;
	}

	public static void set(int cmd, String key, Object value) {
		try {
			synchronized (("schedstat" + key).intern()) {
				Object origin = stats.get(key);
				switch (cmd) {
					case Const.STAT_CMD_SET:
						stats.put(key, value);
						break;
					case Const.STAT_CMD_DEL:
						stats.remove(key);
						break;
					case Const.STAT_CMD_ACC:
						if (origin == null) {
							origin = 0.0D;
						}
						Double originValueAcc = Double.valueOf(String.valueOf(origin));
						stats.put(key, originValueAcc + Double.valueOf(String.valueOf(value)));
						break;
					case Const.STAT_CMD_DSC:
						if (origin != null) {
							Double originValueDsc = Double.valueOf(String.valueOf(origin));
							stats.put(key, originValueDsc - Double.valueOf(String.valueOf(value)));
						}
						break;
					case Const.STAT_CMD_SADD:
						Set<String> originSetAdd = (Set<String>) origin;
						if (originSetAdd == null) {
							originSetAdd = Collections.synchronizedSet(new HashSet<String>());
							stats.put(key, originSetAdd);
						}
						originSetAdd.add(String.valueOf(value));
						break;
					case Const.STAT_CMD_SDEL:
						Set<String> originSetDel = (Set<String>) origin;
						if (originSetDel != null) {
							originSetDel.remove(value);
						}
						break;
					case Const.STAT_CMD_LADD:
					case Const.STAT_CMD_LDEL:
					default:
						break;
				}
			}
		} catch (Exception e) {
			logger.warn("handle stat data storage error.", e);
		}
	}

	public static String get() {
		Gson gson=new Gson();
		return gson.toJson(stats);
	}
}
