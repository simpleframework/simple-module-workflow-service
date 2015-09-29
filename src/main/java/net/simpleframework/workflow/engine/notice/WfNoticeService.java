package net.simpleframework.workflow.engine.notice;

import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.ClassUtils.ScanClassResourcesCallback;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfNoticeService extends AbstractDbBeanService<WfNoticeBean> implements
		IWfNoticeService {

	@Override
	public void onInit() throws Exception {
		super.onInit();

		for (final String packageName : getApplicationContext().getScanPackageNames()) {
			ClassUtils.scanResources(packageName, new ScanClassResourcesCallback() {
				@Override
				public void doResources(final String filepath, final boolean isDirectory)
						throws Exception {
				}
			});
		}
	}
}
