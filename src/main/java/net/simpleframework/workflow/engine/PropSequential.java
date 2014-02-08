package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.participant.Participant;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class PropSequential {

	private static final String KEY = "sequential_participants";

	public static Collection<Participant> list(final ActivityBean activity) {
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		final String[] pArr = StringUtils.split(activity.getProperties().getProperty(KEY), ";");
		if (pArr != null) {
			Participant participant;
			for (final String str : pArr) {
				if ((participant = Participant.of(str)) != null) {
					participants.add(participant);
				}
			}
		}
		return participants;
	}

	public static void set(final ActivityBean activity, final Iterator<Participant> it) {
		if (it == null) {
			return;
		}

		final StringBuilder sb = new StringBuilder();
		int i = 0;
		while (it.hasNext()) {
			final Participant participant = it.next();
			if (i++ > 0) {
				sb.append(";");
			}
			sb.append(participant);
		}

		final Properties properties = activity.getProperties();
		if (sb.length() > 0) {
			properties.put(KEY, sb.toString());
		} else {
			properties.remove(KEY);
		}
	}

	public static void push(final ActivityBean activity, final Participant participant) {
		String nstr = participant.toString();
		final Properties properties = activity.getProperties();
		final String ostr = properties.getProperty(KEY);
		if (StringUtils.hasText(ostr)) {
			nstr += ";" + ostr;
		}
		properties.setProperty(KEY, nstr);
	}
}