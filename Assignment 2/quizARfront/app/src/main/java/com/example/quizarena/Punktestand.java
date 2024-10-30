package com.example.quizarena;

public class Punktestand {
    public Question nextQuestion;
    public long roundsLeft;
    public long punkt1;
    public long punkt2;
    public long punkt3;
    public long punkt4;
    public long punkt5;
    // TODO von host verwaltet und mit anderen Spielern geteilt

    public Punktestand(long runden) {
        roundsLeft = runden;
    }

    public long punkte(int s)
    {
        if(s == 0) {
            return punkt1;
        } else if(s == 1) {
            return punkt2;
        } else if(s == 2) {
            return punkt3;
        } else if(s == 3) {
            return punkt4;
        } else {
            return punkt5;
        }
    }

    public void setPunkt(int s, long p)
    {
        if(s == 0) {
            punkt1 = p;
        } else if(s == 1) {
            punkt2 = p;
        } else if(s == 2) {
            punkt3 = p;
        } else if(s == 3) {
            punkt4 = p;
        } else if(s == 4) {
            punkt5 = p;
        }
    }

    public void addPunkt(int s, long p)
    {
        if(s == 0) {
            punkt1 += p;
        } else if(s == 1) {
            punkt2 += p;
        } else if(s == 2) {
            punkt3 += p;
        } else if(s == 3) {
            punkt4 += p;
        } else if(s == 4) {
            punkt5 += p;
        }
    }
}
