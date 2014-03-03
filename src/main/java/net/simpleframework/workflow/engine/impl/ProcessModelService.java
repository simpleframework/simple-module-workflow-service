package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.InitiateItems;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.event.IProcessModelListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractParticipantType.BaseRole;
import net.simpleframework.workflow.schema.AbstractParticipantType.User;
import net.simpleframework.workflow.schema.AbstractProcessStartupType;
import net.simpleframework.workflow.schema.AbstractProcessStartupType.Manual;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelService extends AbstractWorkflowService<ProcessModelBean> implements
		IProcessModelService {

	@Override
	public ProcessDocument getProcessDocument(final ProcessModelBean processModel) {
		ProcessDocument doc = (ProcessDocument) processModel.getAttr("processDocument");
		if (doc == null) {
			final ProcessModelLobBean lob = getEntityManager(ProcessModelLobBean.class).getBean(
					processModel.getId());
			processModel.setAttr("processDocument", doc = new ProcessDocument(lob.getProcessSchema()));
		}
		return doc;
	}

	@Override
	public ProcessModelBean addModel(final ID userId, ProcessDocument document) {
		final ProcessModelBean bean = createBean();
		if (document == null) {
			document = new ProcessDocument();
		}
		final ProcessNode processNode = document.getProcessNode();
		if (userId != null) {
			bean.setUserId(userId);
			bean.setUserText(permission.getUser(userId).toString());
		}
		bean.setModelName(processNode.getName());
		bean.setModelText(processNode.getText());
		bean.setCreateDate(new Date());

		insert(bean);

		final String schema = document.toString();
		final ProcessModelLobBean lob = new ProcessModelLobBean();
		lob.setId(bean.getId());
		lob.setProcessSchema(schema.toCharArray());
		getEntityManager(ProcessModelLobBean.class).insert(lob);

		return bean;
	}

	@Override
	public void updateModel(final ProcessModelBean processModel, final char[] model) {
		assertStatus(processModel, EProcessModelStatus.edit);
		try {
			final ProcessDocument document = new ProcessDocument(model);
			final ProcessNode processNode = document.getProcessNode();
			processModel.setModelName(processNode.getName());
			processModel.setModelText(processNode.getText());
			update(processModel);

			final ProcessModelLobBean lob = getEntityManager(ProcessModelLobBean.class).getBean(
					processModel.getId());
			lob.setProcessSchema(model);
			getEntityManager(ProcessModelLobBean.class).update(lob);
		} finally {
			processModel.removeAttr("processDocument");
		}
	}

	@Override
	public IDataQuery<ProcessModelBean> getModelList(final EProcessModelStatus... status) {
		final StringBuilder sql = new StringBuilder();
		final ArrayList<Object> params = new ArrayList<Object>();
		sql.append("1=1");
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EProcessModelStatus s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		sql.append(" order by createDate desc");
		return query(sql.toString(), params.toArray());
	}

	@Override
	public ProcessModelBean getProcessModelByName(final String name) {
		return getBean("modelName=?", name);
	}

	@Override
	public ProcessModelBean getProcessModel(final String model) {
		ProcessModelBean processModel = getProcessModelByName(model);
		if (processModel == null) {
			processModel = getBean(model);
		}
		if (processModel == null) {
			throw WorkflowException.of($m("ProcessModelService.1", model));
		}
		return processModel;
	}

	private final Map<ID, InitiateItems> itemsCache = new HashMap<ID, InitiateItems>();

	@Override
	public InitiateItems getInitiateItems(final ID userId) {
		if (userId == null) {
			return InitiateItems.NULL_ITEMS;
		}
		InitiateItems items = itemsCache.get(userId);
		if (items != null) {
			items = items.clone();
			for (final InitiateItem item : items) {
				if (item.model() == null || item.roles().size() == 0) {
					items.remove(item.getModelId());
				}
			}
			return items;
		}

		items = new InitiateItems();
		final IDataQuery<ProcessModelBean> query = getModelList(EProcessModelStatus.deploy);
		ProcessModelBean processModel;
		while ((processModel = query.next()) != null) {
			final AbstractProcessStartupType startupType = getProcessDocument(processModel)
					.getProcessNode().getStartupType();
			if (startupType instanceof Manual) {
				final KVMap variables = new KVMap().add("model", processModel);
				final AbstractParticipantType pt = ((Manual) startupType).getParticipantType();
				final String participant = pt.getParticipant();
				if (pt instanceof User) {
					final ID userId2 = permission.getUser(participant).getId();
					if (userId.equals(userId2)) {
						items.add(new InitiateItem(processModel, userId, permission.getUser(userId)
								.getRoleId(), variables));
					}
				} else if (pt instanceof BaseRole) {
					final ID roleId = permission.getRole(participant).getId();
					if (permission.getUser(userId).isMember(roleId, variables)) {
						items.add(new InitiateItem(processModel, userId, roleId, variables));
					}
				}
			} else {
			}
		}
		itemsCache.put(userId, items);
		return items;
	}

	@Override
	public boolean isStartProcess(final ID userId, final Object model) {
		return getInitiateItems(userId).get(model) != null;
	}

	@Override
	public Map<String, Object> createVariables(final ProcessModelBean model) {
		final Map<String, Object> variables = super.createVariables(model);
		variables.put("model", model);
		return variables;
	}

	@Override
	public void deploy(final ProcessModelBean processModel) {
		assertStatus(processModel, EProcessModelStatus.edit);
		processModel.setStatus(EProcessModelStatus.deploy);
		update(new String[] { "status" }, processModel);
	}

	@Override
	public void suspend(final ProcessModelBean processModel) {
	}

	@Override
	public void resume(final ProcessModelBean processModel) {
		assertStatus(processModel, EProcessModelStatus.deploy, EProcessModelStatus.suspended);
		processModel.setStatus(EProcessModelStatus.edit);
		update(new String[] { "status" }, processModel);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx() {

			@Override
			public void onAfterInsert(final IDbEntityManager<?> manager, final Object[] beans) {
				itemsCache.clear();
			}

			@Override
			public void onAfterUpdate(final IDbEntityManager<?> manager, final String[] columns,
					final Object[] beans) {
				itemsCache.clear();

				if (ArrayUtils.contains(columns, "status")) {
					for (final Object bean : beans) {
						final ProcessModelBean processModel = (ProcessModelBean) bean;
						for (final IWorkflowListener listener : getEventListeners(processModel)) {
							((IProcessModelListener) listener).onStatusChange(processModel);
						}
					}
				}
			}

			@Override
			public void onBeforeDelete(final IDbEntityManager<?> manager,
					final IParamsValue paramsValue) {
				super.onBeforeDelete(manager, paramsValue);
				for (final ProcessModelBean processModel : coll(paramsValue)) {
					if (processModel.getStatus() == EProcessModelStatus.deploy) {
						throw WorkflowException.of($m("ProcessModelService.0"));
					}

					final Object id = processModel.getId();
					if (pService.count("modelId=?", id) > 0) {
						throw WorkflowException.of($m("ProcessModelService.2"));
					}

					// 删除lob
					getEntityManager(ProcessModelLobBean.class).delete(new ExpressionValue("id=?", id));

					// 删除流程变量，静态
					vService.deleteVariables(EVariableSource.model, id);
				}
			}

			@Override
			public void onAfterDelete(final IDbEntityManager<?> manager, final IParamsValue paramsValue) {
				itemsCache.clear();
			}
		});
	}

	@Override
	public boolean isFinalStatus(final ProcessModelBean t) {
		return t.getStatus().ordinal() >= EProcessModelStatus.abort.ordinal();
	}
}
