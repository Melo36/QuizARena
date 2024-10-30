package com.example.quizarena;

public class Question {
    private final String question;
    private final String a;
    private final String b;
    private final String c;
    private final String d;
    private final String category;

    public Question(String q, String aa, String ab, String ac, String ad, String cat) {
        question = q;
        a = aa;
        b = ab;
        c = ac;
        d = ad;
        category = cat;
    }

    public String getQuestion() {
        return question;
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    public String getC() {
        return c;
    }

    public String getD() {
        return d;
    }

    public String getCategory() {
        return category;
    }
}
