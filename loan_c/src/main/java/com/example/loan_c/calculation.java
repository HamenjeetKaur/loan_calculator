package com.example.loan_c;

public class calculation {

    private int month;
    private double amountOwed;
    private double paymentNeeded;
    private double interest;
    private double principal;
    private double endingBalance;

    public calculation(int month, double amountOwed, double principal, double interest, double endingBalance) {
        this.month=month;
        this.amountOwed = amountOwed;
        this.paymentNeeded = principal + interest; // Adjust as needed
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

    public double getPaymentNeeded() {
        return paymentNeeded;
    }

    public void setPaymentNeeded(double paymentNeeded) {
        this.paymentNeeded = paymentNeeded;
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
