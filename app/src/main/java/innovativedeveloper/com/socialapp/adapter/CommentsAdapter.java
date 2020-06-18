package innovativedeveloper.com.socialapp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.dataset.Comment;
import innovativedeveloper.com.socialapp.R;

public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;
    private boolean delayEnterAnimation = true;
    private ArrayList<Comment> comments;
    private OnItemClickListener onItemClickListener;
    private boolean isOwnPost = false;
    private boolean isReply = false;

    public CommentsAdapter(Context context, ArrayList<Comment> comments, boolean isOwnPost, boolean isReply) {
        this.context = context;
        this.comments = comments;
        this.isOwnPost = isOwnPost;
        this.isReply = isReply;
    }

    public void setOwnPost(boolean isOwnPost) {
        this.isOwnPost = isOwnPost;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final CommentViewHolder holder = (CommentViewHolder) viewHolder;
        Link userLink = new Link("#$0" + comments.get(position).getUser().getName());
        userLink.setBold(true);
        userLink.setUnderlined(false);
        userLink.setHighlightAlpha(.0f);
        userLink.setText(comments.get(position).getUser().getName());
        userLink.setTextColor(Color.parseColor("#000000"));
        userLink.setOnClickListener(new Link.OnClickListener() {
            @Override
            public void onClick(String clickedText) {
                onItemClickListener.onProfileClick(holder.icon, viewHolder.getAdapterPosition());
            }
        });
        holder.txtReplies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onRepliesClick(view, viewHolder.getAdapterPosition());
            }
        });
        holder.txtReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onReplyClick(view, viewHolder.getAdapterPosition());
            }
        });
        holder.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onProfileClick(v, viewHolder.getAdapterPosition());
            }
        });
        holder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onMoreClick(v, viewHolder.getAdapterPosition());
            }
        });
        holder.txtDate.setText(AppHandler.getTimestamp(comments.get(position).getCreation()));
        if (isReply) {
            holder.txtReplies.setVisibility(View.GONE);
            holder.txtReply.setVisibility(View.GONE);
        } else {
            holder.txtReplies.setText(comments.get(position).getReplies() > 1 ? (comments.get(position).getReplies() + " replies") : "1 Reply");
            holder.txtReplies.setVisibility(comments.get(position).getReplies() > 0 ? View.VISIBLE : View.GONE);
            holder.txtReply.setVisibility(comments.get(position).getReplies() > 0 ? View.GONE : View.VISIBLE);
        }
        if (isOwnPost || comments.get(position).getUser().getUsername().equals(AppHandler.getInstance().getDataManager().getString("username", "")) && comments.get(position).getCommentId() != null) {
            holder.btnMore.setVisibility(View.VISIBLE);
        } else {
            holder.btnMore.setVisibility(View.INVISIBLE);
        }

        if (!comments.get(position).getUser().getProfilePhoto().isEmpty()) {
            Picasso.with(context)
                    .load(comments.get(position).getUser().getProfilePhoto())
                    .placeholder(R.drawable.ic_people)
                    .error(R.drawable.ic_people)
                    .into(holder.icon);
        } else {
            holder.icon.setImageResource(R.drawable.ic_people);
        }
        holder.txtComment.setText(comments.get(position).getContent().replace("#$0", "").replace("#space", " "));
        LinkBuilder.on(holder.txtComment).addLink(userLink).build();
        holder.txtComment.setMovementMethod(LinkMovementMethod.getInstance());
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
        return comments.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public void addItem() {
        notifyItemInserted(comments.size() - 1);
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    public void setDelayEnterAnimation(boolean delayEnterAnimation) {
        this.delayEnterAnimation = delayEnterAnimation;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onProfileClick(View v, int position);
        void onMoreClick(View v, int position);
        void onReplyClick(View v, int position);
        void onRepliesClick(View v, int position);
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView icon;
        TextView txtComment;
        TextView txtDate;
        TextView txtReply;
        TextView txtReplies;
        ImageView btnMore;
        public CommentViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.icon);
            txtComment = (TextView) view.findViewById(R.id.txtComment);
            txtDate = (TextView) view.findViewById(R.id.txtDate);
            btnMore = (ImageView) view.findViewById(R.id.btnMore);
            txtReply = (TextView) view.findViewById(R.id.txtReply);
            txtReplies = (TextView) view.findViewById(R.id.txtReplies);
        }
    }
}
