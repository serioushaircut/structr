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



package org.structr.web.entity.html;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.web.common.Function;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.Element;
import org.structr.web.entity.Page;
import org.structr.web.servlet.HtmlServlet;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringEscapeUtils;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.web.common.PageHelper;
import org.structr.web.entity.Component;
import org.structr.web.entity.PageElement;
import org.structr.web.entity.RemoteView;
import org.structr.web.entity.View;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public abstract class HtmlElement extends PageElement implements Element {

	protected static final String[] htmlAttributes = new String[] {

		// Core attributes
		"accesskey", "class", "contenteditable", "contextmenu", "dir", "draggable", "dropzone", "hidden", "id", "lang", "spellcheck", "style", "tabindex", "title",

		// Event-handler attributes
		"onabort", "onblur", "oncanplay", "oncanplaythrough", "onchange", "onclick", "oncontextmenu", "ondblclick", "ondrag", "ondragend", "ondragenter", "ondragleave", "ondragover",
		"ondragstart", "ondrop", "ondurationchange", "onemptied", "onended", "onerror", "onfocus", "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload", "onloadeddata",
		"onloadedmetadata", "onloadstart", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onpause", "onplay", "onplaying", "onprogress",
		"onratechange", "onreadystatechange", "onreset", "onscroll", "onseeked", "onseeking", "onselect", "onshow", "onstalled", "onsubmit", "onsuspend", "ontimeupdate", "onvolumechange",
		"onwaiting",
	};
	private static final Logger logger                                             = Logger.getLogger(HtmlElement.class.getName());
	private static final java.util.Map<String, Function<String, String>> functions = new LinkedHashMap<String, Function<String, String>>();
	private static final ThreadLocalMatcher threadLocalFunctionMatcher             = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");
	private static final ThreadLocalMatcher threadLocalTemplateMatcher             = new ThreadLocalMatcher("\\$\\{[^}]*\\}");

	//~--- static initializers --------------------------------------------

	static {

		functions.put("md5", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? DigestUtils.md5Hex(s[0])
				       : null;

			}

		});
		functions.put("upper", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toUpperCase()
				       : null;

			}

		});
		functions.put("lower", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toLowerCase()
				       : null;

			}

		});
		functions.put("capitalize", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? StringUtils.capitalize(s[0])
				       : null;

			}

		});
		functions.put("if", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length < 3) {

					return "";
				}

				if (s[0].equals("true")) {

					return s[1];
				} else {

					return s[2];
				}

			}

		});
		functions.put("equal", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				logger.log(Level.FINE, "Length: {0}", s.length);

				if (s.length < 2) {

					return "true";
				}

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[] { s[0], s[1] });

				return s[0].equals(s[1])
				       ? "true"
				       : "false";

			}

		});
		functions.put("add", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				int result = 0;

				if (s != null) {

					for (int i = 0; i < s.length; i++) {

						try {

							result += Integer.parseInt(s[i]);

						} catch (Throwable t) {}

					}

				}

				return new Integer(result).toString();

			}

		});
		functions.put("active", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length == 0) {

					return "";
				}

				String data = this.dataId.get();
				String page = this.pageId.get();

				if (data != null && page != null) {

					if (data.equals(page)) {

						// return first argument if condition is true
						return s[0];
					} else if (s.length > 1) {

						// return second argument if condition is false and second argument exists
						return s[1];
					}

				}

				return "";

			}

		});

	}

	static {

		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.All, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Public, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Ui, PropertyView.Html, htmlAttributes);
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.fulltext, UiKey.values());
		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.keyword, UiKey.values());
		
		EntityContext.registerPropertyRelation(HtmlElement.class, UiKey.parents, HtmlElement.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		
		EntityContext.registerEntityRelation(HtmlElement.class, Component.class,	RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(HtmlElement.class, View.class,		RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(HtmlElement.class, RemoteView.class,	RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
	}

	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey{ name, tag, path, parents, paths }

	//~--- methods --------------------------------------------------------

	public boolean avoidWhitespace() {

		return false;

	}

	;

	//~--- methods --------------------------------------------------------

	public static String convertValueForHtml(java.lang.Object value) {

		if (value != null) {

			// TODO: do more intelligent conversion here
			return value.toString();
		}

		return null;

	}

	// ----- static methods -----
	public static String replaceVariables(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String rawValue) {

		String value = null;

		if ((rawValue != null) && (rawValue instanceof String)) {

			value = (String) rawValue;

			// re-use matcher from previous calls
			Matcher matcher = threadLocalTemplateMatcher.get();

			matcher.reset(value);

			while (matcher.find()) {

				String group  = matcher.group();
				String source = group.substring(2, group.length() - 1);

				// fetch referenced property
				String partValue = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, source);

				if (partValue != null) {

					value = value.replace(group, partValue);
				}

			}

		}

		return value;

	}

	public static String extractFunctions(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String source) {

		// re-use matcher from previous calls
		Matcher functionMatcher = threadLocalFunctionMatcher.get();

		functionMatcher.reset(source);

		if (functionMatcher.matches()) {

			String viewComponentId            = viewComponent != null
				? viewComponent.getStringProperty(AbstractNode.Key.uuid)
				: null;
			String functionGroup              = functionMatcher.group(1);
			String parameter                  = functionMatcher.group(2);
			String functionName               = functionGroup.substring(0, functionGroup.length());
			Function<String, String> function = functions.get(functionName);

			if (function != null) {

				// store thread "state" in function
				function.setDataId(viewComponentId);
				function.setPageId(pageId);

				if (parameter.contains(",")) {

					String[] parameters = split(parameter);
					String[] results    = new String[parameters.length];

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						results[i] = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, StringUtils.strip(parameters[i]));
					}

					return function.apply(results);

				} else {

					String result = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, StringUtils.strip(parameter));

					return function.apply(new String[] { result });

				}
			}

		}

		// if any of the following conditions match, the literal source value is returned
		if (StringUtils.isNotBlank(source) && StringUtils.isNumeric(source)) {

			// return numeric value
			return source;
		} else if (source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);
		} else if (source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);
		} else {

			// return property key
			return convertValueForHtml(getReferencedProperty(securityContext, page, startNode, pageId, componentId, viewComponent, source));
		}
	}

	public static String[] split(String source) {

		ArrayList<String> tokens   = new ArrayList<String>(20);
		boolean inDoubleQuotes     = false;
		boolean inSingleQuotes     = false;
		int len                    = source.length();
		int level                  = 0;
		StringBuilder currentToken = new StringBuilder(len);

		for (int i = 0; i < len; i++) {

			char c = source.charAt(i);

			// do not strip away separators in nested functions!
			if ((level != 0) || (c != ',')) {

				currentToken.append(c);
			}

			switch (c) {

				case '(' :
					level++;

					break;

				case ')' :
					level--;

					break;

				case '"' :
					if (inDoubleQuotes) {

						inDoubleQuotes = false;

						level--;

					} else {

						inDoubleQuotes = true;

						level++;

					}

					break;

				case '\'' :
					if (inSingleQuotes) {

						inSingleQuotes = false;

						level--;

					} else {

						inSingleQuotes = true;

						level++;

					}

					break;

				case ',' :
					if (level == 0) {

						tokens.add(currentToken.toString().trim());
						currentToken.setLength(0);

					}

					break;

			}

		}

		if (currentToken.length() > 0) {

			tokens.add(currentToken.toString().trim());
		}

		return tokens.toArray(new String[0]);

	}

	//~--- get methods ----------------------------------------------------

	public String[] getHtmlAttributes() {

		return htmlAttributes;

	}

	public String getPropertyWithVariableReplacement(AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, String key) {

		return replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getStringProperty(key));

	}

	public static java.lang.Object getReferencedProperty(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String refKey) {

		AbstractNode node   = startNode;
		String[] parts      = refKey.split("[\\.]+");
		String referenceKey = parts[parts.length - 1];

		// walk through template parts
		for (int i = 0; (i < parts.length) && (node != null); i++) {

			String part = parts[i];

			// special keyword "request"
			if ("request".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringUtils.defaultString(request.getParameter(referenceKey));
				}

			}

			// special keyword "component"
			if ("component".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, componentId);

				continue;

			}

			// special keyword "resource"
			if ("resource".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, pageId);

				continue;

			}

			// special keyword "page"
			if ("page".equals(part.toLowerCase())) {

				node = page;

				continue;

			}

			// special keyword "link"
			if ("link".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.LINK, Direction.OUTGOING)) {

					node = rel.getEndNode();

					break;

				}

				continue;

			}

			// special keyword "parent"
			if ("parent".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

					if (rel.getProperty(pageId) != null) {

						node = rel.getStartNode();

						break;

					}

				}

				continue;

			}

			// special keyword "owner"
			if ("owner".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.OWNS, Direction.INCOMING)) {

					node = rel.getStartNode();

					break;

				}

				continue;

			}

			// special keyword "data"
			if ("data".equals(part.toLowerCase())) {

				node = viewComponent;

				continue;

			}

			// special keyword "root": Find containing page
			if ("root".equals(part.toLowerCase())) {

				List<Page> pages = PageHelper.getPages(securityContext, viewComponent);

				if (pages.isEmpty()) {

					continue;
				}

				node = pages.get(0);

				continue;

			}

			// special keyword "result_size"
			if ("result_size".equals(part.toLowerCase())) {

				Set<Page> pages = HtmlServlet.getResultPages(securityContext, (Page) page);

				if (!pages.isEmpty()) {

					return pages.size();
				}

				return 0;

			}
			
			// special keyword "rest_result"
			if ("rest_result".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.defaultString((String) request.getAttribute(HtmlServlet.REST_RESPONSE)), "\n", ""));
				}

				return 0;

			}
		}

		if (node != null) {

			return node.getProperty(referenceKey);
		}

		return null;

	}

	public boolean isVoidElement() {

		return false;

	}
	
}
