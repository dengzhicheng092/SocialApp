package innovativedeveloper.com.socialapp;

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

import innovativedeveloper.com.socialapp.adapter.LikesAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Like;
import innovativedeveloper.com.socialapp.dataset.User;

public class Likes extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    ArrayList<Like> likesList;
    String postId;
    LinearLayout mainLayout, errorLayout;
    LikesAdapter likesAdapter;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_likes);
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
        errorLayout = (LinearLayout) findViewById(R.id.lyt_error);
        likesList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        errorLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        errorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadLikes();
            }
        });
        // Getting parameters
        Bundle param = getIntent().getExtras();
        if (param != null) {
            postId = param.getString("postId");
        }

        // Setting up likes and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        likesAdapter = new LikesAdapter(this, likesList);
        recyclerView.setAdapter(likesAdapter);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    likesAdapter.setAnimationsLocked(true);
                }
            }
        });

        likesAdapter.updateItems();
        LoadLikes();
    }

    private void LoadLikes() {
        progressBar.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_LIKES + "0?postId="+postId, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray comments = obj.getJSONArray("likes");
                        if (comments.length() != 0) {
                            for (int i = 0; i < comments.length(); i++) {
                                JSONObject like = comments.getJSONObject(i);
                                Like l = new Like();
                                User u1 = new User();
                                u1.setUsername(like.getString("username"));
                                u1.setName(like.getString("name"));
                                u1.setMutualFriends(like.getInt("mutualFriends"));
                                u1.setVerified(like.getInt("isVerified") == 1);
                                l.setCreation(like.getString("creation"));
                                l.setIcon(like.getString("icon"));
                                l.setPostid(like.getString("post_id"));
                                l.setUser(u1);
                                likesList.add(l);
                            }
                            likesAdapter.updateItems();
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(Likes.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                        errorLayout.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                }
                catch (JSONException ex) {
                    Log.e("Likes", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Likes.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                    errorLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Likes", "Unexpected error: " + error.getMessage());
                Toast.makeText(Likes.this, "Couldn't load", Toast.LENGTH_SHORT).show();
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
}
