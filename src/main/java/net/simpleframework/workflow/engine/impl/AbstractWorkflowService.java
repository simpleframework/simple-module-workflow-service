package net.simpleframework.workflow.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalFactory;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.event.IWorkflowEventListener;

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

	public Map<String, Object> createVariables(final T bean) {
		final KVMap variables = new KVMap();
		return variables;
	}

	public void assertStatus(final IIdBeanAware bean, final Enum<?>... status) {
		final Enum<?> status2 = (Enum<?>) BeanUtils.getProperty(bean, "status");
		if (!ArrayUtils.contains(status, status2)) {
			throw WorkflowStatusException.of(status2, status);
		}
	}

	private final Map<ID, Set<String>> listenerClassMap = new ConcurrentHashMap<ID, Set<String>>();

	public Collection<IWorkflowEventListener> getEventListeners(final T bean) {
		final Set<String> set = new LinkedHashSet<String>();
		Set<String> set2 = listenerClassMap.get(bean.getId());
		if (set2 != null) {
			set.addAll(set2);
		}
		set2 = null;
		if (bean instanceof ProcessBean) {
			set2 = pService.getProcessNode((ProcessBean) bean).listeners();
		} else if (bean instanceof ActivityBean) {
			set2 = aService.getTaskNode((ActivityBean) bean).listeners();
		}
		if (set2 != null) {
			set.addAll(set2);
		}
		final ArrayList<IWorkflowEventListener> al = new ArrayList<IWorkflowEventListener>();
		for (final String listenerClass : set) {
			al.add((IWorkflowEventListener) singleton(listenerClass));
		}
		return al;
	}

	public void addEventListener(final T bean,
			final Class<? extends IWorkflowEventListener> listenerClass) {
		final ID id = bean.getId();
		Set<String> set = listenerClassMap.get(id);
		if (set == null) {
			listenerClassMap.put(id, set = new LinkedHashSet<String>());
		}
		set.add(listenerClass.getName());
	}

	public boolean removeEventListener(final T bean,
			final Class<? extends IWorkflowEventListener> listenerClass) {
		final Set<String> set = listenerClassMap.get(bean.getId());
		if (set != null) {
			return set.remove(listenerClass.getName());
		}
		return false;
	}

	protected static ProcessModelService mService = (ProcessModelService) IWorkflowContextAware.mService;
	protected static ProcessService pService = (ProcessService) IWorkflowContextAware.pService;
	protected static ActivityService aService = (ActivityService) IWorkflowContextAware.aService;
	protected static WorkitemService wService = (WorkitemService) IWorkflowContextAware.wService;
	protected static DelegationService dService = (DelegationService) IWorkflowContextAware.dService;

	protected static VariableService vService;

	static void doStartup() {
		vService = singleton(VariableService.class);
	}
}
