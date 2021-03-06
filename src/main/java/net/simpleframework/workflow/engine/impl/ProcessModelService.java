package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.Version;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.permission.PermissionConst;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.IProcessModelService;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.InitiateItems;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.engine.bean.ProcessModelDomainR;
import net.simpleframework.workflow.engine.bean.ProcessModelLobBean;
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
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessModelService extends AbstractWorkflowService<ProcessModelBean>
		implements IProcessModelService {

	@Override
	public ProcessDocument getProcessDocument(final ProcessModelBean processModel) {
		return processModel.getAttrCache(ATTR_PROCESS_DOCUMENT, new CacheV<ProcessDocument>() {
			@Override
			public ProcessDocument get() {
				final ProcessModelLobBean lob = getEntityManager(ProcessModelLobBean.class)
						.getBean(processModel.getId());
				return new ProcessDocument(new StringReader(new String(lob.getProcessSchema()).trim()));
			}
		});
	}

	@Override
	public ProcessModelBean doAddModel(final ID userId, ProcessDocument document) {
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
		bean.setModelVer(processNode.getVersion().toString());
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
	public void doUpdateModel(final ProcessModelBean processModel, final char[] model) {
		_assert(processModel, EProcessModelStatus.edit, EProcessModelStatus.deploy);
		try {
			final ProcessDocument document = new ProcessDocument(model);
			final ProcessNode processNode = document.getProcessNode();
			processNode.getVersion().incMicro();
			final Version nVer = processNode.getVersion();
			final Version oVer = Version
					.getVersion((String) queryFor("modelver", "id=?", processModel.getId()));
			if (oVer.complies(nVer) || oVer.equals(nVer)) {
				throw WorkflowException.of($m("ProcessModelService.3", nVer, oVer));
			}

			processModel.setModelName(processNode.getName());
			processModel.setModelVer(nVer.toString());
			processModel.setModelText(processNode.getText());
			processModel.setLastUpdate(new Date());
			update(processModel);

			final ProcessModelLobBean lob = getEntityManager(ProcessModelLobBean.class)
					.getBean(processModel.getId());
			lob.setProcessSchema(document.toString().toCharArray());
			getEntityManager(ProcessModelLobBean.class).update(lob);
		} finally {
			processModel.removeAttr(ATTR_PROCESS_DOCUMENT);
		}
	}

	private static final String DEFAULT_ORDERBY = " order by createDate desc";

	@Override
	public IDataQuery<ProcessModelBean> getModelList(final EProcessModelStatus... status) {
		final StringBuilder sql = new StringBuilder("1=1");
		final ArrayList<Object> params = new ArrayList<>();
		buildStatusSQL(sql, params, status);
		return query(sql.append(DEFAULT_ORDERBY).toString(), params.toArray());
	}

	@Override
	public IDataQuery<ProcessModelBean> getModelListByDomain(final ID domainId,
			final EProcessModelStatus... status) {
		final StringBuilder sql = new StringBuilder(
				"select m.*, d.processCount as processCount2 from ")
						.append(getTablename(ProcessModelDomainR.class)).append(" d right join ")
						.append(getTablename(ProcessModelBean.class))
						.append(" m on d.modelid = m.id where d.domainid=?");
		final ArrayList<Object> params = new ArrayList<>();
		params.add(domainId);
		buildStatusSQL(sql, params, "m", status);
		return getEntityManager().queryBeans(new SQLValue(sql, params.toArray()));
	}

	@Override
	public void sort(final List<ProcessModelBean> models) {
		Collections.sort(models, new Comparator<ProcessModelBean>() {
			@Override
			public int compare(final ProcessModelBean pm1, final ProcessModelBean pm2) {
				final ProcessNode pn1 = wfpmService.getProcessDocument(pm1).getProcessNode();
				final ProcessNode pn2 = wfpmService.getProcessDocument(pm2).getProcessNode();
				if (pn1.getOorder() == pn2.getOorder()) {
					return 0;// jdk7及以上不返回0会抛 Comparison method violates its general
				}
				// contract!
				return pn1.getOorder() > pn2.getOorder() ? 1 : -1;
			}
		});
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

	@Override
	public InitiateItems getInitiateItems(final ID userId) {
		return getInitiateItems(null, userId);
	}

	@Override
	public InitiateItems getInitiateItems(final ProcessModelBean pb, final ID userId) {
		if (userId == null) {
			return InitiateItems.NULL_ITEMS;
		}

		final InitiateItems items = new InitiateItems();
		if (null != pb) {
			final InitiateItem iitem = getInitiateItem(pb, userId);
			if (null != iitem) {
				items.add(iitem);
			}
		} else {
			final IDataQuery<ProcessModelBean> query = getModelList(EProcessModelStatus.deploy);
			ProcessModelBean processModel;
			while ((processModel = query.next()) != null) {
				final InitiateItem iitem = getInitiateItem(processModel, userId);
				if (null != iitem) {
					items.add(iitem);
				}
			}
		}
		return items;
	}

	@Override
	public boolean isStartProcess(final ID userId, final Object model) {
		if (model instanceof ProcessModelBean) {
			return getInitiateItem((ProcessModelBean) model, userId) != null;
		} else {
			return getInitiateItems(userId).get(model) != null;
		}
	}

	private InitiateItem getInitiateItem(final ProcessModelBean processModel, final ID userId) {
		final AbstractProcessStartupType startupType = getProcessDocument(processModel)
				.getProcessNode().getStartupType();
		if (startupType instanceof Manual) {
			final KVMap variables = new KVMap().add("model", processModel)
					.add(PermissionConst.VAR_USERID, userId);
			final AbstractParticipantType pt = ((Manual) startupType).getParticipantType();
			final String participant = pt.getParticipant();
			if (pt instanceof User) {
				final ID userId2 = permission.getUser(participant).getId();
				if (userId.equals(userId2)) {
					return new InitiateItem(processModel, userId, null, variables);
				}
			} else if (pt instanceof BaseRole) {
				final ID roleId = permission.getRole(participant, variables).getId();
				if (permission.getUser(userId).isMember(roleId, variables)) {
					final ID _roleId = (ID) variables.get(PermissionConst.VAR_ROLEID);
					// 采用VAR_ROLEID定义的角色, 角色嵌套
					return new InitiateItem(processModel, userId, _roleId != null ? _roleId : roleId,
							variables);
				}
			}
		} else {
			//
		}
		return null;
	}

	@Override
	public Map<String, Object> createVariables(final ProcessModelBean model) {
		final Map<String, Object> variables = super.createVariables(model);
		variables.put("model", model);
		return variables;
	}

	@Override
	public void doDeploy(final ProcessModelBean processModel) {
		_assert(processModel, EProcessModelStatus.edit);
		_status(processModel, EProcessModelStatus.deploy);
	}

	@Override
	public void doSuspend(final ProcessModelBean processModel) {
	}

	@Override
	public void doResume(final ProcessModelBean processModel) {
		_assert(processModel, EProcessModelStatus.deploy, EProcessModelStatus.suspended);
		_status(processModel, EProcessModelStatus.edit);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx<ProcessModelBean>() {
			@Override
			public void onAfterUpdate(final IDbEntityManager<ProcessModelBean> manager,
					final String[] columns, final ProcessModelBean[] beans) {
				if (ArrayUtils.contains(columns, "status", true)) {
					for (final ProcessModelBean processModel : beans) {
						for (final IWorkflowListener listener : getEventListeners(processModel)) {
							((IProcessModelListener) listener).onStatusChange(processModel);
						}
					}
				}
			}

			@Override
			public void onBeforeDelete(final IDbEntityManager<ProcessModelBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onBeforeDelete(manager, paramsValue);
				for (final ProcessModelBean processModel : coll(manager, paramsValue)) {
					if (processModel.getStatus() == EProcessModelStatus.deploy) {
						throw WorkflowException.of($m("ProcessModelService.0"));
					}

					final Object id = processModel.getId();
					if (wfpService.count("modelId=?", id) > 0) {
						throw WorkflowException.of($m("ProcessModelService.2"));
					}

					// 删除lob
					getEntityManager(ProcessModelLobBean.class).delete(new ExpressionValue("id=?", id));
					// 删除流程变量，静态
					vServiceImpl.deleteVariables(EVariableSource.model, id);
					// 删除domian
					wfpmdService.deleteWith("modelId=?", id);
				}
			}
		});
	}

	@Override
	public boolean isFinalStatus(final ProcessModelBean t) {
		return t.getStatus().ordinal() >= EProcessModelStatus.abort.ordinal();
	}
}
