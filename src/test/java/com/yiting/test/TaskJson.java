package com.yiting.test;

import com.yiting.taskschedule.task.Task;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by hzyiting on 2016/7/12.
 */
public class TaskJson extends TestCase{

	@Test
	public void testGson(){
		String taskDefine="{\"Version\": \"v1\", \"Metadata\": { \"desc\": \"保存容器镜像\"}, \"BaseJob\": \"ResolveParamsJob\", \"ResolveParamsJob\" : { \"default\" : \"CommitImageJob\"}, \"CommitImageJob\": { \"default\" : \"SendMsgToCtrlJob\"},\"SendMsgToCtrlJob\": {}}";
		Task task=new Task("task1",taskDefine,null);

	}

}
