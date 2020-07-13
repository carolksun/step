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
  /**
   * Finds the possible TimeRanges where the mandatory attendees can attend
   * @param events_list sorted list of events in ascending start time order
   * @param request includes mandatory attendees and duration of meeting
   * @return collection of possible TimeRanges the meeting can be scheduled in  
   */
  private Collection<TimeRange> intervalFinder(List<Event> events_list, MeetingRequest request) {
    Collection<String> mandatory = request.getAttendees();
    long duration = request.getDuration();
    List<TimeRange> possibleTimes = new ArrayList<>();

    int prev_end = 0;
      for (Event e : events_list){
        // Determine if the attendees for the meeting are busy at other events
        Collection<String> intersection = new HashSet<>(mandatory);
        intersection.retainAll(e.getAttendees());
        TimeRange range = e.getWhen();
        if (intersection.size() > 0) {
          TimeRange possibleRange = TimeRange.fromStartEnd(prev_end, range.start(), false);
          if (duration <= possibleRange.duration()) {
            possibleTimes.add(possibleRange);
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

  /**
   * Finds a specific attendee's events for the day.
   * @param events_list sorted list of events in ascending start time order
   * @param attendee specific attendee to search for
   * @return collection of events that attendee has on the schedule
   */
  private Collection<Event> eventFinderWithAttendee(Collection<Event> events_list, String attendee) {
    List<Event> eventsByAttendee = new ArrayList<Event>(); 

    for (Event e : events_list){
      if (e.getAttendees().contains(attendee)){
        eventsByAttendee.add(e);
      }
    }
    return eventsByAttendee;
  }

  /**
   * Split large range into smaller pieces without event overlapping
   * Range:  |-------------|
   * Event:       |---|
   * Return: |----|   |----|
   * @param sorted_events sorted list of events in ascending start time order
   * @param range large range to split into smaller ranges
   * @param duration how long the request is for
   * @return collection of TimeRanges that was part of the large range without event overlaps
   */
  private Collection<TimeRange> rangeSplitByNestedEvents(Collection<Event> sorted_events, TimeRange range, long duration) {
    List<TimeRange> splitEvents = new ArrayList<TimeRange>(); 
    int prevEndTime = range.start();

    for (Event e : sorted_events) {
      if (range.overlaps(e.getWhen())) {
        if (range.start() <= e.getWhen().start()) {
          TimeRange begin = TimeRange.fromStartEnd(prevEndTime, e.getWhen().start(), false);
          if (begin.duration() >= duration){
            splitEvents.add(begin);
          }
        }
        prevEndTime = e.getWhen().end();

      }
    }
    if (prevEndTime < range.end()) {
      splitEvents.add(TimeRange.fromStartEnd(prevEndTime, range.end(), false));
    }
    return splitEvents;
  }

  /**
   * Counts the number of optional attendees can not make it to the meeting at the time range
   * Compares to the minimum number of optional attendees that can not make it
   * @param bestTimes collection which has the TimeRanges of meetings that has the minOverlap attendees
   * @param events events of the day
   * @param optional optional attendees for the meeting
   * @param range range in question to see how many optional attendees can not make it
   * @param minOverlaps current minimum number of optional attendees that can not make it to the meeting at the TimeRange. 
   * @return minimum number of overlaps. Return the minimum of current range's overlaps and minOverlaps
   */
  private int optionalCounter(Collection<TimeRange> bestTimes, Collection<Event> events, Collection<String> optional, TimeRange range, int minOverlaps) {
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
      bestTimes.add(range);
      return counterOverlaps;
    }
    else if (counterOverlaps == minOverlaps) {
      bestTimes.add(range);
    }
    return minOverlaps;
  }

  /**
   * Determines the TimeRanges where the mandatory and as many optional attendees as possible can attend the meeting
   * @param events events of the day
   * @param request includes mandatory and optional attendees, and the duration of the meeting
   * @return collection of TimeRanges where all mandatory and the greatest number of optional attendees can be at the meeting
   */
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

      List<String> allAttendees = new ArrayList<>(mandatory);
      allAttendees.addAll(optional);
      // Create new meeting request with mandatory and optional attendees as mandatory
      MeetingRequest new_request = new MeetingRequest(allAttendees, duration);

      // Find possible time ranges with all mandatory and all optional attendees
      Collection<TimeRange> slotsOptional = intervalFinder(events_list, new_request);
      
      // if there is a time with all mandatory and optional attendees, then return
      if (slotsOptional.size() > 0) {
        return slotsOptional;
      }
      // if there is no time where everyone can make it, and there is more than one optional person
      else if (optional.size() > 1) {
        // find times for just the mandatory attendees
        Collection<TimeRange> availableTimesMand = intervalFinder(events_list, request);

        // store the times with the minimum amount of event overlaps
        List<TimeRange> bestTimes = new ArrayList<>();
        int minOverlaps = Integer.MAX_VALUE;

        // containedSet will store the split ranges of the ranges of the availableTimesMand if an event that has an optional attendee is overlapping with the range for the availableTimesMand
        Set<TimeRange> containedSet = new HashSet<>();
        // Separate by optional attendee 
        for (String opAttendee : optional) {
          Collection<Event> eventsWithAttendee = eventFinderWithAttendee(events_list, opAttendee);
          for (TimeRange range : availableTimesMand) {
            containedSet.addAll(rangeSplitByNestedEvents(eventsWithAttendee, range, duration));
          }
        }
        List<TimeRange> contained = new ArrayList<>(containedSet);

        if (contained.size() > 0) {
          for (TimeRange c : contained) {
            // for every range in the contained, count the number of optional attendees that would not be able to make it
            minOverlaps = optionalCounter(bestTimes, events, optional, c, minOverlaps);
          }
          return bestTimes;
        }
        // if there are no events that are contained within the mandatory ranges
        else {
          for (TimeRange range : availableTimesMand) {
            // for every range in the availableTimesMand, count the number of optional attendees that would not be able to make it
            minOverlaps = optionalCounter(bestTimes, events, optional, range, minOverlaps);
          }
          return bestTimes;
        }
      }
      // if there are no mandatory attendees and there are no meeting times for all optional attendees, then there is no meeting time
      else if (mandatory.size() == 0) {
        return possibleTimes;
      }
    }
    // if there are no optional attendees, then find the possible times.
    return intervalFinder(events_list, request);
  }
}
