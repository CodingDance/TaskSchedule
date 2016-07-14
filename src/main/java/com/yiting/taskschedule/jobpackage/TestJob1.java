package com.yiting.taskschedule.jobpackage;

import com.yiting.taskschedule.job.MessageJob;
import com.yiting.taskschedule.meta.Export;

/**
 * Created by hzyiting on 2016/7/14.
 */
public class TestJob1 extends MessageJob{
	@Override
	public Export call() {
		System.out.println("testjob1");
		return export;
	}
}
