-- =========================================================
-- CLEAN DATABASE
-- Delete all existing data maintaining structure
-- =========================================================

-- Delete data in correct order (respecting foreign keys)
DELETE FROM repetition_errors;
DELETE FROM exercise_repetitions;
DELETE FROM exercise_sessions;
DELETE FROM patient_exercise_assignments;
DELETE FROM landmark_indices;
DELETE FROM landmark_mappings;
DELETE FROM rule_parameters;
DELETE FROM rule_applicable_phases;
DELETE FROM validation_rules;
DELETE FROM error_types;
DELETE FROM validation_parameters;
DELETE FROM phase_transitions;
DELETE FROM exercise_phases;
DELETE FROM exercises;
DELETE FROM patient;

-- Reset sequences
ALTER SEQUENCE exercises_id_seq RESTART WITH 1;
ALTER SEQUENCE exercise_phases_id_seq RESTART WITH 1;
ALTER SEQUENCE phase_transitions_id_seq RESTART WITH 1;
ALTER SEQUENCE validation_parameters_id_seq RESTART WITH 1;
ALTER SEQUENCE error_types_id_seq RESTART WITH 1;
ALTER SEQUENCE validation_rules_id_seq RESTART WITH 1;
ALTER SEQUENCE rule_applicable_phases_id_seq RESTART WITH 1;
ALTER SEQUENCE rule_parameters_id_seq RESTART WITH 1;
ALTER SEQUENCE landmark_mappings_id_seq RESTART WITH 1;
ALTER SEQUENCE landmark_indices_id_seq RESTART WITH 1;
ALTER SEQUENCE exercise_sessions_id_seq RESTART WITH 1;
ALTER SEQUENCE exercise_repetitions_id_seq RESTART WITH 1;
ALTER SEQUENCE repetition_errors_id_seq RESTART WITH 1;
ALTER SEQUENCE patient_exercise_assignments_id_seq RESTART WITH 1;

-- Verify cleanup
SELECT 'exercises' as tabla, COUNT(*) as registros FROM exercises
UNION ALL SELECT 'exercise_phases', COUNT(*) FROM exercise_phases
UNION ALL SELECT 'patient', COUNT(*) FROM patient
UNION ALL SELECT 'patient_exercise_assignments', COUNT(*) FROM patient_exercise_assignments;