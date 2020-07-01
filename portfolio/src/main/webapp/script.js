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

/** Displays a random fact to the page. */
function addRandomFact() {
    // List of facts to choose from.
    const facts = [
        "I've lived in LA my whole life", 
        "I have surpassed level 3000 on Candy Crush Saga", 
        "I have an older sister", 
        "My favorite fruit is watermelon"
    ];

    const fact = facts[Math.floor(Math.random() * facts.length)];

    // Add it to the page.
    const factContainer = document.getElementById('fact-container');
    factContainer.innerText = fact;
}

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
        fetch("/data?limit=0").then(loadComments()) ;
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
