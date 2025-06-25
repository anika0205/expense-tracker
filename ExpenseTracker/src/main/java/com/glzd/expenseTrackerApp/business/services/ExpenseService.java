package com.glzd.expenseTrackerApp.business.services;

import com.glzd.expenseTrackerApp.business.model.Expense;
import com.glzd.expenseTrackerApp.data.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public Expense save(Expense entity) {
        return expenseRepository.save(entity);
    }

    /**
     *
     * @param id
     * @returns the saved entity if found, otherwise throws EntityNotFoundException
     */
    public Expense findById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sorry, the content you are looking for does not exist."));
    }


    /**
     * Retrieves a Page of Expense objects sorted by their creation date in descending order.
     *
     * The provided Pageable object determines the page number, page size, and any additional sorting or filtering options.
     * It is to ensure that each new expense added will stack on top.
     *
     * @param pageable The Pageable object specifying the page number, page size, and sorting preferences.
     * @return A Page containing a list of Expense objects sorted by their creation date in descending order.
     */
    public Page<Expense> findAll(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("creationDate").descending());

        return expenseRepository.findAll(sortedPageable);
    }


    public Iterable<Expense> findAll() {
        return expenseRepository.findAll();
    }

    public void deleteById(Long id) {
        Expense expenseToBeDeleted = findById(id);
        expenseRepository.delete(expenseToBeDeleted);
    }

    /**
     * Estimates the total amount of expenses from the given Iterable of Expense objects.
     *
     * @param expenses An Iterable of Expense objects containing expense data.
     * @return The total amount of expenses as a BigDecimal value.
     */
    public BigDecimal getTotalAmount(Iterable<Expense> expenses){
        return StreamSupport.
                stream(expenses.spliterator(), false)
                .toList()
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retrieves a Page of Expense objects filtered by year, month, and expense type.
     *
     * This method queries the expense repository to retrieve expenses within the specified year and month,
     * belonging to the given expense type. The results are ordered by creation date in descending order.
     *
     * @param year The year for filtering expenses.
     * @param month The month for filtering expenses.
     * @param expenseType The expense type for filtering expenses.
     * @param page The Pageable object specifying the desired page and page size.
     * @return A Page containing filtered Expense objects.
     */
    public Page<Expense> getExpensesByYearMonthAndType(int year, Month month, String expenseType, Pageable page) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return expenseRepository.findByDateBetweenAndExpenseTypeOrderByCreationDateDesc(startDate, endDate, expenseType, page);
    }

    /**
     * Retrieves a Page of Expense objects filtered by year and month.
     *
     * This method queries the expense repository to retrieve expenses within the specified year and month.
     * The results are ordered by creation date in descending order.
     *
     * @param year The year for filtering expenses.
     * @param month The month for filtering expenses.
     * @param page The Pageable object specifying the desired page and page size.
     * @return A Page containing filtered Expense objects.
     */
    public Page<Expense> getExpensesByYearMonth(int year, Month month, Pageable page) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return expenseRepository.findByDateBetweenOrderByCreationDateDesc(startDate, endDate, page);
    }


    /**
     * Retrieves a Page of Expense objects filtered by expense type.
     *
     * This method queries the expense repository to retrieve expenses of the specified expense type.
     * The results are ordered by creation date in descending order.
     *
     * @param expenseType The expense type for filtering expenses.
     * @param page The Pageable object specifying the desired page and page size.
     * @return A Page containing filtered Expense objects.
     */
    public Page<Expense> getExpensesByType(String expenseType, Pageable page) {
        return expenseRepository.findByExpenseTypeOrderByCreationDateDesc(expenseType, page);
    }


    /**
     * Converts a collection of Expense objects into a CSV-formatted string.
     * For a high number of records, it would be ineffective to convert all to String and hold in memory.
     * But for the case of simplicity, I wrote the code in this way since I am dealing with a little amount of data.
     *
     * This method iterates through the collection of Expense objects and formats them into
     * a CSV format, including the Id, Name of Expense, Type of Expense, Amount, Date, and Creation Timestamp.
     * The formatted CSV string is returned.
     *
     * @param expenses The collection of Expense objects to be converted.
     * @return A CSV-formatted string containing the data from the Expense objects.
     */
    public String convertToCSV(Iterable<Expense> expenses) {
        StringBuilder expensesAsCSV = new StringBuilder();
        expensesAsCSV.append("Id,Name of Expense,Type of expense,Amount,Date,Creation Timestamp\n");

        for (Expense expense: expenses) {
            expensesAsCSV.append(expense.getId()).append(",")
                    .append(expense.getName()).append(",")
                    .append(expense.getExpenseType()).append(",")
                    .append(expense.getAmount()).append(",")
                    .append(expense.getDate()).append(",")
                    .append(expense.getCreationDate()).append("\n");
        }

        return expensesAsCSV.toString();
    }

    public List<Expense> getExpensesForCurrentMonth() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // Convert to LocalDateTime for compatibility with Timestamp
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);

        return expenseRepository.findByCreationDateBetween(startDateTime, endDateTime);
    }


    public Map<String, Double> calculateExpenseByType(List<Expense> expenses) {
        Map<String, BigDecimal> expenseMap = new HashMap<>();
        for (Expense e : expenses) {
            String type = e.getExpenseType();
            expenseMap.put(type, expenseMap.getOrDefault(type, BigDecimal.ZERO).add(e.getAmount()));
        }

        return expenseMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    public Map<Integer, Double> calculateExpenseByWeeklyDates(List<Expense> expenses) {
        Map<Integer, BigDecimal> expenseBuckets = new TreeMap<>();

        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        List<Integer> keyDays = new ArrayList<>();
        keyDays.add(1); // always include day 1

        // Add all Sundays in the month
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY && date.getDayOfMonth() != 1) {
                keyDays.add(date.getDayOfMonth());
            }
        }

        // Always include last day of month (if not already)
        int lastDay = end.getDayOfMonth();
        if (!keyDays.contains(lastDay)) {
            keyDays.add(lastDay);
        }

        // Initialize buckets with 0
        for (Integer day : keyDays) {
            expenseBuckets.put(day, BigDecimal.ZERO);
        }

        // Distribute expenses to the appropriate bucket
        for (Expense e : expenses) {
            int expenseDay = e.getDate().getDayOfMonth();

            for (int i = keyDays.size() - 1; i >= 0; i--) {
                if (expenseDay >= keyDays.get(i)) {
                    expenseBuckets.put(keyDays.get(i), expenseBuckets.get(keyDays.get(i)).add(e.getAmount()));
                    break;
                }
            }
        }

        // Convert BigDecimal to Double
        return expenseBuckets.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }


}
