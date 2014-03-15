package it.uniroma1.dis.wsngroup.wochat.core;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import it.uniroma1.dis.wsngroup.wochat.dbfly.DataOnTheFly;
import it.uniroma1.dis.wsngroup.wochat.dbfly.Message;
import it.uniroma1.dis.wsngroup.wochat.dbfly.Questions;
import it.uniroma1.dis.wsngroup.wochat.dbfly.User;
import it.uniroma1.dis.wsngroup.wochat.json.MultiUsersResponse;
import it.uniroma1.dis.wsngroup.wochat.json.QuestionsResponse;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserRequest;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserResponse;
import it.uniroma1.dis.wsngroup.wochat.logging.LogAnswers1;
import it.uniroma1.dis.wsngroup.wochat.logging.LogAnswers2;
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
		
		if(req.getUri().matches(".*\\.(png|jpg)$")) {
			/** Image loading... */
			BufferedImage originalImage = ImageIO.read(new File("./web/" + req.getUri()));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(req.getUri().matches(".*\\.png$")) {
				ImageIO.write( originalImage, "png", baos );
			} else
			if(req.getUri().matches(".*\\.jpg$")) {
				ImageIO.write( originalImage, "jpg", baos );
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
			} 
			
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
						responseJson.setResponse(Constants.HTTP_ADMIN_SUCCESS_CONN);
						responseJson.setData(new User().setUsername(username));
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
			
		/** Answers to 1st survey */
		if(userReq.getRequest().equals(Constants.ANSWERS_SURVEY1)) {
			String id = userReq.getData().getId();
			List<String> answers = userReq.getData().getAnswersSurvey1();
			String line = id + Constants.CVS_DELIMITER;
			for(int i=0; i<answers.size(); i++) {
				line += answers.get(i);
				if(i < answers.size() - 1) {
					line += Constants.CVS_DELIMITER;
				}
			}
			LogAnswers1.logAnswer(line);
			
			//TODO: check if all participants have replied; if yes automatically enable chat
		}
		
		else
			
		/** Answers to 2nd survey */
		if(userReq.getRequest().equals(Constants.ANSWERS_SURVEY2)) {
			String id = userReq.getData().getId();
			List<String> answers = userReq.getData().getAnswersSurvey2();
			String line = id + Constants.CVS_DELIMITER;
			for(int i=0; i<answers.size(); i++) {
				line += answers.get(i);
				if(i < answers.size() - 1) {
					line += Constants.CVS_DELIMITER;
				}
			}
			LogAnswers2.logAnswer(line);
			
			//TODO: check if all participants have replied; if yes automatically enable chat
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
			
			long currentTimestamp = System.currentTimeMillis() / 1000L;
			data.incrementMsgCounter();
			String seqHex = "0x" + String.format("%08x", data.getMsgCounter() & 0xFFFFFFFF);
			String sighting = "S t=" + currentTimestamp + " ip=0x00000000" + " id=" + id + " boot_count=0" + " seq=" + seqHex + " strgth=3 flgs=0 last_seen=0";
			LogInteraction.logInteraction(sighting);
			
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
		
		/** Check if any survey mode is already running. If yes, warn the just connected user. */
		if(data.getMode().equals(Constants.SURVEY1_MODE)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(Constants.PATH_SURVEY_FILE));
				List<String> questionsList = new LinkedList<String>();
				String currentLine;
				while((currentLine = br.readLine()) != null) {
					questionsList.add(currentLine);
				}
				Questions questions = new Questions();
				questions.setQuestionsList(questionsList);
				QuestionsResponse questionsResponse = new QuestionsResponse();
				questionsResponse.setResponse(Constants.START_SURVEY_1);
				questionsResponse.setData(questions);
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
		} else
		if(data.getMode().equals(Constants.SURVEY2_MODE)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(Constants.PATH_SURVEY_FILE));
				List<String> questionsList = new LinkedList<String>();
				String currentLine;
				while((currentLine = br.readLine()) != null) {
					questionsList.add(currentLine);
				}
				Questions questions = new Questions();
				questions.setQuestionsList(questionsList);
				QuestionsResponse questionsResponse = new QuestionsResponse();
				questionsResponse.setResponse(Constants.START_SURVEY_2);
				questionsResponse.setData(questions);
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
					SingleUserResponse adminResp = new SingleUserResponse();
					adminResp.setResponse(Constants.USER_KICKED);
					User userKicked = new User();
					userKicked.setUsername(userToDisconnect);
					adminResp.setData(userKicked);
					adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
				} else {
					logger.warn("Non-existing user ID!");
				}
			}
			
			else
			
			/** Start command */
			if(command.equals(Constants.ADMIN_CMD_START)) {
				String parameter = splitter[1];
				if(parameter.equals("survey1")) {
					data.setMode(Constants.SURVEY1_MODE);
					SingleUserResponse adminResp = new SingleUserResponse();
					adminResp.setResponse(Constants.SURVEY1_MODE);
					adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
					broadcastSurvey(adminChannel, Constants.START_SURVEY_1);
				} else
				if(parameter.equals("survey2")) {
					data.setMode(Constants.SURVEY2_MODE);
					SingleUserResponse adminResp = new SingleUserResponse();
					adminResp.setResponse(Constants.SURVEY2_MODE);
					adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
					broadcastSurvey(adminChannel, Constants.START_SURVEY_2);
				} else
				if(parameter.equals("chat")) {
					data.setMode(Constants.CHAT_MODE);
					SingleUserResponse adminResp = new SingleUserResponse();
					adminResp.setResponse(Constants.CHAT_MODE);
					adminChannel.writeAndFlush(new TextWebSocketFrame(gson.toJson(adminResp)));
					broadcastRespToEveryone(Constants.START_CHAT, null, adminChannel);
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
			
			/** Kill command */
			if(command.equals(Constants.ADMIN_CMD_KILL)) {
				if(splitter[1].equals("wochat")) {
					System.exit(0);
				}
			}
		}
	}
	
	private void deliverMsg(SingleUserRequest userReq, Channel channel) {		
		String remoteHost = getRemoteHost(channel);
		ChannelGroup channelsSender = channelsMap_IpChannelGroup.get(remoteHost);
		
		User userFrom = userReq.getData();
		String userIdFrom = userFrom.getId();
		String usernameFrom = userFrom.getUsername();
		User userTo = userFrom.getMsg().getReceiver();
		String userIdTo = userTo.getId();
		String msgBody = userFrom.getMsg().getBody();
		
		/** KillMe now */
		if(msgBody.equals(Constants.USER_KILL_ME_CMD)) {
			if(data.getKillme().equals(true)) {
				deleteData(remoteHost, userIdFrom, usernameFrom, channelsSender);
			}
			return;
		}
		
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
		String interaction = "C t=" + currentTimestamp + " ip=0x00000000" + " id=" + userIdFrom + " boot_count=0" + " seq=" + seqHex + "[" + userIdTo + "(0)" + " #1]";
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
	
	private void broadcastSurvey(Channel adminChannel, String surveyType) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(Constants.PATH_SURVEY_FILE));
			List<String> questionsList = new LinkedList<String>();
			String currentLine;
			while((currentLine = br.readLine()) != null) {
				questionsList.add(currentLine);
			}
			Questions questions = new Questions();
			questions.setQuestionsList(questionsList);
			QuestionsResponse questionsResponse = new QuestionsResponse();
			questionsResponse.setResponse(surveyType);
			questionsResponse.setData(questions);
			broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(questionsResponse)), ChannelMatchers.isNot(adminChannel));
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
	
	private String getRemoteHost(Channel channel) {
		InetSocketAddress sockAddress = (InetSocketAddress) channel.remoteAddress();
		return sockAddress.getHostString();
	}
	
}
