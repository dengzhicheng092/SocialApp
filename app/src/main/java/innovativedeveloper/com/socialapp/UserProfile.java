package innovativedeveloper.com.socialapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.adapter.FeedItemAdapter;
import innovativedeveloper.com.socialapp.adapter.FeedItemAnimator;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.messaging.ChatActivity;
import innovativedeveloper.com.socialapp.preferences.SettingsActivity;
import innovativedeveloper.com.socialapp.services.AppService;

public class UserProfile extends AppCompatActivity implements FeedItemAdapter.OnFeedItemClickListener, Button.OnClickListener {

    String strUsername, strName, strId;
    CircleImageView icon;
    ImageView verifiedIcon;
    TextView txtName, txtLocation, txtPhotos, txtFriends, txtVideos, txtAbout, txtPosts, emptyFeedView, txtFollowing;
    View viewPosts, viewPhotos, viewFriends, viewVideos, locationBox;
    Toolbar toolbar;
    ProgressBar progressBar;
    LinearLayout aboutBox, postsBox;
    Button btnAddFriend, btnFollow;
    int relation[] = new int[2];
    User crUser;
    ArrayList<Feed> feedItems;
    FeedItemAdapter feedAdapter;
    RecyclerView recyclerView;
    boolean isRefreshing = false;
    boolean isFinalList = false;
    ProgressDialog progressDialog;
    boolean isOwner = false;

    public static void startUserProfile(Activity startingActivity, String username, String name) {
        Intent intent = new Intent(startingActivity, UserProfile.class);
        intent.putExtra("username", username);
        intent.putExtra("name", name);
        startingActivity.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        if (isOwner) {
            menu.findItem(R.id.action_block).setVisible(false);
            menu.findItem(R.id.action_message).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_message) {
            Intent messageIntent = new Intent(this, ChatActivity.class);
            messageIntent.putExtra("username", strUsername);
            messageIntent.putExtra("name", crUser.getName());
            messageIntent.putExtra("icon", crUser.getProfilePhoto());
            messageIntent.putExtra("email", crUser.getEmail());
            messageIntent.putExtra("creation", crUser.getCreation());
            startActivity(messageIntent);
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(UserProfile.this, SettingsActivity.class));
        } else if (item.getItemId() == R.id.action_block) {
            new AlertDialog.Builder(UserProfile.this)
                    .setTitle("Block $name?".replace("$name", strName))
                    .setMessage("Are you sure that you want to block $name?".replace("$name", strName))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.setIndeterminate(true);
                            progressDialog.setCancelable(false);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setMessage("Please wait for a moment...");
                            progressDialog.show();
                            StringRequest request = new StringRequest(Request.Method.PUT, Config.BLOCK_USER.replace(":id", strId), new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    progressDialog.dismiss();
                                    try {
                                        JSONObject obj = new JSONObject(response);
                                        if (!obj.getBoolean("error")) {
                                            Toast.makeText(UserProfile.this, ":user has been blocked.".replace(":user", strName), Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(UserProfile.this, "Your request was not proceed.", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException ex) {
                                        Log.e("UserProfile", "Error: " + ex.getMessage() + "\nResponse: " + response);
                                        Toast.makeText(UserProfile.this, "Your request was not proceed.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressDialog.dismiss();
                                    Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                    Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                }
                            }) {
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    return AppHandler.getInstance().getAuthorization();
                                }
                            };
                            int socketTimeout = 0;
                            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                            request.setRetryPolicy(policy);
                            AppHandler.getInstance().addToRequestQueue(request);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.push_up, R.anim.fade_out);
        setContentView(R.layout.activity_user_profile);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        aboutBox = (LinearLayout) findViewById(R.id.aboutBox);
        postsBox = (LinearLayout) findViewById(R.id.postsBox);
        txtName = (TextView) findViewById(R.id.txtName);
        txtLocation = (TextView) findViewById(R.id.txtLocation);
        txtPhotos = (TextView) findViewById(R.id.txtPhotos);
        txtFriends = (TextView) findViewById(R.id.txtFriends);
        txtVideos = (TextView) findViewById(R.id.txtVideos);
        txtAbout = (TextView) findViewById(R.id.txtAbout);
        icon = (CircleImageView) findViewById(R.id.icon);
        btnAddFriend = (Button) findViewById(R.id.btnAddFriend);
        txtPosts = (TextView) findViewById(R.id.txtPosts);
        btnFollow = (Button) findViewById(R.id.btnFollow);
        verifiedIcon = (ImageView) findViewById(R.id.verifiedIcon);
        txtFollowing = (TextView) findViewById(R.id.txtFollowing);
        viewPosts = findViewById(R.id.view_posts);
        viewPhotos = findViewById(R.id.view_photos);
        viewFriends = findViewById(R.id.lv_friends);
        viewVideos = findViewById(R.id.lv_videos);
        locationBox = findViewById(R.id.locationBox);
        progressDialog = new ProgressDialog(this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        feedItems = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        emptyFeedView = (TextView) findViewById(R.id.empty_feed);
        txtAbout.setMovementMethod(LinkMovementMethod.getInstance());

        feedAdapter = new FeedItemAdapter(this, feedItems);
        feedAdapter.setOnFeedItemClickListener(this);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setAdapter(feedAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new FeedItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int totalItemCount = linearLayoutManager.getItemCount();
                    int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (totalItemCount <= (lastVisibleItem + 1)) {
                        if (!isRefreshing && !isFinalList) {
                            getUserFeed(String.valueOf(feedItems.size()));
                            isRefreshing = true;
                        }
                    }
                }
            }
        });

        strUsername = getIntent().getStringExtra("username");
        if (strUsername.equals(AppHandler.getInstance().getUser().getUsername())) isOwner = true;
        strName = getIntent().getStringExtra("name");
        txtName.setText(strName);
        getUserInfo(strUsername);
        btnAddFriend.setOnClickListener(this);
        btnFollow.setOnClickListener(this);
    }

    private void setupUser(User u) {
        crUser = u;
        strId = u.getId();
        if (isOwner) {
            btnFollow.setVisibility(View.GONE);
            btnAddFriend.setVisibility(View.GONE);
            crUser.setFriend(true);
            invalidateOptionsMenu();
        } else {
            btnFollow.setVisibility(View.VISIBLE);
            btnAddFriend.setVisibility(View.VISIBLE);
        }
        Picasso.with(this).load(u.getProfilePhoto()).placeholder(R.drawable.ic_people).into(icon);
        txtName.setText(u.getName());
        strName = u.getName();
        setLocation(u.getLocation());
        setAbout(u.getDescription());
        txtPosts.setText(String.valueOf(u.totalPosts));
        txtFriends.setText(String.valueOf(u.totalFriends));
        txtPhotos.setText(String.valueOf(u.totalPhotos));
        txtVideos.setText(String.valueOf(u.totalVideos));
        viewPosts.setVisibility(u.totalPhotos == 0 ? View.VISIBLE : View.GONE);
        viewPhotos.setVisibility(u.totalPhotos == 0 ? View.GONE : View.VISIBLE);
        btnFollow.setText(crUser.isFollowing() ? "Following" : getResources().getString(R.string.str_follow));
        setIsFollowed(crUser.isFollower());
        btnFollow.setEnabled(true);
        verifiedIcon.setVisibility(u.isVerified() ? View.VISIBLE : View.GONE);
        setupOptions();
    }

    private void setIsFollowed(boolean isFollowing) {
        if (isFollowing) {
            txtFollowing.setText("$name is also following you.".replace("$name", crUser.getName()));
            txtFollowing.setVisibility(View.VISIBLE);
        } else {
            txtFollowing.setVisibility(View.GONE);
        }
    }

    private void setLocation(String location) {
        if (!location.equals("")) {
            txtLocation.setText(location);
            locationBox.setVisibility(View.VISIBLE);
        } else {
            locationBox.setVisibility(View.GONE);
        }
    }

    private void setAbout(String about) {
        if (!about.equals("")) {
            txtAbout.setText(about);
            aboutBox.setVisibility(View.VISIBLE);
        } else {
            aboutBox.setVisibility(View.GONE);
        }
    }

    void setupOptions() {
        if (relation[0] == 1 && relation[1] == Integer.valueOf(AppHandler.getInstance().getUser().getId())) {
            btnAddFriend.setText(getResources().getString(R.string.request_confirm));
            return;
        } else {
            btnAddFriend.setText(getResources().getString(R.string.request_pending));
        }
        if (relation[0] == 2) {
            btnAddFriend.setText(getResources().getString(R.string.request_friends));
            return;
        }
        if (relation[0] == 0) {
            btnAddFriend.setText(getResources().getString(R.string.add_as_friend));
        }
    }

    public void getUserInfo(String username) {
        progressBar.setVisibility(View.VISIBLE);
        progressDialog.setMessage("Loading profile...");
        progressDialog.show();
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_USER.replace(":user", username), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                progressDialog.dismiss();
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONObject relationObj = obj.getJSONObject("relation");
                        User u = new User();
                        u.setId(obj.getString("id"));
                        u.setUsername(obj.getString("username"));
                        u.setName(obj.getString("name"));
                        u.setEmail(obj.getString("email"));
                        u.setRelationship(obj.getInt("relationship"));
                        u.setGender(obj.getInt("gender"));
                        u.setVerified(obj.getInt("isVerified") == 1);
                        u.setIsDisabled(obj.getInt("isDisabled"));
                        u.setLocation(obj.getString("location"));
                        u.setProfilePhoto(obj.getString("profilePhoto"));
                        u.setDescription(obj.getString("description"));
                        u.setFollower(relationObj.getInt("isFollowed") == 1);
                        u.setFollowing(relationObj.getInt("isFollowing") == 1);
                        u.setFriend(relationObj.getInt("isFriend") == 2);
                        u.totalPosts = obj.getInt("totalPosts");
                        u.totalFollowers = obj.getInt("totalFollowers");
                        u.totalFriends = obj.getInt("totalFriends");
                        u.totalVideos = obj.getInt("totalVideos");
                        u.totalPhotos = obj.getInt("totalPhotos");
                        relation[0] = relationObj.getInt("isFriend");
                        if (relation[0] == 1) {
                            relation[1] = relationObj.getInt("action");
                        }
                        setupUser(u);
                        getUserFeed("0");
                    } else {
                        new AlertDialog.Builder(UserProfile.this)
                                .setTitle("Sorry!")
                                .setCancelable(false)
                                .setMessage("You are either not allowed to view this profile or the user account doesn't exist.")
                                .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        dialog.dismiss();
                                    }
                                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        }).show();
                    }
                }
                catch (JSONException ex) {
                    progressBar.setVisibility(View.GONE);
                    progressDialog.dismiss();
                    Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                progressDialog.dismiss();
                Log.e("UserProfile", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    public void getUserFeed(final String from) {
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_USER_FEED.replace(":user", strUsername) + "?from="+from, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray feeds = obj.getJSONArray("feeds");
                        if (feeds.length() != 0) {
                            for (int i = 0; i < feeds.length(); i++) {
                                JSONObject feed = feeds.getJSONObject(i);
                                JSONObject post = feed.getJSONObject("post");
                                JSONObject sharedUser = null;
                                if (feed.has("sharedUser")) { sharedUser = feed.getJSONObject("sharedUser"); }

                                Feed f = new Feed();
                                User u1 = new User();

                                // Post
                                f.setId(post.getString("postId"));
                                f.setLiked(post.getInt("isLiked") == 1);
                                f.setDescription(post.getString("description"));
                                f.setCreation(post.getString("creation"));
                                f.setType(post.getInt("type"));
                                f.setComments(post.getInt("comments"));
                                f.likes = (post.getInt("likes"));
                                f.setShares(post.getInt("shares"));
                                f.setContent(post.getString("content"));
                                f.setAudience(post.getInt("audience"));
                                f.setIsShared(post.getInt("isShared"));
                                f.setShare_id(post.getString("shareId"));
                                f.setShare_post_id(post.getInt("sharedPostId"));

                                // Original Poster (if any)
                                if (sharedUser != null) {
                                    u1.setId(sharedUser.getString("user_id"));
                                    u1.setUsername(sharedUser.getString("username"));
                                    u1.setName(sharedUser.getString("name"));
                                }

                                f.user[0] = crUser;
                                f.user[1] = u1;
                                feedItems.add(f);
                            }
                            isFinalList = false;
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            feedAdapter.updateItems(true);
                        } else {
                            isFinalList = true;
                            if (feedAdapter.getItemCount() < 1) {
                                emptyFeedView.setText(getResources().getString(R.string.empty_user_feed).replace("$name", strName));
                                emptyFeedView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                emptyFeedView.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        new AlertDialog.Builder(UserProfile.this)
                                .setTitle("Sorry!")
                                .setCancelable(false)
                                .setMessage("You are either not allowed to view this profile or the user account doesn't exist.")
                                .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        dialog.dismiss();
                                    }
                                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        }).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(UserProfile.this, "Couldn't retrieve user feed.", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                Toast.makeText(UserProfile.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    public void actionClick(View view) {
        switch (view.getId()){
            case R.id.lv_friends: {
                Friends.startActivity(this, strUsername, strName);
                break;
            }
            case R.id.lv_photos: {
                MediaActivity.startActivity(this, strUsername, strName, true, false);
                break;
            }
            case R.id.lv_videos: {
                if (crUser.totalVideos > 0) {
                    MediaActivity.startActivity(this, strUsername, strName, false, true);
                }
                break;
            }
        }
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(UserProfile.this, Comments.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        intent.putExtra("isDisabled", feedItems.get(position).getAudience() == 2 && !crUser.isFriend());
        intent.putExtra("isOwnPost", (f.getIsShared() == 1 ? f.user[1] : f.user[0]).getUsername().equals(AppHandler.getInstance().getUser().getUsername()));
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    int selectedItem;
    @Override
    public void onMoreClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu_post_action);
        if (isOwner) {
            popupMenu.getMenu().getItem(1).setVisible(false);
        } else {
            popupMenu.getMenu().getItem(0).setVisible(false);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_delete) {
                    new AlertDialog.Builder(UserProfile.this).setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    progressDialog.setMessage("Deleting...");
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                    StringRequest request = new StringRequest(Request.Method.PUT, Config.DELETE_POST.replace(":postId", feedItems.get(position).getId()), new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            progressDialog.dismiss();
                                            try {
                                                JSONObject obj = new JSONObject(response);
                                                if (!obj.getBoolean("error")) {
                                                    Log.d("position", "p:" + position);
                                                    feedItems.remove(position);
                                                    feedAdapter.notifyItemRemoved(position);
                                                } else {
                                                    Toast.makeText(UserProfile.this, "Unable to delete this post.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(UserProfile.this, "Unknown error while parsing server response.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            progressDialog.dismiss();
                                            Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(UserProfile.this, "Unable to delete the post. There is some error from server-side or internet connection.", Toast.LENGTH_SHORT).show();
                                        }
                                    }) {
                                        @Override
                                        public Map<String, String> getHeaders() throws AuthFailureError {
                                            return AppHandler.getInstance().getAuthorization();
                                        }
                                    };
                                    int socketTimeout = 0;
                                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                                    request.setRetryPolicy(policy);
                                    AppHandler.getInstance().addToRequestQueue(request);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).setCancelable(false).show();
                    return true;
                } else if (item.getItemId() == R.id.action_report_post) {
                    selectedItem = -1;
                    final CharSequence[] items = {" It's annoying or not interesting ", " It contains explicit content ", " I think it shouldn\'t be here. ", " Other "};
                    final AlertDialog.Builder builder = new AlertDialog.Builder(UserProfile.this);
                    builder.setTitle("What\'s happening?");
                    builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch(item)
                            {
                                case 0:
                                    selectedItem = 0;
                                    break;
                                case 1:
                                    selectedItem = 1;
                                    break;
                                case 2:
                                    selectedItem = 2;
                                    break;
                                case 3:
                                    selectedItem = 3;
                                    break;
                            }
                        }
                    }).setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String name = feedItems.get(position).user[0].getName();
                            proceedReport(String.valueOf(feedItems.get(position).getId()), selectedItem, name);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
                }
                return false;
            }
        });
        popupMenu.show();
    }


    void proceedReport(final String postId, final int action, String name) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(UserProfile.this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        dialogBuilder.setView(dialogView);
        final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
        dialogBuilder.setTitle("Reporting $name\'s post".replace("$name", name));
        dialogBuilder.setMessage("Tell us more about it:");
        dialogBuilder.setPositiveButton("Report", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                StringRequest request = new StringRequest(Request.Method.POST, Config.REPORT_POST.replace(":id", postId), new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                Toast.makeText(UserProfile.this, "Report submitted.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException ex) {
                            Log.e("UserProfile", "Error: " + ex.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("UserProfile", "Error: " + error.getMessage());
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        return AppHandler.getInstance().getAuthorization();
                    }

                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("action", String.valueOf(action));
                        params.put("reason", editText.getText().toString());
                        return params;
                    }
                };
                int socketTimeout = 0;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                request.setRetryPolicy(policy);
                AppHandler.getInstance().addToRequestQueue(request);
                dialog.dismiss();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    public void onProfileClick(View v, int position, int type) {

    }

    @Override
    public void onLikeClick(View v, final int position, final int action) {
        AppHandler.getInstance().getAppService().setOnLikeChanged(new AppService.OnLikeChanged() {
            @Override
            public void onLikeChanged(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        feedItems.get(position).setLiked(action == 1);
                    } else {
                        Log.e("UserProfile", "Server response: " + obj.getString("code"));
                        feedAdapter.notifyItemChanged(position);
                        Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException ex) {
                    Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                    feedAdapter.notifyItemChanged(position);
                    Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLikeErrorResponse(VolleyError error) {
                Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                feedAdapter.notifyItemChanged(position);
                Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
            }
        });
        AppHandler.getInstance().getAppService().updateLike(feedItems.get(position).getId(), action);
    }

    @Override
    public void onLikesClick(View v, int position) {
        final Intent intent = new Intent(this, Likes.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        startActivity(intent);
    }

    @Override
    public void onHashTagPressed(String hashTag) {
        SearchHashtag.startHashtagActivity(hashTag, this);
    }

    @Override
    public void onShareClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu_share);
        if (feedItems.get(position).getAudience() != 2 ||
                feedItems.get(position).user[0].getUsername().equals(AppHandler.getInstance().getUser().getUsername())) {
            popupMenu.getMenu().getItem(0).setVisible(false);
            popupMenu.getMenu().getItem(1).setVisible(false);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            boolean isShared = feedItems.get(position).getIsShared() == 1;
            String strName = isShared ? feedItems.get(position).user[1].getName() :
                    feedItems.get(position).user[0].getName();
            final String strPost = getPostType(feedItems.get(position).getType());
            final String postId = isShared ? String.valueOf(feedItems.get(position).getShare_post_id()) : feedItems.get(position).getId();
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_share_public) {
                    new AlertDialog.Builder(UserProfile.this)
                            .setTitle("Share $name\'s $post".replace("$name", strName).replace("$post", strPost))
                            .setMessage("Do you want to share $name\'s $post publicly?".replace("$name", strName).replace("$post", strPost))
                            .setPositiveButton("Share now", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    StringRequest request = new StringRequest(Request.Method.POST, Config.SHARE_POST.replace(":postId", postId), new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject obj = new JSONObject(response);
                                                if (!obj.getBoolean("error")) {
                                                    Toast.makeText(UserProfile.this, "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                        }
                                    }) {
                                        @Override
                                        protected Map<String, String> getParams() throws AuthFailureError {
                                            Map<String, String> params = new HashMap<>();
                                            params.put("audience", "2");
                                            return params;
                                        }

                                        @Override
                                        public Map<String, String> getHeaders() throws AuthFailureError {
                                            return AppHandler.getInstance().getAuthorization();
                                        }
                                    };
                                    int socketTimeout = 0;
                                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                                    request.setRetryPolicy(policy);
                                    AppHandler.getInstance().addToRequestQueue(request);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })
                            .show();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_share_custom) {
                    new AlertDialog.Builder(UserProfile.this)
                            .setTitle("Share $name\'s $post".replace("$name", strName).replace("$post", strPost))
                            .setMessage("Do you want to share $name\'s $post with friends and followers?".replace("$name", strName).replace("$post", strPost))
                            .setPositiveButton("Share now", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    StringRequest request = new StringRequest(Request.Method.POST, Config.SHARE_POST.replace(":postId", postId), new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject obj = new JSONObject(response);
                                                if (!obj.getBoolean("error")) {
                                                    Toast.makeText(UserProfile.this, "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(UserProfile.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                        }
                                    }) {
                                        @Override
                                        protected Map<String, String> getParams() throws AuthFailureError {
                                            Map<String, String> params = new HashMap<>();
                                            params.put("audience", "1");
                                            return params;
                                        }

                                        @Override
                                        public Map<String, String> getHeaders() throws AuthFailureError {
                                            return AppHandler.getInstance().getAuthorization();
                                        }
                                    };
                                    int socketTimeout = 0;
                                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                                    request.setRetryPolicy(policy);
                                    AppHandler.getInstance().addToRequestQueue(request);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })
                            .show();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_share_message) {
                    Intent selectActivity = new Intent(UserProfile.this, FriendsList.class);
                    selectActivity.putExtra("isShare", true);
                    selectActivity.putExtra("shareMessage", feedItems.get(position).user[0].getName() + ":" + feedItems.get(position).getId());
                    startActivity(selectActivity);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnAddFriend) {
            if (relation[0] == 2) {
                new AlertDialog.Builder(UserProfile.this)
                        .setTitle("You and $name are friends.".replace("$name", strName))
                        .setMessage("You both are friends already! Select with specified options below to continue.")
                        .setPositiveButton("Remove as friend", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                btnAddFriend.setEnabled(false);
                                StringRequest request = new StringRequest(Request.Method.POST, Config.REMOVE_FRIEND.replace(":id", strId), new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        btnAddFriend.setEnabled(true);
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            if (!obj.getBoolean("error")) {
                                                crUser.setFriend(false);
                                                relation[0] = 0;
                                                setupUser(crUser);
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        btnAddFriend.setEnabled(true);
                                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                    }
                                }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        return AppHandler.getInstance().getAuthorization();
                                    }
                                };
                                int socketTimeout = 0;
                                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                                request.setRetryPolicy(policy);
                                AppHandler.getInstance().addToRequestQueue(request);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        }).show();
            } else if (relation[0] == 0) {
                btnAddFriend.setEnabled(false);
                btnAddFriend.setText(getResources().getString(R.string.request_pending));
                StringRequest request = new StringRequest(Request.Method.POST, Config.ADD_FRIEND.replace(":id", strId), new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        btnAddFriend.setEnabled(true);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                relation[0] = 1;
                                setupUser(crUser);
                            }
                        } catch (JSONException ex) {
                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        btnAddFriend.setEnabled(true);
                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        return AppHandler.getInstance().getAuthorization();
                    }
                };
                int socketTimeout = 0;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                request.setRetryPolicy(policy);
                AppHandler.getInstance().addToRequestQueue(request);
            } else if (relation[0] == 1 && relation[1] == Integer.valueOf(strId)) {
                new AlertDialog.Builder(UserProfile.this)
                        .setTitle("Request on pending".replace("$name", strName))
                        .setMessage("Your request is still pending. Select from the specified options below.")
                        .setPositiveButton("Cancel request", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                StringRequest request = new StringRequest(Request.Method.POST, Config.REMOVE_FRIEND.replace(":id", strId), new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        btnAddFriend.setEnabled(true);
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            if (!obj.getBoolean("error")) {
                                                relation[0] = 0;
                                                setupUser(crUser);
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        btnAddFriend.setEnabled(true);
                                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                    }
                                }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        return AppHandler.getInstance().getAuthorization();
                                    }
                                };
                                int socketTimeout = 0;
                                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                                request.setRetryPolicy(policy);
                                AppHandler.getInstance().addToRequestQueue(request);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Lets wait.", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
            } else if (relation[0] == 1 && relation[1] == Integer.valueOf(AppHandler.getInstance().getUser().getId())) {
                btnAddFriend.setEnabled(false);
                StringRequest request = new StringRequest(Request.Method.POST, Config.CONFIRM_FRIEND.replace(":id", strId), new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        btnAddFriend.setEnabled(true);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                relation[0] = 2;
                                setupUser(crUser);
                            }
                        } catch (JSONException ex) {
                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        btnAddFriend.setEnabled(true);
                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        return AppHandler.getInstance().getAuthorization();
                    }
                };
                int socketTimeout = 0;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                request.setRetryPolicy(policy);
                AppHandler.getInstance().addToRequestQueue(request);
            }
        } else if (v.getId() == R.id.btnFollow) {
            if (crUser.isFollowing()) {
                new AlertDialog.Builder(UserProfile.this)
                        .setTitle("You are following $name".replace("$name", strName))
                        .setMessage("You are already following $name. Select from the specified options below.".replace("$name", strName))
                        .setPositiveButton("Unfollow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                btnFollow.setEnabled(false);
                                StringRequest request = new StringRequest(Request.Method.POST, Config.UNFOLLOW.replace(":user", strUsername), new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            if (!obj.getBoolean("error")) {
                                                Toast.makeText(UserProfile.this, "You're no longer following $name.".replace("$name", strName), Toast.LENGTH_SHORT).show();
                                                btnFollow.setEnabled(true);
                                                crUser.setFollowing(false);
                                                setupUser(crUser);
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                    }
                                }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        return AppHandler.getInstance().getAuthorization();
                                    }
                                };
                                AppHandler.getInstance().addToRequestQueue(request);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        }).show();
            } else {
                new AlertDialog.Builder(UserProfile.this)
                        .setTitle("Follow $name".replace("$name", strName))
                        .setMessage("Do you want to follow $name?".replace("$name", strName))
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                btnFollow.setEnabled(false);
                                StringRequest request = new StringRequest(Request.Method.POST, Config.FOLLOW.replace(":user", strUsername), new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            if (!obj.getBoolean("error")) {
                                                Toast.makeText(UserProfile.this, "You're now following.", Toast.LENGTH_SHORT).show();
                                                btnFollow.setEnabled(true);
                                                crUser.setFollowing(true);
                                                setupUser(crUser);
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("UserProfile", "Unexpected error: " + ex.getMessage());
                                            Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.e("UserProfile", "Unexpected error: " + error.getMessage());
                                        Toast.makeText(UserProfile.this, "Unable to process your request.", Toast.LENGTH_SHORT).show();
                                    }
                                }) {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        return AppHandler.getInstance().getAuthorization();
                                    }
                                };
                                AppHandler.getInstance().addToRequestQueue(request);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }
    }

    @Override
    public void onVideoThumbnailClick(View v, int position) {
        VideoActivity.startActivity(this, feedItems.get(position).getContent());
    }

    @Override
    public void onImageClick(View v, int position) {
        Intent intent = new Intent(this, ActivityPhoto.class);
        intent.putExtra("name", feedItems.get(position).user[0].getUsername());
        intent.putExtra("content", feedItems.get(position).getContent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "photo");
            startActivity(intent, options.toBundle());
        }
        else {
            startActivity(intent);
        }
    }

    private String getPostType(int type) {
        switch (type) {
            case 1: {
                return "photo";
            }
            case 3: {
                return "video";
            }
            default: {
                return "post";
            }
        }
    }
}
