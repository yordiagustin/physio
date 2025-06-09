-- =========================================================
-- RELATIONAL SCHEMA
-- Exercise System for Physiotherapy
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================================
-- PATIENTS TABLE
-- =========================================================

CREATE TABLE patient
(
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         varchar(255),
    surname      varchar(255),
    phone_number varchar(20),
    created_at   timestamp        DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- EXERCISES TABLE
-- =========================================================

CREATE TABLE exercises (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    target_condition VARCHAR(50) NOT NULL,
    difficulty_level INTEGER CHECK (difficulty_level BETWEEN 1 AND 5),
    estimated_duration_minutes INTEGER,
    instructions TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exercise_phases (
    id SERIAL PRIMARY KEY,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    phase_name VARCHAR(50) NOT NULL,
    phase_order INTEGER NOT NULL,
    instruction_message VARCHAR(200) NOT NULL,
    success_message VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(exercise_id, phase_name),
    UNIQUE(exercise_id, phase_order)
);

CREATE TABLE phase_transitions (
    id SERIAL PRIMARY KEY,
    phase_id INTEGER NOT NULL REFERENCES exercise_phases(id) ON DELETE CASCADE,
    parameter_name VARCHAR(50) NOT NULL,
    operator VARCHAR(10) NOT NULL CHECK (operator IN ('<', '>', '<=', '>=', '==', 'between')),
    value DECIMAL(8,3) NOT NULL,
    value2 DECIMAL(8,3),
    hysteresis DECIMAL(6,3) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE validation_parameters (
    id SERIAL PRIMARY KEY,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    parameter_name VARCHAR(50) NOT NULL,
    parameter_type VARCHAR(20) NOT NULL CHECK (parameter_type IN ('angle', 'time', 'distance', 'velocity')),
    phase_specific VARCHAR(50),
    default_value DECIMAL(8,3) NOT NULL,
    min_value DECIMAL(8,3) NOT NULL,
    max_value DECIMAL(8,3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(exercise_id, parameter_name)
);

CREATE TABLE error_types (
    id SERIAL PRIMARY KEY,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    error_code VARCHAR(50) NOT NULL,
    error_name VARCHAR(100) NOT NULL,
    error_category VARCHAR(30) NOT NULL CHECK (error_category IN ('position', 'time', 'form', 'safety')),
    severity INTEGER NOT NULL CHECK (severity BETWEEN 1 AND 5),
    feedback_message VARCHAR(200) NOT NULL,
    correction_hint TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(exercise_id, error_code)
);

CREATE TABLE validation_rules (
    id SERIAL PRIMARY KEY,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('angle_check', 'time_check', 'position_check', 'symmetry_check')),
    error_code VARCHAR(50) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (exercise_id, error_code) REFERENCES error_types(exercise_id, error_code)
);

CREATE TABLE rule_applicable_phases (
    id SERIAL PRIMARY KEY,
    rule_id INTEGER NOT NULL REFERENCES validation_rules(id) ON DELETE CASCADE,
    phase_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(rule_id, phase_name)
);

CREATE TABLE rule_parameters (
    id SERIAL PRIMARY KEY,
    rule_id INTEGER NOT NULL REFERENCES validation_rules(id) ON DELETE CASCADE,
    parameter_key VARCHAR(50) NOT NULL,
    parameter_value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(rule_id, parameter_key)
);

CREATE TABLE landmark_mappings (
    id SERIAL PRIMARY KEY,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    mapping_type VARCHAR(20) NOT NULL CHECK (mapping_type IN ('primary_joint', 'secondary_joint', 'reference_point')),
    joint_name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(exercise_id, mapping_type, joint_name)
);

CREATE TABLE landmark_indices (
    id SERIAL PRIMARY KEY,
    mapping_id INTEGER NOT NULL REFERENCES landmark_mappings(id) ON DELETE CASCADE,
    landmark_index INTEGER NOT NULL,
    index_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(mapping_id, index_order)
);

-- =========================================================
-- EXERCISE SESSIONS AND REPETITIONS TABLES
-- =========================================================

CREATE TABLE exercise_sessions (
    id SERIAL PRIMARY KEY,
    session_uuid UUID DEFAULT uuid_generate_v4() UNIQUE,
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    session_start TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    session_end TIMESTAMP WITH TIME ZONE,
    total_reps INTEGER DEFAULT 0,
    successful_reps INTEGER DEFAULT 0,
    total_errors INTEGER DEFAULT 0,
    session_score DECIMAL(5,2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exercise_repetitions (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES exercise_sessions(id) ON DELETE CASCADE,
    rep_number INTEGER NOT NULL,
    effective_time_ms INTEGER NOT NULL,
    range_of_motion_degrees DECIMAL(6,2),
    error_count INTEGER DEFAULT 0,
    max_angle_reached DECIMAL(6,2),
    min_angle_reached DECIMAL(6,2),
    rep_start_time TIMESTAMP WITH TIME ZONE,
    rep_end_time TIMESTAMP WITH TIME ZONE,
    quality_score DECIMAL(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(session_id, rep_number)
);

CREATE TABLE repetition_errors (
    id SERIAL PRIMARY KEY,
    repetition_id INTEGER NOT NULL REFERENCES exercise_repetitions(id) ON DELETE CASCADE,
    error_code VARCHAR(50) NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(repetition_id, error_code)
);

-- =========================================================
-- ASSIGNMENTS TABLE
-- =========================================================

CREATE TABLE patient_exercise_assignments (
    id SERIAL PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    assigned_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    target_reps_per_session INTEGER DEFAULT 10,
    target_sessions_per_week INTEGER DEFAULT 3,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(patient_id, exercise_id)
);

-- =========================================================
-- INDEXES FOR OPTIMIZATION
-- =========================================================

-- Patient indexes
CREATE INDEX idx_patient_phone_number ON patient(phone_number);

-- Exercise sessions indexes
CREATE INDEX idx_exercise_sessions_patient ON exercise_sessions(patient_id);
CREATE INDEX idx_exercise_sessions_patient_exercise ON exercise_sessions(patient_id, exercise_id);
CREATE INDEX idx_exercise_sessions_uuid ON exercise_sessions(session_uuid);

-- Exercise repetitions indexes
CREATE INDEX idx_exercise_repetitions_session ON exercise_repetitions(session_id);
CREATE INDEX idx_exercise_repetitions_session_rep ON exercise_repetitions(session_id, rep_number);
CREATE INDEX idx_exercise_repetitions_time ON exercise_repetitions(effective_time_ms);
CREATE INDEX idx_exercise_repetitions_range ON exercise_repetitions(range_of_motion_degrees);

-- Repetition errors indexes
CREATE INDEX idx_repetition_errors_repetition ON repetition_errors(repetition_id);
CREATE INDEX idx_repetition_errors_code ON repetition_errors(error_code);

-- Patient assignments indexes
CREATE INDEX idx_patient_assignments_patient ON patient_exercise_assignments(patient_id);
CREATE INDEX idx_patient_assignments_active ON patient_exercise_assignments(patient_id, is_active);

-- Exercises indexes
CREATE INDEX idx_exercises_condition ON exercises(target_condition);
CREATE INDEX idx_exercise_phases_exercise_order ON exercise_phases(exercise_id, phase_order);
CREATE INDEX idx_validation_rules_exercise_active ON validation_rules(exercise_id, is_active);

-- New relational tables indexes
CREATE INDEX idx_rule_applicable_phases_rule ON rule_applicable_phases(rule_id);
CREATE INDEX idx_rule_parameters_rule ON rule_parameters(rule_id);
CREATE INDEX idx_landmark_indices_mapping ON landmark_indices(mapping_id);
CREATE INDEX idx_landmark_indices_mapping_order ON landmark_indices(mapping_id, index_order);