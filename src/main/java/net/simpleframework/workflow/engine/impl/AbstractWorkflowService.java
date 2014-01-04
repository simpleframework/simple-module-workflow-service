package net.simpleframework.workflow.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalFactory;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.ctx.task.ITaskExecutor;
import net.simpleframework.workflow.engine.AbstractWorkflowBean;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.event.IWorkflowListener;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractWorkflowService<T extends AbstractIdBean> extends
		AbstractDbBeanService<T> implements IWorkflowContextAware {
	static Collection<String> defaultExpr;
	static {
		defaultExpr = new ArrayList<String>();
		defaultExpr.add("import " + WorkflowContext.class.getPackage().getName() + ".*;");
	}

	public IScriptEval createScriptEval(final T bean) {
		final IScriptEval script = ScriptEvalFactory.createDefaultScriptEval(createVariables(bean));
		for (final String expr : defaultExpr) {
			script.eval(expr);
		}
		return script;
	}

	public KVMap createVariables(final T bean) {
		final KVMap variables = new KVMap();
		return variables;
	}

	public void assertStatus(final AbstractWorkflowBean bean, final Enum<?>... status) {
		final Enum<?> status2 = (Enum<?>) BeanUtils.getProperty(bean, "status");
		if (!ArrayUtils.contains(status, status2)) {
			throw WorkflowStatusException.of(status2, status);
		}
	}

	private final Map<ID, Set<String>> listenerClassMap = new ConcurrentHashMap<ID, Set<String>>();

	public Collection<IWorkflowListener> getEventListeners(final T bean) {
		final Set<String> set = new LinkedHashSet<String>();
		Set<String> set2 = listenerClassMap.get(bean.getId());
		if (set2 != null) {
			set.addAll(set2);
		}
		set2 = null;
		if (bean instanceof ProcessBean) {
			set2 = getProcessService().processNode((ProcessBean) bean).listeners();
		} else if (bean instanceof ActivityBean) {
			set2 = getActivityService().taskNode((ActivityBean) bean).listeners();
		}
		if (set2 != null) {
			set.addAll(set2);
		}
		final ArrayList<IWorkflowListener> al = new ArrayList<IWorkflowListener>();
		for (final String listenerClass : set) {
			al.add((IWorkflowListener) singleton(listenerClass));
		}
		return al;
	}

	public void addEventListener(final T bean, final Class<? extends IWorkflowListener> listenerClass) {
		final ID id = bean.getId();
		Set<String> set = listenerClassMap.get(id);
		if (set == null) {
			listenerClassMap.put(id, set = new LinkedHashSet<String>());
		}
		set.add(listenerClass.getName());
	}

	public boolean removeEventListener(final T bean,
			final Class<? extends IWorkflowListener> listenerClass) {
		final Set<String> set = listenerClassMap.get(bean.getId());
		if (set != null) {
			return set.remove(listenerClass.getName());
		}
		return false;
	}

	protected ProcessModelService getModelService() {
		return (ProcessModelService) context.getProcessModelService();
	}

	protected ProcessService getProcessService() {
		return (ProcessService) context.getProcessService();
	}

	protected ActivityService getActivityService() {
		return (ActivityService) context.getActivityService();
	}

	protected WorkitemService getWorkitemService() {
		return (WorkitemService) context.getWorkitemService();
	}

	protected ITaskExecutor getTaskExecutor() {
		return context.getTaskExecutor();
	}
}
