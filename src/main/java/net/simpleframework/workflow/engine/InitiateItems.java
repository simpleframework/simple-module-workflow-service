package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.simpleframework.common.ID;
import net.simpleframework.workflow.engine.bean.ProcessModelBean;
import net.simpleframework.workflow.schema.ProcessNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class InitiateItems extends ArrayList<InitiateItem> implements IWorkflowContextAware {
	public static final InitiateItems NULL_ITEMS = new InitiateItems();

	public InitiateItem get(final Object model) {
		if (model == null) {
			return null;
		}
		final ID id = model instanceof ProcessModelBean ? ((ProcessModelBean) model).getId() : ID
				.of(model);
		for (final InitiateItem item : this) {
			if (id.equals(item.getModelId())) {
				return item;
			}
		}
		return null;
	}

	public InitiateItems sort() {
		Collections.sort(this, new Comparator<InitiateItem>() {
			@Override
			public int compare(final InitiateItem item1, final InitiateItem item2) {
				final ProcessModelBean pm1 = wfpmService.getBean(item1.getModelId());
				final ProcessModelBean pm2 = wfpmService.getBean(item2.getModelId());
				if (pm1 != null && pm2 != null) {
					final ProcessNode pn1 = wfpmService.getProcessDocument(pm1).getProcessNode();
					final ProcessNode pn2 = wfpmService.getProcessDocument(pm2).getProcessNode();
					return pn1.getOorder() > pn2.getOorder() ? 1 : -1;
				}
				return 0;
			}
		});
		return this;
	}

	@Override
	public InitiateItems clone() {
		return (InitiateItems) super.clone();
	}

	private static final long serialVersionUID = 5892570280414976017L;
}
