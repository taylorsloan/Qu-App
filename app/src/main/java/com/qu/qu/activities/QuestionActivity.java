package com.qu.qu.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.qu.qu.R;
import com.qu.qu.fragments.QuestionFragment;

import butterknife.ButterKnife;
import timber.log.Timber;


public class QuestionActivity extends Activity implements QuestionFragment.OnLoadNextQuestionListener {

    private final static String CURRENT_QUESTION_TAG = "current";

    FirebaseAuth mAuth;
    FirebaseUser mUser;

    QuestionFragment currentQuestion;

    int styleIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        ButterKnife.inject(this);
        mAuth = FirebaseAuth.getInstance();
        styleIndex += 1;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUser = mAuth.getCurrentUser();
        if(mUser == null){
            mAuth.signInAnonymously().addOnCompleteListener(this, anonymousSignInListener);
        }else{
            loadFirstQuestion();
        }
    }

    @Override
    public void loadNextQuestion() {
        if (styleIndex >= QuestionFragment.STYLES.length - 1) {
            styleIndex = 0;
        } else {
            styleIndex += 1;
        }
        currentQuestion = QuestionFragment.newInstance(styleIndex);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.fadein, R.animator.fadeout)
                .replace(R.id.fragment_container, currentQuestion, CURRENT_QUESTION_TAG)
                .commit();
    }

    OnCompleteListener<AuthResult> anonymousSignInListener = new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Timber.d("signInAnonymously:success");
                mUser = mAuth.getCurrentUser();
                loadFirstQuestion(); // Load first question after successful authentication
            } else {
                // If sign in fails, display a message to the user.
                Timber.w("signInAnonymously:failure", task.getException());
                Toast.makeText(QuestionActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void loadFirstQuestion(){
        currentQuestion = (QuestionFragment) getFragmentManager().findFragmentByTag(CURRENT_QUESTION_TAG);
        if (currentQuestion == null) {
            currentQuestion = QuestionFragment.newInstance(styleIndex);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, currentQuestion, CURRENT_QUESTION_TAG)
                    .commit();
        }
    }
}
