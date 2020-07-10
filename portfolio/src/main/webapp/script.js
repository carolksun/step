/** jQuery for handling the menu navigation and fade ins. */
$(document).ready(function () {
  $('.menu-toggler').on('click', function(){
    $('.menu-toggler').toggleClass('open');
    $('.top-nav').toggleClass('open');
  });

  $('.top-nav .nav-link').on('click', function() {
    $('.menu-toggler').removeClass('open');
    $('.top-nav').removeClass('open');
  });
  
  $(`nav a[href*='#']`).on('click', function() {
    $('html, body').animate({
      scrollTop: $($(this).attr('href')).offset().top - 100
    }, 1000);
  });

  $('#up').on('click', function(){
    $('html, body').animate({
      scrollTop: 0
    }, 1000);
  });

  AOS.init({
    easing: 'ease',
    duration: 1800,
    once: true
  });
});

document.addEventListener('DOMContentLoaded', function() {
  loadComments();
});


/**
 * Load a specified number of comments to the comments section. If the specified
 * number is greater than the total number of comments, all comments are shown.
 */
function loadComments() {
  var numCommentsShown = parseInt(document.getElementById('comment-choice').value);

  fetch('/data?limit='.concat(numCommentsShown))
  .then(response => response.json())
  .then((messages) => {
    const messageListElement = document.getElementById('comments-container');
    messageListElement.innerHTML = '';
    messages.forEach((comment) => {
      messageListElement.appendChild(createCommentElement(comment));
    });
  });
}

/**
 * Create comment on the page with a delete button. After a comment is deleted, 
 * the comments are reloaded with the correct number of comments shown.
 * 
 * This function creates HTML element that has the format:
 *
 *    <li class="comment">
 *      <span>COMMENT_TEXT</span>
 *      <button>Delete</button>
 *    </li>
 */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.className = 'comment';

  const textElement = document.createElement('span');
  textElement.innerText = comment.text;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);
    commentElement.remove();
    setTimeout(function() {
      fetch('/data?limit=10').then(loadComments());
    }, 100); 
  });

  commentElement.appendChild(textElement);
  commentElement.appendChild(deleteButtonElement);
  return commentElement;
}

/** Tells the server to delete the comment.*/
function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-data', {method: 'POST', body: params});
}

/** Load charts using callback functions. */
google.charts.load('current', 
  {packages:['corechart'], callback: 
    function() { 
      drawScoreChart();
      drawMinChart();
    }
  });

/**
 * Creates line plot of raw data and corresponding 7-day moving average. To reduce
 * code duplication, the function is called for both the sleep score and the deep
 * sleep minutes charts. The variable "score" is a boolean representing which chart
 * is being plotted. Depending on the truth value of "score", different data is loaded
 * and the corresponding chart elements are filled.
 */
function drawChart(score){
  fetch('/sleep-data').then(response => response.json())
  .then((sleep) => {
    const data = new google.visualization.DataTable();
    data.addColumn('date', 'Day');
    if (score) {
      data.addColumn('number', 'Sleep Score');
    }
    else {
      data.addColumn('number', 'Deep Sleep Minutes');
    }
    data.addColumn('number', 'Moving Average');
    data.addColumn({type:'string', role:'annotation'});
    data.addColumn({type:'string', role:'annotationText'}); 

    /** Hard-coded specific dates to apply annotation text to certain portion of graph. */
    Object.keys(sleep).forEach((day) => {
      if (score) {
        rawScore = sleep[day][0];
        maScore = sleep[day][2];
          if (day == '2020-05-30') {
            data.addRow([
              new Date(day),
              rawScore,
              maScore,
              'Finals',
              'Blame the algorithms final.'
            ]);
          }
          else if (day == '2020-03-14') {
            data.addRow([
              new Date(day),
              rawScore,
              maScore,
              'Return Home',
              `COVID cancelling classes means a good night's sleep.`
            ]);
          }
          else {
            data.addRow([
              new Date(day),
              rawScore,
              maScore,
              undefined,
              undefined
            ]);
          }
      }
      else {
        rawMinutes = sleep[day][1];
        maMinutes = sleep[day][3];
          if (day == '2020-01-12') {
            data.addRow([
              new Date(day),
              rawMinutes,
              maMinutes,
              'Back to School',
              'Goodbye winter break.'
            ]);
          }
          else if (day == '2020-02-15') {
            data.addRow([
              new Date(day),
              rawMinutes,
              maMinutes,
              'Midterms',
              'Tests. Again.'
            ]);
          }
          else {
            data.addRow([
              new Date(day),
              rawMinutes,
              maMinutes,
              undefined,
              undefined
            ]);
        }
      }
    });

    /** Chart styling that applies to both charts. */
    var commonOptions = {
      curveType: 'function',
      width: 600,
      height: 450,
      hAxis: {
        format: 'MM/dd/yy',
        gridlines: {color: 'none'},
        title: 'Date'
      },
      legend: { 
        position: 'bottom' 
      },
      chartArea: {
        'width': '85%', 'height': '70%'
      },
      animation: {
        startup: true,
        duration: 1500,
        easing: 'out'
      }
    };

    if (score) {
      /** Chart styling that applies to only sleep score chart. */
      var scoreOptions = {
        title: 'Sleep Quality',
        colors: ['#CBBAFF', '#4911FF'],
        vAxis: {
          title: 'Sleep Score',
        }
      }
      /** Dictionaries of the options are merged to create overall chart styling. */
      var options = Object.assign({}, commonOptions, scoreOptions);
      const chart = new google.visualization.LineChart(
        document.getElementById('scoreDiv'));
      chart.draw(data, options);
    }
    else {
      /** Chart styling that applies to only deep sleep minutes chart. */
      var minOptions = {
        title: 'Deep Sleep Minutes',
        colors: ['#FFB2FB', '#FF00C4'],
        vAxis: {
          title: 'Minutes',
          gridlines: {count: 15}
        },
      }
      var options = Object.assign({}, commonOptions, minOptions);
      const chart = new google.visualization.LineChart(
        document.getElementById('minDiv'));
      chart.draw(data, options);
    }
  });
}

function drawScoreChart() {
  drawChart(true);
}

function drawMinChart() {
  drawChart(false);
}

var markers = [];
var map;

function createMap() {
  map = new google.maps.Map(document.getElementById('map-div'), {
    zoom: 1.8,
    center: {lat: 30, lng: 0}
  });

  for (var i = 0; i < locations.length; i++) {
    addMarkerWithTimeout(locations[i], i * 200);
  }
}

function addMarkerWithTimeout(position, timeout) {
  window.setTimeout(function() {
    markers.push(new google.maps.Marker({
      position: new google.maps.LatLng(position[1], position[2]),
      map: map,
      animation: google.maps.Animation.DROP,
      title: position[0]
    }));
  }, timeout);
}
