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
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalFactory;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.IWorkflowService;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.event.IActivityEventListener;
import net.simpleframework.workflow.engine.event.IProcessEventListener;
import net.simpleframework.workflow.engine.event.IProcessModelEventListener;
import net.simpleframework.workflow.engine.event.IWorkflowEventListener;
import net.simpleframework.workflow.engine.event.IWorkitemEventListener;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.ProcessNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractWorkflowService<T extends AbstractIdBean> extends
		AbstractDbBeanService<T> implements IWorkflowService<T>, IWorkflowContextAware {
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

	public Object getVariable(final T bean, final String name) {
		return null;
	}

	public int getIntVariable(final T bean, final String name) {
		return Convert.toInt(getVariable(bean, name));
	}

	public boolean getBoolVariable(final T bean, final String name) {
		return Convert.toBool(getVariable(bean, name));
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
		Set<String> _set = null;
		if (bean instanceof ProcessModelBean) {
			_set = mService.getProcessDocument((ProcessModelBean) bean).getProcessNode().listeners();
		} else if (bean instanceof ProcessBean) {
			_set = pService.getProcessNode((ProcessBean) bean).listeners();
		} else {
			AbstractTaskNode taskNode = null;
			if (bean instanceof ActivityBean) {
				taskNode = aService.getTaskNode((ActivityBean) bean);
			} else if (bean instanceof WorkitemBean) {
				taskNode = aService.getTaskNode(wService.getActivity((WorkitemBean) bean));
			}
			if (taskNode != null) {
				_set = ((ProcessNode) taskNode.getParent()).listeners();
				if (_set != null) {
					set.addAll(_set);
				}
				_set = taskNode.listeners();
			}
		}
		if (_set != null) {
			set.addAll(_set);
		}
		if ((_set = listenerClassMap.get(bean.getId())) != null) {
			set.addAll(_set);
		}
		final ArrayList<IWorkflowEventListener> al = new ArrayList<IWorkflowEventListener>();
		for (final String listenerClass : set) {
			final IWorkflowEventListener l = (IWorkflowEventListener) singleton(listenerClass);
			if ((bean instanceof ProcessModelBean && l instanceof IProcessModelEventListener)
					|| (bean instanceof ProcessBean && l instanceof IProcessEventListener)
					|| (bean instanceof ActivityBean && l instanceof IActivityEventListener)
					|| (bean instanceof WorkitemBean && l instanceof IWorkitemEventListener)) {
				al.add(l);
			}
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
