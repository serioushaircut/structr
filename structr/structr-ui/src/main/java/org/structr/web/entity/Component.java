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
import org.neo4j.graphdb.Relationship;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.NodeService;
import org.structr.web.common.PageHelper;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a component. A component is an assembly of elements
 *
 * @author axel
 */
public class Component extends AbstractNode implements Element {

	private static final int MAX_DEPTH = 10;
	private static final Logger logger = Logger.getLogger(Component.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Component.class, PropertyView.All, UiKey.values());
		EntityContext.registerPropertySet(Component.class, PropertyView.Public, UiKey.values());
		EntityContext.registerPropertySet(Component.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerEntityRelation(Component.class, Page.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Component.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.keyword.name(), UiKey.values());

	}

	//~--- fields ---------------------------------------------------------

	private Map<String, AbstractNode> contentNodes = new WeakHashMap<String, AbstractNode>();
	private Set<String> subTypes                   = new LinkedHashSet<String>();

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ componentId, pageId }

	public enum UiKey implements PropertyKey{ type, name, kind, paths }

	//~--- methods --------------------------------------------------------

	@Override
	public void onNodeInstantiation() {

		collectProperties(this, getStringProperty(AbstractNode.Key.uuid), 0, null);

	}

	@Override
	public void onNodeDeletion() {

		try {

			Command deleteCommand = Services.command(securityContext, DeleteNodeCommand.class);
			boolean cascade       = true;

			for (AbstractNode contentNode : contentNodes.values()) {

				deleteCommand.execute(contentNode, cascade);
			}

			// delete linked components
//                      for(AbstractRelationship rel : getRelationships(RelType.DATA, Direction.INCOMING)) {
//                              deleteCommand.execute(rel.getStartNode());
//                      }

		} catch (Throwable t) {

			logger.log(Level.SEVERE, "Exception while deleting nested Components: {0}", t.getMessage());

		}

	}

	// ----- private methods ----
	private void collectProperties(AbstractNode startNode, String componentId, int depth, AbstractRelationship ref) {

		if (depth > MAX_DEPTH) {

			return;
		}

		if (ref != null) {

			if (componentId.equals(ref.getStringProperty(Key.componentId.name()))) {

				String dataKey = startNode.getStringProperty("data-key");

				if (dataKey != null) {

					contentNodes.put(dataKey, startNode);

					return;

				}

			}

		}

		// collection of properties must not depend on page
		for (AbstractRelationship rel : PageHelper.getChildRelationships(null, startNode, null, componentId)) {

			AbstractNode endNode = rel.getEndNode();

			if (endNode == null) {

				continue;
			}

			if (endNode instanceof Component) {

				String subType = endNode.getStringProperty(Component.UiKey.kind);

				if (subType != null) {

					subTypes.add(subType);
				}

			} else {

				collectProperties(endNode, componentId, depth + 1, rel);
			}

		}

	}

	private void collectChildren(List<Component> children, AbstractNode startNode, String componentId, int depth, AbstractRelationship ref) {

		if (depth > MAX_DEPTH) {

			return;
		}

		if (ref != null) {

			if (startNode instanceof Component) {

				children.add((Component) startNode);

				return;

			}

		}

		// collection of properties must not depend on page
		for (AbstractRelationship rel : PageHelper.getChildRelationships(null, startNode, null, componentId)) {

			AbstractNode endNode = rel.getEndNode();

			if (endNode == null) {

				continue;
			}

			collectChildren(children, endNode, componentId, depth + 1, rel);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<String> getPropertyKeys(final String propertyView) {

		Set<String> augmentedPropertyKeys = new LinkedHashSet<String>();

		for (String key : super.getPropertyKeys(propertyView)) {

			augmentedPropertyKeys.add(key);
		}

		augmentedPropertyKeys.addAll(contentNodes.keySet());

		for (String subType : subTypes) {

			augmentedPropertyKeys.add(subType.toLowerCase().concat("s"));
		}

		return augmentedPropertyKeys;

	}

	@Override
	public Object getProperty(String key) {

		// try local properties first
		if (contentNodes.containsKey(key)) {

			AbstractNode node = contentNodes.get(key);

			if ((node != null) && (node != this)) {

				return node.getStringProperty("content");
			}

		} else if (subTypes.contains(EntityContext.normalizeEntityName(key))) {

			String componentId      = getStringProperty(AbstractNode.Key.uuid);
			List<Component> results = new LinkedList<Component>();

			collectChildren(results, this, componentId, 0, null);

			return results;

		}

		return super.getProperty(key);
	}

	public Map<String, AbstractNode> getContentNodes() {

		return contentNodes;

	}

	public String getComponentId() {

		for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

			String componentId = in.getStringProperty(Key.componentId.name());

			if (componentId != null) {

				return componentId;
			}

		}

		return null;

	}

	//~--- set methods ----------------------------------------------------

	// ----- public static methods -----
	@Override
	public void setProperty(String key, Object value) throws FrameworkException {

		if (contentNodes.containsKey(key)) {

			AbstractNode node = contentNodes.get(key);

			if (node != null) {

				node.setProperty("content", value);
			}

		} else {

			super.setProperty(key, value);
		}

	}

}
