-- =========================================================
-- SEEDER SQL RELATIONAL
-- Exercises, validation and mock patient
-- =========================================================

-- =========================================================
-- EXERCISE 1: SITTING TRUNK FLEXION
-- =========================================================

-- Base exercise
INSERT INTO exercises (id, name, description, target_condition, difficulty_level, estimated_duration_minutes, instructions) VALUES
(1, 'Flexión de Tronco Sentado', 
 'Ejercicio de movilidad para flexionar el tronco hacia las rodillas desde posición sentada. Mejora la flexibilidad de la columna lumbar.',
 'occupational_lumbar_pain', 2, 10, 
 'Siéntate en una silla con los pies apoyados en el suelo. Mantén la espalda recta al inicio. Flexiona lentamente el tronco hacia las rodillas, mantén la posición y regresa de forma controlada. Realiza el movimiento de forma suave y sin forzar.');

-- Exercise phases
INSERT INTO exercise_phases (exercise_id, phase_name, phase_order, instruction_message, success_message) VALUES
(1, 'STARTING', 1, 'Ponte en posición inicial', 'Posición correcta'),
(1, 'DESCENDING', 2, 'Flexiona el tronco lentamente hacia la rodilla', 'Buen descenso controlado'),
(1, 'BOTTOM_POSITION', 3, 'Mantén esta posición', 'Posición mantenida correctamente'),
(1, 'ASCENDING', 4, 'Levanta el tronco lentamente', 'Buen ascenso controlado'),
(1, 'COMPLETED_REP', 5, '¡Repetición completada!', 'Excelente ejecución');

-- Phase transitions
INSERT INTO phase_transitions (phase_id, parameter_name, operator, value, hysteresis) VALUES
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'STARTING'), 'trunk_angle', '<', 160.0, 0),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'DESCENDING'), 'trunk_angle', '<=', 90.0, 0),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'BOTTOM_POSITION'), 'trunk_angle', '>', 100.0, 10.0),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'ASCENDING'), 'trunk_angle', '>=', 150.0, 10.0);

-- Validation parameters
INSERT INTO validation_parameters (exercise_id, parameter_name, parameter_type, phase_specific, default_value, min_value, max_value, unit, description) VALUES
-- Angles parameters
(1, 'trunk_angle_start', 'angle', NULL, 160.0, 140.0, 180.0, 'degrees', 'Ángulo inicial del tronco en posición erguida'),
(1, 'trunk_angle_end', 'angle', NULL, 90.0, 60.0, 120.0, 'degrees', 'Ángulo objetivo en flexión máxima'),
(1, 'knee_angle_threshold', 'angle', NULL, 70.0, 60.0, 90.0, 'degrees', 'Ángulo mínimo de rodilla en posición baja'),
(1, 'shoulder_symmetry_tolerance', 'distance', NULL, 0.05, 0.03, 0.08, 'normalized', 'Tolerancia máxima para asimetría de hombros'),

-- Time parameters by phase
(1, 'descending_time', 'time', 'DESCENDING', 1000.0, 500.0, 3000.0, 'milliseconds', 'Tiempo para la fase de descenso'),
(1, 'bottom_hold_time', 'time', 'BOTTOM_POSITION', 2000.0, 1000.0, 5000.0, 'milliseconds', 'Tiempo mínimo para mantener posición baja'),
(1, 'ascending_time', 'time', 'ASCENDING', 1000.0, 500.0, 3000.0, 'milliseconds', 'Tiempo para la fase de ascenso');

-- Error types
INSERT INTO error_types (exercise_id, error_code, error_name, error_category, severity, feedback_message, correction_hint) VALUES
-- Position errors
(1, 'KNEE_TOO_HIGH', 'Rodilla muy alta', 'position', 3, 'Baja más la rodilla hacia el suelo', 'La rodilla debe formar un ángulo menor a 70°'),
(1, 'BACK_NOT_STRAIGHT', 'Espalda no recta', 'form', 2, 'Mantén la espalda recta', 'Los hombros deben estar alineados'),
(1, 'INCOMPLETE_RANGE', 'Rango incompleto', 'position', 4, 'Flexiona más el tronco', 'Intenta llegar más cerca de las rodillas'),

-- Time errors
(1, 'DESCENDING_TOO_FAST', 'Descenso muy rápido', 'time', 2, 'Baja más lentamente', 'El descenso debe durar al menos 500ms'),
(1, 'DESCENDING_TOO_SLOW', 'Descenso muy lento', 'time', 1, 'Puedes bajar un poco más rápido', 'No excedas 3 segundos'),
(1, 'NOT_HOLDING_ENOUGH', 'No mantiene posición', 'time', 3, 'Mantén la posición más tiempo', 'Sostén la flexión al menos 2 segundos'),
(1, 'HOLDING_TOO_LONG', 'Mantiene posición demasiado', 'time', 1, 'Ya puedes continuar subiendo', 'No mantengas más de 5 segundos'),
(1, 'ASCENDING_TOO_FAST', 'Ascenso muy rápido', 'time', 2, 'Sube más lentamente', 'El ascenso debe ser controlado'),
(1, 'ASCENDING_TOO_SLOW', 'Ascenso muy lento', 'time', 1, 'Puedes subir un poco más rápido', 'No excedas 3 segundos');

-- Validation rules
INSERT INTO validation_rules (exercise_id, rule_type, error_code, priority) VALUES
-- Position validation rules
(1, 'angle_check', 'KNEE_TOO_HIGH', 1),
(1, 'angle_check', 'INCOMPLETE_RANGE', 2),
(1, 'symmetry_check', 'BACK_NOT_STRAIGHT', 3),

-- Time validation rules by phase
(1, 'time_check', 'DESCENDING_TOO_FAST', 4),
(1, 'time_check', 'DESCENDING_TOO_SLOW', 5),
(1, 'time_check', 'NOT_HOLDING_ENOUGH', 6),
(1, 'time_check', 'HOLDING_TOO_LONG', 7),
(1, 'time_check', 'ASCENDING_TOO_FAST', 8),
(1, 'time_check', 'ASCENDING_TOO_SLOW', 9);

-- Rule applicable phases
INSERT INTO rule_applicable_phases (rule_id, phase_name) VALUES
-- Rule 1: KNEE_TOO_HIGH applies in BOTTOM_POSITION
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'KNEE_TOO_HIGH'), 'BOTTOM_POSITION'),
-- Rule 2: INCOMPLETE_RANGE applies in BOTTOM_POSITION
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'INCOMPLETE_RANGE'), 'BOTTOM_POSITION'),
-- Rule 3: BACK_NOT_STRAIGHT applies in multiple phases
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'BACK_NOT_STRAIGHT'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'BACK_NOT_STRAIGHT'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'BACK_NOT_STRAIGHT'), 'ASCENDING'),
-- Time validation rules by phase
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_FAST'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_SLOW'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'NOT_HOLDING_ENOUGH'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'HOLDING_TOO_LONG'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_FAST'), 'ASCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_SLOW'), 'ASCENDING');

-- Rule parameters
INSERT INTO rule_parameters (rule_id, parameter_key, parameter_value) VALUES
-- Rule 1: KNEE_TOO_HIGH
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'KNEE_TOO_HIGH'), 'parameter', 'knee_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'KNEE_TOO_HIGH'), 'min_value', '70'),

-- Rule 2: INCOMPLETE_RANGE  
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'INCOMPLETE_RANGE'), 'parameter', 'trunk_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'INCOMPLETE_RANGE'), 'max_value', '110'),

-- Rule 3: BACK_NOT_STRAIGHT
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'BACK_NOT_STRAIGHT'), 'parameter', 'shoulders_symmetry'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'BACK_NOT_STRAIGHT'), 'max_asymmetry', '0.05'),

-- Time validation rules
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_FAST'), 'min_time_ms', '500'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_FAST'), 'phase', 'DESCENDING'),

((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_SLOW'), 'max_time_ms', '3000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'DESCENDING_TOO_SLOW'), 'phase', 'DESCENDING'),

((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'NOT_HOLDING_ENOUGH'), 'min_time_ms', '2000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'NOT_HOLDING_ENOUGH'), 'phase', 'BOTTOM_POSITION'),

((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'HOLDING_TOO_LONG'), 'max_time_ms', '5000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'HOLDING_TOO_LONG'), 'phase', 'BOTTOM_POSITION'),

((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_FAST'), 'min_time_ms', '500'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_FAST'), 'phase', 'ASCENDING'),

((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_SLOW'), 'max_time_ms', '3000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'ASCENDING_TOO_SLOW'), 'phase', 'ASCENDING');

-- Landmark mappings
INSERT INTO landmark_mappings (exercise_id, mapping_type, joint_name, description) VALUES
(1, 'primary_joint', 'trunk', 'Ángulo hombro-cadera-rodilla para flexión de tronco'),
(1, 'secondary_joint', 'knee', 'Ángulo cadera-rodilla-tobillo'),
(1, 'reference_point', 'shoulders', 'Puntos de referencia para simetría de hombros');

-- Landmark indices
INSERT INTO landmark_indices (mapping_id, landmark_index, index_order) VALUES
-- Trunk mapping: landmarks [11, 23, 25]
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'trunk'), 11, 0), -- hombro
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'trunk'), 23, 1), -- cadera
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'trunk'), 25, 2), -- rodilla

-- Knee mapping: landmarks [23, 25, 27]
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'knee'), 23, 0), -- cadera
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'knee'), 25, 1), -- rodilla
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'knee'), 27, 2), -- tobillo

-- Shoulders mapping: landmarks [11, 12]
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'shoulders'), 11, 0), -- hombro izquierdo
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'shoulders'), 12, 1); -- hombro derecho

-- =========================================================
-- EXERCISE 2: LUMBAR EXTENSION IN PRONE POSITION
-- =========================================================

-- Base exercise
INSERT INTO exercises (id, name, description, target_condition, difficulty_level, estimated_duration_minutes, instructions) VALUES
(2, 'Extensión Lumbar en Decúbito Prono', 
 'Ejercicio de fortalecimiento donde el paciente se acuesta boca abajo y levanta el pecho para extender la columna lumbar.',
 'occupational_lumbar_pain', 3, 8,
 'Acuéstate boca abajo en una superficie firme. Coloca las manos bajo los hombros. Levanta lentamente el pecho usando los músculos de la espalda, no las manos. Mantén el cuello en posición neutral. Sostén la posición y baja de forma controlada.');

-- Exercise phases
INSERT INTO exercise_phases (exercise_id, phase_name, phase_order, instruction_message, success_message) VALUES
(2, 'PRONE_POSITION', 1, 'Acuéstate boca abajo', 'Posición inicial correcta'),
(2, 'LIFT_CHEST', 2, 'Levanta el pecho lentamente', 'Buen levantamiento'),
(2, 'HOLD_EXTENSION', 3, 'Mantén la extensión', 'Manteniendo posición'),
(2, 'LOWER_CHEST', 4, 'Baja lentamente a la posición inicial', 'Buen descenso'),
(2, 'COMPLETED_REP', 5, '¡Repetición completada!', 'Excelente extensión lumbar');

-- Phase transitions
INSERT INTO phase_transitions (phase_id, parameter_name, operator, value, hysteresis) VALUES
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'PRONE_POSITION'), 'chest_lift_angle', '>', 5.0, 0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'LIFT_CHEST'), 'chest_lift_angle', '>=', 25.0, 0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'HOLD_EXTENSION'), 'chest_lift_angle', '<', 20.0, 5.0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'LOWER_CHEST'), 'chest_lift_angle', '<=', 5.0, 0);

-- Validation parameters
INSERT INTO validation_parameters (exercise_id, parameter_name, parameter_type, phase_specific, default_value, min_value, max_value, unit, description) VALUES
-- Angles parameters
(2, 'chest_lift_angle', 'angle', NULL, 25.0, 15.0, 35.0, 'degrees', 'Ángulo de elevación del pecho'),
(2, 'max_extension_angle', 'angle', NULL, 35.0, 30.0, 45.0, 'degrees', 'Ángulo máximo seguro de extensión'),
(2, 'neck_alignment_tolerance', 'distance', NULL, 0.08, 0.05, 0.12, 'normalized', 'Tolerancia para alineación del cuello'),

-- Time parameters
(2, 'lift_time', 'time', 'LIFT_CHEST', 1200.0, 800.0, 2500.0, 'milliseconds', 'Tiempo para levantar el pecho'),
(2, 'hold_time', 'time', 'HOLD_EXTENSION', 2500.0, 2000.0, 4000.0, 'milliseconds', 'Tiempo de mantenimiento en extensión'),
(2, 'lower_time', 'time', 'LOWER_CHEST', 1200.0, 800.0, 2500.0, 'milliseconds', 'Tiempo para bajar el pecho');

-- Error types
INSERT INTO error_types (exercise_id, error_code, error_name, error_category, severity, feedback_message, correction_hint) VALUES
-- Position errors
(2, 'NECK_OVEREXTENDED', 'Cuello hiperextendido', 'safety', 4, 'No fuerces el cuello hacia atrás', 'Mantén el cuello en posición neutral'),
(2, 'INSUFFICIENT_EXTENSION', 'Extensión insuficiente', 'position', 2, 'Levanta más el pecho', 'La extensión debe ser mayor para ser efectiva'),
(2, 'ARMS_PUSHING', 'Apoyo excesivo en brazos', 'form', 2, 'No te apoyes en los brazos', 'Usa los músculos de la espalda'),

-- Time errors
(2, 'LIFT_TOO_FAST', 'Levantamiento muy rápido', 'time', 2, 'Levanta más lentamente', 'El levantamiento debe ser controlado'),
(2, 'LIFT_TOO_SLOW', 'Levantamiento muy lento', 'time', 1, 'Puedes levantar un poco más rápido', 'No excedas 2.5 segundos'),
(2, 'NOT_HOLDING_EXTENSION', 'No mantiene extensión', 'time', 3, 'Mantén la extensión más tiempo', 'Sostén al menos 2 segundos'),
(2, 'HOLDING_TOO_LONG', 'Mantiene extensión demasiado', 'time', 1, 'Ya puedes bajar', 'No mantengas más de 4 segundos'),
(2, 'LOWER_TOO_FAST', 'Descenso muy rápido', 'time', 2, 'Baja más lentamente', 'El descenso debe ser controlado'),
(2, 'LOWER_TOO_SLOW', 'Descenso muy lento', 'time', 1, 'Puedes bajar un poco más rápido', 'No excedas 2.5 segundos');

-- Validation
INSERT INTO validation_rules (exercise_id, rule_type, error_code, priority) VALUES
(2, 'angle_check', 'INSUFFICIENT_EXTENSION', 1),
(2, 'angle_check', 'NECK_OVEREXTENDED', 2),
(2, 'symmetry_check', 'NECK_OVEREXTENDED', 3),
(2, 'time_check', 'LIFT_TOO_FAST', 4),
(2, 'time_check', 'LIFT_TOO_SLOW', 5),
(2, 'time_check', 'NOT_HOLDING_EXTENSION', 6),
(2, 'time_check', 'HOLDING_TOO_LONG', 7),
(2, 'time_check', 'LOWER_TOO_FAST', 8),
(2, 'time_check', 'LOWER_TOO_SLOW', 9);

-- Rule applicable phases
INSERT INTO rule_applicable_phases (rule_id, phase_name) VALUES
-- Position validation rules
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'INSUFFICIENT_EXTENSION' AND rule_type = 'angle_check'), 'LIFT_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'INSUFFICIENT_EXTENSION' AND rule_type = 'angle_check'), 'HOLD_EXTENSION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'angle_check'), 'LIFT_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'angle_check'), 'HOLD_EXTENSION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'symmetry_check'), 'LIFT_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'symmetry_check'), 'HOLD_EXTENSION'),

-- Time validation rules
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LIFT_TOO_FAST' AND rule_type = 'time_check'), 'LIFT_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LIFT_TOO_SLOW' AND rule_type = 'time_check'), 'LIFT_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_HOLDING_EXTENSION' AND rule_type = 'time_check'), 'HOLD_EXTENSION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'HOLDING_TOO_LONG' AND rule_type = 'time_check'), 'HOLD_EXTENSION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LOWER_TOO_FAST' AND rule_type = 'time_check'), 'LOWER_CHEST'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LOWER_TOO_SLOW' AND rule_type = 'time_check'), 'LOWER_CHEST');

-- Rule parameters
INSERT INTO rule_parameters (rule_id, parameter_key, parameter_value) VALUES
-- INSUFFICIENT_EXTENSION
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'INSUFFICIENT_EXTENSION' AND rule_type = 'angle_check'), 'parameter', 'chest_lift_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'INSUFFICIENT_EXTENSION' AND rule_type = 'angle_check'), 'min_value', '15'),

-- NECK_OVEREXTENDED (first rule - angle_check)
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'angle_check'), 'parameter', 'chest_lift_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'angle_check'), 'max_value', '35'),

-- NECK_OVEREXTENDED (second rule - symmetry_check)
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'symmetry_check'), 'parameter', 'neck_alignment'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NECK_OVEREXTENDED' AND rule_type = 'symmetry_check'), 'max_asymmetry', '0.08'),

-- Time validation rules
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LIFT_TOO_FAST' AND rule_type = 'time_check'), 'min_time_ms', '800'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LIFT_TOO_SLOW' AND rule_type = 'time_check'), 'max_time_ms', '2500'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_HOLDING_EXTENSION' AND rule_type = 'time_check'), 'min_time_ms', '2000'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'HOLDING_TOO_LONG' AND rule_type = 'time_check'), 'max_time_ms', '4000'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LOWER_TOO_FAST' AND rule_type = 'time_check'), 'min_time_ms', '800'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'LOWER_TOO_SLOW' AND rule_type = 'time_check'), 'max_time_ms', '2500');

-- Landmark mappings
INSERT INTO landmark_mappings (exercise_id, mapping_type, joint_name, description) VALUES
(2, 'primary_joint', 'chest_lift', 'Ángulo cadera-hombro-nariz para medir elevación del pecho'),
(2, 'secondary_joint', 'spine_curve', 'Ángulo cadera-hombro-oreja para curvatura espinal'),
(2, 'reference_point', 'neck_alignment', 'Puntos nariz-hombro para alineación del cuello'),
(2, 'reference_point', 'body_symmetry', 'Hombros para verificar simetría corporal');

-- Landmark indices
INSERT INTO landmark_indices (mapping_id, landmark_index, index_order) VALUES
-- Chest lift mapping: landmarks [23, 11, 0]
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'chest_lift'), 23, 0), -- cadera
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'chest_lift'), 11, 1), -- hombro
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'chest_lift'), 0, 2),  -- nariz

-- Spine curve mapping: landmarks [23, 11, 7]
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'spine_curve'), 23, 0), -- cadera
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'spine_curve'), 11, 1), -- hombro
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'spine_curve'), 7, 2),  -- oreja

-- Neck alignment mapping: landmarks [0, 11]
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'neck_alignment'), 0, 0),  -- nariz
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'neck_alignment'), 11, 1), -- hombro

-- Body symmetry mapping: landmarks [11, 12]
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'body_symmetry'), 11, 0), -- hombro izquierdo
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'body_symmetry'), 12, 1); -- hombro derecho

-- =========================================================
-- MOCK PATIENT
-- =========================================================

-- Create mock patient Yordi Agustin
INSERT INTO patient (name, surname, phone_number) VALUES 
('Yordi', 'Agustin', '+51987654321');

-- Assign both exercises
DO $$
DECLARE
    patient_yordi UUID;
BEGIN
    SELECT id INTO patient_yordi FROM patient WHERE name = 'Yordi' AND surname = 'Agustin' LIMIT 1;
    
    IF patient_yordi IS NOT NULL THEN
        -- Assign Trunk Flexion
        INSERT INTO patient_exercise_assignments (patient_id, exercise_id, target_reps_per_session, target_sessions_per_week, notes) 
        VALUES (patient_yordi, 1, 10, 3, 'Ejercicio inicial para dolor lumbar');
        
        -- Assign Lumbar Extension
        INSERT INTO patient_exercise_assignments (patient_id, exercise_id, target_reps_per_session, target_sessions_per_week, notes) 
        VALUES (patient_yordi, 2, 8, 2, 'Ejercicio de fortalecimiento avanzado');
    END IF;
END $$;

-- =========================================================
-- SET SEQUENCES
-- =========================================================

SELECT setval('exercises_id_seq', (SELECT MAX(id) FROM exercises));
SELECT setval('exercise_phases_id_seq', (SELECT MAX(id) FROM exercise_phases));
SELECT setval('phase_transitions_id_seq', (SELECT MAX(id) FROM phase_transitions));
SELECT setval('validation_parameters_id_seq', (SELECT MAX(id) FROM validation_parameters));
SELECT setval('error_types_id_seq', (SELECT MAX(id) FROM error_types));
SELECT setval('validation_rules_id_seq', (SELECT MAX(id) FROM validation_rules));
SELECT setval('rule_applicable_phases_id_seq', (SELECT MAX(id) FROM rule_applicable_phases));
SELECT setval('rule_parameters_id_seq', (SELECT MAX(id) FROM rule_parameters));
SELECT setval('landmark_mappings_id_seq', (SELECT MAX(id) FROM landmark_mappings));
SELECT setval('landmark_indices_id_seq', (SELECT MAX(id) FROM landmark_indices));
SELECT setval('patient_exercise_assignments_id_seq', (SELECT MAX(id) FROM patient_exercise_assignments));

-- =========================================================
-- VERIFICATION OF INSERTED DATA
-- =========================================================

SELECT 
    'Ejercicios' as tabla, COUNT(*) as registros FROM exercises
UNION ALL
SELECT 'Fases', COUNT(*) FROM exercise_phases WHERE exercise_id IN (1, 2)
UNION ALL
SELECT 'Transiciones', COUNT(*) FROM phase_transitions 
UNION ALL
SELECT 'Parámetros', COUNT(*) FROM validation_parameters WHERE exercise_id IN (1, 2)
UNION ALL
SELECT 'Tipos de Error', COUNT(*) FROM error_types WHERE exercise_id IN (1, 2)
UNION ALL
SELECT 'Reglas de Validación', COUNT(*) FROM validation_rules WHERE exercise_id IN (1, 2)
UNION ALL
SELECT 'Fases Aplicables', COUNT(*) FROM rule_applicable_phases
UNION ALL
SELECT 'Parámetros de Reglas', COUNT(*) FROM rule_parameters
UNION ALL
SELECT 'Mapeos de Landmarks', COUNT(*) FROM landmark_mappings WHERE exercise_id IN (1, 2)
UNION ALL
SELECT 'Índices de Landmarks', COUNT(*) FROM landmark_indices
UNION ALL
SELECT 'Pacientes', COUNT(*) FROM patient
UNION ALL
SELECT 'Asignaciones', COUNT(*) FROM patient_exercise_assignments;

-- Show complete configuration available
SELECT 
    e.name as ejercicio,
    COUNT(DISTINCT ep.id) as fases,
    COUNT(DISTINCT vp.id) as parametros,
    COUNT(DISTINCT et.id) as tipos_errores,
    COUNT(DISTINCT vr.id) as reglas_validacion,
    COUNT(DISTINCT lm.id) as mapeos_landmarks
FROM exercises e
LEFT JOIN exercise_phases ep ON e.id = ep.exercise_id
LEFT JOIN validation_parameters vp ON e.id = vp.exercise_id
LEFT JOIN error_types et ON e.id = et.exercise_id
LEFT JOIN validation_rules vr ON e.id = vr.exercise_id
LEFT JOIN landmark_mappings lm ON e.id = lm.exercise_id
WHERE e.id IN (1, 2)
GROUP BY e.id, e.name
ORDER BY e.id;

-- Show patient with assigned exercises
SELECT 
    p.name || ' ' || p.surname as paciente,
    e.name as ejercicio_asignado,
    e.estimated_duration_minutes as duracion_minutos,
    e.difficulty_level as dificultad,
    pea.target_reps_per_session as reps_objetivo,
    pea.target_sessions_per_week as sesiones_semanales
FROM patient p
JOIN patient_exercise_assignments pea ON p.id = pea.patient_id
JOIN exercises e ON pea.exercise_id = e.id
WHERE p.name = 'Yordi' AND p.surname = 'Agustin';

-- Verify relational structure
SELECT 
    'Estructura Ejercicio 1' as seccion,
    'Reglas configuradas correctamente' as estado
FROM validation_rules vr
JOIN rule_applicable_phases rap ON vr.id = rap.rule_id
JOIN rule_parameters rp ON vr.id = rp.rule_id
WHERE vr.exercise_id = 1
LIMIT 1;

-- Verify landmarks configured correctly
SELECT 
    lm.joint_name,
    COUNT(li.id) as cantidad_landmarks,
    string_agg(li.landmark_index::text, ', ' ORDER BY li.index_order) as indices_configurados
FROM landmark_mappings lm
JOIN landmark_indices li ON lm.id = li.mapping_id
WHERE lm.exercise_id = 1
GROUP BY lm.id, lm.joint_name
ORDER BY lm.joint_name;