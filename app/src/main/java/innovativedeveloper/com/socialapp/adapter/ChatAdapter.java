package innovativedeveloper.com.socialapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.dataset.Message;

public class ChatAdapter  extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Message> messageArrayList;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onAttachmentItemClick(View v, int position, int id);
    }

    public ChatAdapter(Context context, ArrayList<Message> messages) {
        this.context = context;
        this.messageArrayList = messages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == 20) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_post_right, parent, false);
        } else if (viewType == 15) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right, parent, false);
        } else if (viewType == 10) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_post_left, parent, false);
        } else if (viewType == 5){
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_left, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right, parent, false);
        }
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Message m = messageArrayList.get(holder.getAdapterPosition());
        if (m.getType() == 2) {
            final String[] message = m.getMessage().split(":");
            holder.txtMessage.setText("View " + message[0] + "'s Post");
            holder.form.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onAttachmentItemClick(v, holder.getAdapterPosition(), Integer.valueOf(message[1]));
                }
            });
        } else {
            holder.txtMessage.setText(m.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message m = messageArrayList.get(position);
        if (m.getType() == 2 && m.getIsOwn() == 1) {
            return 20;
        }
        else if (m.getType() == 1 && m.getIsOwn() == 1) {
            return 15;
        }
        else if (m.getType() == 2 && m.getIsOwn() == 0) {
            return 10;
        }
        else if (m.getType() == 1 && m.getIsOwn() == 0) {
            return 5;
        }
        else {
            return 0;
        }
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtMessage;
        View form;
        public ViewHolder(View itemView) {
            super(itemView);
            this.txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
            this.form = itemView.findViewById(R.id.form);
        }

    }
}
