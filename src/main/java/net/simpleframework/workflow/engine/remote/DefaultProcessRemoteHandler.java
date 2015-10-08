package net.simpleframework.workflow.engine.remote;

import java.io.IOException;
import java.util.Map;

import net.simpleframework.common.web.HttpClient;
import net.simpleframework.workflow.remote.AbstractWorkflowRemoteHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */

public class DefaultProcessRemoteHandler extends AbstractWorkflowRemoteHandler implements
		IProcessRemoteHandler {

	@Override
	public Map<String, Object> call(final String url, final String method,
			final Map<String, Object> data) throws IOException {
		return new HttpClient(url).post(wfSettings.getSubtaskUrl() + "?method=" + method, data);
	}
}
