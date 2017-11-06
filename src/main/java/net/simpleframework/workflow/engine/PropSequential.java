package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.participant.Participant;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class PropSequential implements IWorkflowContextAware {

	/* 保存顺序执行的参与者或已有的工作项id(前缀#) */
	static final String SEQUENTIAL = "sequential";

	public static List<?> list(final ActivityBean activity) {
		final List<Object> l = new ArrayList<>();
		for (final String str : StringUtils.split(activity.getProperties().getProperty(SEQUENTIAL),
				";")) {
			final Object o = str.charAt(0) == '#' ? wfwService.getBean(str.substring(1))
					: Participant.of(str);
			if (o != null) {
				l.add(o);
			}
		}
		return l;
	}

	public static void set(final ActivityBean activity, final List<?> l) {
		final Properties properties = activity.getProperties();
		if (l == null) {
			properties.remove(SEQUENTIAL);
			return;
		}

		final StringBuilder sb = new StringBuilder();
		for (final Object o : l) {
			if (sb.length() > 0) {
				sb.append(";");
			}
			if (o instanceof WorkitemBean) {
				sb.append("#").append(((WorkitemBean) o).getId());
			} else {
				sb.append(o);
			}
		}

		if (sb.length() > 0) {
			properties.put(SEQUENTIAL, sb.toString());
		} else {
			properties.remove(SEQUENTIAL);
		}
	}

	public static void push(final ActivityBean activity, final WorkitemBean workitem) {
		String nstr = "#" + workitem.getId();
		final Properties properties = activity.getProperties();
		final String ostr = properties.getProperty(SEQUENTIAL);
		if (StringUtils.hasText(ostr)) {
			nstr += ";" + ostr;
		}
		properties.setProperty(SEQUENTIAL, nstr);
	}
}