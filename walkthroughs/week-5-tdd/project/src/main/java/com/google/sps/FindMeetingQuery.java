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
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public final class FindMeetingQuery {
  private Collection<TimeRange> intervalFinder(List<Event> events_list, MeetingRequest request, Collection<TimeRange> possibleTimes) {
    Collection<String> mandatory = request.getAttendees();
    long duration = request.getDuration();

    int prev_end = 0;
      for (Event e : events_list){
        Collection<String> intersection = new HashSet<>(mandatory);
        intersection.retainAll(e.getAttendees());
        TimeRange range = e.getWhen();
        if (intersection.size() > 0){
          if (duration <= range.start() - prev_end) {
            possibleTimes.add(TimeRange.fromStartEnd(prev_end, range.start(), false));
          }
          if (prev_end < range.end()) {
            prev_end = range.end();
          }
        }
      }
      if (prev_end != TimeRange.END_OF_DAY + 1) {
        possibleTimes.add(TimeRange.fromStartEnd(prev_end, TimeRange.END_OF_DAY, true));
      }
      return possibleTimes;
  }

  private Collection<Event> eventFinderWithAttendee(Collection<Event> events_list, String attendee) {
    List<Event> eventsByAttendee = new ArrayList<Event>(); 

    for (Event e : events_list){
      if (e.getAttendees().contains(attendee)){
        eventsByAttendee.add(e);
      }
    }
    return eventsByAttendee;
  }

  private Collection<TimeRange> rangeSplitByNestedEvents(Collection<Event> sorted_events, TimeRange range, long duration) {
    List<TimeRange> splitEvents = new ArrayList<TimeRange>(); 
    int prevEndTime = range.start();

    for (Event e : sorted_events) {
      System.out.println("range: "+range+", event: "+e.getWhen());
      if (range.overlaps(e.getWhen())) {
        if (range.start() <= e.getWhen().start()) {
          TimeRange begin = TimeRange.fromStartEnd(prevEndTime, e.getWhen().start(), false);
          System.out.println("begin: "+begin);
          if (begin.duration() >= duration){
            splitEvents.add(begin);
          }
        }
        prevEndTime = e.getWhen().end();

      }
    }
    System.out.println("prevEndTime: " + prevEndTime + ", range: " + range.end());
    if (prevEndTime < range.end()) {
      System.out.println(TimeRange.fromStartEnd(prevEndTime, range.end(), false));
      splitEvents.add(TimeRange.fromStartEnd(prevEndTime, range.end(), false));
    }
    return splitEvents;
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long duration = request.getDuration();
 
    // get mandatory and optional attendees
    Collection<String> mandatory = request.getAttendees();
    Collection<String> optional = request.getOptionalAttendees();
    
    // sort the events to be in order by start time
    List<Event> events_list = new ArrayList<Event>(events); 
    Collections.sort(events_list, Event.ORDER_BY_START); 
    List<TimeRange> possibleTimes = new ArrayList<>();

    // if the duration of meeting is invalid, then there are no meeting times.
    if (duration > TimeRange.getTimeInMinutes(23, 59) || duration < 0) {
      return possibleTimes;
    }
    
    // if there are no events for the day, then the meeting can be any time.
    if (events.size() == 0) {
      possibleTimes.add(TimeRange.WHOLE_DAY);
      return possibleTimes;
    }
    
    // If there are optional attendees
    if (optional.size() > 0) {
      System.out.println("in optional.size > 0");

      List<String> allAttendees = new ArrayList<>(mandatory);
      allAttendees.addAll(optional);
      // Create new meeting request with mandatory and optional attendees as mandatory
      MeetingRequest new_request = new MeetingRequest(allAttendees, duration);
      Collection<TimeRange> slotsOptional = intervalFinder(events_list, new_request, possibleTimes);
      // if there is a slot with all mandatory and optional attendees, then return
      if (slotsOptional.size() > 0) {
        return slotsOptional;
      }
      else if (optional.size() > 1) {
        System.out.println("in optional.size > 1");
        Collection<TimeRange> availableTimesMand = intervalFinder(events_list, request, possibleTimes);
        System.out.println("Mandatory times: " + availableTimesMand);
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

        Set<TimeRange> containedSet = new HashSet<>();
        for (String opAttendee : optional) {
          System.out.println(opAttendee);
          Collection<Event> eventsWithAttendee = eventFinderWithAttendee(events_list, opAttendee);
          for (TimeRange range : availableTimesMand) {
            containedSet.addAll(rangeSplitByNestedEvents(eventsWithAttendee, range, duration));
          }
          System.out.println("contained Set: " + containedSet);

        }
        List<TimeRange> contained = new ArrayList<>(containedSet);

        System.out.println("contained: " + contained);
        if (contained.size() > 0) {
          for (TimeRange c : contained) {
            int counterOverlaps = 0;
            for (String opAttendee : optional) {
              Collection<Event> eventsWithAttendee = eventFinderWithAttendee(events, opAttendee);
              for (Event e : eventsWithAttendee){
                if (c.overlaps(e.getWhen())) {
                  counterOverlaps += 1;
                }
              }
            }
            System.out.println("timerange: " + c + ", overlaps: " + counterOverlaps);
            if (counterOverlaps < minOverlaps) {
              bestTimes.clear();
              minOverlaps = counterOverlaps;
              bestTimes.add(c);
            }
            else if (counterOverlaps == minOverlaps) {
              bestTimes.add(c);
            }
          }
          return bestTimes;
        }
        else {
          for (TimeRange range : availableTimesMand) {
            int counterOverlaps = 0;
            for (String opAttendee : optional) {
              Collection<Event> eventsWithAttendee = eventFinderWithAttendee(events, opAttendee);
              for (Event e : eventsWithAttendee){
                if (e.getWhen().overlaps(range)) {
                  counterOverlaps += 1;
                }
              }
            }
            if (counterOverlaps < minOverlaps) {
              bestTimes.clear();
              minOverlaps = counterOverlaps;
              bestTimes.add(range);
            }
            else if (counterOverlaps == minOverlaps) {
              bestTimes.add(range);
            }
          }
          return bestTimes;
        }
      }
      // if there are no mandatory attendees and there are no meeting times for all optional attendees, then there is no meeting time
      else if (mandatory.size() == 0) {
        return possibleTimes;
      }
    }
    return intervalFinder(events_list, request, possibleTimes);
  }
}
