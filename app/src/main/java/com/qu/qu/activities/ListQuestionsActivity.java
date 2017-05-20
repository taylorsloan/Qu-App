package com.qu.qu.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.qu.qu.BaseApplication;
import com.qu.qu.R;
import com.qu.qu.data.QuestionManager;
import com.qu.qu.data.models.Question;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import me.drakeet.materialdialog.MaterialDialog;
import rx.Subscription;
import timber.log.Timber;

public class ListQuestionsActivity extends ListActivity {

    protected static final int REQUEST_OK = 1;
    View.OnClickListener micButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            try {
                startActivityForResult(i, REQUEST_OK);
            } catch (Exception e) {
                Toast.makeText(ListQuestionsActivity.this, "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
            }
        }
    };

    View dialogView;
    EditText askingText;
    ImageButton micAskButton;
    ArrayList<Question> askedQuestions = new ArrayList<>();
    QuestionAdapter questionAdapter;

    QuestionManager questionManager;
    DatabaseReference userQuestions;
    FirebaseListAdapter firebaseQuestionAdapter;

    FirebaseAuth mAuth;
    FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_questions);
        ButterKnife.inject(this);
        registerForContextMenu(getListView());
        mAuth = FirebaseAuth.getInstance();
        questionManager = ((BaseApplication)getApplication()).getQuestionManager();
//        questionAdapter = new QuestionAdapter(this, askedQuestions);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_questions, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.action_remove:
                removeQuestion(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Question askedQuestion = askedQuestions.get(position);
    }

    void removeQuestion(int position) {
        try {
            Question selectedQuestion = askedQuestions.get(position);
            Timber.d("Removing Question: %s Position: %s", selectedQuestion.getQuestion()
                    , String.valueOf(position));
            askedQuestions.remove(position);
            questionAdapter.notifyDataSetChanged();
        } catch (IndexOutOfBoundsException e) {
            Timber.e("Could not remove question at index %s", String.valueOf(position));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUser = mAuth.getCurrentUser();
        if(mUser == null){
            mAuth.signInAnonymously().addOnCompleteListener(this, anonymousSignInListener);
        }
        userQuestions = questionManager.getUserQuestionDatabase(mUser.getUid());
//        userQuestions.addChildEventListener(questionEventListener);
        firebaseQuestionAdapter = new FirebaseListAdapter<Question>(this, Question.class, R.layout.item_asked_question, userQuestions){
            @Override
            protected void populateView(View v, Question model, int position) {
                ((TextView)v.findViewById(R.id.text_question)).setText(model.getQuestion());
                ((TextView)v.findViewById(R.id.text_positive)).setText(String.valueOf(model.getPositiveCount()));
                ((TextView)v.findViewById(R.id.text_negative)).setText(String.valueOf(model.getNegativeCount()));
            }
        };
        getListView().setAdapter(firebaseQuestionAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseQuestionAdapter.cleanup();
//        userQuestions.removeEventListener(questionEventListener);
    }

    OnCompleteListener<AuthResult> anonymousSignInListener = new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Timber.d("signInAnonymously:success");
                mUser = mAuth.getCurrentUser();
            } else {
                // If sign in fails, display a message to the user.
                Timber.w("signInAnonymously:failure", task.getException());
                Toast.makeText(ListQuestionsActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @OnClick(R.id.button_ask)
    void createQuestion() {
        dialogView = getLayoutInflater().inflate(R.layout.dialog_ask_question, null);
        askingText = (EditText) dialogView.findViewById(R.id.et_question);
        micAskButton = (ImageButton) dialogView.findViewById(R.id.button_mic_ask);
        micAskButton.setOnClickListener(micButtonListener);
        final MaterialDialog materialDialog = new MaterialDialog(this);
        materialDialog.setTitle(getString(R.string.ask_question))
                .setContentView(dialogView)
                .setPositiveButton(getString(R.string.ASK), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String questionText = askingText.getText().toString();
                        if ((questionText != null) && questionText.length() > 0) {
                            questionText = prepareQuestion(askingText.getText().toString());
                            Question question = new Question(questionText);
                            questionManager.createQuestion(question, mUser.getUid());
                            materialDialog.dismiss();
                        }
                    }
                }).setNegativeButton(getString(R.string.CANCEL), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        materialDialog.dismiss();
                    }
                });
        materialDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OK && resultCode == RESULT_OK) {
            ArrayList<String> thingsYouSaid = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            askingText.setText(thingsYouSaid.get(0));
        }
    }

    String prepareQuestion(String text) {
        String questionText = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        if (questionText.endsWith(".")) {
            questionText.substring(0, questionText.length() - 1);
            questionText = questionText + "?";
        } else if (!questionText.endsWith("?")) {
            questionText = questionText + "?";
        }
        questionText.trim();
        return questionText;
    }

    ChildEventListener questionEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Question q = dataSnapshot.getValue(Question.class);
            askedQuestions.add(q);
            questionAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    class QuestionAdapter extends BaseAdapter {

        List<Question> questions;
        LayoutInflater inflater;

        QuestionAdapter(Activity context, List<Question> questions) {
            this.questions = questions;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return questions.size();
        }

        @Override
        public Question getItem(int position) {
            return questions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            QuestionViewHolder viewHolder;
            if (convertView == null) {
                v = inflater.inflate(R.layout.item_asked_question, null);
                viewHolder = new QuestionViewHolder(v);
                v.setTag(viewHolder);
            } else {
                viewHolder = (QuestionViewHolder) v.getTag();
            }
            Question question = getItem(position);
            viewHolder.textQuestion.setText(question.getQuestion());
            viewHolder.textPositive.setText(String.valueOf(question.getPositiveCount()));
            viewHolder.textNegative.setText(String.valueOf(question.getNegativeCount()));
            return v;
        }

        class QuestionViewHolder {
            @InjectView(R.id.text_question)
            TextView textQuestion;
            @InjectView(R.id.text_positive)
            TextView textPositive;
            @InjectView(R.id.text_negative)
            TextView textNegative;

            QuestionViewHolder(View base) {
                ButterKnife.inject(this, base);
            }
        }
    }
}
