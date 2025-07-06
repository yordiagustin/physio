import pandas as pd
import numpy as np
import random
from typing import List, Dict, Tuple

class HybridPhysioDatasetGenerator:
    def __init__(self):
        """Generador para enfoque híbrido: exercise_id + expected_phase + landmarks"""
        
        # Definir ejercicios y sus fases específicas
        self.exercise_definitions = {
            "TRUNK_FLEXION": {
                "name": "Flexión de Tronco",
                "phases": ["STARTING", "GOING_DOWN", "HOLD_POSITION", "GOING_UP", "COMPLETED"]
            },
            "SHOULDER_ABDUCTION": {
                "name": "Abducción de Hombros", 
                "phases": ["STARTING", "RAISING", "HOLD_TOP", "LOWERING", "COMPLETED"]
            },
            "LEG_RAISE": {
                "name": "Elevación de Pierna",
                "phases": ["STARTING", "RAISING_LEG", "HOLD_HIGH", "LOWERING_LEG", "COMPLETED"]
            },
            "HIP_FLEXION": {
                "name": "Flexión de Cadera",
                "phases": ["STARTING", "FLEXING", "HOLD_FLEXED", "EXTENDING", "COMPLETED"]  
            },
            "KNEE_EXTENSION": {
                "name": "Extensión de Rodilla",
                "phases": ["STARTING", "EXTENDING", "HOLD_EXTENDED", "RELAXING", "COMPLETED"]
            }
        }
        
        self.error_types = [
            "INCORRECT_POSTURE", "INSUFFICIENT_RANGE", "TOO_FAST_MOVEMENT",
            "ASYMMETRIC_MOVEMENT", "INCORRECT_ALIGNMENT", "INCOMPLETE_HOLD"
        ]
    
    def generate_base_landmarks(self) -> List[float]:
        """Landmarks base (persona neutral de pie)"""
        return [
            # Cara (0-10) 
            0.50, 0.10, 0.48, 0.08, 0.47, 0.08, 0.46, 0.08, 0.52, 0.08,
            0.53, 0.08, 0.54, 0.08, 0.47, 0.06, 0.53, 0.06, 0.485, 0.12, 0.515, 0.12,
            
            # Torso superior (11-16) - CRÍTICOS PARA VALIDACIÓN
            0.45, 0.25,  # hombro izq (11) 
            0.55, 0.25,  # hombro der (12)
            0.42, 0.35,  # codo izq (13)
            0.58, 0.35,  # codo der (14)
            0.40, 0.45,  # muñeca izq (15)
            0.60, 0.45,  # muñeca der (16)
            
            # Manos (17-22)
            0.39, 0.47, 0.38, 0.46, 0.37, 0.47,
            0.61, 0.47, 0.62, 0.46, 0.63, 0.47,
            
            # Torso inferior (23-24) - CRÍTICOS
            0.47, 0.55,  # cadera izq (23)
            0.53, 0.55,  # cadera der (24)
            
            # Piernas (25-28) - CRÍTICOS
            0.47, 0.75,  # rodilla izq (25)
            0.53, 0.75,  # rodilla der (26) 
            0.47, 0.90,  # tobillo izq (27)
            0.53, 0.90,  # tobillo der (28)
            
            # Pies (29-32)
            0.46, 0.95, 0.54, 0.95, 0.48, 0.98, 0.52, 0.98,
        ]
    
    def modify_landmarks_for_exercise_phase(self, landmarks: List[float], 
                                          exercise_id: str, phase: str) -> List[float]:
        """Modifica landmarks para exercise_id + phase específicos"""
        
        modified = landmarks.copy()
        
        if exercise_id == "TRUNK_FLEXION":
            if phase == "STARTING":
                # Posición inicial - completamente erguido
                modified[22] = 0.20  # hombro izq muy arriba
                modified[24] = 0.20  # hombro der muy arriba
                modified[1] = 0.08   # cabeza alta
                modified[30] = 0.45  # manos normales
                modified[32] = 0.45
                
            elif phase == "GOING_DOWN":
                # Bajando - posición intermedia
                modified[22] = 0.40  # hombro baja moderadamente  
                modified[24] = 0.40  # hombro der baja
                modified[1] = 0.15   # cabeza baja un poco
                modified[30] = 0.55  # manos bajan
                modified[32] = 0.55
                
            elif phase == "HOLD_POSITION":
                # Manteniendo - posición más baja
                modified[22] = 0.65  # hombro MUY bajo
                modified[24] = 0.65  # hombro der MUY bajo  
                modified[1] = 0.30   # cabeza muy baja
                modified[30] = 0.70  # manos muy abajo
                modified[32] = 0.70
                
            elif phase == "GOING_UP":
                # Subiendo - posición intermedia hacia arriba
                modified[22] = 0.35  # hombro subiendo
                modified[24] = 0.35  # hombro der subiendo
                modified[1] = 0.12   # cabeza subiendo
                modified[30] = 0.50  # manos subiendo
                modified[32] = 0.50
                
            elif phase == "COMPLETED":
                # Completado - casi como inicial
                modified[22] = 0.22  # hombro casi normal
                modified[24] = 0.22  # hombro der casi normal
                modified[1] = 0.09   # cabeza casi normal
                modified[30] = 0.46  # manos casi normales
                modified[32] = 0.46
                
        elif exercise_id == "SHOULDER_ABDUCTION":
            if phase == "STARTING":
                # Brazos completamente abajo
                modified[30] = 0.55  # muñeca muy abajo
                modified[32] = 0.55  # muñeca der muy abajo
                modified[26] = 0.40  # codo abajo
                modified[28] = 0.40  # codo der abajo
                
            elif phase == "RAISING":
                # Elevando brazos - posición intermedia
                modified[30] = 0.35  # muñeca sube
                modified[32] = 0.35  # muñeca der sube
                modified[26] = 0.25  # codo sube
                modified[28] = 0.25  # codo der sube
                modified[29] = 0.35  # muñeca X se extiende
                modified[31] = 0.65  # muñeca der X se extiende
                
            elif phase == "HOLD_TOP":
                # Brazos arriba - posición máxima
                modified[30] = 0.10  # muñeca MUY arriba
                modified[32] = 0.10  # muñeca der MUY arriba
                modified[26] = 0.15  # codo muy arriba
                modified[28] = 0.15  # codo der muy arriba
                modified[29] = 0.25  # muñeca muy extendida
                modified[31] = 0.75  # muñeca der muy extendida
                
            elif phase == "LOWERING":
                # Bajando brazos - posición intermedia
                modified[30] = 0.35  # muñeca bajando
                modified[32] = 0.35  # muñeca der bajando
                modified[26] = 0.30  # codo bajando
                modified[28] = 0.30  # codo der bajando
                
            elif phase == "COMPLETED":
                # Completado - brazos abajo
                modified[30] = 0.52  # muñeca casi normal
                modified[32] = 0.52  # muñeca der casi normal
                modified[26] = 0.37  # codo casi normal
                modified[28] = 0.37  # codo der casi normal
                
        elif exercise_id == "LEG_RAISE":
            if phase == "STARTING":
                # Ambas piernas abajo
                modified[50] = 0.80  # rodilla normal
                modified[54] = 0.95  # tobillo abajo
                
            elif phase == "RAISING_LEG":
                # Levantando pierna izquierda
                modified[50] = 0.55  # rodilla sube
                modified[54] = 0.65  # tobillo sube
                
            elif phase == "HOLD_HIGH":
                # Pierna muy arriba
                modified[50] = 0.35  # rodilla MUY arriba
                modified[54] = 0.40  # tobillo MUY arriba
                
            elif phase == "LOWERING_LEG":
                # Bajando pierna
                modified[50] = 0.60  # rodilla bajando
                modified[54] = 0.75  # tobillo bajando
                
            elif phase == "COMPLETED":
                # Pierna abajo
                modified[50] = 0.78  # rodilla casi normal
                modified[54] = 0.92  # tobillo casi normal
                
        elif exercise_id == "HIP_FLEXION":
            if phase == "STARTING":
                # Posición neutral
                modified[46] = 0.50  # cadera normal
                modified[50] = 0.80  # rodilla normal
                
            elif phase == "FLEXING":
                # Flexionando cadera
                modified[46] = 0.40  # cadera baja
                modified[50] = 0.60  # rodilla se acerca
                
            elif phase == "HOLD_FLEXED":
                # Flexión máxima
                modified[46] = 0.30  # cadera MUY baja
                modified[50] = 0.45  # rodilla MUY cerca
                
            elif phase == "EXTENDING":
                # Extendiendo
                modified[46] = 0.45  # cadera subiendo
                modified[50] = 0.70  # rodilla alejándose
                
            elif phase == "COMPLETED":
                # Posición final
                modified[46] = 0.52  # cadera casi normal
                modified[50] = 0.78  # rodilla casi normal
                
        elif exercise_id == "KNEE_EXTENSION":
            if phase == "STARTING":
                # Rodilla flexionada
                modified[50] = 0.60  # rodilla flexionada
                modified[54] = 0.75  # tobillo cerca
                
            elif phase == "EXTENDING":
                # Extendiendo rodilla
                modified[50] = 0.75  # rodilla bajando
                modified[54] = 0.85  # tobillo alejándose
                
            elif phase == "HOLD_EXTENDED":
                # Extensión completa
                modified[50] = 0.88  # rodilla MUY baja
                modified[54] = 0.96  # tobillo muy lejos
                
            elif phase == "RELAXING":
                # Relajando
                modified[50] = 0.70  # rodilla subiendo
                modified[54] = 0.80  # tobillo acercándose
                
            elif phase == "COMPLETED":
                # Posición final
                modified[50] = 0.62  # rodilla casi inicial
                modified[54] = 0.77  # tobillo casi inicial
        
        return modified
    
    def introduce_errors(self, landmarks: List[float], exercise_id: str, 
                        phase: str) -> Tuple[List[float], str]:
        """Introduce errores específicos según exercise + phase"""
        
        modified = landmarks.copy()
        error_type = random.choice(self.error_types)
        
        if error_type == "ASYMMETRIC_MOVEMENT":
            # Asimetría corporal
            modified[22] += 0.08  # hombro izq más alto
            modified[24] -= 0.06  # hombro der más bajo
            modified[30] += 0.10  # muñeca izq más alta
            modified[32] -= 0.08  # muñeca der más baja
            
        elif error_type == "INSUFFICIENT_RANGE":
            # Rango insuficiente según el ejercicio y fase
            if exercise_id == "TRUNK_FLEXION" and phase in ["HOLD_POSITION", "GOING_DOWN"]:
                modified[22] -= 0.15  # no baja tanto
                modified[24] -= 0.15
            elif exercise_id == "SHOULDER_ABDUCTION" and phase in ["HOLD_TOP", "RAISING"]:
                modified[30] += 0.15  # no sube tanto
                modified[32] += 0.15
            elif exercise_id == "LEG_RAISE" and phase in ["HOLD_HIGH", "RAISING_LEG"]:
                modified[50] += 0.20  # pierna no sube tanto
                modified[54] += 0.25
                
        elif error_type == "INCORRECT_POSTURE":
            # Postura base incorrecta
            modified[46] += 0.05  # cadera desplazada
            modified[48] -= 0.04  # cadera der desplazada
            modified[1] += 0.03   # cabeza ladeada
            
        elif error_type == "INCORRECT_ALIGNMENT":
            # Alineación incorrecta
            modified[22] += 0.06  # hombro desalineado
            modified[46] += 0.07  # cadera desalineada
            modified[0] += 0.04   # nariz ladeada
            
        elif error_type == "TOO_FAST_MOVEMENT":
            # Movimiento inestable/rápido
            for i in range(22, 33, 2):  # landmarks de torso
                modified[i] += random.uniform(-0.04, 0.04)
                
        elif error_type == "INCOMPLETE_HOLD":
            # No mantiene la posición correctamente
            if "HOLD" in phase:
                # Reduce la intensidad del movimiento
                if exercise_id == "TRUNK_FLEXION":
                    modified[22] -= 0.10  # no mantiene flexión completa
                    modified[24] -= 0.10
                elif exercise_id == "SHOULDER_ABDUCTION":
                    modified[30] += 0.08  # no mantiene brazos arriba
                    modified[32] += 0.08
        
        return modified, error_type
    
    def add_realistic_noise(self, landmarks: List[float]) -> List[float]:
        """Ruido realista de MediaPipe"""
        return [coord + random.gauss(0, 0.003) for coord in landmarks]
    
    def generate_sample(self, exercise_id: str, phase: str, is_correct: bool) -> Dict:
        """Genera UNA muestra para exercise_id + phase específicos"""
        
        # 1. Landmarks base
        base_landmarks = self.generate_base_landmarks()
        
        # 2. Modificar para exercise + phase
        exercise_landmarks = self.modify_landmarks_for_exercise_phase(
            base_landmarks, exercise_id, phase
        )
        
        # 3. Introducir errores si es incorrecto
        if is_correct:
            final_landmarks = exercise_landmarks
            error_type = "NONE"
        else:
            final_landmarks, error_type = self.introduce_errors(
                exercise_landmarks, exercise_id, phase
            )
        
        # 4. Ruido realista
        final_landmarks = self.add_realistic_noise(final_landmarks)
        
        # 5. Crear muestra
        sample = {
            'exercise_id': exercise_id,
            'exercise_name': self.exercise_definitions[exercise_id]['name'],
            'expected_phase': phase,
            'is_correct': is_correct,
            'error_type': error_type
        }
        
        # 6. Agregar landmarks
        for i in range(0, len(final_landmarks), 2):
            landmark_idx = i // 2
            sample[f'landmark_{landmark_idx}_x'] = round(final_landmarks[i], 4)
            sample[f'landmark_{landmark_idx}_y'] = round(final_landmarks[i+1], 4)
        
        return sample
    
    def generate_dataset(self, samples_per_exercise_phase: int = 300, 
                        error_rate: float = 0.35) -> pd.DataFrame:
        """Generate dataset for hybrid approach"""
        
        print("GENERATING HYBRID DATASET")
        print("=" * 50)
        print("Approach: exercise_id + expected_phase + landmarks → is_correct")
        print(f"Samples per (exercise × phase): {samples_per_exercise_phase}")
        print(f"Error rate: {error_rate}")
        
        all_samples = []
        total_combinations = 0
        
        for exercise_id, exercise_info in self.exercise_definitions.items():
            print(f"{exercise_id} ({exercise_info['name']}):")
            
            for phase in exercise_info['phases']: 
                print(f"   - {phase}: {samples_per_exercise_phase} samples")
                
                # Calculate correct and incorrect samples
                incorrect_count = int(samples_per_exercise_phase * error_rate)
                correct_count = samples_per_exercise_phase - incorrect_count
                
                # Generate correct samples
                for _ in range(correct_count):
                    sample = self.generate_sample(exercise_id, phase, is_correct=True)
                    all_samples.append(sample)
                
                # Generate incorrect samples  
                for _ in range(incorrect_count):
                    sample = self.generate_sample(exercise_id, phase, is_correct=False)
                    all_samples.append(sample)
                
                total_combinations += 1
        
        # Create DataFrame
        df = pd.DataFrame(all_samples)
        df = df.sample(frac=1, random_state=42).reset_index(drop=True)
        
        print(f"\n✅ DATASET HÍBRIDO GENERADO:")
        print(f"   📊 Total muestras: {len(df):,}")
        print(f"   🏋️ Ejercicios: {len(self.exercise_definitions)}")
        print(f"   📋 Combinaciones (ejercicio×fase): {total_combinations}")
        print(f"   ✅ Muestras correctas: {df['is_correct'].sum():,}")
        print(f"   ❌ Muestras incorrectas: {(~df['is_correct']).sum():,}")
        print(f"   🔢 Features: exercise_id + expected_phase + {len([c for c in df.columns if 'landmark_' in c])} landmarks")
        
        # Verificar separabilidad por ejercicio
        print(f"\n🔍 VERIFICACIÓN DE SEPARABILIDAD:")
        for exercise_id in list(self.exercise_definitions.keys())[:3]:
            for phase in self.exercise_definitions[exercise_id]['phases'][:2]:  # Solo primeras 2 fases
                subset = df[(df['exercise_id'] == exercise_id) & (df['expected_phase'] == phase)]
                if len(subset) > 0:
                    correct_mean = subset[subset['is_correct']]['landmark_11_y'].mean()
                    incorrect_mean = subset[~subset['is_correct']]['landmark_11_y'].mean()
                    diff = abs(correct_mean - incorrect_mean)
                    print(f"   {exercise_id}_{phase}: landmark_11_y diff = {diff:.3f}")
        
        return df

def create_hybrid_dataset():
    """Función principal para generar dataset híbrido"""
    
    generator = HybridPhysioDatasetGenerator()
    
    df = generator.generate_dataset(
        samples_per_exercise_phase=300,  # 300 por cada (ejercicio × fase)
        error_rate=0.35                  # 35% incorrectas
    )
    
    filename = 'physio_hybrid_dataset.csv'
    df.to_csv(filename, index=False)
    
    print(f"\n💾 DATASET GUARDADO: {filename}")
    print(f"\n🎯 PERFECTO PARA:")
    print(f"   ✅ Modelo 1: exercise_id + expected_phase + landmarks → is_correct")
    print(f"   ✅ Modelo 2: exercise_id + expected_phase + landmarks → error_type")
    print(f"   ✅ Enfoque híbrido con control de secuencias")
    print(f"   ✅ Fácil escalabilidad a nuevos ejercicios")
    
    # Mostrar estadísticas detalladas
    print(f"\n📊 ESTADÍSTICAS DETALLADAS:")
    exercise_stats = df.groupby(['exercise_id', 'expected_phase']).agg({
        'is_correct': ['count', 'sum']
    }).round(0)
    exercise_stats.columns = ['Total', 'Correctas']
    exercise_stats['Incorrectas'] = exercise_stats['Total'] - exercise_stats['Correctas']
    print(exercise_stats.head(10))
    
    return df, filename

if __name__ == "__main__":
    dataset, filename = create_hybrid_dataset()