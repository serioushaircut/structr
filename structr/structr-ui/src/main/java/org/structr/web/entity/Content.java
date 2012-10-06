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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.search.Search;
import org.structr.core.notion.PropertyNotion;
import org.structr.web.common.PageHelper;
import org.structr.web.converter.DynamicConverter;
import org.structr.web.converter.PathsConverter;
import org.structr.web.entity.html.*;
import org.structr.web.validator.DynamicValidator;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.node.NodeService.NodeIndex;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content container
 *
 * @author axel
 */
public class Content extends AbstractNode {

	private static final Logger logger         = Logger.getLogger(Content.class.getName());
	protected static final String[] attributes = new String[] {

		UiKey.name.name(), UiKey.tag.name(), UiKey.content.name(), UiKey.contentType.name(), UiKey.size.name(), UiKey.type.name(), UiKey.paths.name(), UiKey.typeDefinitionId.name(),

		// support for microformats
		"data-key"

	};

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertyConverter(Content.class, UiKey.paths, PathsConverter.class);
		EntityContext.registerPropertySet(Content.class, PropertyView.All, attributes);
		EntityContext.registerPropertySet(Content.class, PropertyView.Public, attributes);
		EntityContext.registerPropertySet(Content.class, PropertyView.Ui, attributes);
		EntityContext.registerPropertyRelation(Content.class, UiKey.typeDefinitionId, TypeDefinition.class, RelType.IS_A, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne,
			new PropertyNotion(AbstractNode.Key.uuid));
		EntityContext.registerEntityRelation(Content.class, TypeDefinition.class, RelType.IS_A, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne,
			new PropertyNotion(AbstractNode.Key.uuid));
		EntityContext.registerEntityRelation(Content.class, Element.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Title.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Body.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Style.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Script.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, P.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Div.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H1.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H2.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H3.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H4.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H5.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, H6.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, A.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Em.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Strong.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Small.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, S.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Cite.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, G.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Dfn.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Abbr.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Time.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Code.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Var.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Samp.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Kbd.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Sub.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Sup.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, I.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, B.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, U.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Mark.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Ruby.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Rt.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Rp.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Bdi.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Bdo.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Content.class, Span.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(Content.class, NodeIndex.fulltext, UiKey.values());
		EntityContext.registerSearchablePropertySet(Content.class, NodeIndex.keyword, UiKey.values());
		EntityContext.registerPropertyValidator(Content.class, UiKey.content, new DynamicValidator("content"));
		EntityContext.registerPropertyConverter(Content.class, UiKey.content, DynamicConverter.class);

	}

	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey {

		name, tag, content, contentType, size, type, paths, dataKey, typeDefinitionId

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Do necessary updates on all containing pages
	 *
	 * @throws FrameworkException
	 */
	private void updatePages(SecurityContext securityContext) throws FrameworkException {

		List<Page> pages = PageHelper.getPages(securityContext, this);

		for (Page page : pages) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();

		}

	}

	@Override
	public void afterModification(SecurityContext securityContext) {

		try {

			updatePages(securityContext);

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Updating page versions failed", ex);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public java.lang.Object getPropertyForIndexing(final String key) {

		if (key.equals(Content.UiKey.content.name())) {

			String value = getStringProperty(key);

			if (value != null) {

				return Search.escapeForLucene(value);
			}

		}

		return getProperty(key);

	}

	public Element getParent() {

		// FIXME: this is an ugly hack :)
		return (Element) getRelToParent().getStartNode();
	}

	public AbstractRelationship getRelToParent() {

		// FIXME: this is an ugly hack :)
		return getRelationships(RelType.CONTAINS, Direction.INCOMING).get(0);
	}

	public Component getParentComponent() {

		for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

			String componentId = in.getStringProperty(Component.Key.componentId.name());

			if (componentId != null) {

				AbstractNode node = in.getStartNode();

				while (!(node instanceof Page)) {

					if (node instanceof Component) {

						return (Component) node;
					}

					node = node.getIncomingRelationships(RelType.CONTAINS).get(0).getStartNode();

				}

			}

		}

		return null;

	}

	public String getPropertyWithVariableReplacement(HttpServletRequest request, AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, String key) {

		if (securityContext.getRequest() == null) {

			securityContext.setRequest(request);
		}

		return HtmlElement.replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getStringProperty(key));

	}

	public TypeDefinition getTypeDefinition() {

		return (TypeDefinition) getRelatedNode(TypeDefinition.class);

	}

}
