package innovativedeveloper.com.socialapp.fragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import innovativedeveloper.com.socialapp.ItemDivider;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.adapter.SearchPeoplesAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.User;

public class SearchPeople extends Fragment implements SearchPeoplesAdapter.OnProfileItemClickListener {

    public String lastSearch;
    TextView txtStatus;
    RecyclerView recyclerView;
    ProgressBar progressBar;
    ArrayList<User> peoples = new ArrayList<>();
    SearchPeoplesAdapter pAdapter;

    public SearchPeople() {
    }

    public String getLastSearch() {
        return lastSearch;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search_people, container, false);
        txtStatus = (TextView) v.findViewById(R.id.txtStatus);
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        pAdapter = new SearchPeoplesAdapter(getActivity(), peoples);
        pAdapter.setOnProfileItemClickListener(this);

        // Setting up peoples and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(pAdapter);
        recyclerView.addItemDecoration(new ItemDivider(getActivity()));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    pAdapter.setAnimationsLocked(true);
                }
            }
        });
        return v;
    }

    @Override
    public void onProfileClick(View v, int position) {
        UserProfile.startUserProfile(getActivity(), peoples.get(position).getUsername(), peoples.get(position).getName());
    }

    public void updateSearch(String query) {
        if (!query.equals(""))
            SearchOnline(query);

        lastSearch = query;
    }

    private void SearchOnline(String query) {
        recyclerView.setVisibility(View.INVISIBLE);
        txtStatus.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.SEARCH.replace(":toFind", query), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                peoples.clear();
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray usersArray = obj.getJSONArray("users");
                        if (usersArray.length() != 0) {
                            recyclerView.setVisibility(View.VISIBLE);
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject user = usersArray.getJSONObject(i);
                                User u = new User();
                                u.setId(user.getString("id"));
                                u.setUsername(user.getString("username"));
                                u.setName(user.getString("name"));
                                u.setEmail(user.getString("email"));
                                u.setProfilePhoto(user.getString("icon"));
                                u.setCreation(user.getString("creation"));
                                u.setVerified(user.getInt("isVerified") == 1);
                                u.setFriend(user.getInt("isFriend") == 1);
                                u.setFollower(user.getInt("isFollowed") == 1);
                                u.setFollowing(user.getInt("isFollowing") == 1);
                                if (user.has("location")) {
                                    u.setLocation(user.getString("location"));
                                }
                                peoples.add(u);
                            }
                            pAdapter.updateItems();
                            txtStatus.setVisibility(View.GONE);
                        } else {
                            txtStatus.setText("We couldn't find anything for ':query'".replace(":query", lastSearch));
                            txtStatus.setVisibility(View.VISIBLE);
                        }
                    }
                } catch (JSONException ex) {
                    txtStatus.setText(getString(R.string.no_connectivity));
                    txtStatus.setVisibility(View.VISIBLE);
                    Log.e("SearchPeoples", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(getActivity(), "Couldn't connect to server", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtStatus.setText(getString(R.string.no_connectivity));
                txtStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                Log.e("SearchPeoples", "Unexpected error: " + error.getMessage());
                Toast.makeText(getActivity(), "Couldn't connect to server", Toast.LENGTH_SHORT).show();
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
