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
@SessionAttributes("payments")
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


        if (payments == null || payments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null); // Handle as appropriate
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Loan Payments");

        Row headerRow = sheet.createRow(0);
        String[] headerNames = {"Period (Month)", "Beginning Balance", "Payment", "Interest", "Principal", "Ending Balance"};
        for (int i = 0; i < headerNames.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerNames[i]);
        }

        for (int i = 0; i < payments.size(); i++) {
            calculation payment = payments.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(payment.getMonth());
            row.createCell(1).setCellValue(payment.getAmountOwed());
            row.createCell(2).setCellValue(payment.getMonthlyPayment());
            row.createCell(3).setCellValue(payment.getInterest());
            row.createCell(4).setCellValue(payment.getPrincipal());
            row.createCell(5).setCellValue(payment.getEndingBalance());
        }

        // Write the output to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        // Prepare the response
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=loan_payments.xlsx");
        return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, HttpStatus.OK);
    }




    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
