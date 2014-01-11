package it.uniroma1.dis.wsngroup.wochat.core;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import it.uniroma1.dis.wsngroup.wochat.dbfly.DataOnTheFly;
import it.uniroma1.dis.wsngroup.wochat.dbfly.UserInfo;
import it.uniroma1.dis.wsngroup.wochat.json.MultiUsersInfoResponse;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserInfoRequest;
import it.uniroma1.dis.wsngroup.wochat.json.SingleUserInfoResponse;
import it.uniroma1.dis.wsngroup.wochat.utils.Constants;
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
	
	private Map<String, String> usersMap_IpId;
	private Set<String> usernamesSet;
	private Map<String, String> usersMap_IdUsername;
	private Map<String, ChannelGroup> channelsMap_IpChannelGroup;
	private ChannelGroup broadcastChannelGroup;
	
	public WoChatHandler(DataOnTheFly data) {
		gson = new Gson();
		this.usersMap_IpId = data.get_usersMap_IpId();
		this.usernamesSet = data.get_usernamesSet();
		this.usersMap_IdUsername = data.get_usersMap_IdUsername();
		this.channelsMap_IpChannelGroup = data.get_channelsMap_IpChannelGroup();
		this.broadcastChannelGroup = data.get_broadcastChannelGroup();
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
		
		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
			sendHttpResponse(ctx, req, res);
			return;
		}
		
		/** New user's connection */
		if("/newuser".equals(req.getUri())) {
			ByteBuf receivedContent = req.content();
			if(receivedContent.isReadable()) {
				String receivedMsg = receivedContent.toString(CharsetUtil.UTF_8);
				logger.debug("Request Content: " + receivedMsg);
				String splitter[] = receivedMsg.split("=");
				
				if(splitter.length > 1) {	
					String username = splitter[1];
					SingleUserInfoResponse responseJson = new SingleUserInfoResponse();
					
					if(!usernamesSet.contains(username)) {
						responseJson.setResponse(Constants.SUCCESS);
						responseJson.setData(new UserInfo().setUsername(username));
						usernamesSet.add(username);
					} else {
						responseJson.setResponse(Constants.FAIL);
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
			manageDisconnections(ctx.channel());
			return;
		}
		
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
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
		logger.debug(jsonReq);
		SingleUserInfoRequest userInfoReq = gson.fromJson(jsonReq, SingleUserInfoRequest.class);
		
		/** Message request on the connection status */
		if(userInfoReq.getRequest().equals(Constants.GET_CONN_STATUS)) {
			String remoteHost = getRemoteHost(channel);
			SingleUserInfoResponse userInfoResp = new SingleUserInfoResponse();
			
			logger.debug("Request of connection status by:" + remoteHost);
			
			/** If the user's IP address is already registered, then send user info ... */
			if(usersMap_IpId.containsKey(remoteHost)) {
				String id = usersMap_IpId.get(remoteHost);
				String username = usersMap_IdUsername.get(id);
				UserInfo userInfo = new UserInfo();
				userInfo.setId(id).setUsername(username);
				userInfoResp.setResponse(Constants.REG_USER_STATUS);
				userInfoResp.setData(userInfo);
				logger.debug("Sending response to already registered user: " + id + " - " + username);
			}
			
			/** ... otherwise send new_user_status string */
			else {
				userInfoResp.setResponse(Constants.NEW_USER_STATUS);
				logger.debug("Sending response to the new user");
			}
			
			channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(userInfoResp)));
		}
		
		else
		
		/** Message request to join the chat */
		if(userInfoReq.getRequest().equals(Constants.JOIN_CHAT)) {
			UserInfo userInfo = userInfoReq.getData();
			logger.debug("Request to join chat from: " + userInfo.getUsername());
			
			/** Manage users */
			manageUsers(channel, userInfo.getUsername());
		}
	}
	
	private void manageUsers(Channel channel, String username) {
		String remoteHost = getRemoteHost(channel);
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
			
			/** The user reloaded the page or he disconnected and connected again using just one browser. */
			if(channelsMap_IpChannelGroup.get(remoteHost).size() == 1) {
				
				/** Broadcast new user to everyone */
				broadcastUserStatus(id, username, channel, Constants.USERS_ADD);
			}
		}
		
		/** New User */
		else {
			logger.debug("New IP address");
			id = String.valueOf(usersMap_IpId.size() + Constants.MIN_USERS_ID_RANGE);
			usersMap_IpId.put(remoteHost, id);
			usersMap_IdUsername.put(id, username);
			
			/** Broadcast new user to everyone */
			broadcastUserStatus(id, username, channel, Constants.USERS_ADD);
		}
		
		/** Creating a JSON object including all users connected */
		MultiUsersInfoResponse connectedUsersResp = new MultiUsersInfoResponse();
		List<UserInfo> connectedUsersList = new LinkedList<UserInfo>();
		Set<Map.Entry<String, String>> usersSet = usersMap_IdUsername.entrySet();
		for (Map.Entry<String, String> user : usersSet) {
			UserInfo userInfo = new UserInfo().setId(id).setUsername(user.getValue());
			connectedUsersList.add(userInfo);
	    }
		connectedUsersResp.setResponse(Constants.USERS_ADD);
		connectedUsersResp.setData(connectedUsersList);
		
		/** Send all users list to the connected user (new or already registered) */
		channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(connectedUsersResp)));
	}
	
	private void manageDisconnections(Channel channel) {
		String remoteHost = getRemoteHost(channel);
		String id = usersMap_IpId.get(remoteHost);
		String username = usersMap_IdUsername.get(id);
		
		/** If all user's connections are closed then send an update message to delete user from the list */
		if(channelsMap_IpChannelGroup.get(remoteHost).size() == 0) {
			
			/** Update the users' list deleting the disconnected user */
			broadcastUserStatus(id, username, channel, Constants.USERS_REM);
		}
	}
	
	private void broadcastUserStatus(String id, String username, Channel channel, String status) {
		/** Creating a JSON object including just the corresponding new user */
		MultiUsersInfoResponse userInfoResp = new MultiUsersInfoResponse();
		List<UserInfo> userList = new LinkedList<UserInfo>();
		userList.add(new UserInfo().setId(id).setUsername(username));
		userInfoResp.setResponse(status);
		userInfoResp.setData(userList);
		
		/** Broadcast new user to everyone (excepting the new user who will processed later) */
		broadcastChannelGroup.writeAndFlush(new TextWebSocketFrame(gson.toJson(userInfoResp)), ChannelMatchers.isNot(channel));
	}
	
	private String getRemoteHost(Channel channel) {
		InetSocketAddress sockAddress = (InetSocketAddress) channel.remoteAddress();
		return sockAddress.getHostString();
	}
	
}
