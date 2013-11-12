package net.simpleframework.workflow.engine;

import java.util.ArrayList;

import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class InitiateItems extends ArrayList<InitiateItem> {
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

	@Override
	public InitiateItems clone() {
		return (InitiateItems) super.clone();
	}

	private static final long serialVersionUID = 5892570280414976017L;
}
