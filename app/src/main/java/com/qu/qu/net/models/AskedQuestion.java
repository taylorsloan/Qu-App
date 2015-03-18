package com.qu.qu.net.models;

import com.orm.SugarRecord;

/**
 * Created by Taylor on 3/16/2015.
 */
public class AskedQuestion extends SugarRecord<AskedQuestion> {
    int pk;
    String question;
    int positiveCount;
    int negativeCount;

    public AskedQuestion() {
    }

    public AskedQuestion(int pk, int positiveCount, String question, int negativeCount) {
        this.pk = pk;
        this.positiveCount = positiveCount;
        this.question = question;
        this.negativeCount = negativeCount;
    }

    public AskedQuestion(Question question) {
        this.pk = question.getId();
        this.question = question.getQuestion();
        this.positiveCount = question.getPositiveCount();
        this.negativeCount = question.getNegativeCount();
    }

    public int getPk() {
        return pk;
    }

    public void setPk(int pk) {
        this.pk = pk;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getPositiveCount() {
        return positiveCount;
    }

    public void setPositiveCount(int positiveCount) {
        this.positiveCount = positiveCount;
    }

    public int getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(int negativeCount) {
        this.negativeCount = negativeCount;
    }
}
