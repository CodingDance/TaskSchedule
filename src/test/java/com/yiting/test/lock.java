package com.yiting.test;

import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by hzyiting on 2016/6/12.
 */
public class lock {


	@Test
	public void test() {

		for (int i = 0; i < 10000; i++) {
			Thread thread = new Thread(new MyThread());
			thread.start();
		}

	}
}


class MyThread implements Runnable {
	static final ReentrantLock reentrantLock = new ReentrantLock();

	public void run() {
		boolean success = false;
		try {
			success = reentrantLock.tryLock();
			if (success) {
				System.out.println("try success");
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("try error");
			}
		} finally {
			if (success) {
				reentrantLock.unlock();
			}
		}
	}
}
