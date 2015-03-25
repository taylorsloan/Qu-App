package com.qu.qu.activities;

import android.app.Activity;
import android.os.Bundle;

import com.qu.qu.R;
import com.qu.qu.fragments.QuestionFragment;

import butterknife.ButterKnife;


public class QuestionActivity extends Activity implements QuestionFragment.OnLoadNextQuestionListener {

    private final static String CURRENT_QUESTION_TAG = "current";

    QuestionFragment currentQuestion;

    int styleIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        ButterKnife.inject(this);

        currentQuestion = (QuestionFragment) getFragmentManager().findFragmentByTag(CURRENT_QUESTION_TAG);
        if (currentQuestion == null) {
            currentQuestion = QuestionFragment.newInstance(styleIndex);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, currentQuestion, CURRENT_QUESTION_TAG)
                    .commit();
        }

        styleIndex += 1;
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
                .setCustomAnimations(R.anim.fadein, R.anim.fadeout)
                .replace(R.id.fragment_container, currentQuestion, CURRENT_QUESTION_TAG)
                .commit();
    }
}
