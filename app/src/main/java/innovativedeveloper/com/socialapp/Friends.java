package innovativedeveloper.com.socialapp;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.UsersListAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.User;

public class Friends extends AppCompatActivity {

    RecyclerView recyclerView;
    TabLayout tabLayout;
    ArrayList<User> friendsList = new ArrayList<>();
    ArrayList<User> followersList = new ArrayList<>();
    ArrayList<User> followingList = new ArrayList<>();
    boolean isFollowersLoaded, isFriendsLoaded, isFollowingsLoaded = false;
    String strUsername, strName;
    UsersListAdapter usersListAdapter;
    ProgressBar progressBar;
    Toolbar toolbar;
    TextView txtEmpty;

    public static void startActivity(Activity startingActivity, String username, String name) {
        Intent intent = new Intent(startingActivity, Friends.class);
        intent.putExtra("username", username);
        intent.putExtra("name", name);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        strName = getIntent().getStringExtra("name");
        strUsername = getIntent().getStringExtra("username");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(strName);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtEmpty = (TextView) findViewById(R.id.txtEmpty);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showFriendsList();
                } else if (tab.getPosition() == 1) {
                    showFollowersList();
                } else if (tab.getPosition() == 2) {
                    showFollowingList();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        // Setting up likes and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    usersListAdapter.setAnimationsLocked(true);
                }
            }
        });

        tabLayout.addTab(tabLayout.newTab().setText("Friends"), true);
        tabLayout.addTab(tabLayout.newTab().setText("Followers"));
        tabLayout.addTab(tabLayout.newTab().setText("Followings"));
    }

    private void loadFriendsList() {
        txtEmpty.setVisibility(View.GONE);
        friendsList.clear();
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_FRIENDS + (strUsername.equals(AppHandler.getInstance().getUser().getUsername()) ? ":" : strUsername), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray friendsArr = obj.getJSONArray("friends");
                        if (friendsArr.length() != 0) {
                            for (int i = 0; i < friendsArr.length(); i++) {
                                JSONObject friend = friendsArr.getJSONObject(i);
                                User u = new User();
                                u.setId(friend.getString("id"));
                                u.setName(friend.getString("name"));
                                u.setUsername(friend.getString("username"));
                                u.setProfilePhoto(friend.getString("icon"));
                                u.setVerified(friend.getInt("isVerified") == 1);
                                u.setCreation(friend.getString("creation"));
                                u.setLocation(friend.getString("location"));
                                u.setFriend(friend.getInt("isFriend") == 2);
                                u.setFollower(friend.getInt("isFollowed") == 1);
                                u.setFollowing(friend.getInt("isFollowing") == 1);
                                friendsList.add(u);
                            }
                            isFriendsLoaded = true;
                            showFriendsList();
                        } else {
                            isFriendsLoaded = true;
                            showFriendsList();
                        }
                    }
                }
                catch (JSONException ex) {
                    Log.e("Friends", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Friends", "Unexpected error: " + error.getMessage());
                Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
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

    private void showFriendsList() {
        txtEmpty.setVisibility(View.GONE);
        if (friendsList.isEmpty() && !isFriendsLoaded) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            loadFriendsList();
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            usersListAdapter = new UsersListAdapter(this, friendsList);
            usersListAdapter.setOnItemClickListener(new UsersListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    UserProfile.startUserProfile(Friends.this, friendsList.get(position).getUsername(), friendsList.get(position).getName());
                }
            });
            recyclerView.setAdapter(usersListAdapter);
            usersListAdapter.updateItems();
            if (friendsList.size() == 0) {
                recyclerView.setVisibility(View.INVISIBLE);
                txtEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadFollowersList() {
        txtEmpty.setVisibility(View.GONE);
        followersList.clear();
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_FOLLOWERS + (strUsername.equals(AppHandler.getInstance().getUser().getUsername()) ? ":" : strUsername), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray followersArr = obj.getJSONArray("followers");
                        if (followersArr.length() != 0) {
                            for (int i = 0; i < followersArr.length(); i++) {
                                JSONObject follower = followersArr.getJSONObject(i);
                                User u = new User();
                                u.setId(follower.getString("id"));
                                u.setName(follower.getString("name"));
                                u.setUsername(follower.getString("username"));
                                u.setProfilePhoto(follower.getString("icon"));
                                u.setLocation(follower.getString("location"));
                                u.setVerified(follower.getInt("isVerified") == 1);
                                u.setCreation(follower.getString("creation"));
                                followersList.add(u);
                            }
                            isFollowersLoaded = true;
                            showFollowersList();
                        } else {
                            isFollowersLoaded = true;
                            showFollowersList();
                        }
                    }
                }
                catch (JSONException ex) {
                    Log.e("Friends", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Friends", "Unexpected error: " + error.getMessage());
                Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
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

    private void showFollowersList() {
        txtEmpty.setVisibility(View.GONE);
        if (followersList.isEmpty() && !isFollowersLoaded) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            loadFollowersList();
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            usersListAdapter = new UsersListAdapter(this, followersList);
            usersListAdapter.setOnItemClickListener(new UsersListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    UserProfile.startUserProfile(Friends.this, followersList.get(position).getUsername(), followersList.get(position).getName());
                }
            });
            recyclerView.setAdapter(usersListAdapter);
            usersListAdapter.updateItems();
            if (followersList.size() == 0) {
                recyclerView.setVisibility(View.INVISIBLE);
                txtEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadFollowingsList() {
        txtEmpty.setVisibility(View.GONE);
        followingList.clear();
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_FOLLOWINGS + (strUsername.equals(AppHandler.getInstance().getUser().getUsername()) ? ":" : strUsername), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray followingsArr = obj.getJSONArray("followings");
                        if (followingsArr.length() != 0) {
                            for (int i = 0; i < followingsArr.length(); i++) {
                                JSONObject following = followingsArr.getJSONObject(i);
                                User u = new User();
                                u.setId(following.getString("id"));
                                u.setName(following.getString("name"));
                                u.setUsername(following.getString("username"));
                                u.setProfilePhoto(following.getString("icon"));
                                u.setLocation(following.getString("location"));
                                u.setVerified(following.getInt("isVerified") == 1);
                                u.setCreation(following.getString("creation"));
                                followingList.add(u);
                            }
                            isFollowingsLoaded = true;
                            showFollowingList();
                        } else {
                            isFollowingsLoaded = true;
                            showFollowingList();
                        }
                    }
                }
                catch (JSONException ex) {
                    Log.e("Friends", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Friends", "Unexpected error: " + error.getMessage());
                Toast.makeText(Friends.this, "Couldn't refresh", Toast.LENGTH_SHORT).show();
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

    private void showFollowingList() {
        txtEmpty.setVisibility(View.GONE);
        if (followingList.isEmpty() && !isFollowingsLoaded) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            loadFollowingsList();
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            usersListAdapter = new UsersListAdapter(this, followingList);
            usersListAdapter.setOnItemClickListener(new UsersListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    UserProfile.startUserProfile(Friends.this, followingList.get(position).getUsername(), followingList.get(position).getName());
                }
            });
            recyclerView.setAdapter(usersListAdapter);
            usersListAdapter.updateItems();
            if (followingList.size() == 0) {
                recyclerView.setVisibility(View.INVISIBLE);
                txtEmpty.setVisibility(View.VISIBLE);
            }
        }
    }
}
