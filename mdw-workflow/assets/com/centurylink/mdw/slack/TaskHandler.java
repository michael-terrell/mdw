/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.slack;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserList;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.UserServices;

public class TaskHandler implements Handler {

    @Override
    public JSONObject handleRequest(String userId, String id, SlackRequest request)
            throws ServiceException {
        Long instanceId;
        try {
            instanceId = Long.parseLong(id);
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid id: " + id);
        }
        
        if (request.getActions() != null) {
            // request is direct action
            String action = request.getActions().get(0); // TODO multiples?
            if (action == null)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Missing action");
            
            String assigneeId = null;
            if (action.equalsIgnoreCase(TaskAction.ASSIGN)) {
                assigneeId = request.getValue();
            }
            else if (action.equalsIgnoreCase(TaskAction.CLAIM)) {
                assigneeId = userId;
            }
            
            String comment = null;
            
            // TODO: TaskActionValidator?
            TaskServices taskServices = ServiceLocator.getTaskServices();
            taskServices.performAction(instanceId, action, userId, assigneeId, comment, null, true);

            JSONObject json = new JSONObject();
            json.put("response_type", "ephemeral");
            json.put("replace_original", false);
            json.put("text", "Okay, action: '" + action + "' performed on task: " + instanceId + ".");
            return json;
        }
        else {
            // options request
            String name = request.getName();
            if ("task_users".equals(name)) {
                User user = UserGroupCache.getUser(userId);
                UserServices userServices = ServiceLocator.getUserServices();
                try {
                   UserList assignees = userServices.findWorkgroupUsers(user.getGroupNames(), request.getValue());
                   JSONObject json = new JSONObject();
                   JSONArray optionsArr = new JSONArray();
                   json.put("options", optionsArr);
                   for (User assignee : assignees.getUsers()) {
                       JSONObject option = new JSONObject();
                       option.put("text", assignee.getName());
                       option.put("value", assignee.getCuid());
                       optionsArr.put(option);
                   }
                   return json;
                }
                catch (DataAccessException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad options request: " + name);
        }
    }
}