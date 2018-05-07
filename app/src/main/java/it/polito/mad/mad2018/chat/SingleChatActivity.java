package it.polito.mad.mad2018.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Conversation;

public class SingleChatActivity extends AppCompatActivity {
    private String peerId;
    private EditText message;
    private TextView noMessages;
    private ImageButton btnSend;
    private RecyclerView messages;
    private Conversation conversation;
    private SingleChatAdapter.OnItemCountChangedListener onItemCountChangedListener;
    private SingleChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Set the toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null) {
            peerId = (String) intent.getExtras().get("user_id");
            conversation = (Conversation) intent.getExtras().get(Conversation.CONVERSATION_KEY);

            if(conversation == null){
                conversation = new Conversation((Book) intent.getExtras().get(Book.BOOK_KEY));
            }
            this.setTitle(peerId);
        }

        findViews();

        messages.setLayoutManager(new LinearLayoutManager(this));

        btnSend.setOnClickListener(v -> onClickButtonSend());


        onItemCountChangedListener = (count) -> {
            noMessages.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            messages.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        };

        FirebaseRecyclerOptions<Conversation.Message> options = conversation.getMessages();
        adapter = new SingleChatAdapter(options, null, onItemCountChangedListener);
        messages.setAdapter(adapter);

    }

    private void onClickButtonSend() {
        String msg = message.getText().toString();



    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void findViews(){
        this.message = this.findViewById(R.id.message_send);
        this.btnSend = this.findViewById(R.id.button_send);
        this.noMessages = this.findViewById(R.id.chat_no_messages);
        this.messages = this.findViewById(R.id.chat_messages);
    }

}
