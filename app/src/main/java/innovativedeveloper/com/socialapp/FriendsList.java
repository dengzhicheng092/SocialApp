package innovativedeveloper.com.socialapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import innovativedeveloper.com.socialapp.adapter.SelectUserAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.messaging.ChatActivity;

public class FriendsList extends AppCompatActivity implements SelectUserAdapter.OnItemClickListener {

    RecyclerView recyclerView;
    Toolbar toolbar;
    ProgressBar progressBar;
    LinearLayout mainLayout, errorLayout, emptyLayout;
    ArrayList<User> friendsList = new ArrayList<>();
    SelectUserAdapter selectUserAdapter;
    boolean isShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in, R.anim.fade_out);
        setContentView(R.layout.activity_friends_list);
        // Binding Views
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mainLayout = (LinearLayout) findViewById(R.id.layoutMain);
        emptyLayout = (LinearLayout) findViewById(R.id.lyt_empty);
        errorLayout = (LinearLayout) findViewById(R.id.lyt_error);
        selectUserAdapter = new SelectUserAdapter(this, friendsList);
        selectUserAdapter.setOnItemClickListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(selectUserAdapter);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        emptyLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        errorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFriends();
            }
        });

        isShare = getIntent().getBooleanExtra("isShare", false);
        loadFriends();
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.GET_FRIENDS + ":", new Response.Listener<String>() {
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
                                u.setFriend(friend.getInt("isFriend") == 1);
                                u.setFollower(friend.getInt("isFollowed") == 1);
                                u.setFollowing(friend.getInt("isFollowing") == 1);
                                friendsList.add(u);
                            }
                            selectUserAdapter.updateItems();
                            toolbar.setSubtitle("$total friends".replace("$total", String.valueOf(friendsList.size())));
                            progressBar.setVisibility(View.GONE);
                        } else {
                            errorLayout.setVisibility(View.GONE);
                            emptyLayout.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                }
                catch (JSONException ex) {
                    errorLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    Log.e("FriendsList", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("FriendsList", "Unexpected error: " + error.getMessage());
                errorLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
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
    public void onMessageClick(View v, int position) {
        if (AppHandler.getInstance().getDBHandler().isUserExists(friendsList.get(position).getUsername())) {
            if (!isShare) {
                ChatActivity.startActivity(this, friendsList.get(position).getUsername(), friendsList.get(position).getName());
            } else {
                ChatActivity.startActivity(this, friendsList.get(position).getUsername(), friendsList.get(position).getName(), isShare, getIntent().getStringExtra("shareMessage"));
            }
            finish();
        } else {
            AppHandler.getInstance().getDBHandler().addUser(friendsList.get(position));
            if (!isShare) {
                ChatActivity.startActivity(this, friendsList.get(position).getUsername(), friendsList.get(position).getName());
            } else {
                ChatActivity.startActivity(this, friendsList.get(position).getUsername(), friendsList.get(position).getName(), isShare, getIntent().getStringExtra("shareMessage"));
            }
            finish();
        }
    }
}
