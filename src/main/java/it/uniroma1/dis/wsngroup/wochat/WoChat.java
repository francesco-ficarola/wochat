package it.uniroma1.dis.wsngroup.wochat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.uniroma1.dis.wsngroup.wochat.conf.Constants;
import it.uniroma1.dis.wsngroup.wochat.conf.ServerConfManager;
import it.uniroma1.dis.wsngroup.wochat.core.WoChatInitializer;
import it.uniroma1.dis.wsngroup.wochat.dbfly.DataOnTheFly;

import org.apache.log4j.Logger;

/**
 * @author Francesco Ficarola
 */

public class WoChat {

	private static Logger logger = Logger.getLogger(WoChat.class);
	private final Integer port;

	public WoChat(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		DataOnTheFly data = new DataOnTheFly();
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new WoChatInitializer(data));

			Channel ch = b.bind(port).sync().channel();
			logger.info("Web socket server started at port " + port + '.');
			logger.info("Open your browser and navigate to http://localhost:" + port + '/');

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		Integer port = Integer.parseInt(ServerConfManager.getInstance().getProperty(Constants.CONF_SERVER_PORT));
		try {
			new WoChat(port).run();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
