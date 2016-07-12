package com.yiting.future;

import org.apache.log4j.Logger;

import java.util.concurrent.FutureTask;

/**
 * Created by hzyiting on 2016/6/12.
 */
public class MFutureTask extends FutureTask<Object> implements ListenerableFuture<Object> {
	private static final Logger logger=Logger.getLogger(MFutureTask.class);

	private Runnable listener;

	public MFutureTask(Runnable runnable) {
		super(runnable, null);//该调用会直接开始执行runnable
	}

	public void addListener(Runnable runnable) {
		this.listener = listener;
		if(isDone()){
			fireListener();
		}
	}

	//异常返回注入
	public void setException(final Exception e) {
		super.setException(e);
	}

	//value返回注入
	public void complete() {
		super.set(null);
	}

	@Override
	protected void done() {
		fireListener();
	}


	private void fireListener() {
		if(listener!=null){
			try {
				listener.run();
			}catch (Exception e){
				logger.error("fire listener err:{}",e);
			}
		}
	}
}
