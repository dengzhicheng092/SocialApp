package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Filter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.dataset.Inbox;

public class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Inbox> inboxArrayList;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public InboxAdapter(Context context, ArrayList<Inbox> inboxArrayList)
    {
        this.context = context;
        this.inboxArrayList = inboxArrayList;
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new InboxAdapter.InboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final InboxViewHolder holder = (InboxViewHolder) viewHolder;
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(holder.view, holder.getAdapterPosition());
            }
        });
        holder.txtName.setText(inboxArrayList.get(position).getUser().getName());
        holder.txtMessage.setText(inboxArrayList.get(position).getType() == 2 ? "Attachment" : inboxArrayList.get(position).getMessage());
        holder.txtStamp.setText(AppHandler.getTimestampShort(inboxArrayList.get(position).getCreation()));
        holder.txtCount.setVisibility(inboxArrayList.get(position).getCounts() > 0 ? View.VISIBLE : View.GONE);
        holder.txtCount.setText(String.valueOf(inboxArrayList.get(position).getCounts()));
        Picasso.with(context).load(inboxArrayList.get(position).getUser().getProfilePhoto()).centerCrop().resize(80, 80).error(R.drawable.ic_people).placeholder(R.drawable.ic_people).into(holder.icon);
    }

    @Override
    public int getItemCount() {
        return inboxArrayList.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    private static class InboxViewHolder extends RecyclerView.ViewHolder {

        CircleImageView icon;
        TextView txtName, txtMessage, txtCount, txtStamp;
        View view;

        public InboxViewHolder(View itemView) {
            super(itemView);
            icon = (CircleImageView) itemView.findViewById(R.id.icon);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
            txtCount = (TextView) itemView.findViewById(R.id.txtCount);
            txtStamp = (TextView) itemView.findViewById(R.id.txtTimestamp);
            view = itemView;
        }
    }
}
