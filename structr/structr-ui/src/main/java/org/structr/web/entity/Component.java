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

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.node.NodeService.NodeIndex;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a component. A component is an assembly of elements
 *
 * @author axel
 */
public class Component extends AbstractNode implements Element {

	private static final int MAX_DEPTH                          = 10;
	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	private static final Logger logger                          = Logger.getLogger(Component.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Component.class, PropertyView.All, UiKey.values());
		EntityContext.registerPropertySet(Component.class, PropertyView.Public, UiKey.values());
		EntityContext.registerPropertySet(Component.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerEntityRelation(Component.class, Page.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Component.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(Component.class, NodeIndex.fulltext, UiKey.values());
		EntityContext.registerSearchablePropertySet(Component.class, NodeIndex.keyword, UiKey.values());

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
		for (AbstractRelationship rel : getChildRelationships(null, startNode, null, componentId)) {

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
		for (AbstractRelationship rel : getChildRelationships(null, startNode, null, componentId)) {

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

//      public String getPageId() {
//
//              for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {
//
//                      String pageId = in.getStringProperty(Key.pageId.name());
//
//                      if (pageId != null) {
//
//                              return pageId;
//
//                      }
//
//              }
//
//              return null;
//      }
	public static List<AbstractRelationship> getChildRelationships(final HttpServletRequest request, final AbstractNode node, final String pageId, final String componentId) {

		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		for (AbstractRelationship abstractRelationship : node.getOutgoingRelationships(RelType.CONTAINS)) {

			Relationship rel = abstractRelationship.getRelationship();

			if ((pageId == null) || ((pageId != null) && rel.hasProperty(pageId)) || rel.hasProperty("*")) {

				AbstractNode endNode = abstractRelationship.getEndNode();

				if (endNode == null || (endNode instanceof Component && !isVisible(request, endNode, abstractRelationship, componentId))) {

					continue;

				}

				if ((componentId != null) && ((endNode instanceof Content) || (endNode instanceof Component))) {
					
					// Add content nodes if they don't have the data-key property set
					if (endNode instanceof Content && endNode.getStringProperty("data-key") == null) {
						
						rels.add(abstractRelationship);
						
					// Add content or component nodes if rel's componentId attribute matches
					} else if (componentId.equals(abstractRelationship.getStringProperty(Key.componentId.name()))) {

						rels.add(abstractRelationship);

					}
						
				} else {

					rels.add(abstractRelationship);

				}

			}

		}

		if (pageId != null) {

			Collections.sort(rels, new Comparator<AbstractRelationship>() {

				@Override
				public int compare(AbstractRelationship o1, AbstractRelationship o2) {

					Long pos1 = getPosition(o1, pageId);
					Long pos2 = getPosition(o2, pageId);

					return pos1.compareTo(pos2);
				}

			});

		}

		return rels;
	}

	public static long getPosition(final AbstractRelationship relationship, final String pageId) {

//              final Relationship rel = relationship.getRelationship();
		long position = 0;

		try {

//                      Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			Object prop = null;
			final String key;

			// "*" is a wildcard for "matches any page id"
			// TOOD: use pattern matching here?
			if (relationship.getProperty("*") != null) {

				prop = relationship.getProperty("*");
				key  = "*";

			} else if (relationship.getProperty(pageId) != null) {

				prop = relationship.getLongProperty(pageId);
				key  = pageId;

			} else {

				key = null;

			}

			if ((key != null) && (prop != null)) {

				if (prop instanceof Long) {

					position = (Long) prop;

				} else if (prop instanceof Integer) {

					position = ((Integer) prop).longValue();

				} else if (prop instanceof String) {

					position = Long.parseLong((String) prop);

				} else {

					throw new java.lang.IllegalArgumentException("Expected Long, Integer or String");

				}

//
//                              Integer originalPos = position;
//
//                              // find free slot
//                              while (sortedRelationshipMap.containsKey(position)) {
//
//                                      position++;
//
//                              }
//
//                              sortedRelationshipMap.put(position, rel);
//
//                              if (originalPos != position) {
//
//                                      final Integer newPos = position;
//
//                                      Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {
//
//                                              @Override
//                                              public Object execute() throws FrameworkException {
//
//                                                      rel.setProperty(key, newPos);
//
//                                                      return null;
//                                              }
//
//                                      });
//
//                              }

			}
		} catch (Throwable t) {

			// fail fast, no check
			logger.log(Level.SEVERE, "While reading property " + pageId, t);
		}

		return position;
	}

	private static boolean hasAttribute(HttpServletRequest request, String key) {
		return (key != null) && (request.getAttribute(key) != null);
	}

	// ----- public static methods -----
	public static boolean isVisible(HttpServletRequest request, AbstractNode node, AbstractRelationship incomingRelationship, String parentComponentId) {

		if (request == null) {

			return true;

		}

		// check if component is in "list" mode
		if (node instanceof Component) {

			Boolean requestContainsUuidsValue = (Boolean) request.getAttribute(REQUEST_CONTAINS_UUID_IDENTIFIER);
			boolean requestContainsUuids      = false;

			if (requestContainsUuidsValue != null) {

				requestContainsUuids = requestContainsUuidsValue.booleanValue();

			}

			String componentId = node.getStringProperty(AbstractNode.Key.uuid);

			// new default behaviour: make all components visible
			// only filter if uuids are present in the request URI
			// and we are examining a top-level component (children
			// of filtered components are not reached anyway)
			if (requestContainsUuids) {

				if (hasAttribute(request, componentId) || (parentComponentId != null)) {

					return true;

				}

				return false;

			} else {

				return true;

			}

		}

		// we can return false here by default, as we're only examining nodes of type Component
		return false;
	}

	//~--- set methods ----------------------------------------------------

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
