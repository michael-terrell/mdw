CREATE INDEX ACTINST_PROCINST_FK ON ACTIVITY_INSTANCE
(PROCESS_INSTANCE_ID);

CREATE INDEX ATTRIBUTE_OWNERID_IDX ON ATTRIBUTE
(ATTRIBUTE_OWNER_ID);

CREATE INDEX ATTRIBUTE_OWNER_NAME_IDX ON ATTRIBUTE
(ATTRIBUTE_OWNER, ATTRIBUTE_OWNER_ID, ATTRIBUTE_NAME);

CREATE INDEX EVENTLOG_OWNER_IDX ON EVENT_LOG
(EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID);

CREATE INDEX EVENTWAITINST_OWNER_IDX ON EVENT_WAIT_INSTANCE
(EVENT_WAIT_INSTANCE_OWNER, EVENT_WAIT_INSTANCE_OWNER_ID);

CREATE INDEX EVENTWAITINST_EVENT_NAME_IDX ON EVENT_WAIT_INSTANCE
(EVENT_NAME(255));

CREATE INDEX EVENTWAITINST_OWNERID_IDX ON EVENT_WAIT_INSTANCE
(EVENT_WAIT_INSTANCE_OWNER_ID);

CREATE INDEX PROCINST_OWNER_IDX ON PROCESS_INSTANCE
(OWNER, OWNER_ID);

CREATE INDEX TASKINST_OWNER_IDX ON TASK_INSTANCE
(TASK_INSTANCE_OWNER);

CREATE INDEX TASKINST_OWNER_OWNERID_IDX ON TASK_INSTANCE
(TASK_INSTANCE_OWNER, TASK_INSTANCE_OWNER_ID);

CREATE INDEX TASKINST_OWNERID_IDX ON TASK_INSTANCE
(TASK_INSTANCE_OWNER_ID);

CREATE INDEX VARINST_PROCINST_FK ON VARIABLE_INSTANCE
(PROCESS_INST_ID);

CREATE INDEX VARINST_VALUE_IDX ON VARIABLE_INSTANCE
(VARIABLE_VALUE(255));

CREATE INDEX TRANSINST_DESTINST_IDX ON WORK_TRANSITION_INSTANCE
(DEST_INST_ID);
           
CREATE INDEX TRANSINST_PROCINST_FK ON WORK_TRANSITION_INSTANCE
(PROCESS_INST_ID);

CREATE INDEX EVENTINST_DOCUMENT_FK ON EVENT_INSTANCE
(DOCUMENT_ID);

CREATE INDEX DOCCONTENT_DOCUMENT_FK ON DOCUMENT_CONTENT
(DOCUMENT_ID);

CREATE INDEX TASKINSTGRP_GROUP_FK
ON TASK_INST_GRP_MAPP (USER_GROUP_ID);

CREATE INDEX INSTANCEIDX_IDXKEY_FK
ON INSTANCE_INDEX (INDEX_KEY);

CREATE UNIQUE INDEX USERGROUP_GROUPNAME_UK
ON USER_GROUP (GROUP_NAME);

CREATE INDEX DOCUMENT_OWNER_ID_IDX 
ON DOCUMENT (OWNER_ID);

CREATE INDEX DOCUMENT_CREATE_DT_IDX 
ON DOCUMENT (CREATE_DT);

CREATE INDEX ASSET_REF_NAME_IDX 
ON ASSET_REF (NAME);

CREATE INDEX ASSET_REF_ARCHIVE_DT_IDX 
ON ASSET_REF (ARCHIVE_DT);
