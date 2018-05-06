package it.polito.mad.mad2018.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Conversation;

public class ChatAdapter extends FirebaseRecyclerAdapter<Conversation, ChatAdapter.ChatHolder> {

    private final OnItemClickListener onItemClickListener;
    private final OnItemCountChangedListener onItemCountChangedListener;

    ChatAdapter(@NonNull FirebaseRecyclerOptions<Conversation> options,
                @NonNull OnItemClickListener onItemClickListener,
                @NonNull OnItemCountChangedListener onItemCountChangedListener) {
        super(options);
        this.onItemClickListener = onItemClickListener;
        this.onItemCountChangedListener = onItemCountChangedListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Conversation model) {
        holder.update(model);
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new ChatAdapter.ChatHolder(view, onItemClickListener);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        onItemCountChangedListener.onCountChangedListener(this.getItemCount());
    }

    interface OnItemClickListener {
        void onClick(View view, Conversation conversation);
    }

    interface OnItemCountChangedListener {
        void onCountChangedListener(int count);
    }

    static class ChatHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final TextView peerId;
        private final TextView message;
        private final TextView date;

        private Conversation model;

        ChatHolder(View view, @NonNull OnItemClickListener listener) {
            super(view);

            this.context = view.getContext();
            this.peerId = view.findViewById(R.id.cl_chat_item_peer);
            this.message = view.findViewById(R.id.cl_chat_item_message);
            this.date = view.findViewById(R.id.cl_chat_item_date);

            view.setOnClickListener(v -> listener.onClick(v, model));
        }

        private void update(Conversation model) {
            this.model = model;
            peerId.setText(model.getPeerUserId());
            date.setText(model.getLastMessage().getDateTime());
            message.setText(model.getLastMessage().getText());
        }
    }
}
