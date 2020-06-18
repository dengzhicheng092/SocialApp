package innovativedeveloper.com.socialapp.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.MediaActivity;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.Friends;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.preferences.SettingsActivity;
import jp.wasabeef.blurry.Blurry;

public class Profile extends Fragment {

    ImageView imgBackground;
    CircleImageView icon;
    TextView txtName, txtLocation, txtUsername, txtEmail, txtPhotos, txtPosts, txtFriends, txtVideos;
    View viewPosts, lviewPosts, viewPhotos, lviewPhotos, viewFriends, viewVideos, viewLocation;

    public Profile() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        imgBackground = (ImageView) v.findViewById(R.id.imgIcon);
        icon = (CircleImageView) v.findViewById(R.id.icon);
        txtName = (TextView) v.findViewById(R.id.txtName);
        txtLocation = (TextView) v.findViewById(R.id.txtLocation);
        txtUsername = (TextView) v.findViewById(R.id.txtUsername);
        txtEmail = (TextView) v.findViewById(R.id.txtEmail);
        txtPhotos = (TextView) v.findViewById(R.id.txtPhotos);
        txtVideos = (TextView) v.findViewById(R.id.txtVideos);
        txtPosts = (TextView) v.findViewById(R.id.txtPosts);
        txtFriends = (TextView) v.findViewById(R.id.txtFriends);
        viewLocation = v.findViewById(R.id.viewLocation);
        viewPosts = v.findViewById(R.id.view_posts);
        viewPhotos = v.findViewById(R.id.view_photos);
        lviewPosts = v.findViewById(R.id.lv_post);
        lviewPhotos = v.findViewById(R.id.lv_photos);
        viewFriends = v.findViewById(R.id.lv_friends);
        viewVideos = v.findViewById(R.id.lv_videos);
        loadAccount(AppHandler.getInstance().getUser());
        return v;
    }

    public void loadAccount(User u) {
        if (!u.getProfilePhoto().equals("")) {
            Picasso.with(getContext()).load(u.getProfilePhoto()).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    icon.setImageBitmap(bitmap);
                    Blurry.with(getContext()).from(bitmap).into(imgBackground);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        }
        txtName.setText(u.getName());
        setLocation(u.getLocation());
        txtUsername.setText(u.getUsername());
        txtEmail.setText(u.getEmail());
        viewPosts.setVisibility(u.totalPhotos == 0 ? View.VISIBLE : View.GONE);
        viewPhotos.setVisibility(u.totalPhotos == 0 ? View.GONE : View.VISIBLE);
        txtPhotos.setText(String.valueOf(u.totalPhotos));
        txtVideos.setText(String.valueOf(u.totalVideos));
        txtFriends.setText(String.valueOf(u.totalFriends));
        txtPosts.setText(String.valueOf(u.totalPosts));
    }

    private void setLocation(String location) {
        if (!location.equals("")) {
            txtLocation.setText(location);
            viewLocation.setVisibility(View.VISIBLE);
        } else {
            viewLocation.setVisibility(View.INVISIBLE);
        }
    }

    public void actionClick(View view) {
        switch (view.getId()){
            case R.id.lv_friends: {
                Friends.startActivity(getActivity(), AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName());
                break;
            }
            case R.id.lv_media: {
                MediaActivity.startActivity(getActivity(), AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName(), true, true);
                break;
            }
            case R.id.lv_viewProfile: {
                UserProfile.startUserProfile(getActivity(), AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName());
                break;
            }
            case R.id.lv_photos: {
                MediaActivity.startActivity(getActivity(), AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName(), true, false);
                break;
            }
            case R.id.lv_videos: {
                if (AppHandler.getInstance().getUser().totalVideos > 0) {
                    MediaActivity.startActivity(getActivity(), AppHandler.getInstance().getUser().getUsername(), AppHandler.getInstance().getUser().getName(), false, true);
                }
                break;
            }
            case R.id.account_settings: {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            }
            case R.id.lv_about: {
                new AlertDialog.Builder(getActivity()).setMessage(getResources().getString(R.string.about))
                        .setTitle("About").show();
            }
        }
    }

    @Override
    public void onResume() {
        loadAccount(AppHandler.getInstance().getUser());
        super.onResume();
    }
}
