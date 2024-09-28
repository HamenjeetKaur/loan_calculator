package com.example.loan_c;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CalcuController {

    @GetMapping("/")
    public String signIn() {
        return "signIn";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @PostMapping("/signIn")
    public String processLogin() {
        return "redirect:/dashboard";
    }

    @GetMapping("/calculation")
    public String calculation() {
        return "calculation";
    }




    @PostMapping("/calculation")
    public String calculateLoan(
            @RequestParam("loanAmount") double loanAmount,
            @RequestParam("loanTermYears") int loanTermYears,
            @RequestParam("loanTermMonths") int loanTermMonths,
            @RequestParam("interestRate") double interestRate,
            @RequestParam("compoundPeriod") String compoundPeriod,
            @RequestParam("payBack") String payBack,
            Model model) {

        // Total loan term in months
        int totalMonths = (loanTermYears * 12) + loanTermMonths;

        // Here you would calculate the schedule
        List<calculation> payments = calculatePaymentSchedule(loanAmount, totalMonths, interestRate);

        model.addAttribute("payments", payments);
        return "calculation"; // Return the view name for displaying the schedule
    }

    private List<calculation> calculatePaymentSchedule(double loanAmount, int totalMonths, double interestRate) {
        List<calculation> payments = new ArrayList<>();

        // Monthly interest rate
        double monthlyInterestRate = interestRate / 100 / 12;

        // Calculate the payment using the formula provided
        double monthlyPayment = (loanAmount * monthlyInterestRate) / (1 - Math.pow((1 + monthlyInterestRate), -totalMonths));

        // Initialize beginning balance
        double beginningBalance = loanAmount;

        for (int i = 1; i <= totalMonths; i++) {
            double interest = beginningBalance * monthlyInterestRate; // Interest based on beginning balance
            double principal = monthlyPayment - interest; // Principal payment
            double endingBalance = beginningBalance - principal; // Ending balance after payment

            // Format the values to two decimal places
            payments.add(new calculation(i,
                    Double.parseDouble(String.format("%.2f", beginningBalance)),
                    Double.parseDouble(String.format("%.2f", principal)),
                    Double.parseDouble(String.format("%.2f", interest)),
                    Double.parseDouble(String.format("%.2f", endingBalance))));

            // Update the beginning balance for the next iteration
            beginningBalance = endingBalance;
        }

        return payments;
    }




    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
