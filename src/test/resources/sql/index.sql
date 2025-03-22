-- indexes.sql
-- Indexes for Workflow Management Engine

-- Workflow Definitions Indexes
CREATE INDEX idx_workflow_def_name ON workflow_definitions(name);
CREATE INDEX idx_workflow_def_name_version ON workflow_definitions(name, version);

-- Workflow Instances Indexes
CREATE INDEX idx_workflow_inst_def_id ON workflow_instances(workflow_def_id);
CREATE INDEX idx_workflow_inst_status ON workflow_instances(status);
CREATE INDEX idx_workflow_inst_created ON workflow_instances(created_at);
CREATE INDEX idx_workflow_inst_end_time ON workflow_instances(end_time);

-- Task Group Definitions Indexes
CREATE INDEX idx_task_group_def_workflow ON task_group_definitions(workflow_def_id);
CREATE INDEX idx_task_group_def_parent ON task_group_definitions(parent_group_def_id);
CREATE INDEX idx_task_group_def_order ON task_group_definitions(workflow_def_id, group_order);

-- Task Definitions Indexes
CREATE INDEX idx_task_def_workflow ON task_definitions(workflow_def_id);
CREATE INDEX idx_task_def_group ON task_definitions(task_group_def_id);
CREATE INDEX idx_task_def_order ON task_definitions(workflow_def_id, task_order);
CREATE INDEX idx_task_def_type ON task_definitions(task_type);

-- Task Group Instances Indexes
CREATE INDEX idx_task_group_inst_workflow ON task_group_instances(workflow_instance_id);
CREATE INDEX idx_task_group_inst_def ON task_group_instances(task_group_def_id);
CREATE INDEX idx_task_group_inst_parent ON task_group_instances(parent_group_inst_id);
CREATE INDEX idx_task_group_inst_status ON task_group_instances(status);
CREATE INDEX idx_task_group_inst_end_time ON task_group_instances(end_time);

-- Task Instances Indexes
CREATE INDEX idx_task_inst_workflow ON task_instances(workflow_instance_id);
CREATE INDEX idx_task_inst_def ON task_instances(task_def_id);
CREATE INDEX idx_task_inst_group ON task_instances(task_group_instance_id);
CREATE INDEX idx_task_inst_status ON task_instances(status);
CREATE INDEX idx_task_inst_assignee ON task_instances(assignee);
CREATE INDEX idx_task_inst_due_date ON task_instances(due_date);
CREATE INDEX idx_task_inst_end_time ON task_instances(end_time);

-- Workflow Execution Queue Indexes
CREATE INDEX idx_workflow_queue_workflow ON workflow_execution_queue(workflow_instance_id);
CREATE INDEX idx_workflow_queue_status ON workflow_execution_queue(status);
CREATE INDEX idx_workflow_queue_priority ON workflow_execution_queue(priority DESC, created_at ASC);
CREATE INDEX idx_workflow_queue_created ON workflow_execution_queue(created_at);

-- Workflow History Indexes
CREATE INDEX idx_workflow_history_workflow ON workflow_history(workflow_instance_id);
CREATE INDEX idx_workflow_history_entity ON workflow_history(entity_type, entity_id);
CREATE INDEX idx_workflow_history_change ON workflow_history(change_type);
CREATE INDEX idx_workflow_history_timestamp ON workflow_history(timestamp);
CREATE INDEX idx_workflow_history_username ON workflow_history(username);