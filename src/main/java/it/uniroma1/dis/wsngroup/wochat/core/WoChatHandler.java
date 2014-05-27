package it.uniroma1.dis.wsngroup.wochat.core;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import it.uniroma1.dis.wsngroup.wochat.conf.Constants;
import it.uniroma1.dis.wsngroup.wochat.dbfly.Answer;
import it.uniroma1.dis.wsngroup.wochat.dbfly.DataOnTheFly;
import it.uniroma1.dis.wsngroup.wochat.dbfly.Message;
import it.uniroma1.dis.wsngroup.wochat.dbfly.Survey;
import it.uniroma1.dis.wsngroup.wochat.dbfly.User;
import it.uniroma1.dis.wsngroup.wochat.json.MultiUsersResponse;
import it.uniroma1.dis.wsngroup.wochat.json.SurveyResponse;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserRequest;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserResponse;
import it.uniroma1.dis.wsngroup.wochat.logging.LogAnswers;
import it.uniroma1.dis.wsngroup.wochat.logging.LogConnection;
import it.uniroma1.dis.wsngroup.wochat.logging.LogInteraction;
import it.uniroma1.dis.wsngroup.wochat.logging.LogMessage;
import it.uniroma1.dis.wsngroup.wochat.logging.LogUsersList;
import it.uniroma1.dis.wsngroup.wochat.utils.Functions;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class WoChatHandler extends SimpleChannelInboundHandler<Object> {
	private static final Logger logger = Logger.getLogger(WoChatHandler.class.getName());
	private static final Pattern chatUrlPattern = Pattern.compile("/chat");

	private WebSocketServerHandshaker handshaker;
	private Gson gson;
	
	private DataOnTheFly data;
	private Map<String, String> usersMap_IpId;
	private Map<String, String> usersMap_IdIp;
	private Map<String, Set<String>> ackMap_IpAcks;
	private Map<String, Integer> checkPendingMsgMap_IpChecks;
	private Set<String> usernamesSet;
	private Map<String, String> usersMap_IdUsername;
	private Map<String, ChannelGroup> channelsMap_IpChannelGroup;
	private ChannelGroup broadcastChannelGroup;
	
	public WoChatHandler(DataOnTheFly data) {
		gson = new Gson();
		this.data = data;
		this.usersMap_IpId = this.data.get_usersMap_IpId();
		this.usersMap_IdIp = this.data.get_usersMap_IdIp();
		this.ackMap_IpAcks = this.data.get_ackMap_IpAcks();
		this.checkPendingMsgMap_IpChecks = this.data.get_checkPendingMsgMap_IpChecks();
		this.usernamesSet = this.data.get_usernamesSet();
		this.usersMap_IdUsername = this.data.get_usersMap_IdUsername();
		this.channelsMap_IpChannelGroup = this.data.get_channelsMap_IpChannelGroup();
		this.broadcastChannelGroup = this.data.get_broadcastChannelGroup();
	}
	
	public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		
		/** A bad request */
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}

		/** Allow only GET and POST methods */
		if (req.getMethod() != GET && req.getMethod() != POST) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}
		
		logger.debug("Requested URI: " + req.getUri());

		/** Homepage */
		if ("/".equals(req.getUri())) {			
			ByteBuf content = Unpooled.copiedBuffer(Functions.readFile("./web/index.html", CharsetUtil.UTF_8), CharsetUtil.UTF_8);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

			res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
			setContentLength(res, content.readableBytes());

			sendHttpResponse(ctx, req, res);
			return;
		}
		
		/** Website content */
		if(req.getUri().matches(".*\\.html$")) {
			ByteBuf content = Unpooled.copiedBuffer(Functions.readFile("./web/" + req.getUri(), CharsetUtil.UTF_8), CharsetUtil.UTF_8);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
			
			res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
			setContentLength(res, content.readableBytes());

			sendHttpResponse(ctx, req, res);
			return;
		}
		
		if(req.getUri().matches(".*\\.(css|js|map)$")) {
			ByteBuf content = Unpooled.copiedBuffer(Functions.readFile("./web/" + req.getUri(), CharsetUtil.UTF_8), CharsetUtil.UTF_8);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
			
			if(req.getUri().matches(".*\\.css$")) {
				res.headers().set(CONTENT_TYPE, "text/css; charset=UTF-8");
			} else
			if(req.getUri().matches(".*\\.js$")) {
				res.headers().set(CONTENT_TYPE, "text/javascript; charset=UTF-8");
			}
			
			setContentLength(res, content.readableBytes());

			sendHttpResponse(ctx, req, res);
			return;
		}
		
		if(req.getUri().matches(".*\\.(png|jpg|gif)$")) {
			/** Image loading... */
			BufferedImage originalImage = ImageIO.read(new File("./web/" + req.getUri()));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(req.getUri().matches(".*\\.png$")) {
				ImageIO.write( originalImage, "png", baos );
			} else
			if(req.getUri().matches(".*\\.jpg$")) {
				ImageIO.write( originalImage, "jpg", baos );
			} else
			if(req.getUri().matches(".*\\.gif$")) {
					ImageIO.write( originalImage, "gif", baos );
			}
			baos.flush();
			byte[] imageInByte = baos.toByteArray();
			baos.close();
			
			ByteBuf content = Unpooled.copiedBuffer(imageInByte);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
			
			if(req.getUri().matches(".*\\.png$")) {
				res.headers().set(CONTENT_TYPE, "image/png");
			} else
			if(req.getUri().matches(".*\\.jpg$")) {
				res.headers().set(CONTENT_TYPE, "image/jpeg");
			} else
			if(req.getUri().matches(".*\\.gif$")) {
				res.headers().set(CONTENT_TYPE, "image/gif");
			} 
			
			setContentLength(res, content.readableBytes());

			sendHttpResponse(ctx, req, res);
			return;
		}
		
		if(req.getUri().matches(".*\\.mp3$")) {
			/** Sounds loading... */
			InputStream is = new FileInputStream(new File("./web/" + req.getUri()));
			byte[] soundInByte = IOUtils.toByteArray(is);
			
			ByteBuf content = Unpooled.copiedBuffer(soundInByte);
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
			res.headers().set(CONTENT_TYPE, "audio/mpeg");
			
			setContentLength(res, content.readableBytes());

			sendHttpResponse(ctx, req, res);
			return;
		}
		
		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
			sendHttpResponse(ctx, req, res);
			return;
		}
		
		/** New user's connection */
		if("/newuser".equals(req.getUri())) {
			
			/** Just for simple users (not admin): already connected (one browser left on the login page and connected with another one) */
			String remoteHost = getRemoteHost(ctx.channel());
			if(usersMap_IpId.containsKey(remoteHost)) {
				logger.info("User already logged in. Just reload its page...");
				SingleUserResponse responseJson = new SingleUserResponse();
				responseJson.setResponse(Constants.HTTP_ALREADY_CONN);
				
				ByteBuf sendingContent = Unpooled.copiedBuffer(gson.toJson(responseJson), CharsetUtil.UTF_8);
				FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, sendingContent);
				
				res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
				setContentLength(res, sendingContent.readableBytes());
	
				sendHttpResponse(ctx, req, res);
				return;
			}
			
			/** User checking... */
			ByteBuf receivedContent = req.content();
			if(receivedContent.isReadable()) {
				String receivedMsg = receivedContent.toString(CharsetUtil.UTF_8);
				logger.debug("Request Content: " + receivedMsg);
				String splitter[] = receivedMsg.split("=");
				
				if(splitter.length > 1) {
					String username = splitter[1];
					SingleUserResponse responseJson = new SingleUserResponse();
					
					/** Admin connection */
					if(username.equals(data.getUsernameAdmin())) {
						if(data.getAdminChannel() == null) {
							responseJson.setResponse(Constants.HTTP_ADMIN_SUCCESS_CONN);
							responseJson.setData(new User().setUsername(username));
						} else 
						if(!data.getAdminChannel().isActive()) {
							responseJson.setResponse(Constants.HTTP_ADMIN_SUCCESS_CONN);
							responseJson.setData(new User().setUsername(username));
						} else {
							responseJson.setResponse(Constants.HTTP_ADMIN_FAIL_CONN);
						}
					}
					
					else
					
					/** New User */
					if(!usernamesSet.contains(username)) {
						responseJson.setResponse(Constants.HTTP_SUCCESS_CONN);
						responseJson.setData(new User().setUsername(username));
						usernamesSet.add(username);
					}
					
					else {
						responseJson.setResponse(Constants.HTTP_FAIL_CONN);
					}
					
					ByteBuf sendingContent = Unpooled.copiedBuffer(gson.toJson(responseJson), CharsetUtil.UTF_8);
					FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, sendingContent);
					
					res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
					setContentLength(res, sendingContent.readableBytes());
		
					sendHttpResponse(ctx, req, res);
					return;
				}
			}
			
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		
		
		/** Web Socket does not match to chat path */
		Matcher m = chatUrlPattern.matcher(req.getUri());
		if (!m.matches()) {
			logger.error("WebSocket does not match to chat path: " + req.getUri());
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
			return;
		}
		
		/** Web socket handshake */
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
			logger.debug(ctx.channel().toString());
		}
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

		/** Check for closing frame */
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			logger.info(ctx.channel().toString() + " was closed.");
			
			/** A closed channel is automatically removed from any group*/
			logger.debug("Number of channels in the broadcast group: " + broadcastChannelGroup.size());
			broadcastDisconnections(getRemoteHost(ctx.channel()));
			return;
		}
		
		if (frame instanceof PingWebSocketFrame) {
			logger.debug("Ping frame from " + getRemoteHost(ctx.channel()) + ". Sending pong...");
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		
		if (frame instanceof PongWebSocketFrame) {
			logger.debug("Pong frame from " + getRemoteHost(ctx.channel()));
			return;
		}
		
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}
		
		String jsonReq = ((TextWebSocketFrame) frame).text();
		logger.debug(String.format("%s received %s", ctx.channel(), jsonReq));
		manageMessages(ctx.channel(), jsonReq);
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		/** Generate an error page if response getStatus code is not OK (200) */
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}

		/** Send the response and close the connection if necessary */
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get(HOST) + Constants.WEBSOCKET_PATH;
	}

	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		messageReceived(ctx, msg);
	}
	
	private void manageMessages(Channel channel, String jsonReq) {
		SingleUserRequest userReq = gson.fromJson(jsonReq, SingleUserRequest.class);
		
		/** Message request on the connection status */
		if(userReq.getRequest().equals(Constants.GET_CONN_STATUS)) {
			manageConnectionStatus(channel);
		}
		
		else
		
		/** Message request to join the chat */
		if(userReq.getRequest().equals(Constants.JOIN_CHAT)) {
			User user = userReq.getData();
			logger.debug("Request to join the chat from: " + user.getUsername());
			
			/** Manage admin */
			if(user.getUsername().equals(data.getUsernameAdmin())) {
				data.setAdminChannel(channel);
				manageAdmin(channel);
			}
			
			/** Manage users */
			else {
				manageUsers(channel, user.getUsername());
			}
		}
		
		else
		
		/** Messages from the admin */
		if(userReq.getRequest().equals(Constants.ADMIN_DELIVER_MSG)) {
			String msgBody = userReq.getData().getMsg().getBody();
			adminMsg(channel, msgBody);
		}
		
		else
		
		/** Messages among users */
		if(userReq.getRequest().equals(Constants.DELIVER_MSG)) {
			deliverMsg(userReq, channel);
		}
		
		else
		
		/** ACKs */
		if(userReq.getRequest().equals(Constants.ACK_MSG)) {
			/** ACK mechanism (only if the checkingtimes.pendingmessages property is greater than 0) */
			if(data.getMaxCheckingTimes() > 0) {
				String seqHex = userReq.getData().getMsg().getSeqHex();
				String remoteHost = getRemoteHost(channel);
				logger.debug("Received ACK from " + remoteHost + ": " + seqHex);
				if(ackMap_IpAcks.containsKey(remoteHost)) {
					Set<String> acksToBeReceived = ackMap_IpAcks.get(remoteHost);
					acksToBeReceived.remove(seqHex);
					logger.debug("#Msg with no ACKS for " + remoteHost + ": " + acksToBeReceived.size());
				}
				if(checkPendingMsgMap_IpChecks.containsKey(remoteHost)) {
					logger.debug("Resetting checkPendingMsgMap_IpChecks for " + remoteHost);
					checkPendingMsgMap_IpChecks.put(remoteHost, 0);
				}
			}
		}
		
		else
			
		/** Answers to survey */
		if(userReq.getRequest().equals(Constants.ANSWERS_SURVEY)) {
			String numSurvey = userReq.getData().getNumSurvey();
			String numRound = userReq.getData().getNumRound();
			String id = userReq.getData().getId();
			List<Answer> answers = userReq.getData().getAnswersSurvey();
			String line = "survey" + numSurvey + Constants.CVS_DELIMITER + "round" + numRound + Constants.CVS_DELIMITER + id + Constants.CVS_DELIMITER;
			for(int i=0; i<answers.size(); i++) {
				Answer answer = answers.get(i);
				line += answer.getAnswerString() + Constants.CVS_DELIMITER + answer.getConfidence();
				if(i < answers.size() - 1) {
					line += Constants.CVS_DELIMITER;
				}
			}
			LogAnswers.logAnswer(line);
			
			/** Notify how many participants have completed survey */
			data.get_usersIdCompletedSurvey().add(id);
			Integer completedSurveys = data.get_usersIdCompletedSurvey().size();
			String systemMsgBody = "Completed surveys: " + completedSurveys;
			sendSystemNotification(systemMsgBody);
		}
		
		else
			
		if(userReq.getRequest().equals(Constants.PONG)) {
			String idPong = userReq.getData().getId();
			String usernamePong = usersMap_IdUsername.get(idPong);
			data.get_usersIdActive().add(idPong);
			logger.debug("Pong response from " + idPong + "(" + usernamePong + ")");
		}
	}

	private void manageConnectionStatus(Channel channel) {
		String remoteHost = getRemoteHost(channel);
		SingleUserResponse userResp = new SingleUserResponse();
		
		logger.debug("Request of connection status by: " + remoteHost);
		
		/** If the user's IP address is already registered, then send user info ... */
		if(usersMap_IpId.containsKey(remoteHost)) {
			String id = usersMap_IpId.get(remoteHost);
			String username = usersMap_IdUsername.get(id);
			User user = new User();
			user.setId(id).setUsername(username);
			userResp.setResponse(Constants.REG_USER_STATUS);
			userResp.setData(user);
			logger.debug("Sending response to already registered user: " + id + " - " + username);
		}
		
		/** ... otherwise send new_user_status string */
		else {
			userResp.setResponse(Constants.NEW_USER_STATUS);
			logger.debug("Sending response to the new user");
		}
		
		channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(userResp)));
	}
	
	private void manageAdmin(Channel channel) {
		logger.debug("Admin successfully connected");
		
		/** Add the channel to the ChannelGroup; it will not add duplicate, like a set */
		broadcastChannelGroup.add(channel);
		logger.debug("Number of channels in the broadcast group: " + broadcastChannelGroup.size());
		
		/** Send a token to manage the admin in the client-side */
		SingleUserResponse adminResp = new SingleUserResponse();
		adminResp.setResponse(Constants.ADMIN_READY);
		channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
		
		/** Send all connected users to the admin */
		broadcastUsersList(channel);
		
		/** Send the number of connected users */
		String systemMsgBody = "Connected users: " + usersMap_IdUsername.size();
		sendSystemNotification(systemMsgBody);
	}
	
	private void manageUsers(Channel channel, String username) {
		final String remoteHost = getRemoteHost(channel);
		logger.debug("Remote Host: " + remoteHost);
		
		/** Add the channel to the ChannelGroup; it will not add duplicate, like a set */
		broadcastChannelGroup.add(channel);
		logger.debug("Number of channels in the broadcast group: " + broadcastChannelGroup.size());
		
		/** Adding the current channel to the group of the corresponding IP (e.g., multiple connections from multiple browsers) */
		if(channelsMap_IpChannelGroup.containsKey(remoteHost)) {
			channelsMap_IpChannelGroup.get(remoteHost).add(channel);
		} else {
			ChannelGroup userChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
			userChannelGroup.add(channel);
			channelsMap_IpChannelGroup.put(remoteHost, userChannelGroup);
		}
		
		
		/** Managing the user... */
		String id = "";
		
		/** IP address already registered */
		if(usersMap_IpId.containsKey(remoteHost)) {
			logger.debug("IP address already registered");
			id = usersMap_IpId.get(remoteHost);
			
			LogConnection.logConnection("[RE-CONNECTION]\t(" + remoteHost + ", " + id + ", " + username + ") connected. #Channels: " + channelsMap_IpChannelGroup.get(remoteHost).size());
			
			/** The user reloaded the page or he disconnected and connected again using just one browser. */
			if(channelsMap_IpChannelGroup.get(remoteHost).size() == 1) {
				
				/** Broadcast new user to everyone */
				broadcastUserStatus(id, username, channel, Constants.USERS_ADD);
			}
		}
		
		/** New User */
		else {
			logger.debug("New IP address");
			
			/** ID assignment... */
			int userCounter = data.getUserCounter();
			id = String.valueOf(Constants.MIN_USERS_ID_RANGE + userCounter);
			userCounter++;
			data.setUserCounter(userCounter);
			
			usersMap_IpId.put(remoteHost, id);
			usersMap_IdIp.put(id, remoteHost);
			usersMap_IdUsername.put(id, username);
			
			LogUsersList.logUsersList(remoteHost + Constants.CVS_DELIMITER + id + Constants.CVS_DELIMITER + username);
			LogConnection.logConnection("[NEW CONNECTION]\t(" + remoteHost + ", " + id + ", " + username + ") connected. #Channels: 1");
			
			/** Broadcast new user to everyone */
			broadcastUserStatus(id, username, channel, Constants.USERS_ADD);
			
			/** This part of code is to periodically check if there are pending messages, i.e., disconnections. */
			/** ACK mechanism (only if the checkingtimes.pendingmessages property is greater than 0) */
			if(data.getMaxCheckingTimes() > 0) {
				if(!checkPendingMsgMap_IpChecks.containsKey(remoteHost)) {
					checkPendingMsgMap_IpChecks.put(remoteHost, 0);
				}
				/** ScheduledExecutorService installation... */
				final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
				scheduler.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						if(ackMap_IpAcks.containsKey(remoteHost)) {
							Set<String> acksToBeReceived = ackMap_IpAcks.get(remoteHost);
							if(acksToBeReceived.size() > 0) {
								Integer checkingTimes = checkPendingMsgMap_IpChecks.get(remoteHost);
								checkingTimes++;
								logger.warn("[PENDING MESSAGES - " + remoteHost +"] ACKs Set size: " + acksToBeReceived.size() + ", Checking times: " + checkingTimes);
								if(checkingTimes >= data.getMaxCheckingTimes()) {
									logger.warn("[DISCONNECTION] " + remoteHost + " seems to be disconnected... Deleting from maps!");
									String idToDisconnect = usersMap_IpId.get(remoteHost);
									String userToDisconnect = usersMap_IdUsername.get(idToDisconnect);
									ChannelGroup channelsToDisconnect = channelsMap_IpChannelGroup.get(remoteHost);
									deleteData(remoteHost, idToDisconnect, userToDisconnect, channelsToDisconnect);
									scheduler.shutdown();
								} else {
									checkPendingMsgMap_IpChecks.put(remoteHost, checkingTimes);
								}
							}
						}
					}
				}, data.getCommunicationTimeout(), data.getCommunicationTimeout(), TimeUnit.SECONDS);
			}
		}
		
		broadcastUsersList(channel);
		
		String systemMsgBody = username + " [" + id + "] connected";
		sendSystemNotification(systemMsgBody);
		
		/** Check if any survey mode is already running. If yes, warn the just connected user. */
		if(data.getMode().equals(Constants.SURVEY_MODE)) {
			if(!data.get_usersIdCompletedSurvey().contains(id)) {
				String numSurvey = data.getNumSurvey();
				String numRound = data.getNumRound();
				sendSurveyToSingleUser(channel, Constants.START_SURVEY, numSurvey, numRound);
			} else {
				SingleUserResponse alreadySurveyedResp = new SingleUserResponse();
				alreadySurveyedResp.setResponse(Constants.ALREADY_SURVEY);
				channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(alreadySurveyedResp)));
			}
		}
	}
	
	private void adminMsg(Channel adminChannel, String msgBody) {
		logger.debug("[ADMIN] " + msgBody);
		String splitter[] = msgBody.split(" ");
		if(splitter.length > 1) {
			String command = splitter[0];
			
			/** Forcing user disconnection */
			if(command.equals(Constants.ADMIN_CMD_DISCONNECT)) {
				String idToDisconnect = splitter[1];
				if(usersMap_IdUsername.containsKey(idToDisconnect) && usersMap_IdIp.containsKey(idToDisconnect)) {
					String userToDisconnect = usersMap_IdUsername.get(idToDisconnect);
					String ipToDisconnect = usersMap_IdIp.get(idToDisconnect);
					ChannelGroup channelsToDisconnect = channelsMap_IpChannelGroup.get(ipToDisconnect);
					deleteData(ipToDisconnect, idToDisconnect, userToDisconnect, channelsToDisconnect);
				} else {
					logger.warn("Non-existing user ID!");
				}
			}
			
			else
			
			/** Start command */
			if(command.equals(Constants.ADMIN_CMD_START)) {
				String parameter = splitter[1];
				if(parameter.equals("survey") && splitter.length > 3) {
					data.get_usersIdCompletedSurvey().clear();
					// Command example: /start survey s1 r2
					if(splitter[2].matches("\\bs\\d+\\b") && splitter[3].matches("\\br\\d+\\b")) {
						String numSurvey = splitter[2].substring(1);
						String numRound = splitter[3].substring(1);
						boolean isSurveyEnabled = broadcastSurvey(adminChannel, Constants.START_SURVEY, numSurvey, numRound);
						if(isSurveyEnabled) {
							data.setMode(Constants.SURVEY_MODE);
							data.setNumSurvey(numSurvey);
							data.setNumRound(numRound);
							Survey surveyData = new Survey();
							surveyData.setNumSurvey(numSurvey);
							surveyData.setNumRound(numRound);
							SurveyResponse adminResp = new SurveyResponse();
							adminResp.setResponse(Constants.SURVEY_MODE);
							adminResp.setData(surveyData);
							adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
							String surveyStringForLog = "### survey s" + numSurvey + " r" + numRound;
							LogInteraction.logInteraction(surveyStringForLog);
						}
					} else {
						String systemMsgBody = "Command not valid!";
						sendSystemNotification(systemMsgBody);
					}
				} else
				if(parameter.equals("chat")) {
					data.setMode(Constants.CHAT_MODE);
					SingleUserResponse adminResp = new SingleUserResponse();
					adminResp.setResponse(Constants.CHAT_MODE);
					adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
					broadcastRespToEveryone(Constants.START_CHAT, null, adminChannel);
					String chatStringForLog = "### chat";
					LogInteraction.logInteraction(chatStringForLog);
				} else {
					String systemMsgBody = "Command not valid!";
					sendSystemNotification(systemMsgBody);
				}
			}
			
			else
			
			/** Message command */
			if(command.equals(Constants.ADMIN_CMD_MSG)) {
				String msgString = "";
				for(int i=1; i<splitter.length; i++) {
					msgString += splitter[i];
					if(i < splitter.length - 1) {
						msgString += " ";
					}
				}
				User allUsers = new User();
				Message msgToBroadcast = new Message();
				msgToBroadcast.setBody(msgString);
				allUsers.setMsg(msgToBroadcast);
				broadcastRespToEveryone(Constants.ADMIN_MSG, allUsers, adminChannel);
			}
			
			else
			
			/** users command: request for the number of connected users */
			if(command.equals(Constants.ADMIN_CMD_USERS)) {
				if(splitter[1].equals("count")) {
					String systemMsgBody = "Connected users: " + usersMap_IdUsername.size();
					sendSystemNotification(systemMsgBody);
				} else
				if(splitter[1].equals("ping")) {
					data.get_usersIdActive().clear();
					broadcastRespToEveryone(Constants.PING, null, adminChannel);
					String systemMsgBody = "Sending PING requests...";
					sendSystemNotification(systemMsgBody);
				} else
				if(splitter[1].equals("active")) {
					Integer usersActive = data.get_usersIdActive().size();
					String systemMsgBody = "";
					if(usersActive == 0) {
						systemMsgBody = "No active users... Have you performed a PING request?";
						sendSystemNotification(systemMsgBody);
						logger.warn(systemMsgBody);
					} else {
						systemMsgBody = "Active users: " + usersActive + ", Set: " + data.get_usersIdActive().toString();
						sendSystemNotification(systemMsgBody);
					}
					/** Resetting active users set to always have an up-to-date set */
					data.get_usersIdActive().clear();
				} else
				if(splitter[1].equals("surveying")) {
					if(data.get_usersIdActive().size() > 0) {
						Set<String> surveyingUsers = new HashSet<String>(data.get_usersIdActive());
						surveyingUsers.removeAll(data.get_usersIdCompletedSurvey());
						String systemMsgBody = "Users still surveying: " + surveyingUsers.size() + ", Set: " + surveyingUsers.toString();
						sendSystemNotification(systemMsgBody);
						/** Resetting active users set to always have an up-to-date set */
						data.get_usersIdActive().clear();
					} else {
						String systemMsgBody = "There are no active users in set. Please performe a PING request!";
						sendSystemNotification(systemMsgBody);
					}
				}
			}
			
			else
			
			/** Kill command */
			if(command.equals(Constants.ADMIN_CMD_KILL)) {
				if(splitter[1].equals("wochat")) {
					System.exit(0);
				}
			}
			
			/** Command not valid */
			else {
				String systemMsgBody = "Command not valid!";
				sendSystemNotification(systemMsgBody);
			}
		}
	}
	
	private void deliverMsg(SingleUserRequest userReq, Channel channel) {		
		String remoteHost = getRemoteHost(channel);
		ChannelGroup channelsSender = channelsMap_IpChannelGroup.get(remoteHost);
		
		// Source user information
		User userFrom = userReq.getData();
		String userIdFrom = userFrom.getId();
		String usernameFrom = userFrom.getUsername();
		
		// Message
		String msgBody = userFrom.getMsg().getBody();
		
		/**************************/
		/** User commands section */
		/**************************/
		
		/** KillMe now */
		if(msgBody.equals(Constants.USER_KILL_ME_CMD)) {
			if(data.getKillme().equals(true)) {
				deleteData(remoteHost, userIdFrom, usernameFrom, channelsSender);
			}
			return;
		}
		
		/*************************/
		
		// Destination user information: not available for commands like /killme now
		User userTo = userFrom.getMsg().getReceiver();
		String userIdTo = userTo.getId();
		
		
		/** Req message must be forwarded to other opened channels of sender */
		if(channelsSender.size() > 1) {
			SingleUserResponse forwardResp = new SingleUserResponse();
			forwardResp.setResponse(Constants.FORWARD_TO_OTHER_CHANNELS);
			forwardResp.setData(userFrom);
			channelsSender.writeAndFlush(new TextWebSocketFrame(gson.toJson(forwardResp)), ChannelMatchers.isNot(channel));
		}
		
		long currentTimestamp = System.currentTimeMillis() / 1000L;
		data.incrementMsgCounter();
		String seqHex = "0x" + String.format("%08x", data.getMsgCounter() & 0xFFFFFFFF);
		
		String receiverIp = usersMap_IdIp.get(userIdTo);
		ChannelGroup channelsReceiver = channelsMap_IpChannelGroup.get(receiverIp);
		
		/** Receiver close the chat's window while forwarding a message to him */
		if(channelsReceiver.size() == 0) {
			logger.error("Message not delivered. ChannelGroup for IP " + receiverIp + " is empty");
			SingleUserResponse userResp = new SingleUserResponse();
			userResp.setResponse(Constants.FAIL_DELIVERING);
			userResp.setData(userFrom);
			channelsSender.writeAndFlush(new TextWebSocketFrame(gson.toJson(userResp)));
			
			broadcastDisconnections(receiverIp);
			return;
		}
		
		/** Adding the sequence number inside the message: this is required by the ACKs mechanism */
		userFrom.getMsg().setSeqHex(seqHex);
		
		/** Delivering... */
		SingleUserResponse userResp = new SingleUserResponse();
		userResp.setResponse(Constants.DELIVER_MSG);
		userResp.setData(userFrom);
		channelsReceiver.writeAndFlush(new TextWebSocketFrame(gson.toJson(userResp)));
		
		/** ACK mechanism (only if the checkingtimes.pendingmessages property is greater than 0) */
		if(data.getMaxCheckingTimes() > 0) {
			if(!ackMap_IpAcks.containsKey(receiverIp)) {
				Set<String> acksToBeReceived = new HashSet<String>();
				acksToBeReceived.add(seqHex);
				ackMap_IpAcks.put(receiverIp, acksToBeReceived);
			} else {
				Set<String> acksToBeReceived = ackMap_IpAcks.get(receiverIp);
				acksToBeReceived.add(seqHex);
			}
			
			logger.debug("#Msg with no ACKS for " + receiverIp + ": " + ackMap_IpAcks.get(receiverIp).size());
		}
		
		/** Logging */
		String interaction = currentTimestamp + Constants.CVS_DELIMITER + userIdFrom + Constants.CVS_DELIMITER + userIdTo + Constants.CVS_DELIMITER + seqHex;
		LogInteraction.logInteraction(interaction);
		String message = "(" + currentTimestamp + ") " + "[" + userIdFrom + "," + userIdTo + "] " + "{" + msgBody + "}";
		LogMessage.logMsg(message);
	}
	
	private void deleteData(String ip, String id, String username, ChannelGroup channels) {
		usernamesSet.remove(username);
		usersMap_IdUsername.remove(id);
		usersMap_IdIp.remove(id);
		usersMap_IpId.remove(ip);
		checkPendingMsgMap_IpChecks.remove(ip);
		ackMap_IpAcks.remove(ip);
		
		for(Channel channel : channels) {
			broadcastChannelGroup.disconnect(ChannelMatchers.is(channel));
			broadcastChannelGroup.remove(channel);
			channel.disconnect();
		}
		
		channelsMap_IpChannelGroup.remove(ip);
		
		LogConnection.logConnection("[DISCONNECTION]\t(" + ip + ", " + id + ", " + username + ") disconnected. #Channels: 0");
		logger.debug("Number of channels in the broadcast group: " + broadcastChannelGroup.size());
		
		/** Broadcast deleting user... */
		MultiUsersResponse userDel = new MultiUsersResponse();
		List<User> userList = new LinkedList<User>();
		userList.add(new User().setId(id).setUsername(username));
		userDel.setResponse(Constants.USERS_REM);
		userDel.setData(userList);
		broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(userDel)));
		
		/** System notification */
		String systemMsgBody = username + " [" + id + "] disconnected";
		sendSystemNotification(systemMsgBody);
	}
	
	private void broadcastDisconnections(String remoteHost) {
		if(usersMap_IpId.containsKey(remoteHost) && channelsMap_IpChannelGroup.containsKey(remoteHost)) {
			String id = usersMap_IpId.get(remoteHost);
			String username = usersMap_IdUsername.get(id);
			
			/** If all user's connections are closed then send an update message to delete user from the list */
			if(channelsMap_IpChannelGroup.get(remoteHost).size() == 0) {
				LogConnection.logConnection("[DISCONNECTION]\t(" + remoteHost + ", " + id + ", " + username + ") disconnected. #Channels: 0");
				
				/** Update the users' list deleting the disconnected user */
				broadcastUserStatus(id, username, null, Constants.USERS_REM);
				
				/** System notification */
				String systemMsgBody = username + " [" + id + "] disconnected";
				sendSystemNotification(systemMsgBody);
			}
		}
	}
	
	private void broadcastUserStatus(String id, String username, Channel channel, String status) {
		/** Creating a JSON object including just the corresponding new user */
		MultiUsersResponse userResp = new MultiUsersResponse();
		List<User> userList = new LinkedList<User>();
		userList.add(new User().setId(id).setUsername(username));
		userResp.setResponse(status);
		userResp.setData(userList);
		
		/** Broadcast new user to everyone (excepting the new user who will processed later) */
		broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(userResp)), ChannelMatchers.isNot(channel));
	}
	
	private void broadcastUsersList(Channel channel) {
		/** Creating a JSON object including all users connected */
		MultiUsersResponse connectedUsersResp = new MultiUsersResponse();
		List<User> connectedUsersList = new LinkedList<User>();
		Set<Map.Entry<String, String>> usersSet = usersMap_IdUsername.entrySet();
		for (Map.Entry<String, String> userIt : usersSet) {
			User user = new User().setId(userIt.getKey()).setUsername(userIt.getValue());
			connectedUsersList.add(user);
	    }
		connectedUsersResp.setResponse(Constants.USERS_ADD);
		connectedUsersResp.setData(connectedUsersList);
		
		/** Send all users list to the user who wants to know that list */
		channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(connectedUsersResp)));
	}
	
	private void broadcastRespToEveryone(String resp, User data, Channel channel) {
		SingleUserResponse respToBeBroadcasted = new SingleUserResponse();
		respToBeBroadcasted.setResponse(resp);
		respToBeBroadcasted.setData(data);
		broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(respToBeBroadcasted)), ChannelMatchers.isNot(channel));
	}
	
	private boolean broadcastSurvey(Channel adminChannel, String command, String numSurvey, String numRound) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(Constants.PATH_SURVEY_FILE + "." + numSurvey));
			List<String> questionsList = new LinkedList<String>();
			String currentLine;
			Integer indexQuestions = 0;
			while((currentLine = br.readLine()) != null) {
				if(currentLine.trim().matches("\\[img\\].+\\[/img\\]")) {
					currentLine = currentLine.trim().replaceAll("(\\[img\\])", "").replaceAll("\\[/img\\]", "");
					if(indexQuestions > 0) {
						String addImg = questionsList.get(indexQuestions-1);
						addImg += "<br /><br />" + "<img src=\"" + currentLine + "\" class=\"img-survey\" />";
						questionsList.set(indexQuestions-1, addImg);
					}
				} else {
					questionsList.add(currentLine.trim());
					indexQuestions++;
				}
			}
			Survey survey = new Survey();
			survey.setNumSurvey(numSurvey);
			survey.setNumRound(numRound);
			survey.setQuestionsList(questionsList);
			SurveyResponse questionsResponse = new SurveyResponse();
			questionsResponse.setResponse(command);
			questionsResponse.setData(survey);
			broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(questionsResponse)), ChannelMatchers.isNot(adminChannel));
			return true;
		} catch (FileNotFoundException e) {
			sendSystemNotification("Error: No file " + Constants.PATH_SURVEY_FILE + "." + numSurvey + " was found");
			logger.error(e.getMessage());
			return false;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return false;
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void sendSurveyToSingleUser(Channel channel, String command, String numSurvey, String numRound) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(Constants.PATH_SURVEY_FILE + "." + numSurvey));
			List<String> questionsList = new LinkedList<String>();
			String currentLine;
			Integer indexQuestions = 0;
			while((currentLine = br.readLine()) != null) {
				if(currentLine.trim().matches("\\[img\\].+\\[/img\\]")) {
					currentLine = currentLine.trim().replaceAll("(\\[img\\])", "").replaceAll("\\[/img\\]", "");
					if(indexQuestions > 0) {
						String addImg = questionsList.get(indexQuestions-1);
						addImg += "<br /><br />" + "<img src=\"" + currentLine + "\" class=\"img-survey\" />";
						questionsList.set(indexQuestions-1, addImg);
					}
				} else {
					questionsList.add(currentLine);
					indexQuestions++;
				}
			}
			Survey survey = new Survey();
			survey.setNumSurvey(numSurvey);
			survey.setNumRound(numRound);
			survey.setQuestionsList(questionsList);
			SurveyResponse questionsResponse = new SurveyResponse();
			questionsResponse.setResponse(command);
			questionsResponse.setData(survey);
			channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(questionsResponse)));
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void sendSystemNotification(String systemMsgBody) {
		if(data.getAdminChannel() != null) {
			if(data.getAdminChannel().isActive()) {
				SingleUserResponse systemResp = new SingleUserResponse();
				systemResp.setResponse(Constants.SYSTEM_NOTIFICATION);
				User systemData = new User();
				Message systemMsg = new Message();
				systemMsg.setBody(systemMsgBody);
				systemData.setMsg(systemMsg);
				systemResp.setData(systemData);
				data.getAdminChannel().writeAndFlush(new TextWebSocketFrame(gson.toJson(systemResp)));
			} else {
				logger.debug("Admin channel not active. Discarding notification...");
			}
		} else {
			logger.debug("Admin not connected. Discarding notification...");
		}
	}
	
	private String getRemoteHost(Channel channel) {
		InetSocketAddress sockAddress = (InetSocketAddress) channel.remoteAddress();
		return sockAddress.getHostString();
	}
	
}
