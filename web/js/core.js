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

const p_users_list_DEFAULT_BACKGROUND = '#dff6ff';
const p_users_list_HOVER_BACKGROUND = '#beedff';
const p_users_list_CLICKED_BACKGROUND = '#00baff';

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
		
		// Disable input, textarea and sending button in the chatroom div before logging:
		$("#div-chatroom").find('input,textarea,button').prop('disabled', true);
	
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
		
		$(document).on('mousewheel', '.div-chat-text', function (e, delta){
			this.scrollTop += (delta < 0 ? 1 : -1) * 30;
			e.preventDefault();
		});
		
		$(document).on('click', '.p-users-list', function() {			
			if(id != $(this).attr('id')) {
				$('.p-users-list').css('background-color', p_users_list_DEFAULT_BACKGROUND);
				$(this).css('background-color', p_users_list_CLICKED_BACKGROUND);
				
				recipient_id = $(this).attr('id');
				recipient_username = $(this).html();
				console.log('Recipient: ' + recipient_id + ', ' + recipient_username);
				
				var chat_title = 'Chatting with ' + recipient_username + '...';
				$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
				
				// Hide all previous chat box
				$('.div-chat-text').css('display', 'none');
				
				if($('#div-' + recipient_id).exists()) {
					console.log('#div-' + recipient_id + ' already exists...');
					var $recipient_div = $('#div-' + recipient_id);
					var msg_body = '\
							<div class="div-chat-user-msg">\
								<div class="div-chat-username">\
									pippoasdasd\
								</div>\
								<div class="div-chat-message">\
									aaaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbbbbbb\
								</div>\
							</div>';
					$recipient_div.append(msg_body);
					$recipient_div.css('display', 'block');
					$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 500); // must be after display:block property
				} else {
					console.log('div-' + recipient_id + ' does not exist. Building...');
					var recipient_div_html = '<div id="div-' + recipient_id + '" class="div-chat-text"></div>';
					$('#div-chat-box').append(recipient_div_html);
					var $recipient_div = $('#div-' + recipient_id);
					$recipient_div.css('display', 'block');
					$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 500); // must be after display:block property
				}
			}
		});
		
		$('#form-send-message').submit(function() {
			if(recipient_id != undefined) {
				
			}
		});

		//TODO $('.div-chat-text').scrollTop(9999);
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
							<form name="form-send-username" id="form-send-username">\
								<input type="text" name="user-input-box" id="user-input-box" placeholder="Your username here..." maxlength="20" />\
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
			
			else
			
			if(jsonMsg.response === DELIVER_MSG) {
				//TODO
			}
			
			else
			
			if(jsonMsg.response === FAIL_DELIVERING) {
				//TODO
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
	$("#div-chatroom").find('input,textarea,button').prop('disabled', false);
	username = json_username;
	console.log('My username is: ' + username);
	var user_status_message = 'You are ' + username;
	$('#p-send-username').html(user_status_message);
	
	// Sending message to join the chat
	var join_chat_message = '{ "request": "' + JOIN_CHAT + '", "data": { "username": "' + username + '" } }';
	sendMessage(join_chat_message);
	
	var chat_title = 'Click on a user in the users\' list and start chatting!';
	$('#span-chat-title').hide().html(chat_title).fadeIn('slow');
}


function chatLog(msg, classCSS) {
	div_chat_log.append('<p class="' + classCSS + '">' + msg + '</p>');
	var height = div_chat_log[0].scrollHeight;
	div_chat_log.scrollTop(height);
}
