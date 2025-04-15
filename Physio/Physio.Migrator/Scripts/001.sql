CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE patient
(
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         varchar(255),
    surname      varchar(255),
    phone_number varchar(20),
    created_at   timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plan
(
    id         uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id uuid REFERENCES patient (id),
    diagnosis  varchar(255),
    created_at timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE phase
(
    id                    uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    number                int,
    description           text,
    estimated_duration    int,
    frequency_description varchar(255),
    created_at            timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE phase_assigned
(
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id      uuid REFERENCES plan (id),
    phase_id     uuid REFERENCES phase (id),
    order_number int,
    status       int,
    is_active    boolean,
    created_at   timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exercise
(
    id                 uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name               varchar(255),
    description        text,
    estimated_duration int,
    repetitions        int,
    difficulty         int,
    instructions       text,
    created_at         timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routine
(
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    phase_id       uuid REFERENCES phase_assigned (id),
    exercise_id    uuid REFERENCES exercise (id),
    execution_date date,
    order_number   int,
    created_at     timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE session
(
    id                  uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id          uuid REFERENCES patient (id),
    plan_id             uuid REFERENCES plan (id),
    phase_assigned_id   uuid REFERENCES phase_assigned (id),
    routine_id          uuid REFERENCES routine (id),
    exercise_id         uuid REFERENCES exercise (id),
    start_time          timestamp,
    end_time            timestamp,
    total_time interval,
    correct_repetitions int,
    error_count         int,
    average_accuracy    float,
    created_at          timestamp        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patient_phone_number ON patient (phone_number);
CREATE INDEX idx_plan_patient_id ON plan (patient_id);
CREATE INDEX idx_phase_assigned_plan_id ON phase_assigned (plan_id);
CREATE INDEX idx_routine_phase_id ON routine (phase_id);
CREATE INDEX idx_routine_execution_date ON routine (execution_date);
CREATE INDEX idx_session_patient_id ON session (patient_id);
CREATE INDEX idx_session_plan_id ON session (plan_id);
CREATE INDEX idx_session_phase_assigned_id ON session (phase_assigned_id);
CREATE INDEX idx_session_routine_id ON session (routine_id);
CREATE INDEX idx_session_exercise_id ON session (exercise_id);
