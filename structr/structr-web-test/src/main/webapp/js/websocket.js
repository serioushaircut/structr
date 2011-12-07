/* 
 *  Copyright (C) 2011 Axel Morgner
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


var ws;

function connect() {

    try {

        var host = document.location.host;
        if ('WebSocket' in window) {
            ws = new WebSocket('ws://' + host + '/structr-web-test/ws/', 'structr');
        } else if ('MozWebSocket' in window) {
            ws = new MozWebSocket('ws://localhost:8080/structr-web-test/ws/', 'structr');
        } else {
            alert('Your browser doesn\'t support WebSocket. Go home!');
            return;
        }

        log('State: ' + ws.readyState);

        ws.onopen = function() {
            log('Open: ' + ws.readyState);
        }

        ws.onmessage = function(message) {

            log('Message received: ' + message);

            var data = $.parseJSON(message.data);

            console.log(data);

            if (data.command == 'CREATE') {

                if (data.type == 'user') {

                    data.command = null;
                    var user = data;
                    groupId = user.groupId;
                    if (groupId) appendUserElement(user, groupId);
                    appendUserElement(user);
                    disable($('.' + groupId + '_ .delete_icon')[0]);
                    if (buttonClicked) enable(clickedButton);

                } else if (data.type == 'group') {

                    appendGroupElement(data);
                    if (buttonClicked) enable(buttonClicked);
                    
                }
                else {
                    
                    appendEntityElement(data, parentElement);
                    if (buttonClicked) enable(buttonClicked);

                }

            } else if (data.command == 'DELETE') {

                if (data.type = 'User') {
                    var elementSelector = '.' + data.id + '_';
                    $(elementSelector).hide('blind', {
                        direction: "vertical"
                    }, 200);
                    $(elementSelector).remove();
                    refreshIframes();
                    if (buttonClicked) enable(clickedButton);
                    //if (callback) callback();
                }

            } else if (data.command == 'UPDATE') {

            } else {
                console.log('Unknown command: ' + data.command);
            }


        }

        ws.onclose = function() {
            log('Close: ' + ws.readyState);
        }

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function send(text) {

    if (!text) {
        log('No text to send!');
        return;
    }

    try {

        ws.send(text);
        log('Sent: ' + text);

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function log(msg) {
    $("#log").append("<br />" + msg);
}
