package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.dataset.User;

public class NearbyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private boolean delayEnterAnimation = true;
    private ArrayList<User> peoples;
    private OnProfileItemClickListener onProfileItemClickListener;

    public NearbyAdapter(Context context, ArrayList<User> peoples) {
        this.context = context;
        this.peoples = peoples;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby, parent, false);
        return new PeoplesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        runEnterAnimation(viewHolder.itemView, position);
        PeoplesViewHolder holder = (PeoplesViewHolder) viewHolder;
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onProfileItemClickListener.onProfileClick(v, viewHolder.getAdapterPosition());
            }
        });
        holder.txtName.setText(peoples.get(position).getName());
        holder.verifiedIcon.setVisibility(peoples.get(position).isVerified() ? View.VISIBLE : View.INVISIBLE);
        holder.txtLocation.setText(peoples.get(position).getLocation() + " miles");
        Picasso.with(context).load(peoples.get(position).getProfilePhoto()).placeholder(R.drawable.ic_people).error(R.drawable.ic_people).into(holder.icon);
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(100);
            view.setAlpha(0.f);
            view.animate()
                    .translationY(0).alpha(1.f)
                    .setStartDelay(delayEnterAnimation ? 20 * (position) : 0)
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
        return peoples.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    public void setDelayEnterAnimation(boolean delayEnterAnimation) {
        this.delayEnterAnimation = delayEnterAnimation;
    }
    public void setOnProfileItemClickListener(OnProfileItemClickListener onProfileItemClickListener) {
        this.onProfileItemClickListener = onProfileItemClickListener;
    }

    public interface OnProfileItemClickListener {
        void onProfileClick(View v, int position);
    }

    public static class PeoplesViewHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtName;
        TextView txtMutualFriends;
        TextView txtLocation;
        ImageView verifiedIcon, imgLocation;
        View view;

        public PeoplesViewHolder(View view) {
            super(view);
            this.view = view;
            icon = (CircleImageView) view.findViewById(R.id.icon);
            txtName = (TextView) view.findViewById(R.id.txtName);
            txtMutualFriends = (TextView) view.findViewById(R.id.txtMutual);
            txtLocation = (TextView) view.findViewById(R.id.txtLocation);
            imgLocation = (ImageView) view.findViewById(R.id.imgLocation);
            verifiedIcon = (ImageView) view.findViewById(R.id.verifiedIcon);
        }
    }
}
