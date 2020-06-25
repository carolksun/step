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

/**
 * Adds a random greeting to the page.
 */
function addRandomFact() {
  // Pick a random fact.
  const facts =
      ["I've lived in LA my whole life", 
       "I have surpassed level 3000 on Candy Crush Saga", 
       "I have an older sister", 
       "My favorite fruit is watermelon"];


  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

function loadComments() {
  fetch('/data').then(response => response.json()).then((messages) => {
    const messageListElement = document.getElementById('comments-container');
    messageListElement.innerHTML = "";
    var numCommentsShown = document.getElementById('comment-choice').value; 
    var numComments = messages.length;
    console.log(numComments);
    if (numComments < numCommentsShown){
        numCommentsShown = numComments;
    }
    var i;
    for (i = 0; i < numCommentsShown; i++) {
        messageListElement.appendChild(createCommentElement(messages[i]));
    }
  });
}

function createCommentElement(comment) {
  const coElement = document.createElement('li');
  coElement.className = 'comment';

  const textElement = document.createElement('span');
  textElement.innerText = comment.text;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

    // Remove the task from the DOM.
    coElement.remove();
    loadComments();
  });

  coElement.appendChild(textElement);
  coElement.appendChild(deleteButtonElement);
  return coElement;
}

/** Tells the server to delete the task. */
function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-data', {method: 'POST', body: params});
}



