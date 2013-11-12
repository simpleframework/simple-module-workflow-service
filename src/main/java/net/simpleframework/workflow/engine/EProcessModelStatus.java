package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EProcessModelStatus {
	edit {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.edit");
		}
	},

	deploy {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.deploy");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.abort");
		}
	}
}
