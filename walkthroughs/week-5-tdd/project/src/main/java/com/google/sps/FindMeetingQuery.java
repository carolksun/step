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
   * Finds the possible TimeRanges where the mandatory attendees can attend.
   *
   * @param eventsList sorted list of events in ascending start time order
   * @param request includes mandatory attendees and duration of meeting
   * @return collection of possible TimeRanges the meeting can be scheduled in  
   */
  private Collection<TimeRange> intervalFinder(List<Event> eventsList, MeetingRequest request) {
    Collection<String> mandatory = request.getAttendees();
    long duration = request.getDuration();
    List<TimeRange> possibleTimes = new ArrayList<>();

    /** 
     * If there are conflicting mandatory attendees, then a possible range for 
     * the meeting request starts from the end of the previous event to the 
     * start of the conflicting event. If this time range fits the duration of 
     * the meeting, then it is a possible time.
     */
    int prevEnd = 0;
    for (Event e : eventsList){
      Collection<String> intersection = new HashSet<>(mandatory);
      intersection.retainAll(e.getAttendees());
      TimeRange range = e.getWhen();
      if (!intersection.isEmpty()) {
        TimeRange possibleRange = TimeRange.fromStartEnd(prevEnd, range.start(), false);
        if (duration <= possibleRange.duration()) {
          possibleTimes.add(possibleRange);
        }
        if (prevEnd < range.end()) {
          prevEnd = range.end();
        }
      }
    }
    if (prevEnd != TimeRange.END_OF_DAY + 1) {
      possibleTimes.add(TimeRange.fromStartEnd(prevEnd, TimeRange.END_OF_DAY, true));
    }
    return possibleTimes;
  }

  /**
   * Finds a specific attendee's events for the day.
   *
   * @param eventsList sorted list of events in ascending start time order
   * @param attendee specific attendee to search for
   * @return collection of events that attendee has on the schedule
   */
  private Collection<Event> eventFinderWithAttendee(Collection<Event> eventsList, String attendee) {
    List<Event> eventsByAttendee = new ArrayList<Event>(); 

    for (Event e : eventsList){
      if (e.getAttendees().contains(attendee)){
        eventsByAttendee.add(e);
      }
    }
    return eventsByAttendee;
  }

  /**
   * Split large range into smaller pieces excluding overlapping events. If an 
   * event overlaps with the range, then the range can be broken into the subset
   * of the range that occurs before the first event and additional gaps between
   * events. We take the ranges between the end of the previous event and the 
   * start of the next event, adding a range from the last event's end time to 
   * the end of the range if necessary.
   * Range:  |-------------|      |--------------|      |------------|
   * Event:       |---|       OR    |--|   |--|     OR    |--|   |-------|
   * Return: |----|   |----|      |-|  |---|  |--|      |-|  |---|
   * 
   * @param eventsList sorted list of events in ascending start time order
   * @param range large range to split into smaller ranges
   * @param duration how long the request is for
   * @return collection of TimeRanges that was part of the large range without event overlaps
   */
  private Collection<TimeRange> rangeSplitByNestedEvents(Collection<Event> eventsList, TimeRange range, long duration) {
    List<TimeRange> splitEvents = new ArrayList<TimeRange>(); 
    int prevEndTime = range.start();

    for (Event e : eventsList) {
      if (!range.overlaps(e.getWhen())) {
        continue;
      }
      TimeRange begin = TimeRange.fromStartEnd(prevEndTime, e.getWhen().start(), false);
      if (begin.duration() >= duration){
        splitEvents.add(begin);
      }
      if (prevEndTime < e.getWhen().end()){
        prevEndTime = e.getWhen().end();
      }
    }

    if (prevEndTime < range.end() && range.end() - prevEndTime >= duration) {
      splitEvents.add(TimeRange.fromStartEnd(prevEndTime, range.end(), false));
    }

    return splitEvents;
  }

  /** 
   * Counts the number of optional attendees that can not make it to the meeting at
   * the time range and compares it to the minimum number of optional attendees that
   * can not make it for other time ranges.
   *
   * @param bestTimes collection which has the TimeRanges of meetings that has 
   * the minOverlap attendees 
   * @param events events of the day
   * @param optional optional attendees for the meeting 
   * @param range range in question to see how many optional attendees can not make it 
   * @param minOverlaps current minimum number of optional attendees that can not make
   * it to the meeting at the TimeRange
   * @return the minimum of current range's overlaps and minOverlaps
   */
  private int optionalCounter(Collection<TimeRange> bestTimes,
		  Collection<Event> events, Collection<String> optional,
		  TimeRange range, int minOverlaps) {
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
    if (counterOverlaps == minOverlaps) {
      bestTimes.add(range);
    }
    return minOverlaps;
  }

  /** 
   * Determines the TimeRanges where the mandatory and as many optional
   * attendees as possible can attend the meeting. It finds the time ranges that
   * all the mandatory attendees can attend. It then counts the number of 
   * optional attendees that would not be able to make it to a meeting at the 
   * time range, splitting the time range into smaller ranges if an event overlaps.
   * The TimeRanges that accomodates as many optional attendees as possible are
   * the ranges that have the least event overlaps.
   *
   * @param events events of the day 
   * @param request includes mandatory and optional attendees, and the duration 
   * of the meeting 
   * @return collection of TimeRanges where all mandatory and the greatest 
   * number of optional attendees can be at the meeting
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long duration = request.getDuration();
 
    Collection<String> mandatory = request.getAttendees();
    Collection<String> optional = request.getOptionalAttendees();
    
    // Sort the events to be in ascending order by start time.
    List<Event> eventsList = new ArrayList<Event>(events); 
    Collections.sort(eventsList, Event.ORDER_BY_START_ASCENDING); 
    List<TimeRange> possibleTimes = new ArrayList<>();

    if (duration > TimeRange.WHOLE_DAY.duration() || duration < 0) {
    /**
     * If the duration of meeting is longer than a day or less than 0 minutes,
     * then there are no meeting times. 
     */
      return possibleTimes;
    }
    
    if (events.isEmpty()) {
      // If there are no events for the day, then the meeting can be any time.
      possibleTimes.add(TimeRange.WHOLE_DAY);
      return possibleTimes;
    }

    if (optional.isEmpty()){
      // If there are no optional attendees, then find the possible times. 
      return intervalFinder(eventsList, request);
    }

    List<String> allAttendees = new ArrayList<>(mandatory);
    allAttendees.addAll(optional);

    // Create new meeting request with mandatory and optional attendees as mandatory.
    MeetingRequest newRequest = new MeetingRequest(allAttendees, duration);

    // Find possible time ranges with all mandatory and all optional attendees.
    Collection<TimeRange> slotsOptional = intervalFinder(eventsList, newRequest);
    
    if (!slotsOptional.isEmpty()) {
      // If there is a time with all mandatory and optional attendees, then return
      return slotsOptional;
    }

    // Find time ranges for just mandatory attendees.
    Collection<TimeRange> availableTimesMand = intervalFinder(eventsList, request);

   /** 
    * If there is no time where everyone can make it, find the times for just 
    * the mandatory attendees. If an event with an optional attendee is 
    * overlapping with a range that all mandatory attendees are available, then 
    * split the mandatory range into smaller ranges that exclude the overlapping 
    * optional event.
    */

    Set<TimeRange> containedSet = new HashSet<>();
    for (String opAttendee : optional) {
      Collection<Event> eventsWithAttendee = eventFinderWithAttendee(eventsList, opAttendee);
      for (TimeRange range : availableTimesMand) {
        containedSet.addAll(rangeSplitByNestedEvents(eventsWithAttendee, range, duration));
      }
    }
    List<TimeRange> contained = new ArrayList<>(containedSet);

    // Store the times with the minimum amount of event overlaps.
    List<TimeRange> bestTimes = new ArrayList<>();
    int minOverlaps = Integer.MAX_VALUE;

    if (!contained.isEmpty()) {
       /**
        * If there are events that are contained within the mandatory ranges,
        * for every range in contained, count the number of optional attendees 
        * that would not be able to make it. Return the ranges
        * with the least amount of overlaps with the optional attendees.
        */
      for (TimeRange c : contained) {
        minOverlaps = optionalCounter(bestTimes, events, optional, c, minOverlaps);
      }
      return bestTimes;
    }

     /**
      * If there are no events that are contained within the mandatory ranges,
      * for every range in the availableTimesMand, count the number of 
      * optional attendees that would not be able to make it. Return the ranges
      * with the least amount of overlaps with the optional attendees.
      */
    for (TimeRange range : availableTimesMand) {
      minOverlaps = optionalCounter(bestTimes, events, optional, range, minOverlaps);
    }
    return bestTimes;
  }
}
