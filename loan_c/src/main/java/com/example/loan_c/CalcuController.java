package com.example.loan_c;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private double parseInterestRate(String interestRateInput) {
        try {
            String trimmedInput = interestRateInput.trim().replace("%", "");
            return Double.parseDouble(trimmedInput) / 100;
        } catch (NumberFormatException e) {
            System.err.println("Invalid interest rate input: " + interestRateInput);
            return 0.0;
        }
    }

    private List<calculation> calculatePaymentSchedule(double loanAmount, int totalMonths, double interestRate) {
        List<calculation> payments = new ArrayList<>();
        double monthlyInterestRate = interestRate / 100 / 12;
        double monthlyPayment = (loanAmount * monthlyInterestRate) / (1 - Math.pow((1 + monthlyInterestRate), -totalMonths));

        double beginningBalance = loanAmount;

        for (int i = 1; i <= totalMonths; i++) {
            double interest = beginningBalance * monthlyInterestRate;
            double principal = monthlyPayment - interest;
            double endingBalance = beginningBalance - principal;

            if (endingBalance < 0 && Math.abs(endingBalance) < 0.01) {
                principal += endingBalance;
                endingBalance = 0.0;
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
        String interestRateInput = (String) model.getAttribute("interestRate").toString();
        double interestRate = parseInterestRate(interestRateInput);
        String compoundPeriod = (String) model.getAttribute("compoundPeriod");
        String payBack = (String) model.getAttribute("payBack");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Loan Payments");


        CellStyle accountingStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        accountingStyle.setDataFormat(format.getFormat("$#,##0.00"));

        CellStyle percentageStyle = workbook.createCellStyle();
        percentageStyle.setDataFormat(format.getFormat("0.00%"));


        createLoanDetailRow(sheet, 0, "Loan Amount ($):", loanAmount);
        createLoanDetailRow(sheet, 1, "Loan Term (Months):", loanTerm);
        createLoanDetailRow(sheet, 2, "Interest Rate (%):", interestRate * 100);
        createLoanDetailRow(sheet, 3, "Compound Period:", compoundPeriod);
        createLoanDetailRow(sheet, 4, "Payback:", payBack);


        Row interestRateRow = sheet.getRow(2);
        Cell interestRateCell = interestRateRow.getCell(1);
        interestRateCell.setCellStyle(percentageStyle);


        Row headerRow = sheet.createRow(5);
        String[] headerNames = {"Period (Month)", "Beginning Balance", "Payment", "Interest", "Principal", "Ending Balance"};
        for (int i = 0; i < headerNames.length; i++) {
            headerRow.createCell(i).setCellValue(headerNames[i]);
        }


        for (int i = 0; i < payments.size(); i++) {
            calculation payment = payments.get(i);
            Row row = sheet.createRow(i + 6);

            row.createCell(0).setCellValue(payment.getMonth());
            Cell beginningBalanceCell = row.createCell(1);
            beginningBalanceCell.setCellValue(payment.getAmountOwed());
            beginningBalanceCell.setCellStyle(accountingStyle);


            row.createCell(2).setCellFormula("(B1*(B3/12))/(1-(1+(B3/12))^(-10*12))");

            row.createCell(3).setCellFormula(String.format("(B%d * B$3 * (1/12))", i + 7));
            row.getCell(3).setCellStyle(accountingStyle);

            row.createCell(4).setCellFormula(String.format("C%d - D%d", i + 7, i + 7));
            row.getCell(4).setCellStyle(accountingStyle);

            if (i == payments.size() - 1) {
                row.createCell(5).setCellFormula(String.format("B%d - E%d", i + 7, i + 7));
            } else {
                row.createCell(5).setCellFormula(String.format("B%d - E%d", i + 7, i + 7));
            }
            row.getCell(5).setCellStyle(accountingStyle);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=loan_payments.xlsx");
        return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, HttpStatus.OK);
    }

    private void createLoanDetailRow(Sheet sheet, int rowIndex, String label, Object value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);

        if (label.equals("Interest Rate (%):")) {
            double interestRate = (double) value;
            valueCell.setCellValue(interestRate / 100);
            // Create percentage style
            CellStyle percentageStyle = sheet.getWorkbook().createCellStyle();
            DataFormat format = sheet.getWorkbook().createDataFormat();
            percentageStyle.setDataFormat(format.getFormat("0.00%")); 
            valueCell.setCellStyle(percentageStyle);
        } else {
            valueCell.setCellValue(value != null ? value.toString() : "N/A");
        }
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
