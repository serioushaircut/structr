/*
 *  Copyright (C) 2010-2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.error;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import org.structr.common.PropertyKey;

/**
 * A buffer that collects error tokens to allow for i18n
 * and human readable output later.
 *
 * @author Christian Morgner
 */
public class ErrorBuffer {

	private Map<String, Map<PropertyKey, Set<ErrorToken>>> tokens = new LinkedHashMap<String, Map<PropertyKey, Set<ErrorToken>>>();

	public void add(String type, ErrorToken msg) {
		getTokenSet(type, msg.getKey()).add(msg);
	}

	public boolean hasError() {
		return !tokens.isEmpty();
	}

	public Map<String, Map<PropertyKey, Set<ErrorToken>>> getErrorTokens() {
		return tokens;
	}

	// ----- private methods -----
	private Set<ErrorToken> getTokenSet(String type, PropertyKey key) {

		Map<PropertyKey, Set<ErrorToken>> map = getTypeSet(type);
		Set<ErrorToken> list = map.get(key);
		if (list == null) {
			list = new LinkedHashSet<ErrorToken>();
			map.put(key, list);
		}

		return list;
	}

	private Map<PropertyKey, Set<ErrorToken>> getTypeSet(String type) {

		Map<PropertyKey, Set<ErrorToken>> map = tokens.get(type);
		if (map == null) {
			map = new LinkedHashMap<PropertyKey, Set<ErrorToken>>();
			tokens.put(type, map);
		}

		return map;
	}
}
