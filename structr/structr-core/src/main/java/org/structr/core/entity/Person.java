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

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Person extends PrincipalImpl {

	static {

		EntityContext.registerPropertySet(Person.class, PropertyView.All, Key.values());

		// public properties
		EntityContext.registerPropertySet(Person.class, PropertyView.Public, Key.salutation, Key.firstName, Key.middleNameOrInitial, Key.lastName);

	}

	//~--- constant enums -------------------------------------------------

	public static enum Key implements PropertyKey {

		salutation, firstName, middleNameOrInitial, lastName, email, email2, phoneNumber1, phoneNumber2, faxNumber1, faxNumber2, street, zipCode, city, state, country, birthday, gender,
		newsletter

	}

	//~--- get methods ----------------------------------------------------

	public String getFirstName() {

		return getStringProperty(Key.firstName);

	}

	public String getLastName() {

		return getStringProperty(Key.lastName);

	}

	public String getSalutation() {

		return getStringProperty(Key.salutation);

	}

	public String getMiddleNameOrInitial() {

		return getStringProperty(Key.middleNameOrInitial);

	}

	public String getEmail() {

		return getStringProperty(Key.email);

	}

	public String getEmail2() {

		return getStringProperty(Key.email2);

	}

	public String getPhoneNumber1() {

		return getStringProperty(Key.phoneNumber1);

	}

	public String getPhoneNumber2() {

		return getStringProperty(Key.phoneNumber2);

	}

	public String getFaxNumber1() {

		return getStringProperty(Key.faxNumber1);

	}

	public String getFaxNumber2() {

		return getStringProperty(Key.faxNumber2);

	}

	public String getStreet() {

		return getStringProperty(Key.street);

	}

	public String getZipCode() {

		return getStringProperty(Key.zipCode);

	}

	public String getState() {

		return getStringProperty(Key.state);

	}

	public String getCountry() {

		return getStringProperty(Key.country);

	}

	public String getCity() {

		return getStringProperty(Key.city);

	}

	public boolean getNewsletter() {

		return getBooleanProperty(Key.newsletter);

	}

	public Date getBirthday() {

		return getDateProperty(Key.birthday);

	}

	public String getGender() {

		return getStringProperty(Key.gender);

	}

	//~--- set methods ----------------------------------------------------

	public void setFirstName(final String firstName) throws FrameworkException {

		setProperty(Key.firstName, firstName);

		String lastName = ((getLastName() != null) &&!(getLastName().isEmpty()))
				  ? getLastName()
				  : "";

		setName(lastName + ", " + firstName);

	}

	public void setLastName(final String lastName) throws FrameworkException {

		setProperty(Key.lastName, lastName);

		String firstName = ((getFirstName() != null) &&!(getFirstName().isEmpty()))
				   ? getFirstName()
				   : "";

		setProperty(AbstractNode.Key.name, lastName + ", " + firstName);

	}

	@Override
	public void setName(final String name) throws FrameworkException {

		setProperty(AbstractNode.Key.name, name);

	}

	public void setSalutation(final String salutation) throws FrameworkException {

		setProperty(Key.salutation, salutation);

	}

	public void setMiddleNameOrInitial(final String middleNameOrInitial) throws FrameworkException {

		setProperty(Key.middleNameOrInitial, middleNameOrInitial);

	}

	public void setEmail(final String email) throws FrameworkException {

		setProperty(Key.email, email);

	}

	public void setEmail2(final String email2) throws FrameworkException {

		setProperty(Key.email2, email2);

	}

	public void setPhoneNumber1(final String value) throws FrameworkException {

		setProperty(Key.phoneNumber1, value);

	}

	public void setPhoneNumber2(final String value) throws FrameworkException {

		setProperty(Key.phoneNumber2, value);

	}

	public void setFaxNumber1(final String value) throws FrameworkException {

		setProperty(Key.faxNumber1, value);

	}

	public void setFaxNumber2(final String value) throws FrameworkException {

		setProperty(Key.faxNumber2, value);

	}

	public void setStreet(final String value) throws FrameworkException {

		setProperty(Key.street, value);

	}

	public void setZipCode(final String value) throws FrameworkException {

		setProperty(Key.zipCode, value);

	}

	public void setState(final String value) throws FrameworkException {

		setProperty(Key.state, value);

	}

	public void setCountry(final String value) throws FrameworkException {

		setProperty(Key.country, value);

	}

	public void setCity(final String value) throws FrameworkException {

		setProperty(Key.city, value);

	}

	public void setNewsletter(final boolean value) throws FrameworkException {

		setProperty(Key.newsletter, value);

	}

	public void setBirthday(final Date value) throws FrameworkException {

		setProperty(Key.birthday, value);

	}

	public void setGender(final String value) throws FrameworkException {

		setProperty(Key.gender, value);

	}

}
