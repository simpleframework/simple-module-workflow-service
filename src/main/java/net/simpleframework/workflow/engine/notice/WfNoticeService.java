package net.simpleframework.workflow.engine.notice;

import static net.simpleframework.common.I18n.$m;

import java.util.Date;
import java.util.Map;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.ctx.task.ExecutorRunnableEx;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.notice.WfNoticeBean.ENoticeStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfNoticeService extends AbstractDbBeanService<WfNoticeBean> implements
		IWfNoticeService {
	@Override
	public WfNoticeBean addWfNotice(final ProcessBean process, final Date dsentDate,
			final String smessage, final int typeno) {
		return _addWfNotice(process.getId(), null, dsentDate, smessage, typeno);
	}

	@Override
	public WfNoticeBean addWfNotice(final WorkitemBean workitem, final Date dsentDate,
			final String smessage, final int typeno) {
		return _addWfNotice(workitem.getProcessId(), workitem.getId(), dsentDate, smessage, typeno);
	}

	WfNoticeBean _addWfNotice(final ID processId, final ID workitemId, final Date dsentDate,
			final String smessage, final int typeno) {
		if (getWfNoticeTypeHandler(typeno) == null) {
			throw WorkflowException.of($m("WfNoticeService.0"));
		}
		WfNoticeBean notice = createBean();
		notice.setProcessId(processId);
		notice.setTypeNo(typeno);
		notice.setWorkitemId(workitemId);
		notice.setDsentDate(dsentDate);
		notice.setSmessage(smessage);
		insert(notice);
		return notice;
	}

	@Override
	public IWfNoticeTypeHandler getWfNoticeTypeHandler(final int no) {
		return AbstractWfNoticeTypeHandler.regists.get(no);
	}

	void _doCheck() {
		final IDataQuery<WfNoticeBean> dq = wfnService.query("status=? and dsentdate<?",
				ENoticeStatus.ready, new Date());
		WfNoticeBean wfNotice;
		while ((wfNotice = dq.next()) != null) {
			final IWfNoticeTypeHandler handler = wfnService.getWfNoticeTypeHandler(wfNotice
					.getTypeNo());
			if (handler != null) {
				try {
					handler.doSent(wfNotice);
					// 修改状态
					wfNotice.setStatus(ENoticeStatus.sent);
					wfNotice.setSentDate(new Date());
					wfnService.update(new String[] { "status", "sentDate" }, wfNotice);
				} catch (final Exception e) {
					getLog().warn(e);
				}
			}
		}
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 通知消息检测
		getTaskExecutor().execute(new ExecutorRunnableEx("wfnotice_check") {
			@Override
			protected void task(final Map<String, Object> cache) throws Exception {
				_doCheck();
			}
		});
	}
}
