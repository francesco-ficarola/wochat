const SUCCESS_CONN = 'success_conn';
const FAIL_CONN = 'fail_conn';
const ALREADY_CONN = 'already_conn';
const USERS_ADD = 'users_add';
const USERS_REM = 'users_rem';
const GET_CONN_STATUS = 'get_conn_status';
const NEW_USER_STATUS = 'new_user_status';
const REG_USER_STATUS = 'reg_user_status';
const JOIN_CHAT = 'join_chat';
const DELIVER_MSG = 'deliver_msg';
const FAIL_DELIVERING = 'fail_delivering';
const FORWARD_TO_OTHER_CHANNELS = 'forward_to_other_channels';

const p_users_list_DEFAULT_BACKGROUND = '#dff6ff';
const p_users_list_HOVER_BACKGROUND = '#beedff';
const p_users_list_CLICKED_BACKGROUND = '#00baff';
const p_users_list_NEW_MESSAGE_BACKGROUND = '#beedff';

var service_location;
var socket;
var username;
var id;
var recipient_id;
var recipient_username;
var div_chat_log;
var div_users_list;


$(document).ready(function() {
	div_chat_log = $('#div-chat-log');
	div_users_list = $('#div-users-list');

	if(!window.WebSocket) {
	
		$(document.body).html('<p style="color:red; text-align:center; font-weight:bold;">Error: Your browser or device does not support Web Socket. Try Google Chrome!</p>');
	
	} else {
		// Function exists
		$.fn.exists = function() { return this.length>0; }
		
		// Disable textarea in the chatroom div before logging:
		$('#ta-message').prop('disabled', true);
	
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
		
		$('#ta-message').keydown(function(e) {
			if (e.keyCode === 13 && !e.shiftKey) {
				$('#form-send-message').submit();
				e.preventDefault();
			}
		});
		
		$('#ta-message').focus(function() {
			$(this).css('background-color', '#eaffee');
		});
		
		$('#ta-message').blur(function() {
			$(this).css('background-color', '#fff');
		});
		
		$('#div-submit').click(function() {
			if(username !== undefined) {
				$(this).hide().fadeIn(250);
				$('#form-send-message').submit();
			}
		});
		
		$('#div-submit').mouseenter(function() {
			if(username !== undefined) {
				$(this).css('background-color', '#555');
			}
		});
		
		$('#div-submit').mouseleave(function() {
			if(username !== undefined) {
				$(this).css('background-color', '#333');
			}
		});
		
		$(document).on('mousewheel', '.div-chat-text', function (e, delta) {
			this.scrollTop += (delta < 0 ? 1 : -1) * 30;
			e.preventDefault();
		});
		
		$(document).on('click', '.p-users-list', function() {			
			if(id != $(this).attr('id')) {
				$('.p-users-list').css('background-color', p_users_list_DEFAULT_BACKGROUND);
				$('.p-users-list').css('color', '#333');
				$('.p-users-list').css('font-weight', 'normal');
				$(this).css('background-color', p_users_list_CLICKED_BACKGROUND);
				$(this).css('color', '#fff');
				$(this).css('font-weight', 'bold');
				
				recipient_id = $(this).attr('id');
				recipient_username = $(this).attr('title');
				$(this).html(recipient_username);
				console.log('Recipient: ' + recipient_id + ', ' + recipient_username);
				
				var chat_title = 'Chatting with <span style="color:#ca41f7">' + recipient_username + '</span> . . .';
				$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
				
				// Hide all previous chat box
				$('.div-chat-text').css('display', 'none');
				
				checkRecipientDiv(recipient_id, 'block');
				$recipient_div = $('#div-' + recipient_id);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
				
				$('#ta-message').focus();
			}
		});
		
		$('#form-send-message').submit(function(event) {
			event.preventDefault();
			if(recipient_id != undefined || recipient_id != null) {
				var $textarea = $('#ta-message');
				var msg_body = $textarea.val().trim();
				if(msg_body != '') {
					msg_body = htmlEscape(msg_body);
					var $recipient_div = $('#div-' + recipient_id);
					var msg_struct = '\
							<div class="div-chat-user-msg">\
								<div class="div-chat-username">' + username + '</div>\
								<div class="div-chat-message">' + msg_body + '</div>\
							</div>';
					$recipient_div.append(msg_struct);
					$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
					$textarea.val('');
					$textarea.focus();
					
					var json_data = '{"request": "' + DELIVER_MSG + '", "data": {"id": "' + id + '", "username": "' + username + '", "msg": {"receiver": {"id": "' + recipient_id + '", "username": "' + recipient_username + '"}, "body": "' + msg_body + '"}}}';
					sendMessage(json_data);
				}
			} else {
				var chat_title = 'Click on a user in the users\' list and start chatting!';
				$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
				console.warn("recipient_id is undefined!");
			}
		});
	}
});


function connectToServer() {
	try {
		socket = window['MozWebSocket'] ? new MozWebSocket(service_location) : new WebSocket(service_location);
		socket.onmessage = onMessageReceived;
		socket.onopen = onSocketOpen;
		socket.onclose = onSocketClose;
		//TODO onerror
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
	var system_msg = 'Chat went down or you disconnected. Thank you!';
	var table_loading = '\
				<div id="div-system-msg-container">\
					<p>' + system_msg + '</p>\
				</div>';

	$('#div-system-msg').html(table_loading);
	$('#div-system-msg').css('display', 'block');
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
							<form name="form-send-username" id="form-send-username">\
								<input type="text" name="user-input-box" id="user-input-box" placeholder="Your username here..." maxlength="15" />\
								<input type="submit" name="user-submit-button" id="user-submit-button" value="Connect" onfocus="this.blur();" />\
							</form>\
							';
				$('#p-send-username').hide().html(registration_form).fadeIn('slow');
				$('#user-input-box').focus();
				$('#form-send-username').on('submit', registrationFormListener);
			}
			
			else
			
			if(jsonMsg.response === REG_USER_STATUS) {
				loggedin(jsonMsg.data.username);
			}
			
			else
			
			if(jsonMsg.response === USERS_ADD) {
				var usersList = jsonMsg.data;
				for(var i=0; i<usersList.length; i++) {
					if(usersList[i].username === username) {
						id = usersList[i].id;
						console.log('My ID is: ' + id);
					}
					div_users_list.append('<p class="p-users-list" id="' + usersList[i].id + '" title="' + usersList[i].username + '">' + usersList[i].username + '</p>');
				}
			}
			
			else
			
			if(jsonMsg.response === USERS_REM) {
				var usersList = jsonMsg.data;
				for(var i=0; i<usersList.length; i++) {
					$('#' + usersList[i].id).remove();
					if($('#div-' + usersList[i].id).exists()) {
						var receiver_id =  usersList[i].id;
						var msg_body = 'User seems to be disconnected. Closing...';
						var $recipient_div = $('#div-' + receiver_id);
						var msg_struct = '\
								<div class="div-chat-user-msg">\
									<div class="div-chat-username" style="background-color:red;">system message</div>\
									<div class="div-chat-message">' + msg_body + '</div>\
								</div>';
						$recipient_div.append(msg_struct);
						$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
						$recipient_div.fadeOut(5000, function() {
							$(this).remove();
						});
						
						if(recipient_id !== undefined) {
							if(recipient_id === receiver_id) {
								recipient_id = null;
								var chat_title = 'Click on a user in the users\' list and start chatting!';
								$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
							}
						}
					}
				}
			}
			
			else
			
			if(jsonMsg.response === DELIVER_MSG) {
				var jsonUser = jsonMsg.data;
				var id_from = jsonUser.id;
				var username_from = jsonUser.username;
				var msg_json = jsonUser.msg;
				var my_received_id = msg_json.receiver.id;
				
				if(id === my_received_id) {
					
					// Move element to top and set a different background color
					var $recipient_p = $('#' + id_from);
					$recipient_p.parent().prepend($('#' + id_from));
					$recipient_p.css('background-color', p_users_list_NEW_MESSAGE_BACKGROUND);
					
					// If I still don't have active chats or if I'm already chatting with user sending this message
					if(recipient_id === undefined || recipient_id === null || recipient_id === id_from) {
						$('#' + id_from).trigger('click');
					}
					
					// I'm chatting with someone else...
					else {
						checkRecipientDiv(id_from, 'none');
					
						// Adding the number of messages received from a user next to the sender's username
						if($('#counter-' + id_from).exists()) {
							var current_msg_counter = $('#counter-' + id_from).html().replaceAll('(', '').replaceAll(')', '');
							current_msg_counter++;
							$('#counter-' + id_from).html('('+ current_msg_counter +')');
						} else {
							$recipient_p.append(' <span id="counter-' + id_from + '">(1)</span>');
						}
					}
					
					var msg_body = msg_json.body;
					var $recipient_div = $('#div-' + id_from);
					var msg_struct = '\
							<div class="div-chat-user-msg">\
								<div class="div-chat-username" style="background-color:#ca41f7;">' + username_from + '</div>\
								<div class="div-chat-message">' + msg_body + '</div>\
							</div>';
					$recipient_div.append(msg_struct);
					$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
					
				} else {
					console.error('Something went wrong... ID (' + id + ') and received ID (' + my_received_id + ') are not equal!');
				}
			}
			
			else
			
			if(jsonMsg.response === FAIL_DELIVERING) {
				var jsonUser = jsonMsg.data;
				var id_from = jsonUser.id;
				var msg_json = jsonUser.msg;
				var receiver_id = msg_json.receiver.id;
				
				if(id === id_from) {
					// if I'm chatting with a disconnected user...
					if(recipient_id === receiver_id && $('#div-' + receiver_id).exists()) {
						var msg_body = 'Message delivering was failed.';
						var $recipient_div = $('#div-' + receiver_id);
						var msg_struct = '\
								<div class="div-chat-user-msg">\
									<div class="div-chat-username" style="background-color:red;">system message</div>\
									<div class="div-chat-message">' + msg_body + '</div>\
								</div>';
						$recipient_div.append(msg_struct);
						$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
					}
				} else {
					console.error('My ID (' + id + ') is not equal to id_from (' + id_from + ')!');
				}
			}
			
			else
			
			if(jsonMsg.response === FORWARD_TO_OTHER_CHANNELS) {
				var jsonUser = jsonMsg.data;
				var id_from = jsonUser.id;
				var username_from = jsonUser.username;
				var msg_json = jsonUser.msg;
				var receiver_id = msg_json.receiver.id;
				
				if(id === id_from) {
					// If I still don't have active chats or if I'm already chatting with user I sent this message
					if(recipient_id === undefined || recipient_id === null || recipient_id === receiver_id) {
						$('#' + receiver_id).trigger('click');
					} else {
						checkRecipientDiv(receiver_id, 'none');
					}
				
					var msg_body = msg_json.body;
					var $recipient_div = $('#div-' + receiver_id);
					var msg_struct = '\
							<div class="div-chat-user-msg">\
								<div class="div-chat-username" style="background-color:#27ade2;">' + username_from + '</div>\
								<div class="div-chat-message">' + msg_body + '</div>\
							</div>';
					$recipient_div.append(msg_struct);
					$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
				} else {
					console.error('This message is not mine! My ID: ' + id + ', id_from: ' + id_from + '!');
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
		console.warn('Message is empty!');  
		return;
	}
	
	if (socket.readyState == WebSocket.OPEN) {
		try {
			socket.send(message);
		} catch(exception){  
			console.error('Error: ' + exception);  
		}
	} else {
		chatLog('The socket is not open.', 'p-warning');
	}
}


function registrationFormListener(event) {
	event.preventDefault();
	console.log($('#user-input-box').val());
	if($('#user-input-box').val().match(/^\w+$/)) {
		// Send the data using post
		var posting = $.post('/newuser', { username: $('#user-input-box').val() });
		posting.done(function(data) {
			console.log(data);
			try {
				var jsonMsg = $.parseJSON(data);
				if(jsonMsg.response) {
					// success_conn: username can be choose
					if(jsonMsg.response === SUCCESS_CONN) {
						loggedin(jsonMsg.data.username);
					} else
					// fail_conn: username already taken
					if(jsonMsg.response === FAIL_CONN) {
						$('#div-username-warning').text('Your username already exists!').show().fadeOut(5000);
					} else
					// already_conn: username already logged in with another browser
					if(jsonMsg.response === ALREADY_CONN) {
						location.reload(true);
					}
				} else {
					console.error('The JSON object does not contain the response property');
				}
			} catch(exception) {
				console.error('Error: ' + data + ", " + exception);
			}
		});
	} else {
		$('#div-username-warning').text('Please enter a valid username. Only alphanumeric characters are allowed.').show().fadeOut(5000);
	}
}


function loggedin(json_username) {
	$('#ta-message').prop('disabled', false);
	$('#div-submit').css('cursor', 'pointer');
	$('#div-submit').css('background-color', '#333');
	
	username = json_username;
	console.log('My username is: ' + username);
	var user_status_message = 'You are <span style="color:#27ade2;">' + username + '</span>';
	$('#p-send-username').html(user_status_message);
	
	// Sending message to join the chat
	var join_chat_message = '{ "request": "' + JOIN_CHAT + '", "data": { "username": "' + username + '" } }';
	sendMessage(join_chat_message);
	
	var chat_title = 'Click on a user in the users\' list and start chatting!';
	$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
}


function checkRecipientDiv(receiver_id, display) {
	var $recipient_div;
	
	if($('#div-' + receiver_id).exists()) {
		console.log('#div-' + receiver_id + ' already exists...');
		$recipient_div = $('#div-' + receiver_id);
	} else {
		console.log('div-' + receiver_id + ' does not exist. Building...');
		var recipient_div_html = '<div id="div-' + receiver_id + '" class="div-chat-text"></div>';
		$('#div-chat-box').append(recipient_div_html);
		$recipient_div = $('#div-' + receiver_id);
	}
	
	if(display) $recipient_div.css('display', display);
}


function htmlEscape(value) {
	return $('<div />').text(value).html().replaceAll('\n', '<br />').replaceAll('"', '&quot;').replaceAll('\'', '&#39;');
}


function chatLog(msg, classCSS) {
	div_chat_log.append('<p class="' + classCSS + '">' + msg + '</p>');
	var height = div_chat_log[0].scrollHeight;
	div_chat_log.scrollTop(height);
}

String.prototype.replaceAll = function(str1, str2, ignore) 
{
	return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g,"\\$&"),(ignore?"gi":"g")),(typeof(str2)=="string")?str2.replace(/\$/g,"$$$$"):str2);
};
