package com.yiting.taskschedule.util;

import com.google.gson.JsonObject;
import com.yiting.taskschedule.annotation.Dependency;
import com.yiting.taskschedule.meta.Export;
import com.yiting.taskschedule.meta.MsgPayload;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by hzyiting on 2016/7/12.
 */
public class ReflectUtil {
	private static final Logger logger = Logger.getLogger(ReflectUtil.class);
	public static final String TASK_NAME = "taskName";
	public static final String TASK_INIT = "init";
	public static final String TASK_FROMSET = "fromSet";
	
	public static void setTaskName(Callable<Export> job, String taskName) {
		try {
			Field field = getField(job.getClass(), TASK_NAME);
			assert field != null;
			field.setAccessible(true);
			field.set(job, taskName);
		} catch (IllegalAccessException e) {
			logger.warn("handle taskName object error.", e);
		}
	}
	
	public static void setInit(Callable<Export> job, MsgPayload msg) {
		try {
			Field field = getField(job.getClass(), TASK_INIT);
			assert field != null;
			field.setAccessible(true);
			field.set(job, msg);
		} catch (Exception e) {
			logger.warn("handle init object error.", e);
		}
	}

	public static void setFromSet(Callable<Export> job, Map<String, String> froms) {
		try {
			Field field = getField(job.getClass(), TASK_FROMSET);
			assert field != null;
			field.setAccessible(true);
			field.set(job, froms);
		} catch (Exception e) {
			logger.warn("handle fromset object error.", e);
		}
	}
	
	public static void initAnnotation(Callable<Export> job,Map<String, Export> allExport,Map<String, Object> globalExport) {
		Field[] fields = job.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.isAnnotationPresent(Dependency.class)) {
				initDependency(job, f,allExport,globalExport);
			} else if (f.isAnnotationPresent(Value.class)) {
				initValue(job, f);
			} else if (f.isAnnotationPresent(Autowired.class)) {
				initAutowired(job, f);
			} else if (f.isAnnotationPresent(Resource.class)) {
				initResource(job, f);
			}
		}
	}
	
	public static void initDependency(Callable<Export> job, Field field,Map<String, Export> allExport,Map<String, Object> globalExport) {
		Dependency dependency = field.getAnnotation(Dependency.class);
		Class parentJob = dependency.job();
		String key = dependency.key();
		field.setAccessible(true);
		try {
			Object value = null;
			if (parentJob.getName().equals("java.lang.Object")) {
				value = globalExport.get(key);
			} else {
				String parentJobName = parentJob.getSimpleName();
				value = allExport.get(parentJobName).get(key);
			}
			if (value != null) {
				if (value instanceof Integer || value instanceof Long || value instanceof String) { // 避免Integer、Long、int、long互相转换失败。
					String sValue = String.valueOf(value);
					field.set(job, ConvertUtils.convert(sValue, field.getType()));
				} else {
					field.set(job, value);
				}
			}
		} catch (IllegalAccessException e) {
			logger.warn("handle @Dependency annotation error.", e);
		}
		
	}
	
	public static void initValue(Callable<Export> job, Field f) {
		Value v = f.getAnnotation(Value.class);
		String value = v.value();
		f.setAccessible(true);
		try {
			String resolveValue = SpringUtils.getPlaceHolderValue(value);
			f.set(job, ConvertUtils.convert(resolveValue, f.getType()));
		} catch (Exception e) {
			logger.warn("handle @Value annotation error.", e);
		}
	}
	
	public static void initAutowired(Callable<Export> job, Field f) {
		try {
			f.setAccessible(true);
			f.set(job, SpringUtils.getBeanByType(f.getType()));
		} catch (Exception e) {
			logger.warn("handle @Autowired annotation error.", e);
		}
	}
	
	public static void initResource(Callable<Export> job, Field f) {
		f.setAccessible(true);
		Resource resource = f.getAnnotation(Resource.class);
		String name = resource.name();
		Class cls = resource.type();
		Object o = null;
		if (!StringUtils.isBlank(name) && !cls.getName().equals("java.lang.Object")) {
			o = SpringUtils.getBean(name, cls);
		} else if (!StringUtils.isBlank(name)) {
			o = SpringUtils.getBean(name);
		} else if (!cls.getName().equals("java.lang.Object")) {
			o = SpringUtils.getBeanByType(cls);
		} else {
			o = SpringUtils.getBean(f.getName());
		}
		try {
			f.set(job, o);
		} catch (Exception e) {
			logger.warn("handle @Resource annotation error.", e);
		}
	}

	public static Field getField(Class cls, String fieldName) {
		try {
			Field field = cls.getDeclaredField(fieldName);
			return field;
		} catch (Exception e) {
			Class parent = cls.getSuperclass();
			if (parent == null) {
				return null;
			}
			return getField(parent, fieldName);
		}
	}

	public static Method getMethod(Class cls, String methodName, Class... params) {
		try {
			return cls.getDeclaredMethod(methodName, params);
		} catch (Exception e) {
			Class parent = cls.getSuperclass();
			if (parent == null) {
				return null;
			}
			return getMethod(parent, methodName, params);
		}
	}

	private static final String DEFAULT = "default";

	public static String getNextJob(JsonObject jo, String result) {
		try {
			String value = jo.get(result).getAsString();
			if (value == null) {
				// 如果result未匹配到NextJob，则使用default来尝试一次。
				return jo.get(DEFAULT).getAsString();
			} else {
				return value;
			}
		} catch (Exception ignore) {
			logger.debug(ignore.getMessage());
		}
		return null;
	}


	public static void invoke(Object object,String methodName){
		Method method=getMethod(object.getClass(), methodName);

		try {
			if(method!=null) {
				method.invoke(object, methodName);
			}else {
				logger.error("destroy method invoke exception,no method find:"+ methodName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("destroy method invoke exception:",e);
		}


	}

}
