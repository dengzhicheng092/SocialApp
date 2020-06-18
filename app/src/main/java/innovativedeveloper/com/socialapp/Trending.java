package innovativedeveloper.com.socialapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.FeedItemAdapter;
import innovativedeveloper.com.socialapp.adapter.FeedItemAnimator;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.AppHelper;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

import static java.security.AccessController.getContext;

public class Trending extends AppCompatActivity implements FeedItemAdapter.OnFeedItemClickListener {

    Toolbar toolbar;
    private AdView mAdView;
    RecyclerView recyclerView;
    ProgressBar progressBar;
    ArrayList<Feed> feedItems = new ArrayList<>();
    FeedItemAdapter feedAdapter;
    boolean isFinalList = false;
    boolean isRefreshing = false;
    TextView txtStatus;
    View lyt_error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAdView = (AdView) findViewById(R.id.adView);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        lyt_error = findViewById(R.id.lyt_error);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        feedAdapter = new FeedItemAdapter(this, feedItems);
        feedAdapter.setOnFeedItemClickListener(this);
        feedAdapter.setMoreButtonDisable(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 100;
            }
        };
        txtStatus.setVisibility(View.GONE);
        recyclerView.setAdapter(feedAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
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
                            updateFeed(false);
                            isRefreshing = true;
                        }
                    }
                }
            }
        });

        if (Config.ENABLE_TRENDING_AD) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
        updateFeed(true);
    }

    public void updateFeed(final boolean fromStart) {
        isRefreshing = true;
        lyt_error.setVisibility(View.GONE);
        txtStatus.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_TRENDING_FEED + (!fromStart ? feedItems.size() : 0), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (fromStart) {
                        feedItems.clear();
                    }
                    JSONObject obj = new JSONObject(response);
                    isRefreshing = false;
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
                                User u2 = new User();

                                // User
                                u1.setId(feed.getString("user_id"));
                                u1.setUsername(feed.getString("username"));
                                u1.setName(feed.getString("name"));
                                u1.setProfilePhoto(feed.getString("profilePhoto"));
                                u1.setVerified(feed.getInt("isVerified") == 1);

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
                                    u2.setId(sharedUser.getString("user_id"));
                                    u2.setUsername(sharedUser.getString("username"));
                                    u2.setName(sharedUser.getString("name"));
                                }

                                f.user[0] = u1;
                                f.user[1] = u2;
                                //if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext()) == ConnectionResult.SUCCESS) {
                                if (feedItems.size() % 5 == 0 && feedItems.size() != 0) {
                                    feedItems.add(null);
                                }
                                //}
                                feedItems.add(f);
                            }
                            isFinalList = false;
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            feedAdapter.updateItems(true);
                            txtStatus.setVisibility(feedAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
                        } else {
                            isFinalList = true;
                            if (feedAdapter.getItemCount() < 1) {
                                txtStatus.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        int code = obj.getInt("code");
                        if (code == Config.ACCOUNT_DISABLED) {
                            new AlertDialog.Builder(Trending.this)
                                    .setTitle("Account Disabled")
                                    .setMessage("Your account is disabled for '%reason'.".replace("%reason", obj.getString("reason")))
                                    .setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        } else if (code == Config.SESSION_EXPIRED) {
                            AppHandler.getInstance().getDBHandler().resetDatabase();
                            AppHandler.getInstance().getDataManager().clear();
                            startActivity(new Intent(Trending.this, AppHelper.class));
                        } else {
                            Toast.makeText(Trending.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                catch (JSONException ex) {
                    isRefreshing = false;
                    lyt_error.setVisibility(View.VISIBLE);
                    Log.e("Trending", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Trending.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                lyt_error.setVisibility(View.VISIBLE);
                isRefreshing = false;
                Log.e("Trending", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(Trending.this, Comments.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        intent.putExtra("isOwnPost", (f.getIsShared() == 1 ? f.user[1] : f.user[0]).getUsername().equals(AppHandler.getInstance().getUser().getUsername()));
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onMoreClick(View v, int position) {

    }

    @Override
    public void onProfileClick(View v, int position, int type) {
        UserProfile.startUserProfile(this, type == 1 ? feedItems.get(position).user[1].getUsername() :
                feedItems.get(position).user[0].getUsername(), type == 1 ? feedItems.get(position).user[1].getName() : feedItems.get(position).user[0].getName());
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
                        Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                        feedAdapter.notifyItemChanged(position);
                        Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException ex) {
                    Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                    feedAdapter.notifyItemChanged(position);
                    Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLikeErrorResponse(VolleyError error) {
                Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                feedAdapter.notifyItemChanged(position);
                Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
            }
        });
        AppHandler.getInstance().getAppService().updateLike(feedItems.get(position).getId(), action);
    }

    @Override
    public void onLikesClick(View v, int position) {
        final Intent intent = new Intent(Trending.this, Likes.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        startActivity(intent);
    }

    @Override
    public void onHashTagPressed(String hashTag) {
        innovativedeveloper.com.socialapp.SearchHashtag.startHashtagActivity(hashTag, this);
    }

    @Override
    public void onShareClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu_share);
        if (feedItems.get(position).getAudience() != 2) {
            popupMenu.getMenu().getItem(0).setVisible(false);
            popupMenu.getMenu().getItem(1).setVisible(false);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                boolean isShared = feedItems.get(position).getIsShared() == 1;
                String strName = isShared ? feedItems.get(position).user[1].getName() :
                        feedItems.get(position).user[0].getName();
                final String strPost = getPostType(feedItems.get(position).getType());
                final String postId = isShared ? String.valueOf(feedItems.get(position).getShare_post_id()) : feedItems.get(position).getId();
                if (menuItem.getItemId() == R.id.action_share_public) {
                    new AlertDialog.Builder(Trending.this)
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
                                                    Toast.makeText(Trending.this, "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(Trending.this)
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
                                                    Toast.makeText(Trending.this, "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(Trending.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                    Intent selectActivity = new Intent(Trending.this, FriendsList.class);
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
}
