package com.example.loan_c;

public class calculation {

    private int month;
    private double amountOwed;
    private double monthlyPayment;
    private double interest;
    private double principal;
    private double endingBalance;

    public calculation(int month, double amountOwed, double monthlyPayment, double principal, double interest, double endingBalance) {
        this.month=month;
        this.amountOwed = amountOwed;
        this.monthlyPayment = monthlyPayment;
        this.interest = interest;
        this.principal = principal;
        this.endingBalance = endingBalance;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public double getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(double amountOwed) {
        this.amountOwed = amountOwed;
    }

    public double getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public double getInterest() {
        return interest;
    }

    public void setInterest(double interest) {
        this.interest = interest;
    }

    public double getPrincipal() {
        return principal;
    }

    public void setPrincipal(double principal) {
        this.principal = principal;
    }

    public double getEndingBalance() {
        return endingBalance;
    }

    public void setEndingBalance(double endingBalance) {
        this.endingBalance = endingBalance;
    }
}
