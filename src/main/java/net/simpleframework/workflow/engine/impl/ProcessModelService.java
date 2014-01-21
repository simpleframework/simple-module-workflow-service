package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbDataQuery;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.InitiateItems;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractParticipantType.Role;
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
			if (lob != null) {
				processModel.setAttr("processDocument",
						doc = new ProcessDocument(lob.getProcessSchema()));
			}
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
			if (!StringUtils.hasText(processNode.getAuthor())) {
				processNode.setAuthor(context.getParticipantService().getUser(userId).getText());
			}
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
	public void updateModel(final ProcessModelBean processModel, final ID userId, final char[] model) {
		try {
			final ProcessDocument document = new ProcessDocument(model);
			final ProcessNode processNode = document.getProcessNode();
			processModel.setModelName(processNode.getName());
			processModel.setModelText(processNode.getText());
			if (userId != null) {
				processModel.setLastUserId(userId);
			}
			processModel.setLastUpdate(new Date());
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
		final IParticipantModel service = context.getParticipantService();
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
					final ID userId2 = service.getUser(participant).getId();
					if (userId.equals(userId2)) {
						items.add(new InitiateItem(processModel, userId, service.getUser(userId)
								.getRoleId(), variables));
					}
				} else if (pt instanceof Role) {
					final ID roleId = service.getRole(participant).getId();
					if (service.getUser(userId).isMember(roleId, variables)) {
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
	public void onInit() throws Exception {
		addListener(new DbEntityAdapterEx() {

			@Override
			public void onAfterInsert(final IDbEntityManager<?> manager, final Object[] objects) {
				itemsCache.clear();
			}

			@Override
			public void onAfterUpdate(final IDbEntityManager<?> manager, final String[] columns,
					final Object[] objects) {
				itemsCache.clear();
			}

			@Override
			public void onBeforeDelete(final IDbEntityManager<?> manager,
					final IParamsValue paramsValue) {
				final IDbDataQuery<Map<String, Object>> qs = manager.queryMapSet(
						new String[] { "status" }, paramsValue);
				Map<String, Object> data;
				while ((data = qs.next()) != null) {
					final EProcessModelStatus status = Convert.toEnum(EProcessModelStatus.class,
							data.get("status"));
					if (status == EProcessModelStatus.deploy) {
						throw WorkflowException.of($m("ProcessModelService.0"));
					}
				}
			}

			@Override
			public void onAfterDelete(final IDbEntityManager<?> manager, final IParamsValue paramsValue) {
				itemsCache.clear();

				// 删除流程实例
				final ProcessService service = getProcessService();
				final Object[] modelIds = paramsValue.getValues();
				final Object[] processIds = service.list("id",
						SqlUtils.getIdsSQLParam("modelId", modelIds.length), modelIds).toArray();
				if (processIds.length > 0) {
					service.delete(processIds);
				}

				// 删除流程变量，静态
				VariableService.get().deleteVariables(EVariableSource.model, modelIds);
			}
		});
	}
}
