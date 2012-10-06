/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity;


//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Relationship;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.node.NodeService.RelationshipIndex;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class GenericRelationship extends AbstractRelationship {

	static {

		EntityContext.registerPropertySet(GenericRelationship.class, PropertyView.All, Key.values());

		EntityContext.registerSearchableProperty(GenericRelationship.class, RelationshipIndex.rel_uuid, Key.uuid);
		
		// register start and end node for ui view
		EntityContext.registerPropertySet(GenericRelationship.class, PropertyView.Ui, UiKey.values());
		
	}

	public enum HiddenKey implements PropertyKey {
		cascadeDelete;
	}
	
	public enum UiKey implements PropertyKey {
		startNodeId, endNodeId
	}
	
	//~--- constructors ---------------------------------------------------

	public GenericRelationship() {}

	public GenericRelationship(SecurityContext securityContext, Relationship dbRelationship) {
		init(securityContext, dbRelationship);
	}

	@Override
	public PropertyKey getStartNodeIdKey() {
		return UiKey.startNodeId;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return UiKey.endNodeId;
	}
		
	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {
		
		Set<PropertyKey> keys = new LinkedHashSet<PropertyKey>();
		
		keys.add(UiKey.startNodeId);
		keys.add(UiKey.endNodeId);
		
		if (dbRelationship != null) {
			
			for (String key : dbRelationship.getPropertyKeys()) {
				keys.add(Key.valueOf(key));
			}
		}
		
		return keys;
	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, Map<String, Object> properties) throws FrameworkException {
		return true;
	}
}
