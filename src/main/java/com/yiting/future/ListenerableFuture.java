package com.yiting.future;

import java.util.concurrent.Future;

/**
 * Created by hzyiting on 2016/6/12.
 */
public interface ListenerableFuture<T> extends Future<T> {
	public void addListener(Runnable runnable);
}
