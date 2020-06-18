package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.R;

public class UsersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private ArrayList<User> userArrayList;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onFeedItemClickListener) {
        this.onItemClickListener = onFeedItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public UsersListAdapter(Context context, ArrayList<User> userArrayList) {
        this.context = context;
        this.userArrayList = userArrayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_person, parent, false);
        return new UserListHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final UserListHolder holder = (UserListHolder) viewHolder;
        Picasso.with(context).load(userArrayList.get(position).getProfilePhoto())
                .placeholder(R.drawable.ic_people)
                .error(R.drawable.ic_people)
                .into(holder.icon);
        holder.txtName.setText(userArrayList.get(position).getName());
        holder.verifiedIcon.setVisibility(userArrayList.get(position).isVerified() ? View.VISIBLE : View.INVISIBLE);
        if (userArrayList.get(position).isFriend()) {
            if (!userArrayList.get(position).getLocation().isEmpty()) {
                holder.txtLocation.setText(userArrayList.get(position).getLocation());
                holder.enableLocation(true);
            } else {
                holder.enableLocation(false);
            }
        } else {
            holder.enableLocation(false);
        }
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(holder.view, holder.getAdapterPosition());
            }
        });
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(100);
            view.setAlpha(0.f);
            view.animate()
                    .translationY(0).alpha(1.f)
                    .setStartDelay(0)
                    .setInterpolator(new DecelerateInterpolator(2.f))
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animationsLocked = true;
                        }
                    })
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return userArrayList.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    private static class UserListHolder extends RecyclerView.ViewHolder {

        CircleImageView icon;
        TextView txtName;
        TextView txtLocation;
        ImageView verifiedIcon, imgLocation;
        View view;

        private UserListHolder(View view) {
            super(view);
            this.view = view;
            this.icon = (CircleImageView) view.findViewById(R.id.icon);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.txtLocation = (TextView) view.findViewById(R.id.txtLocation);
            this.imgLocation = (ImageView) view.findViewById(R.id.imgLocation);
            this.verifiedIcon = (ImageView) view.findViewById(R.id.verifiedIcon);
        }

        void enableLocation(boolean v) {
            txtLocation.setVisibility(v ? View.VISIBLE : View.GONE);
            imgLocation.setVisibility(v ? View.VISIBLE : View.GONE);
        }
    }
}
