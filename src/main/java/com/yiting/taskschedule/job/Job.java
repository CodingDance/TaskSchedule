package com.yiting.taskschedule.job;

import com.yiting.taskschedule.meta.Export;
import com.yiting.taskschedule.meta.MsgPayload;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by hzyiting on 2016/7/11.
 */
public abstract class Job implements Callable<Export> {
	protected String taskName;
	protected MsgPayload init;
	protected Map<String,String> fromSet;
	protected Export export=new Export();
	abstract public Export call();

	public void destory(){
	}

	public String getUniqueId(){
		if(init!=null){
			return init.getUniqueId();
		}
		return null;
	}

}
