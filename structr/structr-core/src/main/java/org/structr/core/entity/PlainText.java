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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class PlainText extends AbstractNode {

	static {

		EntityContext.registerPropertySet(PlainText.class, PropertyView.All, Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ content, contentType, size; }

	//~--- get methods ----------------------------------------------------

	public String getContent() {

		return getStringProperty(Key.content);

	}

	public String getContentType() {

		return getStringProperty(Key.contentType);

	}

	public String getSize() {

		return getStringProperty(Key.size);

	}

	//~--- set methods ----------------------------------------------------

	public void setContent(final String content) throws FrameworkException {

		setProperty(Key.content, content);

	}

	public void setContentType(final String contentType) throws FrameworkException {

		setProperty(Key.contentType, contentType);

	}

	public void setSize(final String size) throws FrameworkException {

		setProperty(Key.size, size);

	}

}
