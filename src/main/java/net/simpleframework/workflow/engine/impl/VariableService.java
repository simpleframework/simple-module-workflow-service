package net.simpleframework.workflow.engine.impl;

import java.util.Collection;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.JsonUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.bean.AbstractWorkflowBean;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.VariableBean;
import net.simpleframework.workflow.engine.bean.VariableLogBean;
import net.simpleframework.workflow.schema.EVariableType;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class VariableService extends AbstractDbBeanService<VariableBean> implements
		IWorkflowContextAware {

	VariableBean createVariableBean(final AbstractWorkflowBean bean,
			final VariableNode variableNode, final Object value) {
		final VariableBean variable = new VariableBean();
		if (bean instanceof ProcessBean) {
			if (variableNode.isStatically()) {
				variable.setVariableSource(EVariableSource.model);
				variable.setSourceId(wfpService.getProcessModel((ProcessBean) bean).getId());
			} else {
				variable.setVariableSource(EVariableSource.process);
				variable.setSourceId(bean.getId());
			}
		} else if (bean instanceof ActivityBean) {
			variable.setVariableSource(EVariableSource.activity);
			variable.setSourceId(bean.getId());
		}
		variable.setVariableName(variableNode.getName());
		setValue(variable, variableNode, value);
		return variable;
	}

	VariableBean getVariableBean(final AbstractWorkflowBean bean, final VariableNode variableNode) {
		EVariableSource vs = null;
		ID id = null;
		if (bean instanceof ProcessBean) {
			if (variableNode.isStatically()) {
				vs = EVariableSource.model;
				id = wfpService.getProcessModel((ProcessBean) bean).getId();
			} else {
				vs = EVariableSource.process;
			}
		} else if (bean instanceof ActivityBean) {
			vs = EVariableSource.activity;
		}
		if (id == null) {
			id = bean.getId();
		}
		assert vs != null;
		return getBean("variableSource=? and sourceId=? and variableName=?", vs, id,
				variableNode.getName());
	}

	@SuppressWarnings("unchecked")
	void setValue(final VariableBean variable, final VariableNode variableNode, Object value) {
		final EVariableType vt = variableNode.getType();
		if (value instanceof String) {
			if (vt == EVariableType.vtCollection) {
				value = JsonUtils.toList((String) value);
			} else if (vt == EVariableType.vtMap) {
				value = JsonUtils.toMap((String) value);
			} else if (vt != EVariableType.vtString) {
				try {
					value = Convert.convert(value, Class.forName(vt.toString()));
				} catch (final ClassNotFoundException e) {
				}
			}
		}
		if (value instanceof String) {
			variable.setClobValue(((String) value).toCharArray());
		} else if (value instanceof Collection) {
			variable.setClobValue(JsonUtils.toJSON((Collection<?>) value).toCharArray());
		} else if (value instanceof Map) {
			variable.setClobValue(JsonUtils.toJSON((Map<String, ?>) value).toCharArray());
		} else if (value == null) {
			variable.setStringValue(null);
			variable.setClobValue(null);
		} else {
			variable.setStringValue(String.valueOf(value));
		}
	}

	Object getVariableValue(final VariableBean variable, final VariableNode variableNode) {
		final EVariableType vt = variableNode.getType();
		final boolean clob = vt == EVariableType.vtString || vt == EVariableType.vtCollection
				|| vt == EVariableType.vtMap;
		String vstr = null;
		if (variable == null) {
			vstr = variableNode.getValue();
		} else if (clob) {
			final char[] clobValue = variable.getClobValue();
			if (clobValue != null) {
				vstr = new String(clobValue);
			}
		} else {
			vstr = variable.getStringValue();
		}
		if (clob) {
			if (vt == EVariableType.vtCollection) {
				return JsonUtils.toList(vstr);
			} else if (vt == EVariableType.vtMap) {
				return JsonUtils.toMap(vstr);
			}
		} else if (vstr != null) {
			try {
				return Convert.convert(vstr, Class.forName(vt.toString()));
			} catch (final ClassNotFoundException e) {
			}
		}
		return vstr;
	}

	Object getVariableValue(final AbstractWorkflowBean bean, final VariableNode variableNode) {
		return getVariableValue(getVariableBean(bean, variableNode), variableNode);
	}

	void setVariableValue(final AbstractWorkflowBean bean, final String[] names,
			final Object[] values) {
		if (names == null || values == null) {
			return;
		}
		final int length = Math.min(names.length, values.length);
		for (int i = 0; i < length; i++) {
			VariableNode variableNode = null;
			if (bean instanceof ProcessBean) {
				variableNode = wfpService.getProcessNode((ProcessBean) bean).getVariableNodeByName(
						names[i]);
			} else if (bean instanceof ActivityBean) {
				variableNode = wfaService.getTaskNode((ActivityBean) bean).getVariableNodeByName(
						names[i]);
			}
			if (variableNode == null) {
				continue;
			}
			final VariableBean variable = getVariableBean(bean, variableNode);
			if (variable != null) {
				setValue(variable, variableNode, values[i]);
				update(new String[] { "stringValue", "clobValue" }, variable);
			} else {
				insert(createVariableBean(bean, variableNode, values[i]));
			}
		}
	}

	void deleteVariables(final EVariableSource source, final Object... beanIds) {
		deleteWith("variableSource=? and " + SqlUtils.getIdsSQLParam("sourceId", beanIds.length),
				ArrayUtils.add(new Object[] { source }, beanIds));
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx<VariableBean>() {
			@Override
			public void onBeforeDelete(final IDbEntityManager<VariableBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);
				for (final VariableBean var : coll(manager, paramsValue)) {
					getEntityManager(VariableLogBean.class).delete(
							new ExpressionValue("variableId=?", var.getId()));
				}
			}
		});
	}
}
