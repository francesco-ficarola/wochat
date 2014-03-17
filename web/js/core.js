var HTTP_SUCCESS_CONN = 'success_conn';
var HTTP_FAIL_CONN = 'fail_conn';
var HTTP_ALREADY_CONN = 'already_conn';
var USERS_ADD = 'users_add';
var USERS_REM = 'users_rem';
var GET_CONN_STATUS = 'get_conn_status';
var NEW_USER_STATUS = 'new_user_status';
var REG_USER_STATUS = 'reg_user_status';
var JOIN_CHAT = 'join_chat';
var DELIVER_MSG = 'deliver_msg';
var ADMIN_DELIVER_MSG = 'admin_deliver_msg';
var FAIL_DELIVERING = 'fail_delivering';
var FORWARD_TO_OTHER_CHANNELS = 'forward_to_other_channels';
var ACK_MSG = 'ack';
var HTTP_ADMIN_SUCCESS_CONN = 'admin_success_conn';
var HTTP_ADMIN_FAIL_CONN = 'admin_fail_conn';
var ADMIN_READY = 'admin_ready';
var SYSTEM_NOTIFICATION = 'system_notification';
var ADMIN_ID = '0000';
var START_SURVEY_1 = 'start_survey_1';
var START_SURVEY_2 = 'start_survey_2';
var START_CHAT = 'start_chat';
var ANSWERS_SURVEY1 = 'answers_survey1';
var ANSWERS_SURVEY2 = 'answers_survey2';
var CHAT_MODE = 'chat_mode';
var SURVEY1_MODE = 'survey1_mode';
var SURVEY2_MODE = 'survey2_mode';
var ADMIN_MSG = 'admin_msg';

var p_users_list_DEFAULT_BACKGROUND = '#dff6ff';
var p_users_list_HOVER_BACKGROUND = '#beedff';
var p_users_list_CLICKED_BACKGROUND = '#00baff';
var p_users_list_NEW_MESSAGE_BACKGROUND = '#beedff';

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

	if(!window.WebSocket || navigator.appName != 'Netscape') {
		$(document.body).html('<p style="color:red; text-align:center; font-weight:bold;">Error: your browser does not support websockets.<br />Try the latest version of <a href="https://www.google.com/chrome/" class="bodylink">Google Chrome</a></p>');
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
			if(id != $(this).attr('id') && id != ADMIN_ID) {
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
				$('#p-chat-title').hide().html(chat_title).fadeIn('slow');
				
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
					
					var json_data;
					if(id === ADMIN_ID) {
						json_data = '{"request": "' + ADMIN_DELIVER_MSG + '", "data": {"id": "' + id + '", "username": "' + username + '", "msg": {"receiver": {"id": "' + recipient_id + '", "username": "' + recipient_username + '"}, "body": "' + msg_body + '"}}}';
					} else {
						json_data = '{"request": "' + DELIVER_MSG + '", "data": {"id": "' + id + '", "username": "' + username + '", "msg": {"receiver": {"id": "' + recipient_id + '", "username": "' + recipient_username + '"}, "body": "' + msg_body + '"}}}';
					}
					
					sendMessage(json_data);
				}
			} else {
				var chat_title = 'Click on a user in the users\' list and start chatting!';
				$('#p-chat-title').hide().html(chat_title).fadeIn('slow');
				console.warn("recipient_id is undefined!");
			}
		});
		
		$(document).on('click', '#survey1-submit-button', function() {
			var message = '{ "request": "' + ANSWERS_SURVEY1 + '", "data": { "id": "' + id + '", "username": "' + username + '", "answersSurvey1": ['; // will be complited in the following
			var num_answers = $('.survey1-answers').length;
			var readyToSend = true;
			$('.survey1-answers').each(function(index) {
				if(!$(this).val().match(/^\d+$/)) {
					alert('Please answer all questions by entering only numeric characters!');
					readyToSend = false;
					return false;
				}
				message += $(this).val();
				if(index < num_answers - 1) {
				 	message += ', ';
				}
			});
			message += '] } }';
			
			if(readyToSend) {
				console.log(message);
				sendMessage(message);
				
				var completed_msg = '<p>Thank you. Please wait for other participants...</p>' + 
							'<iframe id="tetris-iframe" src="tetris/tetris.html" frameborder="0" border="0" cellspacing="0"></iframe>';
				$('.div-survey-container').css('text-align', 'center');
				$('.div-survey-container').css('width', '40%');
				$('.div-survey-container').html(completed_msg);
			}
		});
		
		$(document).on('click', '#survey2-submit-button', function() {
			var message = '{ "request": "' + ANSWERS_SURVEY2 + '", "data": { "id": "' + id + '", "username": "' + username + '", "answersSurvey2": ['; // will be complited in the following
			var num_answers = $('.survey2-answers').length;
			var readyToSend = true;
			$('.survey2-answers').each(function(index) {
				if(!$(this).val().match(/^\d+$/)) {
					alert('Please answer all questions by entering only numeric characters!');
					readyToSend = false;
					return false;
				}
				message += $(this).val();
				if(index < num_answers - 1) {
				 	message += ', ';
				}
			});
			message += '] } }';
			
			if(readyToSend) {
				console.log(message);
				sendMessage(message);
				
				var completed_msg = '<p>Thank you. Please wait for other participants...</p>' + 
							'<iframe id="tetris-iframe" src="tetris/tetris.html" frameborder="0" border="0" cellspacing="0"></iframe>';
				$('.div-survey-container').css('text-align', 'center');
				$('.div-survey-container').css('width', '40%');
				$('.div-survey-container').html(completed_msg);
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
		socket.onerror = onError;
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
	var system_msg = 'Chat went down or you disconnected.<br /><br />Try to reload this page and see what happens!';
	var system_div = '\
				<div id="div-system-msg-container">\
					<p>' + system_msg + '</p>\
				</div>';

	$('.div-survey').css('display', 'none');
	$('#div-system-msg').html(system_div);
	$('#div-system-msg').css('display', 'block');
}


function onError(e) {
	console.log('WebSocket Error ' + e);
}


function onMessageReceived(e) {
	var msg = e.data;
	console.log(msg);
	try {
		var jsonMsg = $.parseJSON(msg);
		
		// Reponse to the previous request
		if(jsonMsg.response) {
		
			// Response received whenever a new user opens the chat
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
			
			// Response received whenever an already-connected user reloads the page or open the chat with another browser
			if(jsonMsg.response === REG_USER_STATUS) {
				loggedin(jsonMsg.data.username);
			}
			
			else
			
			// Response received whenever a user is logged in
			if(jsonMsg.response === USERS_ADD) {
				var usersList = jsonMsg.data;
				for(var i=0; i<usersList.length; i++) {
					if(usersList[i].username === username) {
						id = usersList[i].id;
						console.log('My ID is: ' + id);
					}
					if(!$('#' + usersList[i].id).exists()) {
						div_users_list.append('<p class="p-users-list" id="' + usersList[i].id + '" title="' + usersList[i].username + '">' + usersList[i].username + '</p>');
					}
				}
			}
			
			else
			
			// Response received whenever a user is disconnected
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
								$('#p-chat-title').hide().html(chat_title).fadeIn('slow');
							}
						}
					}
				}
			}
			
			else
			
			// Response received whenever a message needs to be delivered
			if(jsonMsg.response === DELIVER_MSG) {
				var jsonUser = jsonMsg.data;
				var id_from = jsonUser.id;
				var username_from = jsonUser.username;
				var msg_json = jsonUser.msg;
				var my_received_id = msg_json.receiver.id;
				var seqHex = msg_json.seqHex;
				
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
				
				var ackMsg = '{ "request": "' + ACK_MSG + '", "data": { "msg": { "seqHex": "' + seqHex + '" } } }';
				sendMessage(ackMsg);
			}
			
			else
			
			// Response received whenever a message is not delivered
			if(jsonMsg.response === FAIL_DELIVERING) {
				var jsonUser = jsonMsg.data;
				var id_from = jsonUser.id;
				var msg_json = jsonUser.msg;
				var receiver_id = msg_json.receiver.id;
				
				if(id === id_from) {
					// if I'm chatting with a disconnected user...
					if(recipient_id === receiver_id && $('#div-' + receiver_id).exists()) {
						var msg_body = 'Message delivering was failed!';
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
			
			// Response received whenever a message needs to be forwarded to other channels (i.e., other opened browsers)
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
			
			else
			
			// Response received whenever the admin is authorized to join chat
			if(jsonMsg.response === ADMIN_READY) {
				id = ADMIN_ID;
				var chat_title = 'Admin Console';
				$('#p-chat-title').hide().html(chat_title).fadeIn('slow');
				
				recipient_id = ADMIN_ID;
				checkRecipientDiv(recipient_id, 'block');
				$recipient_div = $('#div-' + recipient_id);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
				
				$('#ta-message').focus();
			}
			
			else
			
			if(jsonMsg.response === SYSTEM_NOTIFICATION) {
				var msg_body = jsonMsg.data.msg.body;
				var $recipient_div = $('#div-' + recipient_id);
				var msg_struct = '\
						<div class="div-chat-user-msg">\
							<div class="div-chat-username" style="background-color:red;">system message</div>\
							<div class="div-chat-message">' + msg_body + '</div>\
						</div>';
				$recipient_div.append(msg_struct);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
			}
			
			else
			
			// Response received whenever admin executed the chat mode
			if(jsonMsg.response === CHAT_MODE) {
				var msg_body = 'Chat mode enabled.';
				var $recipient_div = $('#div-' + recipient_id);
				var msg_struct = '\
						<div class="div-chat-user-msg">\
							<div class="div-chat-username" style="background-color:red;">system message</div>\
							<div class="div-chat-message">' + msg_body + '</div>\
						</div>';
				$recipient_div.append(msg_struct);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
			}
			
			else
			
			// Response received whenever admin executed the chat mode
			if(jsonMsg.response === SURVEY1_MODE) {
				var msg_body = 'Survey1 mode enabled.';
				var $recipient_div = $('#div-' + recipient_id);
				var msg_struct = '\
						<div class="div-chat-user-msg">\
							<div class="div-chat-username" style="background-color:red;">system message</div>\
							<div class="div-chat-message">' + msg_body + '</div>\
						</div>';
				$recipient_div.append(msg_struct);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
			}
			
			else
			
			// Response received whenever admin executed the chat mode
			if(jsonMsg.response === SURVEY2_MODE) {
				var msg_body = 'Survey2 mode enabled.';
				var $recipient_div = $('#div-' + recipient_id);
				var msg_struct = '\
						<div class="div-chat-user-msg">\
							<div class="div-chat-username" style="background-color:red;">system message</div>\
							<div class="div-chat-message">' + msg_body + '</div>\
						</div>';
				$recipient_div.append(msg_struct);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
			}
			
			else
			
			// Messages from admin to everyone
			if(jsonMsg.response === ADMIN_MSG) {
				var msg_body = jsonMsg.data.msg.body;
				if(recipient_id === undefined || recipient_id === null) {
					recipient_id = 'temp';
				}
				
				checkRecipientDiv(recipient_id, 'block');
				
				var $recipient_div = $('#div-' + recipient_id);
				var msg_struct = '\
						<div class="div-chat-user-msg">\
							<div class="div-chat-username" style="background-color:red;">system message</div>\
							<div class="div-chat-message">' + msg_body + '</div>\
						</div>';
				$recipient_div.append(msg_struct);
				$recipient_div.animate({scrollTop: $recipient_div.prop("scrollHeight")}, 250);
			}
			
			else
			
			// Response received whenever the admin starts a survey pre-interaction
			if(jsonMsg.response === START_SURVEY_1) {
				var survey_form = '';
				var jsonQuestions = jsonMsg.data.questionsList;
				for(var i in jsonQuestions) {
					var answersCnt = parseInt(i) + 1;
					survey_form += '<p>' + 
							jsonQuestions[i] + '<br />' +
							'<input type="text" name="answer' + answersCnt + '" id="answer' + answersCnt + '" class="survey1-answers" maxlength="20" />' +
							'</p>';
				}
				
				survey_form += '<p>' +
						'<input type="submit" name="survey1-submit-button" id="survey1-submit-button" value="Send" onfocus="this.blur();" />' +
						'</p>';

				var survey_div_container = '\
							<div class="div-survey-container">\
								' + survey_form + '\
							</div>';
				
				$('.div-survey').html(survey_div_container);
				$('.div-survey').css('display', 'block');
			}
			
			else
			
			// Response received whenever the admin starts a survey post-interaction
			if(jsonMsg.response === START_SURVEY_2) {
				var survey_form = '';
				var jsonQuestions = jsonMsg.data.questionsList;
				for(var i in jsonQuestions) {
					var answersCnt = parseInt(i) + 1;
					survey_form += '<p>' + 
							jsonQuestions[i] + '<br />' +
							'<input type="text" name="answer' + answersCnt + '" id="answer' + answersCnt + '" class="survey2-answers" maxlength="20" />' +
							'</p>';
				}
				
				survey_form += '<p>' +
						'<input type="submit" name="survey2-submit-button" id="survey2-submit-button" value="Send" onfocus="this.blur();" />' +
						'</p>';

				var survey_div_container = '\
							<div class="div-survey-container">\
								' + survey_form + '\
							</div>';

				$('.div-survey').html(survey_div_container);
				$('.div-survey').css('display', 'block');
			}
			
			else
			
			// Response received whenever the admin starts the chat
			if(jsonMsg.response === START_CHAT) {
				$('.div-survey').css('display', 'none');
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
					if(jsonMsg.response === HTTP_SUCCESS_CONN) {
						loggedin(jsonMsg.data.username);
					} else
					// fail_conn: username already taken
					if(jsonMsg.response === HTTP_FAIL_CONN) {
						$('#div-username-warning').text('Your username already exists!').show().fadeOut(5000);
					} else
					// already_conn: username already logged in with another browser
					if(jsonMsg.response === HTTP_ALREADY_CONN) {
						location.reload(true);
					} else
					// admin_success_conn: admin connection
					if(jsonMsg.response === HTTP_ADMIN_SUCCESS_CONN) {
						loggedin(jsonMsg.data.username);
					} else
					if(jsonMsg.response === HTTP_ADMIN_FAIL_CONN) {
						$('#div-username-warning').text('Admin already connected!').show().fadeOut(5000);
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
	$('#p-chat-title').hide().html(chat_title).fadeIn('slow');
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
