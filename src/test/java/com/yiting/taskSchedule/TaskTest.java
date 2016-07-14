package com.yiting.taskSchedule;

import com.google.gson.Gson;
import com.yiting.taskschedule.meta.Message;
import com.yiting.taskschedule.meta.MsgPayload;
import com.yiting.taskschedule.util.SchedUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by hzyiting on 2016/7/14.
 */
public class TaskTest extends BaseTest {
	public static final String SERVICE_CONTAINER = "CONTAINER";
	Gson gson = new Gson();

	protected Message createMessge(MsgPayload payload) {
		Message msg = new Message();
		msg.setFrom("ignore");
		msg.setTo(SERVICE_CONTAINER);
		msg.setMsg(gson.toJson(payload).toString());
		System.out.println("msg:" + msg);
		return msg;
	}

	@Test
	public void testSchedule(){
		MsgPayload msgPayload=new MsgPayload();
		msgPayload.setCmd("test");
		msgPayload.setUniqueId(getUUID());
		msgPayload.setContent(new HashMap<String, Object>());

		SchedUtils.commit(createMessge(msgPayload));

		try {
			Thread.sleep(1000*10*10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public static String getUUID(){
		String s = UUID.randomUUID().toString();
		return s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24);
	}




}
