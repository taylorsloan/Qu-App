package com.qu.qu.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.qu.qu.BaseApplication;
import com.qu.qu.R;
import com.qu.qu.activities.ListQuestionsActivity;
import com.qu.qu.net.QuEndpointsService;
import com.qu.qu.net.models.Question;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.app.RxFragment;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;
import tr.xip.errorview.ErrorView;

/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionFragment extends RxFragment {

    public static final Integer[] STYLES = {R.style.QuestionAlizarin, R.style.QuestionEcstasy,
            R.style.QuestionRipeLemon, R.style.QuestionEmerald, R.style.QuestionRoyalBlue,
            R.style.QuestionRebeccaPurple, R.style.QuestionLynch, R.style.QuestionCabaret};
    private static final String KEY_STYLE = "style";
    public static PublishSubject<Boolean> isConnected = PublishSubject.create();
    static QuEndpointsService quEndpointsService = BaseApplication.getQuEndpointsService();
    int styleIndex = 0;
    int backgroundColor;

    @InjectView(R.id.text_question)
    TextView textQuestion;

    @InjectView(R.id.create_question)
    FloatingActionButton createQuestionButton;
    OnLoadNextQuestionListener listener;

    @InjectView(R.id.button_postive)
    ImageButton buttonPositive;

    @InjectView(R.id.button_negative)
    ImageButton buttonNegative;

    @InjectView(R.id.layout_counts)
    LinearLayout layoutCounts;

    @InjectView(R.id.text_positive)
    TextView textPositive;

    @InjectView(R.id.text_negative)
    TextView textNegative;

    @InjectView(R.id.progress_bar_question)
    ProgressWheel progressWheel;

    @InjectView(R.id.error_view)
    ErrorView errorView;

    @InjectView(R.id.main_content)
    View mainContent;
    Observable<Question> getQuestionRequest;
    Subscription getQuestionSubscription, answerPositiveSubscription, answerNegativeSubscription;
    Question question;

    public QuestionFragment() {
        // Required empty public constructor
    }

    public static QuestionFragment newInstance(int style) {
        QuestionFragment fragment = new QuestionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_STYLE, style);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            styleIndex = getArguments().getInt(KEY_STYLE);
        }
        getQuestionRequest = quEndpointsService.getRandomQuestion()
                .observeOn(AndroidSchedulers.mainThread()).retry(3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), STYLES[styleIndex]);
        backgroundColor = getStyleBackgroundColor(contextThemeWrapper);
        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        // inflate the layout using the cloned inflater, not default inflater
        View view = localInflater.inflate(R.layout.fragment_question, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configCreateQuestionButton();
        getQuestionSubscription = getQuestionRequest.subscribe(new QuestionSubscriber());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnLoadNextQuestionListener) activity;
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (answerPositiveSubscription != null) {
            answerPositiveSubscription.unsubscribe();
        }
        if (answerNegativeSubscription != null) {
            answerNegativeSubscription.unsubscribe();
        }
    }

    @SuppressLint("ResourceAsColor")
    void showErrorView(Throwable error) {
        if (error instanceof RetrofitError) {
            RetrofitError retrofitError = (RetrofitError) error;
            if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {
                errorView.setErrorTitle("Could Not Connect");
                errorView.setErrorSubtitle("Check Internet Connectivity");
            } else if (retrofitError.getKind() == RetrofitError.Kind.HTTP) {
                errorView.setError(retrofitError.getResponse().getStatus());
            }
        }
        errorView.setOnRetryListener(() -> {
            getQuestionSubscription.unsubscribe();
            getQuestionSubscription = getQuestionRequest.subscribe(new QuestionSubscriber());
        });
        mainContent.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    void hideErrorView() {
        mainContent.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    public int getStyleBackgroundColor(Context context) {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.background, a, true);
        return a.data;
    }

    public int getLighterColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = 0.5f + 0.8f * hsv[2];
        return Color.HSVToColor(hsv);
    }

    public void configCreateQuestionButton() {
        createQuestionButton.setColorNormal(backgroundColor);
        int lighterBackgroundColor = getLighterColor(backgroundColor);
        createQuestionButton.setColorPressed(lighterBackgroundColor);
    }

    @SuppressLint("unused")
    @OnClick(R.id.create_question)
    public void createQuestion() {
        startActivity(new Intent(getActivity(), ListQuestionsActivity.class));
    }

    @SuppressLint("unused")
    @OnClick(R.id.button_postive)
    public void positiveAnswer() {
        try {
            buttonPositive.setClickable(false);
            buttonNegative.setClickable(false);
            Observable<Question> request = quEndpointsService.answerPositive(question.getId())
                    .observeOn(AndroidSchedulers.mainThread())
                    .cache();
            answerPositiveSubscription = request.subscribe(question -> {
                Timber.d("Positive Votes: %s", question.getPositiveCount());
                textNegative.setTextColor(getResources().getColor(R.color.dim_white));
                showCounts(question);

            });
            new Handler().postDelayed(listener::loadNextQuestion, 3000);
        } catch (NullPointerException e) {
            buttonPositive.setClickable(true);
            buttonNegative.setClickable(true);
        }
    }

    @SuppressLint("unused")
    @OnClick(R.id.button_negative)
    public void negativeAnswer() {
        try {
            buttonPositive.setClickable(false);
            buttonNegative.setClickable(false);
            Observable<Question> request = quEndpointsService.answerNegative(question.getId())
                    .observeOn(AndroidSchedulers.mainThread())
                    .cache();
            answerNegativeSubscription = request.subscribe(question -> {
                Timber.d("Negative Votes: %s", question.getNegativeCount());
                textPositive.setTextColor(getResources().getColor(R.color.dim_white));
                showCounts(question);
            });
            new Handler().postDelayed(listener::loadNextQuestion, 2000);
        } catch (NullPointerException e) {
            buttonPositive.setClickable(true);
            buttonNegative.setClickable(true);
        }
    }

    void showCounts(Question question) {
        textPositive.setText(String.valueOf(question.getPositiveCount()));
        textNegative.setText(String.valueOf(question.getNegativeCount()));
        layoutCounts.setVisibility(View.VISIBLE);
        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(500);
        layoutCounts.startAnimation(in);
    }

    public interface OnLoadNextQuestionListener {
        void loadNextQuestion();
    }

    class QuestionSubscriber extends Subscriber<Question> {
        @Override
        public void onCompleted() {
            hideErrorView();
        }

        @Override
        public void onError(Throwable error) {
            Timber.e("Error: %s", error.getMessage(), error.getCause());
            error.printStackTrace();
            showErrorView(error);
        }

        @Override
        public void onNext(Question question) {
            Timber.d(question.getQuestion());
            QuestionFragment.this.question = question;
            textQuestion.setText(question.getQuestion());
            final Animation in = new AlphaAnimation(0.0f, 1.0f);
            in.setDuration(1000);
            textQuestion.startAnimation(in);
            progressWheel.setVisibility(View.GONE);
        }
    }

}
