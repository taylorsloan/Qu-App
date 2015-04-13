package com.qu.qu.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import me.drakeet.materialdialog.MaterialDialog;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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
    private static final int UPDATE_INTERVAL = 30;
    ScheduledExecutorService scheduler;
    View dialogView;
    EditText askingText;
    ImageButton micAskButton;
    Subscription createQuestionSubscription;
    ArrayList<AskedQuestion> askedQuestions = new ArrayList<>();
    QuestionAdapter questionAdapter;
    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_questions);
        ButterKnife.inject(this);
        realm = Realm.getInstance(this);
        loadQuestions();
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        AskedQuestion askedQuestion = askedQuestions.get(position);
        BaseApplication.getQuEndpointsService().getQuestion(askedQuestion.getPk())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new UpdateQuestionSubscriber(askedQuestion));
    }

    void loadQuestions() {
        questionAdapter = new QuestionAdapter(this, askedQuestions);
        getListView().setAdapter(questionAdapter);
        RealmQuery<AskedQuestion> query = realm.where(AskedQuestion.class);
        RealmResults<AskedQuestion> results = query.findAll();
        results.sort("pk", RealmResults.SORT_ORDER_DESCENDING);
        for (AskedQuestion question : results) {
            askedQuestions.add(question);
        }
        questionAdapter.notifyDataSetChanged();
    }

    void removeQuestion(int position) {
        try {
            AskedQuestion selectedQuestion = askedQuestions.get(position);
            Timber.d("Removing Question: %s Position: %s", selectedQuestion.getQuestion()
                    , String.valueOf(position));
            askedQuestions.remove(position);
            realm.beginTransaction();
            selectedQuestion.removeFromRealm();
            realm.commitTransaction();
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
        scheduler.scheduleAtFixedRate(() -> {
            /*try {
                updateQuestions();
            } catch (Exception e) {
                Timber.e(e, "Error Updating");
            }*/
            updateQuestions();
        }, 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        scheduler.shutdown();
    }

    void updateQuestions() {
        Timber.i("Updating Questions");
        Observable<AskedQuestion> questions = Observable.from(askedQuestions);
        questions.observeOn(AndroidSchedulers.mainThread()).subscribe(askedQuestion -> {
            BaseApplication.getQuEndpointsService().getQuestion(askedQuestion.getPk())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new UpdateQuestionSubscriber(askedQuestion));
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
        realm.close();

    }

    @OnClick(R.id.button_ask)
    void createQuestion() {
        dialogView = getLayoutInflater().inflate(R.layout.dialog_ask_question, null);
        askingText = (EditText) dialogView.findViewById(R.id.et_question);
        micAskButton = (ImageButton) dialogView.findViewById(R.id.button_mic_ask);
        micAskButton.setOnClickListener(micButtonListener);
        MaterialDialog materialDialog = new MaterialDialog(this);
        materialDialog.setTitle(getString(R.string.ask_question))
                .setContentView(dialogView)
                .setPositiveButton(getString(R.string.ASK), v -> {
                    String questionText = askingText.getText().toString();
                    if ((questionText != null) && questionText.length() > 0) {
                        questionText = prepareQuestion(askingText.getText().toString());
                        Question question = new Question(questionText);
                        Observable<Question> request = BaseApplication
                                .getQuEndpointsService()
                                .createQuestion(question)
                                .observeOn(AndroidSchedulers.mainThread())
                                .cache();
                        createQuestionSubscription = request.subscribe(new QuestionSubscriber());
                        materialDialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.CANCEL), v -> {
                    materialDialog.dismiss();
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

    class UpdateQuestionSubscriber extends Subscriber<Question> {

        AskedQuestion askedQuestion;

        UpdateQuestionSubscriber(AskedQuestion askedQuestion) {
            this.askedQuestion = askedQuestion;
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
//            Timber.e(e, "Error Updating");
        }

        @Override
        public void onNext(Question question) {
            Timber.d("Request update on Question: %s", question.getQuestion());
            if (isUpdated(askedQuestion, question)) {
                Timber.d("Updating Question: %s P: %s N: %s", question.getQuestion()
                        , String.valueOf(question.getPositiveCount()),
                        String.valueOf(question.getNegativeCount()));
                realm.beginTransaction();
                askedQuestion.setPositiveCount(question.getPositiveCount());
                askedQuestion.setNegativeCount(question.getNegativeCount());
                realm.commitTransaction();
            }
            questionAdapter.notifyDataSetChanged();
        }
    }

    class QuestionSubscriber extends Subscriber<Question> {

        @Override
        public void onCompleted() {
            questionAdapter.notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            MaterialDialog errorDialog = new MaterialDialog(ListQuestionsActivity.this);
            errorDialog.setTitle(getString(R.string.error))
                    .setMessage(getString(R.string.error_could_not_connect))
                    .setPositiveButton(getString(R.string.ok), v -> {
                        errorDialog.dismiss();
                    });
            errorDialog.show();
        }

        @Override
        public void onNext(Question question) {
            Timber.d("Asked Question: %s", question.getQuestion());
            AskedQuestion askedQuestion = new AskedQuestion(question);
            realm.beginTransaction();
            AskedQuestion saved = realm.copyToRealm(askedQuestion);
            askedQuestions.add(0, saved);
            realm.commitTransaction();
        }
    }


    class QuestionAdapter extends BaseAdapter {

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
