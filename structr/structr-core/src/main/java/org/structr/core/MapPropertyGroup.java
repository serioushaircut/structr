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



package org.structr.core;

import org.structr.common.PropertyKey;
import org.structr.core.node.NodeAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode.Key;

//~--- classes ----------------------------------------------------------------

/**
 * A property group that uses a Map.
 *
 * @author Christian Morgner
 */
public class MapPropertyGroup implements PropertyGroup {

	private PropertyKey[] propertyKeys = null;

	//~--- constructors ---------------------------------------------------

	public MapPropertyGroup(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Object getGroupedProperties(GraphObject source) {

		Map<PropertyKey, Object> groupedProperties = new LinkedHashMap<PropertyKey, Object>();

		for (PropertyKey key : propertyKeys) {

			groupedProperties.put(key, source.getProperty(key));

		}

		return groupedProperties;
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) throws FrameworkException {

		if (source == null) {

			for (PropertyKey key : propertyKeys) {

				destination.setProperty(key, null);

			}

		} else if (source instanceof Map) {

			for (Entry<String, Object> attr : ((Map<String, Object>) source).entrySet()) {

				String key   = attr.getKey();
				Object value = attr.getValue();

				if (value instanceof PropertySet) {

					setGroupedProperties(value, destination);

				} else {

					destination.setProperty(Key.valueOf(key), value);

				}

			}

		} else if (source instanceof PropertySet) {

			for (NodeAttribute attr : ((PropertySet) source).getAttributes()) {

				String key = attr.getKey();
				Object value	= attr.getValue();

				if (value instanceof PropertySet) {

					// recursive put/post
					setGroupedProperties(value, destination);
				} else {

					destination.setProperty(Key.valueOf(key), value);

				}

			}

		}
	}
}
