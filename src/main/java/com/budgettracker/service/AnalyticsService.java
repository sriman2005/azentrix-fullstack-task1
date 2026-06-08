package com.budgettracker.service;

import com.budgettracker.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired private TransactionRepository transactionRepository;

    public Map<String, Object> getDashboardSummary(Long userId) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        BigDecimal income = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "INCOME", year, month));
        BigDecimal expense = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "EXPENSE", year, month));
        BigDecimal balance = income.subtract(expense);
        BigDecimal savings = income.compareTo(BigDecimal.ZERO) > 0 ? balance.max(BigDecimal.ZERO) : BigDecimal.ZERO;

        // Previous month
        LocalDate prevMonth = now.minusMonths(1);
        BigDecimal prevIncome = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(
                userId, "INCOME", prevMonth.getYear(), prevMonth.getMonthValue()));
        BigDecimal prevExpense = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(
                userId, "EXPENSE", prevMonth.getYear(), prevMonth.getMonthValue()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncome", income);
        result.put("totalExpense", expense);
        result.put("balance", balance);
        result.put("savings", savings);
        result.put("savingsRate", income.compareTo(BigDecimal.ZERO) > 0
                ? savings.multiply(BigDecimal.valueOf(100)).divide(income, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("incomeChange", computeChange(income, prevIncome));
        result.put("expenseChange", computeChange(expense, prevExpense));
        result.put("month", month);
        result.put("year", year);
        return result;
    }

    public Map<String, Object> getCategoryBreakdown(Long userId, int year, int month) {
        List<Object[]> rows = transactionRepository.getCategoryBreakdown(userId, year, month);
        List<Map<String, Object>> categories = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Object[] row : rows) {
            BigDecimal amt = (BigDecimal) row[1];
            total = total.add(amt);
        }

        for (Object[] row : rows) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("name", row[0]);
            BigDecimal amt = (BigDecimal) row[1];
            cat.put("amount", amt);
            cat.put("percentage", total.compareTo(BigDecimal.ZERO) > 0
                    ? amt.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            categories.add(cat);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> getMonthlyComparison(Long userId, int year) {
        List<Object[]> rows = transactionRepository.getMonthlyTotals(userId, year);
        BigDecimal[] income = new BigDecimal[13];
        BigDecimal[] expense = new BigDecimal[13];
        Arrays.fill(income, BigDecimal.ZERO);
        Arrays.fill(expense, BigDecimal.ZERO);

        for (Object[] row : rows) {
            int m = ((Number) row[0]).intValue();
            String type = (String) row[1];
            BigDecimal amt = (BigDecimal) row[2];
            if ("INCOME".equals(type)) income[m] = amt;
            else expense[m] = amt;
        }

        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        List<BigDecimal> incomeList = new ArrayList<>();
        List<BigDecimal> expenseList = new ArrayList<>();
        List<BigDecimal> balanceList = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            incomeList.add(income[i]);
            expenseList.add(expense[i]);
            balanceList.add(income[i].subtract(expense[i]));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", Arrays.asList(months));
        result.put("income", incomeList);
        result.put("expense", expenseList);
        result.put("balance", balanceList);
        result.put("year", year);
        return result;
    }

    public Map<String, Object> getFinancialHealthScore(Long userId) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        BigDecimal income = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "INCOME", year, month));
        BigDecimal expense = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "EXPENSE", year, month));

        // Savings rate score (25 pts)
        double savingsRate = 0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savings = income.subtract(expense).max(BigDecimal.ZERO);
            savingsRate = savings.multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP).doubleValue();
        }
        double savingsScore = Math.min(savingsRate * 25 / 20.0, 25); // full score at 20% savings rate

        // Expense ratio score (30 pts) - lower expense/income = better
        double expenseRatio = income.compareTo(BigDecimal.ZERO) > 0
                ? expense.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP).doubleValue() : 100;
        double expenseScore = Math.max(30 - (expenseRatio - 50) * 0.6, 0);
        expenseScore = Math.min(expenseScore, 30);

        // Activity score (20 pts) - has transactions this month
        List<Object[]> cats = transactionRepository.getCategoryBreakdown(userId, year, month);
        double activityScore = cats.isEmpty() ? 0 : Math.min(cats.size() * 4.0, 20);

        // Income tracking (25 pts) - has income this month
        double incomeScore = income.compareTo(BigDecimal.ZERO) > 0 ? 25 : 0;

        double total = savingsScore + expenseScore + activityScore + incomeScore;
        total = Math.min(Math.max(total, 0), 100);

        String grade;
        String message;
        if (total >= 80) { grade = "Excellent"; message = "Outstanding financial health! Keep it up!"; }
        else if (total >= 65) { grade = "Good"; message = "Good financial management. Small improvements can make it great!"; }
        else if (total >= 50) { grade = "Fair"; message = "Room for improvement. Focus on savings and budgeting."; }
        else if (total >= 35) { grade = "Poor"; message = "Your finances need attention. Review your spending habits."; }
        else { grade = "Critical"; message = "Immediate action needed. Start tracking and reducing expenses."; }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", Math.round(total));
        result.put("grade", grade);
        result.put("message", message);
        result.put("breakdown", Map.of(
                "savingsScore", Math.round(savingsScore),
                "expenseScore", Math.round(expenseScore),
                "activityScore", Math.round(activityScore),
                "incomeScore", Math.round(incomeScore)
        ));
        return result;
    }

    public List<Map<String, String>> getSmartInsights(Long userId) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        List<Map<String, String>> insights = new ArrayList<>();

        BigDecimal income = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "INCOME", year, month));
        BigDecimal expense = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(userId, "EXPENSE", year, month));

        // Previous month comparison
        LocalDate prev = now.minusMonths(1);
        BigDecimal prevExpense = getOrZero(transactionRepository.sumByUserAndTypeAndMonthYear(
                userId, "EXPENSE", prev.getYear(), prev.getMonthValue()));

        if (prevExpense.compareTo(BigDecimal.ZERO) > 0 && expense.compareTo(BigDecimal.ZERO) > 0) {
            double change = expense.subtract(prevExpense).multiply(BigDecimal.valueOf(100))
                    .divide(prevExpense, 1, RoundingMode.HALF_UP).doubleValue();
            if (change > 20) {
                insights.add(Map.of("type", "warning", "icon", "trending-up",
                        "title", "Spending Spike Detected",
                        "message", String.format("Your spending is %.0f%% higher than last month. Consider reviewing your expenses.", change)));
            } else if (change < -10) {
                insights.add(Map.of("type", "success", "icon", "trending-down",
                        "title", "Great Savings Progress",
                        "message", String.format("You've reduced spending by %.0f%% compared to last month!", Math.abs(change))));
            }
        }

        // Savings rate
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savings = income.subtract(expense).max(BigDecimal.ZERO);
            double rate = savings.multiply(BigDecimal.valueOf(100))
                    .divide(income, 1, RoundingMode.HALF_UP).doubleValue();
            if (rate < 10) {
                insights.add(Map.of("type", "warning", "icon", "piggy-bank",
                        "title", "Low Savings Rate",
                        "message", String.format("You're saving only %.0f%% of income. Aim for at least 20%% for financial security.", rate)));
            } else if (rate >= 30) {
                insights.add(Map.of("type", "success", "icon", "star",
                        "title", "Excellent Savings Rate",
                        "message", String.format("Amazing! You're saving %.0f%% of your income. You're on track for financial freedom!", rate)));
            }
        }

        // Top spending category
        List<Object[]> cats = transactionRepository.getCategoryBreakdown(userId, year, month);
        if (!cats.isEmpty() && income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal topAmt = (BigDecimal) cats.get(0)[1];
            double topPct = topAmt.multiply(BigDecimal.valueOf(100))
                    .divide(income, 1, RoundingMode.HALF_UP).doubleValue();
            if (topPct > 40) {
                insights.add(Map.of("type", "info", "icon", "chart-pie",
                        "title", "High Category Concentration",
                        "message", String.format("%.0f%% of income goes to %s. Diversifying expenses improves financial stability.", topPct, cats.get(0)[0])));
            }
        }

        // No income tracked
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            insights.add(Map.of("type", "info", "icon", "plus-circle",
                    "title", "Track Your Income",
                    "message", "No income recorded this month. Add your salary or other income sources for accurate insights."));
        }

        if (insights.isEmpty()) {
            insights.add(Map.of("type", "success", "icon", "check-circle",
                    "title", "Finances Look Healthy",
                    "message", "Your financial habits are on track. Keep monitoring regularly!"));
        }

        return insights;
    }

    private BigDecimal getOrZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private double computeChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 0;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
