package net.simpleframework.workflow.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.ctx.script.ScriptEvalFactory;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.IWorkflowService;
import net.simpleframework.workflow.engine.IWorkflowServiceAware;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.event.IActivityListener;
import net.simpleframework.workflow.engine.event.IProcessListener;
import net.simpleframework.workflow.engine.event.IProcessModelListener;
import net.simpleframework.workflow.engine.event.IWorkCalendarListener;
import net.simpleframework.workflow.engine.event.IWorkCalendarListener.WorkCalendarAdapter;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.event.IWorkitemListener;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.ProcessNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractWorkflowService<T extends AbstractIdBean> extends
		AbstractDbBeanService<T> implements IWorkflowService<T>, IWorkflowServiceAware {
	static Collection<String> defaultExpr;
	static {
		defaultExpr = new ArrayList<String>();
		defaultExpr.add("import " + WorkflowContext.class.getPackage().getName() + ".*;");
	}

	/**
	 * 获取脚本解析器
	 * 
	 * @param bean
	 * @return
	 */
	public IScriptEval getScriptEval(final T bean) {
		return bean.getAttrCache("_ScriptEval", new IVal<IScriptEval>() {
			@Override
			public IScriptEval get() {
				final IScriptEval script = ScriptEvalFactory
						.createDefaultScriptEval(createVariables(bean));
				for (final String expr : defaultExpr) {
					script.eval(expr);
				}
				final Package[] arr = workflowContext.getImportPackages();
				if (arr != null) {
					for (final Package p : arr) {
						script.eval("import " + p.getName() + ".*;");
					}
				}
				return script;
			}
		});
	}

	protected Object eval(final T bean, final String script) {
		return getScriptEval(bean).eval(script);
	}

	public Map<String, Object> createVariables(final T bean) {
		final KVMap variables = new KVMap();
		return variables;
	}

	public Object getVariable(final T bean, final String name) {
		return null;
	}

	public byte getByteVariable(final T bean, final String name) {
		return Convert.toByte(getVariable(bean, name));
	}

	public short getShortVariable(final T bean, final String name) {
		return Convert.toShort(getVariable(bean, name));
	}

	public int getIntVariable(final T bean, final String name) {
		return Convert.toInt(getVariable(bean, name));
	}

	public long getLongVariable(final T bean, final String name) {
		return Convert.toLong(getVariable(bean, name));
	}

	public float getFloatVariable(final T bean, final String name) {
		return Convert.toFloat(getVariable(bean, name));
	}

	public double getDoubleVariable(final T bean, final String name) {
		return Convert.toDouble(getVariable(bean, name));
	}

	public boolean getBoolVariable(final T bean, final String name) {
		return Convert.toBool(getVariable(bean, name));
	}

	public IWorkCalendarListener getWorkCalendarListener(final T bean) {
		final Set<String> set = getListeners(bean);
		for (final String listenerClass : set) {
			final IWorkflowListener l = (IWorkflowListener) singleton(listenerClass);
			if (l instanceof IWorkCalendarListener) {
				return (IWorkCalendarListener) l;
			}
		}
		return singleton(WorkCalendarAdapter.class);
	}

	private final Map<ID, Set<String>> listenerClassMap = new ConcurrentHashMap<ID, Set<String>>();

	public Collection<IWorkflowListener> getEventListeners(final T bean) {
		final Set<String> set = getListeners(bean);
		final ArrayList<IWorkflowListener> al = new ArrayList<IWorkflowListener>();
		for (final String listenerClass : set) {
			final IWorkflowListener l = (IWorkflowListener) singleton(listenerClass);
			if ((bean instanceof ProcessModelBean && l instanceof IProcessModelListener)
					|| (bean instanceof ProcessBean && l instanceof IProcessListener)
					|| (bean instanceof ActivityBean && l instanceof IActivityListener)
					|| (bean instanceof WorkitemBean && l instanceof IWorkitemListener)) {
				al.add(l);
			}
		}
		return al;
	}

	private Set<String> getListeners(final T bean) {
		final Set<String> set = new LinkedHashSet<String>();
		Set<String> _set = null;
		if (bean instanceof ProcessModelBean) {
			_set = mService.getProcessDocument((ProcessModelBean) bean).getProcessNode().listeners();
		} else if (bean instanceof ProcessBean) {
			_set = pService._getProcessNode((ProcessBean) bean).listeners();
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
		return set;
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

	protected void _assert(final T t, final Enum<?>... status) {
		final Enum<?> status2 = (Enum<?>) BeanUtils.getProperty(t, "status");
		if (!ArrayUtils.contains(status, status2)) {
			throw WorkflowStatusException.of(status2, status);
		}
	}

	@SuppressWarnings("unchecked")
	protected void _status(final T t, final Enum<?> status) {
		BeanUtils.setProperty(t, "status", status);
		update(new String[] { "status" }, t);
	}

	protected void buildStatusSQL(final StringBuilder sql, final ArrayList<Object> params,
			final String alias, final Enum<?>... status) {
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final Enum<?> s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				if (alias != null) {
					sql.append(alias).append(".");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
	}

	protected static ProcessModelService mService = (ProcessModelService) IWorkflowServiceAware.mService;
	protected static ProcessService pService = (ProcessService) IWorkflowServiceAware.pService;
	protected static ActivityService aService = (ActivityService) IWorkflowServiceAware.aService;
	protected static WorkitemService wService = (WorkitemService) IWorkflowServiceAware.wService;
	protected static WorkviewService vService = (WorkviewService) IWorkflowServiceAware.vService;
	protected static DelegationService dService = (DelegationService) IWorkflowServiceAware.dService;
	protected static ProcessModelDomainRService drService = (ProcessModelDomainRService) IWorkflowServiceAware.drService;

	protected static VariableService varService;

	static void doStartup() {
		varService = singleton(VariableService.class);
	}

	static final String ATTR_PROCESS_DOCUMENT = "_processdocument";
}
