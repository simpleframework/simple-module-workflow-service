package net.simpleframework.workflow.engine.notice;

import static net.simpleframework.common.I18n.$m;

import java.util.HashMap;
import java.util.Map;

import net.simpleframework.ctx.IApplicationContext;
import net.simpleframework.ctx.hdl.AbstractScanHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractWfNoticeTypeHandler extends AbstractScanHandler
		implements IWfNoticeTypeHandler {

	static Map<Integer, IWfNoticeTypeHandler> regists = new HashMap<>();

	@Override
	public void onScan(final IApplicationContext application) throws Exception {
		final int no = this.getNo();
		if (regists.containsKey(no)) {
			oprintln(new StringBuilder("[IWfNoticeTypeHandler, no: ").append(no).append("] ")
					.append($m("AbstractWfNoticeTypeHandler.1")).append(" - ")
					.append(getClass().getName()));
			return;
		}
		regists.put(no, this);
	}

	@Override
	public String toString() {
		return $m("AbstractWfNoticeTypeHandler.0") + " - " + getClass().getName();
	}
}
