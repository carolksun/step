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
import com.google.sps.data.Comment;
import com.google.sps.data.Constants;
import com.google.gson.Gson;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


/** Servlet that handles comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

    /** 
     * Retrieves the (limit number) of most recent comments from Datastore
     * and produces a JSON response containing these comments
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int limit = Integer.parseInt(request.getParameter(Constants.LIMIT_PROPERTY));
        Query query = new Query(Constants.COMMENT_KIND).addSort(Constants.TIMESTAMP_PROPERTY, SortDirection.DESCENDING);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery results = datastore.prepare(query);

        List<Comment> comments = new ArrayList<>();
        int i = 0;
        for (Entity entity : results.asIterable()) {
            if (i == limit) {
                break;
            } 
            i++;
            long id = entity.getKey().getId();
            String text = (String)entity.getProperty(Constants.TEXT_PROPERTY);
            long timestamp = (long)entity.getProperty(Constants.TIMESTAMP_PROPERTY);

            Comment comment = new Comment(id, text, timestamp);
            comments.add(comment);
        }

        Gson gson = new Gson();

        response.setContentType("application/json");
        response.getWriter().println(gson.toJson(comments));
    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String text = request.getParameter(Constants.TEXT_PROPERTY);
        long timestamp = System.currentTimeMillis();

        Entity commentEntity = new Entity(Constants.COMMENT_KIND);
        commentEntity.setProperty(Constants.TEXT_PROPERTY, text);
        commentEntity.setProperty(Constants.TIMESTAMP_PROPERTY, timestamp);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(commentEntity);

        response.sendRedirect("/index.html");
    }
}

