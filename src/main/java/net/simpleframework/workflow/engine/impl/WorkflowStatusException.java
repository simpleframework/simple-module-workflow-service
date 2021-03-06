package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.List;

import net.simpleframework.common.StringUtils;
import net.simpleframework.common.th.RuntimeExceptionEx;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EDelegationStatus;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.bean.AbstractWorkflowBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkflowStatusException extends RuntimeExceptionEx {

	public WorkflowStatusException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	private static List<Class<?>> BEANCLASS_LIST;

	static {
		BEANCLASS_LIST = new ArrayList<>();
		BEANCLASS_LIST.add(EProcessModelStatus.class);
		BEANCLASS_LIST.add(EProcessStatus.class);
		BEANCLASS_LIST.add(EActivityStatus.class);
		BEANCLASS_LIST.add(EWorkitemStatus.class);
		BEANCLASS_LIST.add(EDelegationStatus.class);
	}

	public static WorkflowStatusException of(final String msg) {
		return _of(WorkflowStatusException.class, msg);
	}

	public static WorkflowStatusException of(final AbstractWorkflowBean t,
			final Enum<?> currentStatus, final Enum<?>... needStatus) {
		return of($m(
				"WorkflowStatusException." + BEANCLASS_LIST.indexOf(currentStatus.getDeclaringClass()),
				t, currentStatus, StringUtils.join(needStatus, ", ")));
	}

	private static final long serialVersionUID = 6719697728857540597L;
}
