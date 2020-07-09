$(document).ready(function () {
    $('.menu-toggler').on('click', function(){
        $('.menu-toggler').toggleClass('open');
        $('.top-nav').toggleClass('open');
    });

    $('.top-nav .nav-link').on('click', function() {
        $('.menu-toggler').removeClass('open');
        $('.top-nav').removeClass('open');
    });
    
    $('nav a[href*="#"]').on('click', function() {
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
        once:true
    });
});

document.addEventListener("DOMContentLoaded", function() {
  loadComments();
});

/**
 * Load a specified number of comments to the comments section. If the specified
 * number is greater than the total number of comments, all comments are shown.
 */
function loadComments() {
    var numCommentsShown = parseInt(document.getElementById('comment-choice').value);

    fetch("/data?limit=".concat(numCommentsShown))
    .then(response => response.json())
    .then((messages) => {
        const messageListElement = document.getElementById('comments-container');
        messageListElement.innerHTML = "";
        messages.forEach((comment) => {
            messageListElement.appendChild(createCommentElement(comment));
        });
    });
}

/**
 * Create comment on the page with a delete button. After a comment is deleted, 
 * the comments are reloaded with the correct number of comments shown.
 * 
 * This functions creates HTML element that has the format:
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
        setTimeout(function(){
          fetch("/data?limit=10").then(loadComments()) ;
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

google.charts.load('current', 
    {packages:['corechart'], callback: 
        function() { 
            drawScoreChart();
            drawMinChart();
        }
    });

/** Fetches sleep data and uses it to create a chart. */
function drawScoreChart() {
  fetch('/sleep-data').then(response => response.json())
  .then((sleep) => {
    const data = new google.visualization.DataTable();
    data.addColumn('date', 'Day');
    data.addColumn('number', 'Sleep Score');
    data.addColumn('number', 'Moving Average')
    data.addColumn({type:'string', role:'annotation'});
    data.addColumn({type:'string', role:'annotationText'});  
    Object.keys(sleep).forEach((day) => {
        if (day == "2020-05-30"){
            data.addRow([new Date(day), sleep[day][0], sleep[day][2], "Finals", "Blame the algorithms final."]);
        }
        else if (day == "2020-03-14"){
            data.addRow([new Date(day), sleep[day][0], sleep[day][2], "Return Home", "COVID cancelling classes means a good night's sleep."]);
        }
      else{
          data.addRow([new Date(day), sleep[day][0], sleep[day][2], undefined, undefined]);
      }
    });

    var options = {
        title: 'Sleep Quality',
        curveType: 'function',
        width: 600,
        height: 450,
        colors: ['#CBBAFF', '#4911FF'],
        vAxis: {
            title: "Sleep Score",
        },
        hAxis: {
            format: 'MM/dd/yy',
            gridlines: {color: 'none'},
            title: "Date"
        },
        legend: { position: 'bottom' },
        chartArea: {'width': '85%', 'height': '70%'},
        animation:{
            startup: true,
            duration: 1500,
            easing: 'out'
        }
    };
    const chart = new google.visualization.LineChart(
        document.getElementById('score_div'));
    chart.draw(data, options);
  });
}

function drawMinChart() {
  fetch('/sleep-data').then(response => response.json())
  .then((sleep) => {
    const data = new google.visualization.DataTable();
    data.addColumn('date', 'Day');
    data.addColumn('number', 'Deep Sleep Minutes');
    data.addColumn('number', 'Moving Average');
    data.addColumn({type:'string', role:'annotation'});
    data.addColumn({type:'string', role:'annotationText'});  
    Object.keys(sleep).forEach((day) => {
        if (day == "2020-01-12"){
            data.addRow([new Date(day), sleep[day][1], sleep[day][3], "Back to School", "Goodbye winter break."]);
        }
        else if (day == "2020-02-15"){
            data.addRow([new Date(day), sleep[day][1], sleep[day][3], "Midterms", "Tests. Again."]);
        }
        else{
          data.addRow([new Date(day), sleep[day][1], sleep[day][3], undefined, undefined]);
      }
    });

    var options = {
        title: 'Deep Sleep Minutes',
        curveType: 'function',
        width: 600,
        height: 450,
        colors: ['#FFB2FB', '#FF00C4'],
        vAxis: {
            title: "Minutes",
            gridlines: {count: 15}
        },
        hAxis: {
            format: 'MM/dd/yy',
            gridlines: {color: 'none'},
            title: "Date"
        },
        legend: { position: 'bottom' },
        chartArea: {'width': '80%', 'height': '70%'},
        animation:{
            startup: true,
            duration: 1500,
            easing: 'out'
        }
    };

    const chart = new google.visualization.LineChart(
        document.getElementById('min_div'));
    chart.draw(data, options);
  });
}