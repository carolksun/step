// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Scanner;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@WebServlet("/sleep-data")
public class SleepDataServlet extends HttpServlet {

  private LinkedHashMap<String, Double[]> sleepData = new LinkedHashMap<>();

  @Override
  public void init() {
    Scanner scanner = new Scanner(getServletContext().getResourceAsStream(
            "/WEB-INF/sleep_data.csv"));
    while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] cells = line.split(",");

        String timestamp = cells[0];
        int tindex = timestamp.indexOf("T"); 
        String date_string = timestamp.substring(0 , tindex);
        Double[] data = new Double[]{(double)Integer.parseInt(cells[1]),  
                                    (double)Integer.parseInt(cells[3])}; 
        sleepData.put(date_string, data);
    }
    scanner.close();
    movingAverage();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    String json = gson.toJson(sleepData);
    response.getWriter().println(json);
  }

  private void movingAverage() {
    int size = 7;
    int midway = size/2;
    List<Double> sleepScore = new ArrayList<>();
    List<Double> deepSleep = new ArrayList<>();

    Set<String> keys = sleepData.keySet();
    for(String date:keys){
        Double[] arr = sleepData.get(date);
        sleepScore.add(arr[0]);
        deepSleep.add(arr[1]);
    }

    List<Double> maSleepScore = new ArrayList<>(); 
    List<Double> maDeepSleep = new ArrayList<>();    
    int ssSum, dsSum;
    double ssAverage, dsAverage;
    for (int i = 0; i < sleepScore.size() ; i++){
        ssSum = 0;
        dsSum = 0;
        ssAverage = 0;
        dsAverage = 0;
        if (i < midway) {
           for (int j = 0; j < i + midway + 1; j++) {
                ssSum += sleepScore.get(j);
                dsSum += deepSleep.get(j);
            }
            ssAverage = ssSum / (i + midway + 1);
            dsAverage = dsSum / (i + midway + 1); 
        }
        else if (i + midway + 1> sleepScore.size()){
            for (int j = i - midway; j < sleepScore.size(); j++) {
                ssSum += sleepScore.get(j);
                dsSum += deepSleep.get(j);
            }
            ssAverage = ssSum / (sleepScore.size() - i + midway);
            dsAverage = dsSum / (sleepScore.size() - i + midway); 
        }
        else{
            for (int j = i - midway; j < i + midway + 1; j++) {
                ssSum += sleepScore.get(j);
                dsSum += deepSleep.get(j);
            }
            ssAverage = ssSum / size;
            dsAverage = dsSum / size;
        }
        maSleepScore.add(ssAverage);
        maDeepSleep.add(dsAverage);
    }
    int counter = 0;
    for (String date: keys) {
        Double[] arr = sleepData.get(date);
        Double[] data = new Double[]{arr[0],  
                            arr[1],
                            maSleepScore.get(counter),
                            maDeepSleep.get(counter)};
        counter++;
        sleepData.put(date, data);
    }
    }
}