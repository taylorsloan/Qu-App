package com.qu.qu.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.TextView;

import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.util.Insertable;
import com.qu.qu.BaseApplication;
import com.qu.qu.R;
import com.qu.qu.net.models.AskedQuestion;
import com.qu.qu.net.models.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import me.drakeet.materialdialog.MaterialDialog;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class ListQuestionsActivity extends ListActivity {

    private static final int UPDATE_INTERVAL = 30;

    ScheduledExecutorService scheduler;

    View dialogView;

    Subscription createQuestionSubscription;

    ArrayList<AskedQuestion> askedQuestions = new ArrayList<>();
    QuestionAdapter questionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_questions);
        ButterKnife.inject(this);
        loadQuestions();
        setListAdapter(questionAdapter);
        registerForContextMenu(getListView());
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

    void loadQuestions() {
        questionAdapter = new QuestionAdapter(this, askedQuestions);
        AlphaInAnimationAdapter animationAdapter = new AlphaInAnimationAdapter(questionAdapter);
        animationAdapter.setAbsListView(getListView());
        List<AskedQuestion> storedQuestions = AskedQuestion.find(AskedQuestion.class, null, null, null, " id DESC", null);
        askedQuestions.addAll(storedQuestions);
        questionAdapter.notifyDataSetChanged();
    }

    void removeQuestion(int position) {
        try {
            AskedQuestion selectedQuestion = askedQuestions.get(position);
            Timber.d("Removing Question: %s Position: %s", selectedQuestion.getQuestion()
                    , String.valueOf(position));
            askedQuestions.remove(position);
            selectedQuestion.delete();
            questionAdapter.notifyDataSetChanged();
        } catch (IndexOutOfBoundsException e) {
            Timber.e("Could not remove question at index %s", String.valueOf(position));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ((scheduler == null) || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduler.scheduleAtFixedRate
                (() -> updateQuestions(), 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        scheduler.shutdown();
    }

    void updateQuestions() {
        Observable<AskedQuestion> questions = Observable.from(askedQuestions);
        questions.subscribe(askedQuestion -> {
            BaseApplication.getQuEndpointsService().getQuestion(askedQuestion.getPk())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(questionUpdate -> {
                        Timber.d("Request update on Question: %s", questionUpdate.getQuestion());
                        if (isUpdated(askedQuestion, questionUpdate)) {
                            Timber.d("Updating Question: %s P: %s N: %s", questionUpdate.getQuestion()
                                    , String.valueOf(questionUpdate.getPositiveCount()),
                                    String.valueOf(questionUpdate.getNegativeCount()));
                            askedQuestion.setPositiveCount(questionUpdate.getPositiveCount());
                            askedQuestion.setNegativeCount(questionUpdate.getNegativeCount());
                            askedQuestion.save();
                        }
                        questionAdapter.notifyDataSetChanged();
                    });
        });
    }

    boolean isUpdated(AskedQuestion askedQuestion, Question questionUpdate) {
        boolean isUpdated = false;
        if (askedQuestion.getPositiveCount() != questionUpdate.getPositiveCount()) {
            isUpdated = true;
        }
        if (askedQuestion.getNegativeCount() != questionUpdate.getNegativeCount()) {
            isUpdated = true;
        }
        return isUpdated;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (createQuestionSubscription != null) {
            createQuestionSubscription.unsubscribe();
        }

    }

    @OnClick(R.id.button_ask)
    void createQuestion() {
        dialogView = getLayoutInflater().inflate(R.layout.dialog_ask_question, null);
        MaterialDialog materialDialog = new MaterialDialog(this);
        materialDialog.setTitle(getString(R.string.ask_question))
                .setContentView(dialogView)
                .setPositiveButton(getString(R.string.ASK), v -> {
                    EditText editText = (EditText) dialogView.findViewById(R.id.et_question);
                    String questionText = prepareQuestion(editText.getText().toString());
                    Question question = new Question(questionText);
                    Observable<Question> request = BaseApplication
                            .getQuEndpointsService()
                            .createQuestion(question)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .cache();
                    createQuestionSubscription = request.subscribe(q -> {
                        Timber.d("Asked Question: %s", q.getQuestion());
                        AskedQuestion askedQuestion = new AskedQuestion(q);
                        ((DynamicListView) getListView()).insert(0, askedQuestion);
                        questionAdapter.notifyDataSetChanged();
                        askedQuestion.save();
                    });
                    materialDialog.dismiss();
                })
                .setNegativeButton(getString(R.string.CANCEL), v -> {
                    materialDialog.dismiss();
                });
        materialDialog.show();
    }

    String prepareQuestion(String text) {
        String questionText = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        if (questionText.endsWith(".")) {
            questionText.substring(0, questionText.length() - 1);
            questionText = questionText + "?";
        } else if (!questionText.endsWith("?")) {
            questionText = questionText + "?";
        }
        return questionText;
    }


    class QuestionAdapter extends BaseAdapter implements Insertable<AskedQuestion> {

        List<AskedQuestion> questions;
        LayoutInflater inflater;

        QuestionAdapter(Activity context, List<AskedQuestion> questions) {
            this.questions = questions;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return questions.size();
        }

        @Override
        public AskedQuestion getItem(int position) {
            return questions.get(position);
        }

        @Override
        public void add(int i, @NonNull AskedQuestion askedQuestion) {
            questions.add(i, askedQuestion);
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
            AskedQuestion question = getItem(position);
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
