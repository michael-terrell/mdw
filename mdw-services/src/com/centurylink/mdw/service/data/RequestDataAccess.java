/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

public class RequestDataAccess extends CommonDataAccess {

    public RequestDataAccess() {
        super(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    public RequestList getMasterRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getMasterRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from process_instance pi, document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            List<Request> requests = new ArrayList<Request>();
            RequestList requestList = new RequestList(RequestList.MASTER_REQUESTS, requests);

            //If the total count is 0, stop further execution to prevent it from erroring out while running to get the Responses query.
            if(total == 0){
              return requestList;
            }

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select ").append(PROC_INST_COLS).append(", d.document_id, d.create_dt, d.owner_type, d.status_code, d.status_message\n");
            q.append("from process_instance pi, document d\n");

            q.append(where).append(buildOrderBy(query));
            if (query.getMax() != Query.MAX_ALL)
                q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Long> listenerRequestIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                ProcessInstance pi = buildProcessInstance(rs);
                Request request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                request.setStatusCode(rs.getInt("d.status_code"));
                request.setStatusMessage(rs.getString("d.status_message"));
                request.setMasterRequestId(pi.getMasterRequestId());
                request.setProcessInstanceId(pi.getId());
                request.setProcessId(pi.getProcessId());
                request.setProcessName(pi.getProcessName());
                request.setProcessVersion(pi.getProcessVersion());
                request.setPackageName(pi.getPackageName());
                request.setProcessStatus(pi.getStatus());
                request.setProcessStart(rs.getTimestamp("pi.start_dt"));
                request.setProcessEnd(rs.getTimestamp("pi.end_dt"));
                requests.add(request);
                requestMap.put(request.getId(), request);
                if (OwnerType.LISTENER_REQUEST.equals(rs.getString("d.owner_type")))
                    listenerRequestIds.add(request.getId());
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.LISTENER_RESPONSE, listenerRequestIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve master requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getMasterRequestsWhere(Query query) throws DataAccessException {
        StringBuilder clause = new StringBuilder();
        clause.append("where pi.owner_id = d.document_id\n");
        clause.append("and pi.owner = 'DOCUMENT'\n");

        String find = query.getFind();
        String masterRequestId = query.getFilter("masterRequestId");
        if (find != null) {
            // ignore other criteria
            clause.append(" and pi.master_request_id like '" + find + "%'\n");
        }
        else if (masterRequestId != null) {
            // ignore other criteria
            clause.append(" and pi.master_request_id = '" + masterRequestId + "'\n");
        }
        else {
            // status
            String status = query.getFilter("status");
            if (status != null) {
                if (status.equals(WorkStatus.STATUSNAME_ACTIVE)) {
                    clause.append(" and pi.status_cd not in (")
                      .append(WorkStatus.STATUS_COMPLETED)
                      .append(",").append(WorkStatus.STATUS_FAILED)
                      .append(",").append(WorkStatus.STATUS_CANCELLED)
                      .append(",").append(WorkStatus.STATUS_PURGE)
                      .append(")\n");
                }
                else {
                    clause.append(" and pi.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
                }
            }

            // receivedDate
            try {
                Date receivedDate = query.getDateFilter("receivedDate");
                if (receivedDate != null) {
                    String formatedReceivedDate = getDateFormat().format(receivedDate);
                    clause.append(" and d.create_dt >= STR_TO_DATE('").append(formatedReceivedDate).append("','%d-%M-%Y')\n");
                }
            }
            catch (ParseException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }

            // TODO: respond date
        }

        return clause.toString();
    }

    public Request getMasterRequest(String masterRequestId, boolean withContent, boolean withResponseContent)
    throws DataAccessException {
        StringBuilder query = new StringBuilder();
        query.append("select document_id, process_instance_id from process_instance pi, document d\n");
        query.append("where pi.owner_id = d.document_id\n");
        // query.append("and pi.owner = 'DOCUMENT'\n");  (eg: 'TESTER')
        query.append("and pi.master_request_id = ?");
        Long requestId = null;
        Long processInstanceId = null;
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query.toString(), masterRequestId);
            if (rs.next()) {
                requestId = rs.getLong("document_id");
                processInstanceId = rs.getLong("process_instance_id");
            }
            else
                return null;
        }
        catch (SQLException ex) {
            throw new DataAccessException("Error retrieving masterRequestId: " + masterRequestId, ex);
        }
        finally {
            db.closeConnection();
        }
        Request request = getRequest(requestId, withContent, withResponseContent);
        request.setMasterRequestId(masterRequestId);
        request.setProcessInstanceId(processInstanceId);
        return request;
    }

    public Request getRequest(Long id, boolean withContent, boolean withResponseContent)
    throws DataAccessException {
        try {
            String query = "select create_dt, owner_type, owner_id, status_code, status_message";
            query += " from document where document_id = ?";
            db.openConnection();
            ResultSet rs = db.runSelect(query, id);
            Request request = null;
            String ownerType = null;
            Long ownerId = null;
            if (rs.next()) {
                request = new Request(id);
                request.setCreated(rs.getTimestamp("create_dt"));
                request.setStatusCode(rs.getInt("status_code"));
                request.setStatusMessage(rs.getString("status_message"));
                ownerType = rs.getString("owner_type");
                ownerId = rs.getLong("owner_id");
                if (withContent) {
                    // TODO mongo
                    query = "select content from document_content where document_id = ?";
                    rs = db.runSelect(query, id);
                    if (rs.next())
                        request.setContent(rs.getString("content"));
                }
            }
            else {
                return null;
            }

            ResultSet responseRs = null;
            String responseQuery = "select document_id, create_dt";
            if (OwnerType.ADAPTER_REQUEST.equals(ownerType) && ownerId != null) {
                request.setOutbound(true);
                responseQuery += " from document where owner_type='" + OwnerType.ADAPTER_RESPONSE + "' and owner_id = ?";
                responseRs = db.runSelect(responseQuery, ownerId);
            }
            else if (OwnerType.LISTENER_REQUEST.equals(ownerType)) {
                responseQuery += " from document where owner_type='" + OwnerType.LISTENER_RESPONSE + "' and owner_id = ?";
                responseRs = db.runSelect(responseQuery, ownerId);
            }
            if (responseRs != null && responseRs.next()) {
                request.setResponseId(responseRs.getLong("document_id"));
                request.setResponded(responseRs.getTimestamp("create_dt"));
                if (withResponseContent) {
                    // TODO mongo
                    query = "select content from document_content where document_id = ?";
                    responseRs = db.runSelect(query, request.getResponseId());
                    if (responseRs.next())
                        request.setResponseContent(responseRs.getString("content"));
                }
            }

            return request;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve requestId: ", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public RequestList getInboundRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getInboundRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select d.document_id, d.create_dt, d.status_code, d.status_message\n");
            q.append("from document d\n");
            q.append(where).append(buildOrderBy(query));
            q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Request> requests = new ArrayList<Request>();
            List<Long> requestIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                Request request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                request.setStatusCode(rs.getInt("d.status_code"));
                request.setStatusMessage(rs.getString("d.status_message"));
                requestMap.put(request.getId(), request);
                requests.add(request);
                requestIds.add(request.getId());
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL && !requestIds.isEmpty()) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.LISTENER_RESPONSE, requestIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            RequestList requestList = new RequestList(RequestList.INBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve inbound requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getInboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.LISTENER_REQUEST + "'\n");

        String find = query.getFind();
        Long id = query.getLongFilter("id");
        if (find != null) {
            clause.append(" and d.document_id like '" + find + "%'\n");
        }
        else if (id != null && id > 0) {
            clause.append(" and d.document_id = " + id + "\n");
        }
        else if (query.getFilter("ownerId") != null) {
            clause.append(" and d.owner_id = " + query.getLongFilter("ownerId") + "\n");
        }

        return clause.toString();
    }

    public RequestList getOutboundRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getOutboundRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select d.document_id, d.create_dt, d.owner_id, d.status_code, d.status_message\n");
            q.append("from document d\n");
            q.append(where).append(buildOrderBy(query));
            q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Request> requests = new ArrayList<Request>();
            List<Long> activityIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                Long activityId = rs.getLong("owner_id");
                Request request = new Request(rs.getLong("document_id"));
                request.setCreated(rs.getTimestamp("create_dt"));
                request.setStatusCode(rs.getInt("status_code"));
                request.setStatusMessage(rs.getString("status_message"));
                request.setOutbound(true);
                requestMap.put(activityId, request);
                requests.add(request);
                activityIds.add(activityId);
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL && !activityIds.isEmpty()) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.ADAPTER_RESPONSE, activityIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            RequestList requestList = new RequestList(RequestList.OUTBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve outbound requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getOutboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.ADAPTER_REQUEST + "'\n");

        String find = query.getFind();
        Long id = query.getLongFilter("id");
        if (find != null) {
            clause.append(" and d.document_id like '" + find + "%'\n");
        }
        else if (id != null && id > 0) {
            clause.append(" and d.document_id = " + id + "\n");
        }
        else if (query.getFilter("ownerId") != null) {
            clause.append(" and d.owner_id = " + query.getLongFilter("ownerId") + "\n");
        }
        else {
            Long[] ownerIds = query.getLongArrayFilter("ownerIds");
            if (ownerIds != null) {
                clause.append(" and d.owner_id in (");
                for (int i = 0; i < ownerIds.length; i++) {
                    clause.append(ownerIds[i]);
                    if (i < ownerIds.length - 1)
                        clause.append(", ");
                }
                clause.append(")\n");
            }
        }

        return clause.toString();
    }

    private String getResponsesQuery(String type, List<Long> ids) {
        StringBuilder resp = new StringBuilder("select document_id, owner_id, create_dt from document\n");
        resp.append("where owner_type = '" + type + "'\n");
        resp.append("and owner_id in (");
        int i = 0;
        for (Long id : ids) {
            resp.append(id);
            if (i < ids.size() - 1)
                resp.append(",");
            i++;
        }
        resp.append(")\n");
        return resp.toString();
    }

    private String buildOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by d.document_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
    }
}