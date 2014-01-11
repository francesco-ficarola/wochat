package it.uniroma1.dis.wsngroup.wochat.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import it.uniroma1.dis.wsngroup.wochat.dbfly.DataOnTheFly;

public class WoChatInitializer extends ChannelInitializer<SocketChannel>  {
	private DataOnTheFly data;
	
	public WoChatInitializer(DataOnTheFly data) {
		this.data = data;
	}
	
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("codec-http", new HttpServerCodec());
		pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		pipeline.addLast("handler", new WoChatHandler(data));
	}
}
