package innovativedeveloper.com.socialapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.services.AppService;

import static innovativedeveloper.com.socialapp.config.Config.ACTION_LIKE_BUTTON_CLICKED;
import static innovativedeveloper.com.socialapp.config.Config.FEED_AD_TYPE;
import static innovativedeveloper.com.socialapp.config.Config.FEED_TYPE_DEFAULT;
import static innovativedeveloper.com.socialapp.config.Config.FEED_TYPE_PHOTO;
import static innovativedeveloper.com.socialapp.config.Config.FEED_TYPE_VIDEO;

public class FeedItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Feed> feedItems = new ArrayList<>();
    private Context context;
    private OnFeedItemClickListener onFeedItemClickListener;
    private HashTagHelper hashTagHelper;
    private boolean isMoreButtonDisable = false;

    public FeedItemAdapter(Context context, ArrayList<Feed> feedItems) {
        this.context = context;
        this.feedItems = feedItems;
    }

    public void setMoreButtonDisable(boolean d) {
        isMoreButtonDisable = d;
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, int position);
        void onMoreClick(View v, int position);
        void onProfileClick(View v, int position, int type);
        void onLikeClick(View v, int position, int action);
        void onLikesClick(View v, int position);
        void onHashTagPressed(String hashTag);
        void onShareClick(View v, int position);
        void onVideoThumbnailClick(View v, int position);
        void onImageClick(View v, int position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == FEED_TYPE_DEFAULT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        } else if (viewType == FEED_TYPE_PHOTO) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed_photo, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        } else if (viewType == FEED_TYPE_VIDEO) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed_video, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        } else if (viewType == FEED_AD_TYPE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad, parent, false);
            return new AdFeedViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        hashTagHelper = HashTagHelper.Creator.create(context.getResources().getColor(R.color.colorPrimary), new HashTagHelper.OnHashTagClickListener() {
            @Override
            public void onHashTagClicked(String hashTag) {
                onFeedItemClickListener.onHashTagPressed(hashTag);
            }
        });
        if (getItemViewType(position) == FEED_TYPE_DEFAULT) {
            ((CellFeedViewHolder) holder).btnMore.setVisibility(isMoreButtonDisable ? View.GONE : View.VISIBLE);
            Picasso.with(context).load(feedItems.get(holder.getAdapterPosition()).user[0].getProfilePhoto()).error(R.drawable.ic_people).placeholder(R.drawable.ic_people).into(((CellFeedViewHolder) holder).photo);
            ((CellFeedViewHolder) holder).bindView(feedItems.get(holder.getAdapterPosition()));
            hashTagHelper.handle(((CellFeedViewHolder) holder).txtDescription);
        } else if (getItemViewType(position) == FEED_TYPE_PHOTO) {
            ((CellFeedViewHolder) holder).btnMore.setVisibility(isMoreButtonDisable ? View.GONE : View.VISIBLE);
            Picasso.with(context).load(feedItems.get(holder.getAdapterPosition()).user[0].getProfilePhoto()).error(R.drawable.ic_people).placeholder(R.drawable.ic_people).into(((CellFeedViewHolder) holder).photo);
            ((CellFeedViewHolder) holder).bindView(feedItems.get(holder.getAdapterPosition()));
            hashTagHelper.handle(((CellFeedViewHolder) holder).txtDescription);
            Picasso.with(context).load(feedItems.get(holder.getAdapterPosition()).getContent())
                    .centerCrop()
                    .resize(800, 784).into(((CellFeedViewHolder) holder).imgContent, new Callback() {
                @Override
                public void onSuccess() {
                    ((CellFeedViewHolder) holder).progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {

                }
            });
        } else if (getItemViewType(position) == FEED_TYPE_VIDEO) {
            ((CellFeedViewHolder) holder).btnMore.setVisibility(isMoreButtonDisable ? View.GONE : View.VISIBLE);
            Picasso.with(context).load(feedItems.get(holder.getAdapterPosition()).user[0].getProfilePhoto()).error(R.drawable.ic_people).placeholder(R.drawable.ic_people).into(((CellFeedViewHolder) holder).photo);
            ((CellFeedViewHolder) holder).bindView(feedItems.get(holder.getAdapterPosition()));
            hashTagHelper.handle(((CellFeedViewHolder) holder).txtDescription);
            AppService.addVideoThumbnail(feedItems.get(holder.getAdapterPosition()).getContent(), ((CellFeedViewHolder) holder).videoThumbnail);
        } else if (getItemViewType(holder.getAdapterPosition()) == FEED_AD_TYPE) {
            AdRequest adRequest = new AdRequest.Builder().build();
            ((AdFeedViewHolder) holder).adView.loadAd(adRequest);
        }
        if (getItemViewType(position) != FEED_AD_TYPE && ((CellFeedViewHolder) holder).sharedPostProfile != null) {
            ((CellFeedViewHolder) holder).sharedPostProfile.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    onFeedItemClickListener.onProfileClick(((CellFeedViewHolder) holder).photo, (holder).getAdapterPosition(), feedItems.get(holder.getAdapterPosition()).getIsShared());
                }
            });
            ((CellFeedViewHolder) holder).sharedProfileLink.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    onFeedItemClickListener.onProfileClick(((CellFeedViewHolder) holder).photo, (holder).getAdapterPosition(), 0);
                }
            });
        }
    }

    private void setupViews(final View view, final CellFeedViewHolder cellFeedViewHolder) {
        cellFeedViewHolder.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFeedItemClickListener.onShareClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.likesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onLikesClick(v, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onCommentsClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onMoreClick(v, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = cellFeedViewHolder.getAdapterPosition();
                boolean isLiked = feedItems.get(adapterPosition).isLiked();
                if (!isLiked) {
                    feedItems.get(adapterPosition).likes++;
                } else {
                    feedItems.get(adapterPosition).likes--;
                }
                feedItems.get(adapterPosition).setLiked(!isLiked);
                notifyItemChanged(adapterPosition, ACTION_LIKE_BUTTON_CLICKED);
                onFeedItemClickListener.onLikeClick(view, cellFeedViewHolder.getAdapterPosition(), isLiked ? 0 : 1);
            }
        });
        cellFeedViewHolder.txtName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onProfileClick(cellFeedViewHolder.txtName, cellFeedViewHolder.getAdapterPosition(), feedItems.get(cellFeedViewHolder.getAdapterPosition()).getIsShared());
            }
        });
        cellFeedViewHolder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onProfileClick(cellFeedViewHolder.photo,cellFeedViewHolder.getAdapterPosition(), feedItems.get(cellFeedViewHolder.getAdapterPosition()).getIsShared());
            }
        });
        cellFeedViewHolder.txtComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onCommentsClick(v, cellFeedViewHolder.getAdapterPosition());
            }
        });

        if (cellFeedViewHolder.videoView != null) {
            cellFeedViewHolder.videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFeedItemClickListener.onVideoThumbnailClick(v, cellFeedViewHolder.getAdapterPosition());
                }
            });
        }

        if (cellFeedViewHolder.imgContent != null) {
            cellFeedViewHolder.imgContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFeedItemClickListener.onImageClick(v, cellFeedViewHolder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {

            if (feedItems.get(position) == null)
                return FEED_AD_TYPE;
            if (feedItems.get(position).getType() == 2)
                return FEED_TYPE_VIDEO;
            if (feedItems.get(position).getType() == 1)
                return FEED_TYPE_PHOTO;
            return FEED_TYPE_DEFAULT;
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void updateItems(boolean animated) {
        if (animated) {
            notifyItemRangeInserted(getItemCount(), feedItems.size());
        } else {
            notifyDataSetChanged();
        }
    }

    public static class AdFeedViewHolder extends RecyclerView.ViewHolder {

        NativeExpressAdView adView;
        public AdFeedViewHolder(View itemView) {
            super(itemView);
            adView = (NativeExpressAdView) itemView.findViewById(R.id.adView);
        }
    }

    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {

        CircleImageView photo;
        ImageView imgContent;
        View btnComments;
        View btnShare;
        View btnLike;
        ImageView viewLike;
        ImageView btnMore;
        TextView txtDescription;
        TextView txtDate;
        TextView txtName;
        TextView txtLikes;
        TextView txtComments;
        TextView txtShares;
        ImageView verifiedIcon;
        Feed feedItem;
        ProgressBar progressBar;
        ImageView audience;
        TextView txtShared;
        View sharedView;
        Link sharedProfileLink, sharedPostProfile;

        // Video Feed
        View likesLayout;
        View videoView;
        ImageView videoThumbnail;
        TextView txtLikesCount;


        public CellFeedViewHolder(View view) {
            super(view);
            this.photo = (CircleImageView) view.findViewById(R.id.icon);
            this.btnLike = view.findViewById(R.id.btnLike);
            this.btnComments = view.findViewById(R.id.btnComment);
            this.btnShare = view.findViewById(R.id.btnShare);
            this.btnMore = (ImageView) view.findViewById(R.id.btnMore);
            this.txtDescription = (TextView) view.findViewById(R.id.txtContent);
            this.txtDate = (TextView) view.findViewById(R.id.txtDate);
            this.imgContent = (ImageView) view.findViewById(R.id.photo_content);
            this.txtName = (TextView) view.findViewById(R.id.txtName);
            this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            this.verifiedIcon = (ImageView) view.findViewById(R.id.verifiedIcon);
            this.txtLikes = (TextView) view.findViewById(R.id.txtLikes);
            this.txtComments = (TextView) view.findViewById(R.id.txtComments);
            this.txtShares = (TextView) view.findViewById(R.id.txtShares);
            this.audience = (ImageView) view.findViewById(R.id.audience);
            this.sharedView = view.findViewById(R.id.sharedView);
            this.txtShared = (TextView) view.findViewById(R.id.txtShared);
            this.viewLike = (ImageView) view.findViewById(R.id.viewLike);
            this.videoView = view.findViewById(R.id.videoView);
            this.videoThumbnail = (ImageView) view.findViewById(R.id.video_thumbnail);
            this.likesLayout = view.findViewById(R.id.likesLayout);
            this.txtLikesCount = (TextView) view.findViewById(R.id.txtTotalLikes);
        }

        public void bindView(Feed feedItem) {
            this.feedItem = feedItem;
            txtLikes.setVisibility(feedItem.likes > 0 ? View.VISIBLE : View.GONE);
            txtComments.setVisibility(feedItem.getComments() > 0 ? View.VISIBLE : View.GONE);
            txtShares.setVisibility(feedItem.getShares() > 0 ? View.VISIBLE : View.GONE);
            viewLike.setImageResource(feedItem.isLiked() ? R.drawable.ic_heart_red : R.drawable.ic_like);
            audience.setImageResource(getAudienceIcon(feedItem.getAudience()));
            txtDescription.setText(feedItem.getDescription());
            txtName.setText(feedItem.user[0].getName());
            txtDate.setText(AppHandler.getTimestamp(feedItem.getCreation()));
            verifiedIcon.setVisibility(feedItem.user[0].isVerified() ? View.VISIBLE : View.INVISIBLE);
            txtComments.setText("$no".replace("$no", feedItem.getComments() > 99 ? "99+" : String.valueOf(feedItem.getComments())));
            txtShares.setText("$no".replace("$no", feedItem.getShares() > 99 ? "99+" : String.valueOf(feedItem.getShares())));
            txtLikes.setText(feedItem.likes > 99 ? "99+" : String.valueOf(feedItem.likes));
            likesLayout.setVisibility(feedItem.likes > 0 ? View.VISIBLE : View.GONE);
            txtLikesCount.setText(String.valueOf(feedItem.likes));
            if (feedItem.getIsShared() == 1) {
                txtName.setText(feedItem.user[1].getName());
                verifiedIcon.setVisibility(feedItem.user[1].isVerified() ? View.VISIBLE : View.INVISIBLE);
                txtShared.setText("$name shared $share\'s $post.".replace("$name", feedItem.user[0].getName())
                        .replace("$share", feedItem.user[1].getName()).replace("$post", getPostType(feedItem.getType())));
                sharedProfileLink = new Link(feedItem.user[0].getName())
                        .setTextColor(Color.parseColor("#2b5a83"))
                        .setHighlightAlpha(.0f)
                        .setUnderlined(false)
                        .setBold(true);

                sharedPostProfile = new Link(feedItem.user[1].getName())
                        .setTextColor(Color.parseColor("#2b5a83"))
                        .setHighlightAlpha(.0f)
                        .setUnderlined(false)
                        .setBold(true);
                LinkBuilder.on(txtShared).addLink(sharedProfileLink).addLink(sharedPostProfile).build();
                sharedView.setVisibility(View.VISIBLE);
            }
            txtDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        private String getPostType(int type) {
            switch (type) {
                case 1: {
                    return "photo";
                }
                case 3: {
                    return "video";
                }
                default: {
                    return "post";
                }
            }
        }

        private int getAudienceIcon(int i) {
            if (i == 0) {
                return R.drawable.ic_friends_small;
            } else if (i == 1) {
                return R.drawable.ic_followers_small;
            } else {
                return R.drawable.ic_public_small;
            }
        }

        public Feed getFeedItem() {
            return feedItem;
        }
    }
}
