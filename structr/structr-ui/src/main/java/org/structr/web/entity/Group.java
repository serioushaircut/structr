/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.neo4j.graphdb.Direction;

import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractNode implements Principal {

	static {

		EntityContext.registerPropertySet(Group.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(Group.class, PropertyView.Ui, Key.values());

		// EntityContext.registerPropertyRelation(Group.class, Key.users,        Principal.class, RelType.HAS_CHILD, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Group.class, User.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {}

	//~--- methods --------------------------------------------------------

	@Override
	public void grant(Permission permission, AccessControllable obj) {

		obj.getSecurityRelationship(this).addPermission(permission);

	}

	@Override
	public void revoke(Permission permission, AccessControllable obj) {

		obj.getSecurityRelationship(this).removePermission(permission);

	}

	@Override
	public void block() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	@Override
	public List<Principal> getParents() {

		List<Principal> parents               = new LinkedList<Principal>();
		List<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getEndNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	@Override
	public Boolean getBlocked() {

		return (Boolean) getProperty(Principal.Key.blocked);

	}

	@Override
	public Boolean isBlocked() {

		return getBlocked();

	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setBlocked(Boolean blocked) throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

}
