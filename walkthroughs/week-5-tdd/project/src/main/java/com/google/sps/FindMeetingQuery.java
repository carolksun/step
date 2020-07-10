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

package com.google.sps;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

public final class FindMeetingQuery {
  private Collection<TimeRange> intervalFinder(List<Event> events_list, MeetingRequest request, Collection<TimeRange> possibleTimes)  {
    Collection<String> attendees = request.getAttendees();
    long duration = request.getDuration();

    int prev_end = 0;
      for (Event e : events_list){
          Collection<String> intersection = new HashSet<>(attendees);
          intersection.retainAll(e.getAttendees());
          TimeRange range = e.getWhen();
          if (intersection.size() > 0){
              if (duration <= range.start() - prev_end){
                  possibleTimes.add(TimeRange.fromStartEnd(prev_end, range.start(), false));
              }
              if (prev_end < range.end()){
                  prev_end = range.end();
              }
          }
      }
      if (prev_end != TimeRange.END_OF_DAY + 1){
          possibleTimes.add(TimeRange.fromStartEnd(prev_end, TimeRange.END_OF_DAY, true));
      }

      return possibleTimes;
  }

  // runtime: O(n log n) + O(n^2) = O(n^2)
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long duration = request.getDuration();
 
    // get mandatory and optional attendees
    Collection<String> attendees = request.getAttendees();
    Collection<String> optional = request.getOptionalAttendees();
    
    // sort the events to be in order by start time O(n log n)
    List<Event> events_list = new ArrayList<Event>(events); 
    Collections.sort(events_list, Event.ORDER_BY_START); 

    List<TimeRange> possibleTimes = new ArrayList<>();

    // if the duration of meeting is invalid, then there are no meeting times.
    if (duration > TimeRange.getTimeInMinutes(23, 59) || duration < 0){
        return possibleTimes;
    }
    
    // if there are no events for the day, then the meeting can be any time.
    if (events.size() == 0){
        possibleTimes.add(TimeRange.WHOLE_DAY);
        return possibleTimes;
    }
    
    // intersection - O(n)
    // For loop rutime - O(n^2)
    if (optional.size() > 0) {
      List<String> allAttendees = new ArrayList<>(attendees);
      allAttendees.addAll(optional);
      MeetingRequest new_request = new MeetingRequest(allAttendees, duration);
      Collection<TimeRange> slotsOptional = intervalFinder(events_list, new_request, possibleTimes);
      if (slotsOptional.size() > 0) {
        return slotsOptional;
      }
      else if (attendees.size() == 0) {
        return possibleTimes;
      }
      else if (optional.size() > 1) {
        Collection<TimeRange> availableTimesMand = intervalFinder(events_list, request, possibleTimes);
        List<TimeRange> bestTimes = new ArrayList<>();
        int minOverlaps = Integer.MAX_VALUE;
        Collection<String> optionalAt = request.getOptionalAttendees();
        ArrayList<Event> onlyOptionalEvents = new ArrayList<>();
        for (Event e : events_list){
          Collection<String> eventAttendees = e.getAttendees();
          Collection<String> optionalEventAttendees = new HashSet<>(eventAttendees);
          optionalEventAttendees.retainAll(optionalAt);
          if (optionalEventAttendees.size() > 0) {
            onlyOptionalEvents.add(e);
          }
        }

        List<TimeRange> contained = new ArrayList<>();
        for (TimeRange range : availableTimesMand) {
          for (Event e : onlyOptionalEvents){
            if (e.getWhen().overlaps(range)) {
              if (range.start() <= e.getWhen().start()) {
                TimeRange begin = TimeRange.fromStartEnd(range.start(), e.getWhen().start(), false);
                if (begin.duration() >= duration){
                  contained.add(begin);
                }
              }
              if (range.end() >= e.getWhen().end()) {
                TimeRange end = TimeRange.fromStartEnd(e.getWhen().end(), range.end(), false);
                if (end.duration() >= duration){
                  contained.add(end);
                }
              }
            }
          }
        }
        System.out.println(contained);     
        if (contained.size() > 0){
          for (TimeRange c : contained) {
            int counterOverlaps = 0;
            for (TimeRange range : availableTimesMand){
              if (c.overlaps(range)) {
                counterOverlaps += 1;
              }
            }
            if (counterOverlaps < minOverlaps){
              bestTimes.clear();
              minOverlaps = counterOverlaps;
              bestTimes.add(c);
            }
            else if (counterOverlaps == minOverlaps){
              bestTimes.add(c);
            }
          }
          return bestTimes;
        }
        else {
          for (TimeRange range : availableTimesMand) {
            int counterOverlaps = 0;
            for (Event e : onlyOptionalEvents){
              if (e.getWhen().overlaps(range)) {
                counterOverlaps += 1;
              }
            }
            if (counterOverlaps < minOverlaps){
              bestTimes.clear();
              minOverlaps = counterOverlaps;
              bestTimes.add(range);
            }
            else if (counterOverlaps == minOverlaps){
              bestTimes.add(range);
            }
          }
          return bestTimes;
        }
        
      }
    }
    return intervalFinder(events_list, request, possibleTimes);
  }
}
