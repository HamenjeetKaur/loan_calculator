package com.example.loan_c;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"payments", "loanAmount", "loanTerm", "interestRate", "compoundPeriod", "payBack"})
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


        int totalMonths = (loanTermYears * 12) + loanTermMonths;


        List<calculation> payments = calculatePaymentSchedule(loanAmount, totalMonths, interestRate);
        model.addAttribute("payments", payments);
        model.addAttribute("loanAmount", loanAmount);
        model.addAttribute("loanTerm", totalMonths);
        model.addAttribute("interestRate", interestRate);
        model.addAttribute("compoundPeriod", compoundPeriod);
        model.addAttribute("payBack", payBack);
        return "calculation";
    }

    private List<calculation> calculatePaymentSchedule(double loanAmount, int totalMonths, double interestRate) {
        List<calculation> payments = new ArrayList<>();

        double monthlyInterestRate = interestRate / 100 / 12;
        double monthlyPayment = (loanAmount * monthlyInterestRate) / (1 - Math.pow((1 + monthlyInterestRate), -totalMonths));

        // Initialize beginning balance
        double beginningBalance = loanAmount;

        for (int i = 1; i <= totalMonths; i++) {
            double interest = beginningBalance * monthlyInterestRate;
            double principal = monthlyPayment - interest;
            double endingBalance = beginningBalance - principal;

            // Adjust if ending balance is negative and close to zero
            if (endingBalance < 0 && Math.abs(endingBalance) < 0.01) {
                principal += endingBalance; // Adjust principal by the negative ending balance
                endingBalance = 0.0; // Set ending balance to 0
            }

            payments.add(new calculation(i,
                    Double.parseDouble(String.format("%.2f", beginningBalance)),
                    Double.parseDouble(String.format("%.2f", monthlyPayment)),
                    Double.parseDouble(String.format("%.2f", principal)),
                    Double.parseDouble(String.format("%.2f", interest)),
                    Double.parseDouble(String.format("%.2f", endingBalance))));

            beginningBalance = endingBalance;
        }

        return payments;
    }






    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadExcel(Model model) throws IOException {
        List<calculation> payments = (List<calculation>) model.getAttribute("payments");

        Double loanAmount = (Double) model.getAttribute("loanAmount");
        Integer loanTerm = (Integer) model.getAttribute("loanTerm");
        Double interestRate = (Double) model.getAttribute("interestRate");
        String compoundPeriod = (String) model.getAttribute("compoundPeriod");
        String payBack = (String) model.getAttribute("payBack");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Loan Payments");

        Row loanDetailRow1 = sheet.createRow(0);
        loanDetailRow1.createCell(0).setCellValue("Loan Amount ($):");
        loanDetailRow1.createCell(1).setCellValue(loanAmount != null ? loanAmount.toString() : "N/A");

        Row loanDetailRow2 = sheet.createRow(1);
        loanDetailRow2.createCell(0).setCellValue("Loan Term (Months):");
        loanDetailRow2.createCell(1).setCellValue(loanTerm != null ? loanTerm.toString() : "N/A");

        Row loanDetailRow3 = sheet.createRow(2);
        loanDetailRow3.createCell(0).setCellValue("Interest Rate (%):");
        loanDetailRow3.createCell(1).setCellValue(interestRate != null ? interestRate + "%" : "N/A");

        Row loanDetailRow4 = sheet.createRow(3);
        loanDetailRow4.createCell(0).setCellValue("Compound Period:");
        loanDetailRow4.createCell(1).setCellValue(compoundPeriod != null ? compoundPeriod : "N/A");

        Row loanDetailRow5 = sheet.createRow(4);
        loanDetailRow5.createCell(0).setCellValue("Payback:");
        loanDetailRow5.createCell(1).setCellValue(payBack != null ? payBack : "N/A");

        Row headerRow = sheet.createRow(5);
        String[] headerNames = {"Period (Month)", "Beginning Balance", "Payment", "Interest", "Principal", "Ending Balance"};
        for (int i = 0; i < headerNames.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerNames[i]);
        }

        for (int i = 0; i < payments.size(); i++) {
            calculation payment = payments.get(i);
            Row row = sheet.createRow(i + 6); // Adjust row index if needed

            row.createCell(0).setCellValue(payment.getMonth());
            // Set the beginning balance in B column (index 1)
            row.createCell(1).setCellValue(payment.getAmountOwed());

            // Payment formula in C column
            Cell paymentCell = row.createCell(2);
            String paymentFormula = "ROUND((B1*(B3/12))/(1-(1+(B3/12))^(-" + loanTerm + ")), 2)";
            paymentCell.setCellFormula(paymentFormula);

            // Interest formula in D column (index 3)
            Cell interestCell = row.createCell(3);
            String interestFormula = "ROUND((B" + (i + 7) + " * $B$3 * (1/12)), 2)";
            interestCell.setCellFormula(interestFormula);

            // Principal formula in E column (index 4)
            Cell principalCell = row.createCell(4);
            String principalFormula = "ROUND(C" + (i + 7) + " - D" + (i + 7) + ", 2)";
            principalCell.setCellFormula(principalFormula);

            // Ending Balance formula in F column (index 5)
            Cell endingBalanceCell = row.createCell(5);
            String endingBalanceFormula;

            // Check if it's the last payment row
            if (i == payments.size() - 1) {
                // Set ending balance formula to zero for the last row
                endingBalanceFormula = "ROUND(B" + (i + 7) + " - E" + (i + 7) + ", 2)";

                // Adjust principal by adding the small value to the beginning balance
                double adjustedPrincipal = payment.getAmountOwed() + 0.01; // Adjusted based on requirement
                principalCell.setCellValue(adjustedPrincipal); // Set the adjusted principal directly
            } else {
                // Regular ending balance formula for other rows
                endingBalanceFormula = "ROUND(B" + (i + 7) + " - E" + (i + 7) + ", 2)";
            }

            endingBalanceCell.setCellFormula(endingBalanceFormula);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=loan_payments.xlsx");
        return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, HttpStatus.OK);
    }



    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
