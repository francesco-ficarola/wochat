package it.uniroma1.dis.wsngroup.wochat.conf;

public class Constants {
	public static final String PATH_CONF_FILE = "conf/woc.properties";
	public static final String CONF_SERVER_PORT = "server.port";
	public static final String COMMUNICATION_TIMEOUT = "communication.timeout";
	public static final String ADMIN_USERNAME = "admin.username";
	public static final String MAX_CHECKING_TIMES = "checkingtimes.pendingmessages";
	public static final String KILL_ME_CONF = "killme.now";
	
	public static final String CVS_DELIMITER = "; ";
	public static final String WEBSOCKET_PATH = "/chat";
	public static final int MIN_USERS_ID_RANGE = 1000;
	public static final String USERS_ADD = "users_add";
	public static final String USERS_REM = "users_rem";
	public static final String GET_CONN_STATUS = "get_conn_status";
	public static final String NEW_USER_STATUS = "new_user_status";
	public static final String REG_USER_STATUS = "reg_user_status";
	public static final String JOIN_CHAT = "join_chat";
	public static final String HTTP_SUCCESS_CONN = "success_conn";
	public static final String HTTP_FAIL_CONN = "fail_conn";
	public static final String HTTP_ALREADY_CONN = "already_conn";
	public static final String DELIVER_MSG = "deliver_msg";
	public static final String ADMIN_DELIVER_MSG = "admin_deliver_msg";
	public static final String FAIL_DELIVERING = "fail_delivering";
	public static final String FORWARD_TO_OTHER_CHANNELS = "forward_to_other_channels";
	public static final String ACK_MSG = "ack";
	
	public static final String USER_KILL_ME_CMD = "/killme now";
	public static final String ADMIN_CMD_DISCONNECT = "/disconnect";
	public static final String ADMIN_CMD_START = "/start";
	public static final String ADMIN_CMD_KILL = "/kill";
	public static final String ADMIN_CMD_MSG = "/msg";
	public static final String ADMIN_CMD_USERS = "/users";
	
	public static final String PATH_SURVEY_FILE = "conf/woc.survey";
	public static final String START_SURVEY = "start_survey";
	public static final String ANSWERS_SURVEY = "answers_survey";
	public static final String START_CHAT = "start_chat";

	public static final String HTTP_ADMIN_SUCCESS_CONN = "admin_success_conn";
	public static final String HTTP_ADMIN_FAIL_CONN = "admin_fail_conn";
	public static final String ADMIN_READY = "admin_ready";
	public static final String SYSTEM_NOTIFICATION = "system_notification";
	public static final String CHAT_MODE = "chat_mode";
	public static final String SURVEY_MODE = "survey_mode";
	public static final String ADMIN_MSG = "admin_msg";
	public static final String PING = "ping";
	public static final String PONG = "pong";
	
}
