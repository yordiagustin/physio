CREATE OR REPLACE FUNCTION get_exercise_rules(p_exercise_id INTEGER)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM exercises WHERE id = p_exercise_id AND is_active = true) THEN
        RETURN NULL;
    END IF;
    WITH exercise_base AS (
        SELECT 
            e.id,
            e.name,
            e.description,
            e.target_condition,
            e.difficulty_level,
            e.estimated_duration_minutes
        FROM exercises e
        WHERE e.id = p_exercise_id AND e.is_active = true
    ),
    phases_data AS (
        SELECT json_agg(
            json_build_object(
                'phase_name', ep.phase_name,
                'phase_order', ep.phase_order,
                'instruction_message', ep.instruction_message,
                'success_message', ep.success_message,
                'transitions', COALESCE(transitions_agg.transitions, '[]'::json)
            ) ORDER BY ep.phase_order
        ) as phases
        FROM exercise_phases ep
        LEFT JOIN (
            SELECT 
                pt.phase_id,
                json_agg(
                    json_build_object(
                        'parameter_name', pt.parameter_name,
                        'operator', pt.operator,
                        'value', pt.value,
                        'value2', pt.value2,
                        'hysteresis', pt.hysteresis
                    )
                ) as transitions
            FROM phase_transitions pt
            GROUP BY pt.phase_id
        ) transitions_agg ON ep.id = transitions_agg.phase_id
        WHERE ep.exercise_id = p_exercise_id
    ),
    parameters_data AS (
        SELECT json_object_agg(
            vp.parameter_name,
            json_build_object(
                'name', vp.parameter_name,
                'type', vp.parameter_type,
                'phase_specific', vp.phase_specific,
                'default_value', vp.default_value,
                'min_value', vp.min_value,
                'max_value', vp.max_value,
                'unit', vp.unit,
                'description', vp.description
            )
        ) as parameters
        FROM validation_parameters vp
        WHERE vp.exercise_id = p_exercise_id
    ),
    error_types_data AS (
        SELECT json_agg(
            json_build_object(
                'error_code', et.error_code,
                'error_name', et.error_name,
                'error_category', et.error_category,
                'severity', et.severity,
                'feedback_message', et.feedback_message,
                'correction_hint', et.correction_hint
            )
        ) as error_types
        FROM error_types et
        WHERE et.exercise_id = p_exercise_id
    ),
    validation_rules_data AS (
        SELECT json_agg(
            json_build_object(
                'rule_type', vr.rule_type,
                'applicable_phases', COALESCE(phases_agg.phases, '[]'::json),
                'parameters', COALESCE(params_agg.params, '{}'::json),
                'error_code', vr.error_code,
                'priority', vr.priority,
                'is_active', vr.is_active
            ) ORDER BY vr.priority
        ) as validation_rules
        FROM validation_rules vr
        LEFT JOIN (
            SELECT 
                rap.rule_id,
                json_agg(rap.phase_name) as phases
            FROM rule_applicable_phases rap
            GROUP BY rap.rule_id
        ) phases_agg ON vr.id = phases_agg.rule_id
        LEFT JOIN (
            SELECT 
                rp.rule_id,
                json_object_agg(rp.parameter_key, rp.parameter_value) as params
            FROM rule_parameters rp
            GROUP BY rp.rule_id
        ) params_agg ON vr.id = params_agg.rule_id
        WHERE vr.exercise_id = p_exercise_id AND vr.is_active = true
    ),
    landmarks_data AS (
        SELECT json_object_agg(
            CONCAT(lm.mapping_type, '_', lm.joint_name),
            json_build_object(
                'mapping_type', lm.mapping_type,
                'joint_name', lm.joint_name,
                'description', lm.description,
                'indices', COALESCE(indices_agg.indices, '[]'::json)
            )
        ) as landmark_mappings
        FROM landmark_mappings lm
        LEFT JOIN (
            SELECT 
                li.mapping_id,
                json_agg(li.landmark_index ORDER BY li.index_order) as indices
            FROM landmark_indices li
            GROUP BY li.mapping_id
        ) indices_agg ON lm.id = indices_agg.mapping_id
        WHERE lm.exercise_id = p_exercise_id
    )
    
    SELECT json_build_object(
        'exercise_id', eb.id,
        'exercise_name', eb.name,
        'description', eb.description,
        'target_condition', eb.target_condition,
        'difficulty_level', eb.difficulty_level,
        'estimated_duration_minutes', eb.estimated_duration_minutes,
        'phases', COALESCE(pd.phases, '[]'::json),
        'parameters', COALESCE(prd.parameters, '{}'::json),
        'error_types', COALESCE(etd.error_types, '[]'::json),
        'validation_rules', COALESCE(vrd.validation_rules, '[]'::json),
        'landmark_mappings', COALESCE(ld.landmark_mappings, '{}'::json)
    ) INTO result
    FROM exercise_base eb
    LEFT JOIN (SELECT phases FROM phases_data) pd ON true
    LEFT JOIN (SELECT parameters FROM parameters_data) prd ON true
    LEFT JOIN (SELECT error_types FROM error_types_data) etd ON true
    LEFT JOIN (SELECT validation_rules FROM validation_rules_data) vrd ON true
    LEFT JOIN (SELECT landmark_mappings FROM landmarks_data) ld ON true;

    RETURN result;
END;
$$ LANGUAGE plpgsql;