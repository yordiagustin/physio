-- =========================================================
-- SEEDER EJERCICIOS SIMPLES
-- 1. Elevar pierna a silla
-- 2. Sentadilla con apoyo
-- =========================================================

-- =========================================================
-- EJERCICIO 1: ELEVAR PIERNA A SILLA
-- =========================================================

-- Ejercicio base
INSERT INTO exercises (id, name, description, target_condition, difficulty_level, estimated_duration_minutes, instructions) VALUES
(1, 'Elevar Pierna a Silla', 
 'Ejercicio de fortalecimiento y movilidad donde el paciente levanta una pierna y la apoya en una silla. Mejora la flexibilidad de cadera y fortalece músculos de la pierna.',
 'general_mobility', 2, 5, 
 'Párate frente a una silla. Levanta lentamente una pierna y apóyala en el asiento de la silla. Mantén la posición y baja la pierna de forma controlada. Alterna entre piernas.');

-- Fases del ejercicio
INSERT INTO exercise_phases (exercise_id, phase_name, phase_order, instruction_message, success_message) VALUES
(1, 'STANDING', 1, 'Ponte de pie frente a la silla', 'Posición inicial correcta'),
(1, 'LIFTING_LEG', 2, 'Levanta la pierna hacia la silla', 'Buen levantamiento'),
(1, 'LEG_ON_CHAIR', 3, 'Mantén la pierna apoyada en la silla', 'Posición mantenida'),
(1, 'LOWERING_LEG', 4, 'Baja la pierna lentamente', 'Buen descenso controlado'),
(1, 'COMPLETED_REP', 5, '¡Repetición completada!', 'Excelente ejecución');

-- Transiciones entre fases
INSERT INTO phase_transitions (phase_id, parameter_name, operator, value, hysteresis) VALUES
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'STANDING'), 'knee_height', '>', 0.3, 0.05),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'LIFTING_LEG'), 'knee_height', '>=', 0.6, 0.05),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'LEG_ON_CHAIR'), 'knee_height', '<', 0.5, 0.1),
((SELECT id FROM exercise_phases WHERE exercise_id = 1 AND phase_name = 'LOWERING_LEG'), 'knee_height', '<=', 0.2, 0.05);

-- Parámetros de validación
INSERT INTO validation_parameters (exercise_id, parameter_name, parameter_type, phase_specific, default_value, min_value, max_value, unit, description) VALUES
-- Parámetros de altura
(1, 'knee_height_target', 'distance', NULL, 0.6, 0.5, 0.8, 'normalized', 'Altura objetivo de la rodilla'),
(1, 'hip_angle_target', 'angle', NULL, 90.0, 70.0, 110.0, 'degrees', 'Ángulo objetivo de cadera al levantar pierna'),
(1, 'balance_tolerance', 'distance', NULL, 0.1, 0.05, 0.15, 'normalized', 'Tolerancia para mantener equilibrio'),

-- Parámetros de tiempo
(1, 'lifting_time', 'time', 'LIFTING_LEG', 1500.0, 1000.0, 3000.0, 'milliseconds', 'Tiempo para levantar pierna'),
(1, 'hold_time', 'time', 'LEG_ON_CHAIR', 2000.0, 1500.0, 4000.0, 'milliseconds', 'Tiempo de mantenimiento'),
(1, 'lowering_time', 'time', 'LOWERING_LEG', 1500.0, 1000.0, 3000.0, 'milliseconds', 'Tiempo para bajar pierna');

-- Tipos de errores
INSERT INTO error_types (exercise_id, error_code, error_name, error_category, severity, feedback_message, correction_hint) VALUES
-- Errores de posición
(1, 'LEG_NOT_HIGH_ENOUGH', 'Pierna muy baja', 'position', 3, 'Levanta más la pierna', 'La rodilla debe llegar al nivel de la silla'),
(1, 'LOSING_BALANCE', 'Perdiendo equilibrio', 'safety', 4, 'Mantén el equilibrio', 'Apóyate en algo si es necesario'),
(1, 'WRONG_LEG_ANGLE', 'Ángulo de pierna incorrecto', 'form', 2, 'Ajusta el ángulo de la pierna', 'La pierna debe estar en 90°'),

-- Errores de tiempo
(1, 'LIFTING_TOO_FAST', 'Levantamiento muy rápido', 'time', 2, 'Levanta más lentamente', 'El movimiento debe ser controlado'),
(1, 'LIFTING_TOO_SLOW', 'Levantamiento muy lento', 'time', 1, 'Puedes levantar un poco más rápido', 'No excedas 3 segundos'),
(1, 'NOT_HOLDING_ENOUGH', 'No mantiene posición', 'time', 3, 'Mantén la posición más tiempo', 'Sostén al menos 2 segundos'),
(1, 'LOWERING_TOO_FAST', 'Bajada muy rápida', 'time', 2, 'Baja más lentamente', 'El descenso debe ser controlado');

-- Reglas de validación
INSERT INTO validation_rules (exercise_id, rule_type, error_code, priority) VALUES
-- Validaciones de posición
(1, 'position_check', 'LEG_NOT_HIGH_ENOUGH', 1),
(1, 'position_check', 'LOSING_BALANCE', 2),
(1, 'angle_check', 'WRONG_LEG_ANGLE', 3),

-- Validaciones de tiempo
(1, 'time_check', 'LIFTING_TOO_FAST', 4),
(1, 'time_check', 'LIFTING_TOO_SLOW', 5),
(1, 'time_check', 'NOT_HOLDING_ENOUGH', 6),
(1, 'time_check', 'LOWERING_TOO_FAST', 7);

-- Fases aplicables para cada regla
INSERT INTO rule_applicable_phases (rule_id, phase_name) VALUES
-- LEG_NOT_HIGH_ENOUGH se aplica en LIFTING_LEG y LEG_ON_CHAIR
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LEG_NOT_HIGH_ENOUGH'), 'LIFTING_LEG'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LEG_NOT_HIGH_ENOUGH'), 'LEG_ON_CHAIR'),

-- LOSING_BALANCE se aplica en todas las fases activas
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOSING_BALANCE'), 'LIFTING_LEG'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOSING_BALANCE'), 'LEG_ON_CHAIR'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOSING_BALANCE'), 'LOWERING_LEG'),

-- WRONG_LEG_ANGLE se aplica en LEG_ON_CHAIR
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'WRONG_LEG_ANGLE'), 'LEG_ON_CHAIR'),

-- Validaciones de tiempo por fase
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LIFTING_TOO_FAST'), 'LIFTING_LEG'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LIFTING_TOO_SLOW'), 'LIFTING_LEG'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'NOT_HOLDING_ENOUGH'), 'LEG_ON_CHAIR'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOWERING_TOO_FAST'), 'LOWERING_LEG');

-- Parámetros de reglas
INSERT INTO rule_parameters (rule_id, parameter_key, parameter_value) VALUES
-- LEG_NOT_HIGH_ENOUGH
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LEG_NOT_HIGH_ENOUGH'), 'parameter', 'knee_height'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LEG_NOT_HIGH_ENOUGH'), 'min_value', '0.5'),

-- LOSING_BALANCE
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOSING_BALANCE'), 'parameter', 'body_stability'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOSING_BALANCE'), 'max_deviation', '0.1'),

-- WRONG_LEG_ANGLE
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'WRONG_LEG_ANGLE'), 'parameter', 'hip_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'WRONG_LEG_ANGLE'), 'min_value', '70'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'WRONG_LEG_ANGLE'), 'max_value', '110'),

-- Parámetros de tiempo
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LIFTING_TOO_FAST'), 'min_time_ms', '1000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LIFTING_TOO_SLOW'), 'max_time_ms', '3000'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'NOT_HOLDING_ENOUGH'), 'min_time_ms', '1500'),
((SELECT id FROM validation_rules WHERE exercise_id = 1 AND error_code = 'LOWERING_TOO_FAST'), 'min_time_ms', '1000');

-- Mapeos de landmarks
INSERT INTO landmark_mappings (exercise_id, mapping_type, joint_name, description) VALUES
(1, 'primary_joint', 'hip_angle', 'Ángulo cadera-rodilla-tobillo para elevación de pierna'),
(1, 'secondary_joint', 'knee_position', 'Posición de rodilla para altura'),
(1, 'reference_point', 'body_center', 'Centro de masa para equilibrio'),
(1, 'reference_point', 'support_leg', 'Pierna de apoyo para estabilidad');

-- Índices de landmarks
INSERT INTO landmark_indices (mapping_id, landmark_index, index_order) VALUES
-- Hip angle: cadera(23) - rodilla(25) - tobillo(27)
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'hip_angle'), 23, 0), -- cadera
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'hip_angle'), 25, 1), -- rodilla
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'hip_angle'), 27, 2), -- tobillo

-- Knee position: rodilla(25)
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'knee_position'), 25, 0), -- rodilla

-- Body center: hombros(11,12) y caderas(23,24) para centro de masa
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'body_center'), 11, 0), -- hombro izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'body_center'), 12, 1), -- hombro der
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'body_center'), 23, 2), -- cadera izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'body_center'), 24, 3), -- cadera der

-- Support leg: cadera(24) - rodilla(26) - tobillo(28) (pierna derecha como apoyo)
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'support_leg'), 24, 0), -- cadera der
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'support_leg'), 26, 1), -- rodilla der
((SELECT id FROM landmark_mappings WHERE exercise_id = 1 AND joint_name = 'support_leg'), 28, 2); -- tobillo der

-- =========================================================
-- EJERCICIO 2: SENTADILLA CON APOYO
-- =========================================================

-- Ejercicio base
INSERT INTO exercises (id, name, description, target_condition, difficulty_level, estimated_duration_minutes, instructions) VALUES
(2, 'Sentadilla con Apoyo', 
 'Ejercicio de fortalecimiento de piernas usando una silla como soporte. El paciente baja hasta casi sentarse y luego se levanta manteniendo control.',
 'general_strength', 2, 8,
 'Párate frente a una silla con las manos apoyadas en el respaldo. Baja lentamente como si fueras a sentarte, sin llegar a tocar la silla. Mantén la posición y levántate de forma controlada.');

-- Fases del ejercicio
INSERT INTO exercise_phases (exercise_id, phase_name, phase_order, instruction_message, success_message) VALUES
(2, 'STANDING_READY', 1, 'Ponte de pie con apoyo en la silla', 'Posición inicial correcta'),
(2, 'DESCENDING', 2, 'Baja lentamente como si fueras a sentarte', 'Buen descenso'),
(2, 'BOTTOM_POSITION', 3, 'Mantén la posición baja', 'Posición mantenida'),
(2, 'ASCENDING', 4, 'Levántate lentamente', 'Buen ascenso'),
(2, 'COMPLETED_REP', 5, '¡Repetición completada!', 'Excelente sentadilla');

-- Transiciones entre fases
INSERT INTO phase_transitions (phase_id, parameter_name, operator, value, hysteresis) VALUES
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'STANDING_READY'), 'knee_angle', '<', 160.0, 5.0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'DESCENDING'), 'knee_angle', '<=', 90.0, 5.0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'BOTTOM_POSITION'), 'knee_angle', '>', 100.0, 10.0),
((SELECT id FROM exercise_phases WHERE exercise_id = 2 AND phase_name = 'ASCENDING'), 'knee_angle', '>=', 150.0, 10.0);

-- Parámetros de validación
INSERT INTO validation_parameters (exercise_id, parameter_name, parameter_type, phase_specific, default_value, min_value, max_value, unit, description) VALUES
-- Parámetros de ángulos
(2, 'knee_angle_bottom', 'angle', NULL, 90.0, 70.0, 110.0, 'degrees', 'Ángulo objetivo de rodillas en posición baja'),
(2, 'back_angle', 'angle', NULL, 75.0, 60.0, 90.0, 'degrees', 'Ángulo de inclinación de espalda'),
(2, 'feet_alignment', 'distance', NULL, 0.05, 0.03, 0.1, 'normalized', 'Alineación de pies'),

-- Parámetros de tiempo
(2, 'descending_time', 'time', 'DESCENDING', 2000.0, 1500.0, 3500.0, 'milliseconds', 'Tiempo para descenso'),
(2, 'hold_time', 'time', 'BOTTOM_POSITION', 1500.0, 1000.0, 3000.0, 'milliseconds', 'Tiempo de mantenimiento'),
(2, 'ascending_time', 'time', 'ASCENDING', 2000.0, 1500.0, 3500.0, 'milliseconds', 'Tiempo para ascenso');

-- Tipos de errores
INSERT INTO error_types (exercise_id, error_code, error_name, error_category, severity, feedback_message, correction_hint) VALUES
-- Errores de posición
(2, 'KNEES_NOT_ALIGNED', 'Rodillas desalineadas', 'form', 3, 'Mantén las rodillas alineadas con los pies', 'Las rodillas deben apuntar hacia adelante'),
(2, 'NOT_LOW_ENOUGH', 'No baja lo suficiente', 'position', 2, 'Baja más, como si fueras a sentarte', 'Las rodillas deben llegar a 90°'),
(2, 'BACK_TOO_FORWARD', 'Espalda muy inclinada', 'form', 3, 'Mantén la espalda más recta', 'No te inclines demasiado hacia adelante'),
(2, 'FEET_MISALIGNED', 'Pies mal posicionados', 'form', 2, 'Alinea bien los pies', 'Los pies deben estar paralelos'),

-- Errores de tiempo
(2, 'DESCENDING_TOO_FAST', 'Bajada muy rápida', 'time', 2, 'Baja más lentamente', 'El descenso debe ser controlado'),
(2, 'DESCENDING_TOO_SLOW', 'Bajada muy lenta', 'time', 1, 'Puedes bajar un poco más rápido', 'No excedas 3.5 segundos'),
(2, 'NOT_HOLDING_ENOUGH', 'No mantiene posición', 'time', 3, 'Mantén la posición más tiempo', 'Sostén al menos 1 segundo'),
(2, 'ASCENDING_TOO_FAST', 'Subida muy rápida', 'time', 2, 'Sube más lentamente', 'El ascenso debe ser controlado'),
(2, 'ASCENDING_TOO_SLOW', 'Subida muy lenta', 'time', 1, 'Puedes subir un poco más rápido', 'No excedas 3.5 segundos');

-- Reglas de validación
INSERT INTO validation_rules (exercise_id, rule_type, error_code, priority) VALUES
-- Validaciones de posición
(2, 'angle_check', 'NOT_LOW_ENOUGH', 1),
(2, 'angle_check', 'BACK_TOO_FORWARD', 2),
(2, 'symmetry_check', 'KNEES_NOT_ALIGNED', 3),
(2, 'position_check', 'FEET_MISALIGNED', 4),

-- Validaciones de tiempo
(2, 'time_check', 'DESCENDING_TOO_FAST', 5),
(2, 'time_check', 'DESCENDING_TOO_SLOW', 6),
(2, 'time_check', 'NOT_HOLDING_ENOUGH', 7),
(2, 'time_check', 'ASCENDING_TOO_FAST', 8),
(2, 'time_check', 'ASCENDING_TOO_SLOW', 9);

-- Fases aplicables para cada regla
INSERT INTO rule_applicable_phases (rule_id, phase_name) VALUES
-- NOT_LOW_ENOUGH se aplica en DESCENDING y BOTTOM_POSITION
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_LOW_ENOUGH'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_LOW_ENOUGH'), 'BOTTOM_POSITION'),

-- BACK_TOO_FORWARD se aplica en todas las fases activas
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'BACK_TOO_FORWARD'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'BACK_TOO_FORWARD'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'BACK_TOO_FORWARD'), 'ASCENDING'),

-- KNEES_NOT_ALIGNED se aplica en todas las fases activas
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'KNEES_NOT_ALIGNED'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'KNEES_NOT_ALIGNED'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'KNEES_NOT_ALIGNED'), 'ASCENDING'),

-- FEET_MISALIGNED se aplica en todas las fases
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'STANDING_READY'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'ASCENDING'),

-- Validaciones de tiempo por fase
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'DESCENDING_TOO_FAST'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'DESCENDING_TOO_SLOW'), 'DESCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_HOLDING_ENOUGH'), 'BOTTOM_POSITION'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'ASCENDING_TOO_FAST'), 'ASCENDING'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'ASCENDING_TOO_SLOW'), 'ASCENDING');

-- Parámetros de reglas
INSERT INTO rule_parameters (rule_id, parameter_key, parameter_value) VALUES
-- NOT_LOW_ENOUGH
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_LOW_ENOUGH'), 'parameter', 'knee_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_LOW_ENOUGH'), 'max_value', '110'),

-- BACK_TOO_FORWARD
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'BACK_TOO_FORWARD'), 'parameter', 'back_angle'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'BACK_TOO_FORWARD'), 'min_value', '60'),

-- KNEES_NOT_ALIGNED
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'KNEES_NOT_ALIGNED'), 'parameter', 'knee_alignment'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'KNEES_NOT_ALIGNED'), 'max_deviation', '0.1'),

-- FEET_MISALIGNED
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'parameter', 'feet_alignment'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'FEET_MISALIGNED'), 'max_deviation', '0.05'),

-- Parámetros de tiempo
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'DESCENDING_TOO_FAST'), 'min_time_ms', '1500'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'DESCENDING_TOO_SLOW'), 'max_time_ms', '3500'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'NOT_HOLDING_ENOUGH'), 'min_time_ms', '1000'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'ASCENDING_TOO_FAST'), 'min_time_ms', '1500'),
((SELECT id FROM validation_rules WHERE exercise_id = 2 AND error_code = 'ASCENDING_TOO_SLOW'), 'max_time_ms', '3500');

-- Mapeos de landmarks
INSERT INTO landmark_mappings (exercise_id, mapping_type, joint_name, description) VALUES
(2, 'primary_joint', 'knee_angle', 'Ángulo cadera-rodilla-tobillo para sentadilla'),
(2, 'secondary_joint', 'back_angle', 'Ángulo hombro-cadera-rodilla para postura'),
(2, 'reference_point', 'knee_alignment', 'Alineación de rodillas'),
(2, 'reference_point', 'feet_position', 'Posición de pies para estabilidad');

-- Índices de landmarks
INSERT INTO landmark_indices (mapping_id, landmark_index, index_order) VALUES
-- Knee angle: cadera(23) - rodilla(25) - tobillo(27) (pierna izquierda)
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'knee_angle'), 23, 0), -- cadera izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'knee_angle'), 25, 1), -- rodilla izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'knee_angle'), 27, 2), -- tobillo izq

-- Back angle: hombro(11) - cadera(23) - rodilla(25)
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'back_angle'), 11, 0), -- hombro izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'back_angle'), 23, 1), -- cadera izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'back_angle'), 25, 2), -- rodilla izq

-- Knee alignment: rodillas izq(25) y der(26)
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'knee_alignment'), 25, 0), -- rodilla izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'knee_alignment'), 26, 1), -- rodilla der

-- Feet position: tobillos izq(27) y der(28)
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'feet_position'), 27, 0), -- tobillo izq
((SELECT id FROM landmark_mappings WHERE exercise_id = 2 AND joint_name = 'feet_position'), 28, 1); -- tobillo der

-- =========================================================
-- PACIENTE DE PRUEBA
-- =========================================================

-- Crear paciente Yordi Agustin
INSERT INTO patient (name, surname, phone_number) VALUES 
('Yordi', 'Agustin', '+51934018219');

-- Asignar ambos ejercicios al paciente
DO $$
DECLARE
    patient_yordi UUID;
BEGIN
    SELECT id INTO patient_yordi FROM patient WHERE name = 'Yordi' AND surname = 'Agustin' LIMIT 1;
    
    IF patient_yordi IS NOT NULL THEN
        -- Asignar Elevar Pierna a Silla
        INSERT INTO patient_exercise_assignments (patient_id, exercise_id, target_reps_per_session, target_sessions_per_week, notes) 
        VALUES (patient_yordi, 1, 8, 3, 'Ejercicio básico de movilidad de cadera');
        
        -- Asignar Sentadilla con Apoyo
        INSERT INTO patient_exercise_assignments (patient_id, exercise_id, target_reps_per_session, target_sessions_per_week, notes) 
        VALUES (patient_yordi, 2, 10, 3, 'Ejercicio de fortalecimiento de piernas con apoyo');
    END IF;
END $$;

-- =========================================================
-- AJUSTAR SECUENCIAS
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
-- VERIFICACIÓN DE DATOS INSERTADOS
-- =========================================================

-- Resumen de datos insertados
SELECT 
    'Ejercicios' as tabla, COUNT(*) as registros FROM exercises
UNION ALL
SELECT 'Fases', COUNT(*) FROM exercise_phases
UNION ALL
SELECT 'Transiciones', COUNT(*) FROM phase_transitions 
UNION ALL
SELECT 'Parámetros', COUNT(*) FROM validation_parameters
UNION ALL
SELECT 'Tipos de Error', COUNT(*) FROM error_types
UNION ALL
SELECT 'Reglas de Validación', COUNT(*) FROM validation_rules
UNION ALL
SELECT 'Fases Aplicables', COUNT(*) FROM rule_applicable_phases
UNION ALL
SELECT 'Parámetros de Reglas', COUNT(*) FROM rule_parameters
UNION ALL
SELECT 'Mapeos de Landmarks', COUNT(*) FROM landmark_mappings
UNION ALL
SELECT 'Índices de Landmarks', COUNT(*) FROM landmark_indices
UNION ALL
SELECT 'Pacientes', COUNT(*) FROM patient
UNION ALL
SELECT 'Asignaciones', COUNT(*) FROM patient_exercise_assignments;

-- Mostrar configuración completa por ejercicio
SELECT 
    e.name as ejercicio,
    e.difficulty_level as dificultad,
    e.estimated_duration_minutes as duracion_min,
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
GROUP BY e.id, e.name, e.difficulty_level, e.estimated_duration_minutes
ORDER BY e.id;

-- Mostrar fases de cada ejercicio
SELECT 
    e.name as ejercicio,
    ep.phase_order as orden,
    ep.phase_name as fase,
    ep.instruction_message as instruccion
FROM exercises e
JOIN exercise_phases ep ON e.id = ep.exercise_id
ORDER BY e.id, ep.phase_order;

-- Mostrar paciente con ejercicios asignados
SELECT 
    p.name || ' ' || p.surname as paciente,
    e.name as ejercicio_asignado,
    e.estimated_duration_minutes as duracion_minutos,
    e.difficulty_level as dificultad,
    pea.target_reps_per_session as reps_objetivo,
    pea.target_sessions_per_week as sesiones_semanales,
    pea.notes as notas
FROM patient p
JOIN patient_exercise_assignments pea ON p.id = pea.patient_id
JOIN exercises e ON pea.exercise_id = e.id
ORDER BY e.id;

-- Mostrar landmarks configurados por ejercicio
SELECT 
    e.name as ejercicio,
    lm.mapping_type as tipo_mapeo,
    lm.joint_name as articulacion,
    string_agg(li.landmark_index::text, ', ' ORDER BY li.index_order) as indices_landmarks
FROM exercises e
JOIN landmark_mappings lm ON e.id = lm.exercise_id
JOIN landmark_indices li ON lm.id = li.mapping_id
GROUP BY e.id, e.name, lm.id, lm.mapping_type, lm.joint_name
ORDER BY e.id, lm.mapping_type, lm.joint_name;

-- Verificar estructura relacional completa
SELECT 
    'Estructura configurada correctamente' as estado,
    COUNT(DISTINCT vr.id) as reglas_totales,
    COUNT(DISTINCT rap.id) as fases_aplicables,
    COUNT(DISTINCT rp.id) as parametros_reglas
FROM validation_rules vr
JOIN rule_applicable_phases rap ON vr.id = rap.rule_id
JOIN rule_parameters rp ON vr.id = rp.rule_id;