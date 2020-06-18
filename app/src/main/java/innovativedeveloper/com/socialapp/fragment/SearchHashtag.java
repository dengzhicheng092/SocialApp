package innovativedeveloper.com.socialapp.fragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;

public class SearchHashtag extends Fragment {

    public String lastSearch;
    TextView txtEmpty, txtStatus;
    RecyclerView recyclerView;
    ProgressBar progressBar;

    public SearchHashtag() {
    }

    public String getLastSearch() {
        return lastSearch;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search_hashtag, container, false);
        txtEmpty = (TextView) v.findViewById(R.id.txtEmpty);
        txtStatus = (TextView) v.findViewById(R.id.txtStatus);
        recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        return v;
    }

    public void updateSearch(String query) {
        if (!query.equals(""))
            checkHashtag(query);

        lastSearch = query;
    }

    private void checkHashtag(final String query) {
        txtStatus.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(Request.Method.GET, Config.CHECK_HASHTAG.replace(":hashtag", query), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        if (obj.getBoolean("isAvailable")) {
                            innovativedeveloper.com.socialapp.SearchHashtag.startHashtagActivity(query, getActivity());
                            txtStatus.setText(getResources().getString(R.string.search_hashtag_globe));
                            txtStatus.setVisibility(View.VISIBLE);
                        } else {
                            txtStatus.setText("No results found for #:hashtag".replace(":hashtag", query));
                            txtStatus.setVisibility(View.VISIBLE);
                        }
                    } else {
                        txtStatus.setText("No results found for #:hashtag".replace(":hashtag", query));
                        txtStatus.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException ex) {
                    txtStatus.setText("No results found for #:hashtag".replace(":hashtag", query));
                    txtStatus.setVisibility(View.VISIBLE);
                    Log.e("SearchHashtags", "Unexpected error: " + ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtStatus.setText("No results found for #:hashtag".replace(":hashtag", query));
                txtStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                Log.e("SearchHashtags", "Unexpected error: " + error.getMessage());
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
