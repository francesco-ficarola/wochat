package it.uniroma1.dis.wsngroup.wochat.dbfly;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class DataOnTheFly {
	private Map<String, String> usersMap_IpId;
	private Set<String> usernamesSet;
	private Map<String, String> usersMap_IdUsername;
	private Map<String, ChannelGroup> channelsMap_IpChannelGroup;
	private ChannelGroup broadcastChannelGroup;
	
	public DataOnTheFly() {
		/** Hashtable is synchronized: just one invocation at time */
		usersMap_IpId = new Hashtable<String, String>();
		usersMap_IdUsername = new Hashtable<String, String>();
		
		/** This set is synchronized: just one invocation at time */
		usernamesSet = Collections.synchronizedSet(new HashSet<String>());
		
		/** HashMap is NOT synchronized */
		channelsMap_IpChannelGroup = new HashMap<String, ChannelGroup>();
		
		/** Set */
		broadcastChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}

	public Map<String, String> get_usersMap_IpId() {
		return usersMap_IpId;
	}

	public Set<String> get_usernamesSet() {
		return usernamesSet;
	}

	public Map<String, String> get_usersMap_IdUsername() {
		return usersMap_IdUsername;
	}

	public Map<String, ChannelGroup> get_channelsMap_IpChannelGroup() {
		return channelsMap_IpChannelGroup;
	}

	public ChannelGroup get_broadcastChannelGroup() {
		return broadcastChannelGroup;
	}
}
