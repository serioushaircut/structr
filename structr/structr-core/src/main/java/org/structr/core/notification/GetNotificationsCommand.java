/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

package org.structr.core.notification;

import java.util.List;

/**
 * Returns a list of Notifications for the given session ID when executed. This
 * command takes a single parameter, namely the session ID for which the
 * notifications should be returned. Please note that this command will return
 * any global notification as well.
 * 
 * @author Christian Morgner
 */
public class GetNotificationsCommand extends NotificationServiceCommand {

	@Override
	public Object execute(Object... parameters)
	{
		NotificationService service = (NotificationService)getArgument("service");
		List<Notification> ret = null;

		if(parameters.length == 1 && service != null && parameters[0] instanceof String) {

			String sessionId = (String)parameters[0];
			ret = service.getNotifications(sessionId);
		}

		return(ret);
	}
}