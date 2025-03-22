-- schema.sql
-- Schema for Workflow Management Engine

-- Create UUID extension if not exists (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Workflow Definitions
CREATE TABLE workflow_definitions (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      name VARCHAR(255) NOT NULL,
                                      version INTEGER NOT NULL,
                                      definition_json JSONB NOT NULL,
                                      description TEXT,
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      UNIQUE (name, version)
);

-- Workflow Instances
CREATE TABLE workflow_instances (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    workflow_def_id UUID NOT NULL REFERENCES workflow_definitions(id),
                                    status VARCHAR(50) NOT NULL,
                                    input_json JSONB,
                                    output_json JSONB,
                                    start_time TIMESTAMP,
                                    end_time TIMESTAMP,
                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Task Group Definitions
CREATE TABLE task_group_definitions (
                                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        workflow_def_id UUID NOT NULL REFERENCES workflow_definitions(id),
                                        parent_group_def_id UUID REFERENCES task_group_definitions(id),
                                        name VARCHAR(255) NOT NULL,
                                        group_type VARCHAR(50) NOT NULL,
                                        completion_criteria VARCHAR(50) NOT NULL,
                                        group_order INTEGER NOT NULL,
                                        parameters_json JSONB
);

-- Task Definitions
CREATE TABLE task_definitions (
                                  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                  workflow_def_id UUID NOT NULL REFERENCES workflow_definitions(id),
                                  task_group_def_id UUID REFERENCES task_group_definitions(id),
                                  name VARCHAR(255) NOT NULL,
                                  task_type VARCHAR(50) NOT NULL,
                                  task_order INTEGER NOT NULL,
                                  parameters_json JSONB
);

-- Task Group Instances
CREATE TABLE task_group_instances (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
                                      task_group_def_id UUID NOT NULL REFERENCES task_group_definitions(id),
                                      parent_group_inst_id UUID REFERENCES task_group_instances(id),
                                      status VARCHAR(50) NOT NULL,
                                      min_completion INTEGER NOT NULL DEFAULT 1,
                                      parameters_json JSONB,
                                      start_time TIMESTAMP,
                                      end_time TIMESTAMP
);

-- Task Instances
CREATE TABLE task_instances (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
                                task_def_id UUID NOT NULL REFERENCES task_definitions(id),
                                task_group_instance_id UUID REFERENCES task_group_instances(id),
                                assignee VARCHAR(255),
                                status VARCHAR(50) NOT NULL,
                                input_json JSONB,
                                output_json JSONB,
                                start_time TIMESTAMP,
                                end_time TIMESTAMP,
                                due_date TIMESTAMP,
                                failure_reason TEXT
);

-- Workflow Execution Queue
CREATE TABLE workflow_execution_queue (
                                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
                                          status VARCHAR(50) NOT NULL,
                                          priority INTEGER NOT NULL DEFAULT 0,
                                          last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
                                          created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Workflow History
CREATE TABLE workflow_history (
                                  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                  workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
                                  entity_type VARCHAR(50) NOT NULL, -- "WORKFLOW", "TASK_GROUP", "TASK"
                                  entity_id UUID NOT NULL,
                                  change_type VARCHAR(50) NOT NULL, -- "STATUS_CHANGE", "ASSIGNMENT_CHANGE", etc.
                                  details_json JSONB,
                                  timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
                                  username VARCHAR(255)
);