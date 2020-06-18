package innovativedeveloper.com.socialapp;

import android.app.Activity;
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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
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
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.FeedItemAdapter;
import innovativedeveloper.com.socialapp.adapter.FeedItemAnimator;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

public class SearchHashtag extends AppCompatActivity implements FeedItemAdapter.OnFeedItemClickListener, View.OnClickListener {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    ArrayList<Feed> feedItems;
    FeedItemAdapter feedAdapter;
    View errorLayout;
    View brokenLayout;
    String searchQuery;
    Toolbar toolbar;
    AdView mAdView;

    public static void startHashtagActivity(String searchQuery, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, SearchHashtag.class);
        intent.putExtra("searchQuery", searchQuery);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_hashtag);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mAdView = (AdView) findViewById(R.id.adView);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        brokenLayout = findViewById(R.id.lyt_broken);
        errorLayout =  findViewById(R.id.lyt_error);
        errorLayout.setOnClickListener(this);
        feedItems = new ArrayList<>();
        feedAdapter = new FeedItemAdapter(this, feedItems);
        feedAdapter.setOnFeedItemClickListener(this);
        feedAdapter.setMoreButtonDisable(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 100;
            }
        };
        recyclerView.setAdapter(feedAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new FeedItemAnimator());
        searchQuery = getIntent().getStringExtra("searchQuery");
        setTitle("Search #" + searchQuery);
        searchHashtag(searchQuery);

        if (Config.ENABLE_ACTIVITY_HASHTAG_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.lyt_error) {
            searchHashtag(searchQuery);
        }
    }

    private void setTitle(String title) {
        toolbar.setTitle(title);
    }

    void searchHashtag(String searchQuery) {
        errorLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.SEARCH_HASHTAG.replace(":hashtag", searchQuery), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.INVISIBLE);
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
                                if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(SearchHashtag.this) == 1) {
                                    if (feedItems.size() % 5 == 0 && feedItems.size() != 0) {
                                        feedItems.add(null);
                                    }
                                }
                                feedItems.add(f);
                            }
                            feedAdapter.updateItems(true);
                        } else {
                            brokenLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        brokenLayout.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException ex) {
                    Log.e("ActivityPost", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.INVISIBLE);
                errorLayout.setVisibility(View.VISIBLE);
                Log.e("ActivityPost", "Unexpected error: " + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return super.getParams();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(SearchHashtag.this, Comments.class);
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
                        Log.e("SearchHashtag", "Server response: " + obj.getString("code"));
                        feedAdapter.notifyItemChanged(position);
                        Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("SearchHashtag", "Unexpected error: " + ex.getMessage());
                    feedAdapter.notifyItemChanged(position);
                    Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLikeErrorResponse(VolleyError error) {
                Log.e("SearchHashtag", "Unexpected error: " + error.getMessage());
                feedAdapter.notifyItemChanged(position);
                Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_share_public) {
                    boolean isShared = feedItems.get(position).getIsShared() == 1;
                    String strName = isShared ? feedItems.get(position).user[1].getName() :
                            feedItems.get(position).user[0].getName();
                    final String strPost = getPostType(feedItems.get(position).getType());
                    final String postId = isShared ? String.valueOf(feedItems.get(position).getShare_post_id()) : feedItems.get(position).getId();
                    new AlertDialog.Builder(SearchHashtag.this)
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
                                                    Toast.makeText(SearchHashtag.this, "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(SearchHashtag.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                    boolean isShared = feedItems.get(position).getIsShared() == 1;
                    String strName = isShared ? feedItems.get(position).user[1].getName() :
                            feedItems.get(position).user[0].getName();
                    String strPost = getPostType(feedItems.get(position).getType());
                    new AlertDialog.Builder(SearchHashtag.this)
                            .setTitle("Share $name\'s $post".replace("$name", strName).replace("$post", strPost))
                            .setMessage("Do you want to share $name\'s $post with friends and followers?".replace("$name", strName).replace("$post", strPost))
                            .setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_share_message) {
                    Intent selectActivity = new Intent(SearchHashtag.this, FriendsList.class);
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
    public void onVideoThumbnailClick(View v, int position) {
        ActivityPost.startActivityPost(this, feedItems.get(position).user[0].getUsername(), feedItems.get(position).user[0].getName(), feedItems.get(position).getId());
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
