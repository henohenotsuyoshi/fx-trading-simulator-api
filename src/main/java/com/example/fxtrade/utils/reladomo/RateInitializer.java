package com.example.fxtrade.utils.reladomo;

import com.example.fxtrade.FxTradeApplication;
import com.example.fxtrade.models.Rate;
import com.example.fxtrade.models.RateFinder;
import com.example.fxtrade.models.RateList;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class RateInitializer {
    public static void run() {
        InputStream resourceAsStream = FxTradeApplication.class.getClassLoader().getResourceAsStream("data/rates.csv");
        try {
            long existingCount = RateFinder.findMany(RateFinder.all()).count();
            if (existingCount > 0) {
                System.out.println("Rates already initialized (" + existingCount + " records). Skipping initialization.");
                return;
            }

            // CSVファイルの読み込み
            InputStreamReader isr = new InputStreamReader(resourceAsStream, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            List<String> lines = Lists.mutable.empty();
            while(reader.ready()) {
                lines.add(reader.readLine());
            }

            MutableList<String> currencies = ArrayIterate.drop(lines.get(0).split(","), 1);
            RateList ratesList = Lists.mutable.ofAll(Iterate.drop(lines, 1)).flatCollect(line -> {
                String[] items = line.split(",");
                String[] dateSplit = items[0].split("/");
                LocalDate date = LocalDate.of(Integer.valueOf(dateSplit[0]), Integer.valueOf(dateSplit[1]), Integer.valueOf(dateSplit[2]));
                MutableList<Double> ratesDouble = Lists.mutable.ofAll(ArrayIterate.drop(items, 1)).collect(Double::valueOf);
                MutableList<Rate> rates = ratesDouble.zipWithIndex().collect(rateAndIndex -> {
                    String currency = currencies.get(rateAndIndex.getTwo());
                    return new Rate(currency, Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), rateAndIndex.getOne());
                });
                return rates;
            }, new RateList());
            Multimap<LocalDate, Rate> dateToRates = Iterate.groupBy(ratesList, Rate::getDate).collectKeysValues((date, rates) -> Tuples.pair(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(), rates));
            LocalDate minDate = Iterate.min(dateToRates.keySet(), LocalDate::compareTo);
            LocalDate maxDate = Iterate.max(dateToRates.keySet(), LocalDate::compareTo);

            LocalDate latestDate = minDate;
            RichIterable<Rate> latestRates = dateToRates.get(minDate);
            for (LocalDate date = minDate; date.isBefore(maxDate) ; date = date.plusDays(1)) {
                if(date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY))
                {
                    continue;
                }
                if(dateToRates.containsKey(date)) {
                    latestDate = date;
                    latestRates = dateToRates.get(date);
                } else {
                    LocalDate futureDate = date;
                    for (;!dateToRates.containsKey(futureDate); futureDate = futureDate.plusDays(1));
                    RichIterable<Rate> futureRates = dateToRates.get(futureDate);
                    MutableMap<String, Rate> currencyToLatestRate = Iterate.toMap(latestRates, Rate::getCurrency);
                    MutableMap<String, Rate> currencyToFutureRate = Iterate.toMap(futureRates, Rate::getCurrency);

                    for (String currency: currencyToLatestRate.keySet()) {
                        Rate latestRate = currencyToLatestRate.get(currency);
                        Rate futureRate = currencyToFutureRate.get(currency);
                        double rate = (latestRate.getRate() * ChronoUnit.DAYS.between(latestDate, date) + futureRate.getRate() * ChronoUnit.DAYS.between(date, futureDate)) / ChronoUnit.DAYS.between(latestDate, futureDate);
                        ratesList.add(new Rate(currency, Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), rate));
                    }
                }
            }
            int batchSize = 500;
            for (int i = 0; i < ratesList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, ratesList.size());
                RateList batch = new RateList(ratesList.subList(i, end));
                batch.insertAll();
            }
        } catch (IOException e) {

        }
    }
}
