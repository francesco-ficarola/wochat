const SUCCESS = 'success';
const FAIL = 'fail';
const USERS_ADD = "users_add";
const USERS_REM = "users_rem";
const GET_CONN_STATUS = 'get_conn_status';
const NEW_USER_STATUS = 'new_user_status';
const REG_USER_STATUS = 'reg_user_status';
const JOIN_CHAT = 'join_chat';

var service_location;
var socket;
var div_chat_log;
var div_users_list;


$(document).ready(function() {
	div_chat_log = $('#div-chat-log');
	div_users_list = $('#div-users-list');

	if(!window.WebSocket) {
	
		$(document.body).html('<p style="color:red; text-align:center; font-weight:bold;">Error: Your browser or device does not support Web Socket. Try Google Chrome!</p>');
	
	} else {
		// Web Socket connection
		var url_domain = document.domain;
		service_location = 'ws://' + url_domain + ':8080/chat';
		connectToServer();
	
		// JQuery events functions
		$('#user-input-box').focus(function() {
			$(this).css('background', '#ceffe9');
		});

		$('#user-input-box').focusout(function() {
			$(this).css('background', '#ffffff');
		});
		
	}
});


function connectToServer() {
	try {
		socket = window['MozWebSocket'] ? new MozWebSocket(service_location) : new WebSocket(service_location);
		socket.onmessage = onMessageReceived;
		socket.onopen = onSocketOpen;
		socket.onclose = onSocketClose;
	} catch(exception) {
		chatLog('Error: ' + exception, 'p-warning');
	}
}


function onSocketOpen(e) {
	console.log('Web Socket opened.');
	var message = '{ "request": "' + GET_CONN_STATUS + '" }';
	sendMessage(message);
}


function onSocketClose(e) {
	console.log('Web Socket closed.');
}


function onMessageReceived(e) {
	console.log('Message received.');
	var msg = e.data;
	console.log(msg);
	try {
		var jsonMsg = $.parseJSON(msg);
		
		// Reponse to the previous request
		if(jsonMsg.response) {
			if(jsonMsg.response === NEW_USER_STATUS) {
				var registration_form = '\
							<form name="form-send-username" id="form-send-username" action="#">\
								<input type="text" name="user-input-box" id="user-input-box" placeholder="Your username here..." maxlength="20" />\
								<input type="submit" name="user-submit" id="user-submit" value="Connect" style="width:60px;" onfocus="this.blur();" />\
							</form>\
							';
				$('#p-send-username').hide().html(registration_form).fadeIn('slow');
				$('#form-send-username').on('submit', registrationFormListener);
			}
			
			else
			
			if(jsonMsg.response === REG_USER_STATUS) {
				var user_status_message = 'You are ' + jsonMsg.data.username;
				$('#p-send-username').hide().html(user_status_message).fadeIn('slow');
				
				// Sending message to join the chat
				var join_chat_message = '{ "request": "' + JOIN_CHAT + '", "data": { "username": "' + jsonMsg.data.username + '" } }';
				sendMessage(join_chat_message);
			}
			
			else
			
			if(jsonMsg.response === USERS_ADD) {
				var usersList = jsonMsg.data;
				for(var i=0; i<usersList.length; i++) {
					div_users_list.append('<p class="p-users-list" id="' + usersList[i].id + '">' + usersList[i].username + '</p>');
				}
			}
			
			else
			
			if(jsonMsg.response === USERS_REM) {
				var usersList = jsonMsg.data;
				for(var i=0; i<usersList.length; i++) {
					$('#' + usersList[i].id).remove();
				}
			}
		}
		
		// Other messages
		else {
			console.warn('No supported message.');
		}
		
	} catch(exception) {
		console.error('Error: ' + msg + ", " + exception);
	}
}


function sendMessage(message) {
	if(message == '') {
		chatLog('Please enter a message.', 'p-info');  
		return;
	}
	
	if (socket.readyState == WebSocket.OPEN) {
		try {
			socket.send(message);
		} catch(exception){  
			chatLog('Error: ' + exception, 'p-warning');  
		}
	} else {
		chatLog('The socket is not open.', 'p-warning');
	}
}


function registrationFormListener(event) {
	event.preventDefault();
	if($('#user-input-box').val().match(/\w+/)) {
		// Send the data using post
		var posting = $.post('/newuser', { username: $('#user-input-box').val() });
		posting.done(function(data) {
			console.log(data);
			try {
				var jsonMsg = $.parseJSON(data);
				if(jsonMsg.response) {
					// success: username can be choose
					if(jsonMsg.response === SUCCESS) {
						var user_status_message = 'You are ' + jsonMsg.data.username;
						$('#p-send-username').html(user_status_message);
						
						// Sending message to join the chat
						var join_chat_message = '{ "request": "' + JOIN_CHAT + '", "data": { "username": "' + jsonMsg.data.username + '" } }';
						sendMessage(join_chat_message);
					} else
					// fail: username already taken
					if(jsonMsg.response === FAIL) {
						$('#p-username-exists').text('Your username already exists!').show().fadeOut(2000);
					}
				} else {
					console.error('The JSON object does not contain the response property');
				}
			} catch(exception) {
				console.error('Error: ' + data + ", " + exception);
			}
		});
	} else {
		$('#p-username-exists').text('Please enter a username!').show().fadeOut(2000);
	}
}


function chatLog(msg, classCSS) {
	div_chat_log.append('<p class="' + classCSS + '">' + msg + '</p>');
	var height = div_chat_log[0].scrollHeight;
	div_chat_log.scrollTop(height);
}
