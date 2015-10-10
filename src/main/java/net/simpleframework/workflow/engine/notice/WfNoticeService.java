package net.simpleframework.workflow.engine.notice;

import java.util.Map;

import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.ctx.task.ExecutorRunnableEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfNoticeService extends AbstractDbBeanService<WfNoticeBean> implements
		IWfNoticeService {

	@Override
	public IWfNoticeTypeHandler getWfNoticeTypeHandler(final int no) {
		return AbstractWfNoticeTypeHandler.regists.get(no);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 通知消息检测
		getTaskExecutor().execute(new ExecutorRunnableEx("wfnotice_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {

			}
		});
	}
}
