package innovativedeveloper.com.socialapp.fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import innovativedeveloper.com.socialapp.*;
import innovativedeveloper.com.socialapp.adapter.FeedItemAdapter;
import innovativedeveloper.com.socialapp.adapter.FeedItemAnimator;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.AppHelper;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

public class NewsFeed extends Fragment implements FeedItemAdapter.OnFeedItemClickListener, AppService.OnServiceChanged {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    ArrayList<Feed> feedItems = new ArrayList<>();
    FeedItemAdapter feedAdapter;
    LinearLayout emptyFeedView;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean isRefreshing = false;
    boolean isFinalList = false;
    View v;

    public NewsFeed() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_news_feed, container, false);
        setHasOptionsMenu(true);
        AppHandler.getInstance().getAppService().setOnServiceChanged(this);
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        emptyFeedView = (LinearLayout) v.findViewById(R.id.empty_Feed);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        feedAdapter = new FeedItemAdapter(getContext(), feedItems);
        feedAdapter.setOnFeedItemClickListener(this);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext()) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 100;
            }
        };
        emptyFeedView.setVisibility(View.GONE);
        recyclerView.setAdapter(feedAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new FeedItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    ((MainActivity)getActivity()).setFabVisibility(View.VISIBLE);
                    int totalItemCount = linearLayoutManager.getItemCount();
                    int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (totalItemCount <= (lastVisibleItem + 1)) {
                        if (!isRefreshing && !isFinalList) {
                            swipeRefreshLayout.setRefreshing(true);
                            updateFeed(false);
                            isRefreshing = true;
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 ||dy<0)
                {
                    ((MainActivity)getActivity()).setFabVisibility(View.INVISIBLE);
                }
            }
        });

        // Loading Feed
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorAccentLight);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isRefreshing) {
                    if (AppHandler.getInstance().getAppService().IsAuthorized()) {
                        loadFeed();
                    }
                }
            }
        });
        return v;
    }

    @Override
    public void onAuthorizedStatusChanged(JSONObject obj) {
        try {
            if (!obj.getBoolean("error")) {
                loadFeed();
            }
        } catch (JSONException ex) {
            Log.e("Authorization", "Unexpected error: " + ex.getMessage());
        }
    }

    public void updateFeed(final boolean fromStart) {
        isRefreshing = true;
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_FEED + (!fromStart ? feedItems.size() : 0), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (fromStart) {
                        feedItems.clear();
                    }
                    JSONObject obj = new JSONObject(response);
                    swipeRefreshLayout.setRefreshing(false);
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
                            emptyFeedView.setVisibility(feedAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
                        } else {
                            isFinalList = true;
                            if (feedAdapter.getItemCount() < 1) {
                                emptyFeedView.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        int code = obj.getInt("code");
                        if (code == Config.ACCOUNT_DISABLED) {
                            new AlertDialog.Builder(getContext())
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
                            startActivity(new Intent(getActivity(), AppHelper.class));
                        } else {
                            Toast.makeText(getActivity(), "Couldn't refresh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                catch (JSONException ex) {
                    swipeRefreshLayout.setRefreshing(false);
                    isRefreshing = false;
                    Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(getActivity(), "Couldn't refresh", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;
                Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                Toast.makeText(getActivity(), "Couldn't refresh", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    public void loadFeed() {
        if (feedItems.size() > 0) {
            swipeRefreshLayout.setRefreshing(true);
            updateFeed(true);
            emptyFeedView.setVisibility(feedAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            updateFeed(true);
        }
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(getContext(), Comments.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        intent.putExtra("isOwnPost", (f.getIsShared() == 1 ? f.user[1] : f.user[0]).getUsername().equals(AppHandler.getInstance().getUser().getUsername()));
        startActivity(intent);
        getActivity().overridePendingTransition(0, 0);
    }

    int selectedItem;
    @Override
    public void onMoreClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.menu_post_action);
        popupMenu.getMenu().findItem(R.id.action_delete).setVisible(false);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_report_post) {
                    selectedItem = -1;
                    final CharSequence[] items = {" It's annoying or not interesting ", " It contains explicit content ", " I think it shouldn\'t be here. ", " Other "};
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                return true;
            }
        });
        popupMenu.show();
    }

    void proceedReport(final String postId, final int action, String name) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
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
                                Toast.makeText(getActivity(), "Report submitted.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException ex) {
                            Log.e("NewsFeed", "Error: " + ex.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("NewsFeed", "Error: " + error.getMessage());
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
        UserProfile.startUserProfile(getActivity(), type == 1 ? feedItems.get(position).user[1].getUsername() :
                feedItems.get(position).user[0].getUsername(), type == 1 ? feedItems.get(position).user[1].getName() : feedItems.get(position).user[0].getName());
    }

    @Override
    public void onLikeClick(final View v, final int position, final int action) {
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
                        Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException ex) {
                    Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                    feedAdapter.notifyItemChanged(position);
                    Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLikeErrorResponse(VolleyError error) {
                Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                feedAdapter.notifyItemChanged(position);
                Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
            }
        });
        AppHandler.getInstance().getAppService().updateLike(feedItems.get(position).getId(), action);

    }

    @Override
    public void onLikesClick(View v, int position) {
        final Intent intent = new Intent(getContext(), Likes.class);
        Feed f = feedItems.get(position);
        intent.putExtra("postId", f.getId());
        startActivity(intent);
    }

    @Override
    public void onHashTagPressed(String hashTag) {
        innovativedeveloper.com.socialapp.SearchHashtag.startHashtagActivity(hashTag, getActivity());
    }

    @Override
    public void onShareClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
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
                    new AlertDialog.Builder(getContext())
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
                                                    Toast.makeText(getActivity(), "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(getContext())
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
                                                    Toast.makeText(getActivity(), "$post added to your profile.".replace("$post", strPost), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Log.e("NewsFeed", "Server response: " + obj.getString("code"));
                                                    Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch (JSONException ex) {
                                                Log.e("NewsFeed", "Unexpected error: " + ex.getMessage());
                                                Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("NewsFeed", "Unexpected error: " + error.getMessage());
                                            Toast.makeText(getActivity(), "Your request was not proceed", Toast.LENGTH_SHORT).show();
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
                    Intent selectActivity = new Intent(getActivity(), FriendsList.class);
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
        VideoActivity.startActivity(getActivity(), feedItems.get(position).getContent());
    }

    @Override
    public void onImageClick(View v, int position) {
        Intent intent = new Intent(getActivity(), ActivityPhoto.class);
        intent.putExtra("name", feedItems.get(position).user[0].getUsername());
        intent.putExtra("content", feedItems.get(position).getContent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, "photo");
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
