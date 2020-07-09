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

  /** 
   * Reads in csv file and parses the data into the sleepData HashMap. Calls 
   * the movingAverage function to add the moving average data as addition 
   * elements in the array (value of the HashMap).
   */
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

  /** Produces a JSON response containing the sleepData HashMap. */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    String json = gson.toJson(sleepData);
    response.getWriter().println(json);
  }

  /** 
   * Calculates 7-day moving average for a single data column and returns a list
   * of the moving average data points.
   */
  private List<Double> singleMovingAverage(boolean score){
    int size = 7;
    int midway = size/2;
    List<Double> origData = new ArrayList<>();
    Set<String> keys = sleepData.keySet();
    for(String date:keys){
        Double[] arr = sleepData.get(date);
        if (score){
          origData.add(arr[0]);
        }
        else{
          origData.add(arr[1]);
        }
    }
    List<Double> maList = new ArrayList<>(); 
    int sum;
    double average;
    for (int i = 0; i < origData.size() ; i++){
        sum = 0;
        average = 0;
        /** Handles the first few data points*/
        if (i < midway) {
           for (int j = 0; j < i + midway + 1; j++) {
                sum += origData.get(j);
            }
            average = sum / (i + midway + 1);
        }
        /** Handles the last few data points*/
        else if (i + midway + 1> origData.size()){
            for (int j = i - midway; j < origData.size(); j++) {
                sum += origData.get(j);
            }
            average = sum / (origData.size() - i + midway);
        }
        /** Handles the middle data points*/
        else{
            for (int j = i - midway; j < i + midway + 1; j++) {
                sum += origData.get(j);
            }
            average = sum / size;
        }
        maList.add(average);
    }
    return maList;
  }

  /** 
   * Combines the moving average data lists with the raw data lists to create a 
   * new array to replace the values of the HashMap.
   */
  private void movingAverage() {
    List<Double> maSleepScore = singleMovingAverage(true);
    List<Double> maDeepSleep = singleMovingAverage(false);
    Set<String> keys = sleepData.keySet();
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