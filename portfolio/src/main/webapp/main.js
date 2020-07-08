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
    console.log(numCommentsShown);
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
        fetch("/data?limit=10").then(loadComments()) ;
    });

    commentElement.appendChild(textElement);
    commentElement.appendChild(deleteButtonElement);
    return commentElement;
}

/** Tells the server to delete the comment. */
function deleteComment(comment) {
    const params = new URLSearchParams();
    params.append('id', comment.id);
    fetch('/delete-data', {method: 'POST', body: params});
}