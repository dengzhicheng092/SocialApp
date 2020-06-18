package innovativedeveloper.com.socialapp.config;

public class Config {

    // Response / Error codes
    public static final int USER_ALREADY_EXISTS = 5;
    public static final int USER_INVALID = 6;
    public static final int UNKNOWN_ERROR = 404;
    public static final int PASSWORD_INCORRECT = 10;
    public static final int ACCOUNT_DISABLED = 11;
    public static final int EMAIL_ALREADY_EXISTS = 12;
    public static final int SESSION_EXPIRED = 440;

    // Server Config
    private static final String BASE_URI = "http://<host-ip-address>/socialapp-api/index.php";
    public static final String REGISTER = BASE_URI + "/user/register";
    public static final String LOGIN = BASE_URI + "/user/login";
    public static final String AUTHORIZE = BASE_URI + "/user/authorize";
    public static final String LOAD_FEED = BASE_URI + "/user/feed/";
    public static final String LOAD_TRENDING_FEED = BASE_URI + "/user/trend/";
    public static final String LOAD_LIKES = BASE_URI + "/feed/likes/";
    public static final String LOAD_COMMENTS = BASE_URI + "/feed/comments/";
    public static final String LOAD_REPLIES = BASE_URI + "/comment/replies/";
    public static final String UPDATE_LIKE = BASE_URI + "/post/like/:id";
    public static final String ADD_COMMENT = BASE_URI + "/post/comment/:id";
    public static final String ADD_REPLY = BASE_URI + "/comment/reply/:commentId";
    public static final String DELETE_COMMENT = BASE_URI + "/delete/comment/:postId";
    public static final String DELETE_REPLY = BASE_URI + "/delete/reply/:commentId";
    public static final String DELETE_POST = BASE_URI + "/delete/post/:postId";
    public static final String GET_USER = BASE_URI + "/user/info/:user";
    public static final String GET_USER_FEED = BASE_URI + "/feed/:user";
    public static final String GET_MEDIA_FEED = BASE_URI + "/feed/media/:id";
    public static final String GET_FRIENDS = BASE_URI + "/user/friends/";
    public static final String GET_FOLLOWERS = BASE_URI + "/user/followers/";
    public static final String GET_FOLLOWINGS = BASE_URI + "/user/followings/";
    public static final String ADD_FRIEND = BASE_URI + "/user/add_friend/:id";
    public static final String REMOVE_FRIEND = BASE_URI + "/user/remove_friend/:id";
    public static final String CONFIRM_FRIEND = BASE_URI + "/user/confirm_friend/:id";
    public static final String FOLLOW = BASE_URI + "/user/follow/:user";
    public static final String UNFOLLOW = BASE_URI + "/user/unfollow/:user";
    public static final String SEARCH = BASE_URI + "/users/directory/:toFind";
    public static final String NEARBY = BASE_URI + "/users/nearby";
    public static final String CHECK_HASHTAG = BASE_URI + "/hashtag/:hashtag";
    public static final String SEARCH_HASHTAG = BASE_URI + "/hashtag/feed/:hashtag";
    public static final String SHARE_POST = BASE_URI + "/post/share/:postId";
    public static final String UPDATE_NOTIFICATION = BASE_URI + "/user/notification/:id";
    public static final String UPDATE_MESSAGE = BASE_URI + "/user/message_read/:id";
    public static final String RETRIEVE_REQUESTS_LIST = BASE_URI + "/user/request_list/:from";
    public static final String GET_POST = BASE_URI + "/user/post/:postId";
    public static final String MESSAGE_USER = BASE_URI + "/user/message/:id";
    public static final String LOAD_BLOCK_LIST = BASE_URI + "/user/blockList";
    public static final String UNBLOCK_USER = BASE_URI + "/user/unblock/:id";
    public static final String UPDATE_SETTINGS = BASE_URI + "/user/settings";
    public static final String ADD_POST = BASE_URI + "/user/add_post";
    public static final String UPLOAD_URI = BASE_URI.replace("/index.php", "/upload.php");
    public static final String IMAGE_UPLOAD_URI = BASE_URI + "/upload";
    public static final String PROFILE_PHOTO_UPLOAD = BASE_URI + "/user/update_picture";
    public static final String BLOCK_USER = BASE_URI + "/user/block/:id";
    public static final String DELETE_ACCOUNT = BASE_URI + "/account/delete";
    public static final String REPORT_POST = BASE_URI + "/post/report/:id";

    // Notification Type
    public static final int PUSH_TYPE_NOTIFICATION = 1;
    public static final int PUSH_TYPE_REQUESTS = 2;
    public static final int PUSH_TYPE_MESSAGE = 3;
    public static final int PUSH_TYPE_REPLY = 4;

    // In-app Config
    public static final int FEED_TYPE_DEFAULT = 1;
    public static final int FEED_TYPE_VIDEO = 2;
    public static final int FEED_AD_TYPE = 3;
    public static final int FEED_TYPE_PHOTO = 4;
    public static final String ACTION_LIKE_BUTTON_CLICKED = "action_like_button_button";

    // Advertisement Config
    public static final boolean ENABLE_INBOX_BANNER = true;
    public static final boolean ENABLE_TRENDING_AD = true;
    public static final boolean ENABLE_ACTIVITY_POST_BANNER = true;
    public static final boolean ENABLE_ACTIVITY_UPLOAD_BANNER = true;
    public static final boolean ENABLE_ACTIVITY_HASHTAG_BANNER = true;
    public static final boolean ENABLE_ACTIVITY_SEARCH_BANNER = true;
    public static final boolean ENABLE_ACTIVITY_COMMENTS_BANNER = true;
    public static final boolean ENABLE_ACTIVITY_MEDIA_BANNER = true;

    public static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000;
    public static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 5;

}
