---
id: "REG-LOG-04"
nombre: "Entrenamiento en Casa (Sin Equipamiento de Gimnasio)"
fecha_modificacion: "31/08/2026"
estado: "ACTIVO"
relacionados: ["REG-LOG-03", "USR-03", "USR-01"]
tags: ["reglas", "logica", "excepciones", "vacaciones", "casa", "bodyweight"]
---

# Entrenamiento en Casa (Sin Equipamiento)

## 1. Alcance
Rutina mínima de mantenimiento para usar durante `EXCEPCION_VACACIONES` /
`EXCEPCION_VIAJE` (ver [excepciones.md](excepciones.md) §2.2-2.3), cuando no
hay acceso al gimnasio. Objetivo: mantener estímulo mínimo, NO progresar ni
sustituir el plan real — es un puente hasta volver a Fitness Park.

> ⚠️ **Sin equipamiento de gimnasio** — solo lo disponible en
> [equipamiento.md](../../usuario/equipamiento.md) §3 (Casa): bandas
> elásticas, esterilla. Peso corporal para el resto.

## 2. Criterio de Diseño
- **Simple, sin parafernalia** (a petición del usuario): 1 sola rutina de
  cuerpo completo, no un split — pensada para 2-3 sesiones sueltas durante
  la ausencia, no un mesociclo.
- Mismas prioridades que el plan real ([prioridades.md](../../usuario/prioridades.md)):
  P1 Estética (V-taper: hombros/espalda), P2 Postura, P3 Hipertrofia.
- Esfuerzo moderado (RIR 2-3) — no es momento de buscar fallo sin
  supervisión ni maximizar volumen; es mantenimiento.
- Al volver: aplica igualmente `excepciones.md` §2.2 (RIR+1 primera semana,
  motor Capa 5 reduce peso por el gap).

## 3. Rutina (cuerpo completo, ~30 min)

| Ejercicio | Series x Reps | Foco | Por qué |
|-----------|---------------|------|---------|
| Flexiones (push-ups) | 3 x AMRAP-2 (parar dejando 2 en reserva) | Pecho, hombro anterior, tríceps | Empuje horizontal — sin banco ni barra |
| Flexión pies elevados (pike/declinada) | 3 x 8-12 | Hombro (énfasis, P1 V-taper) | Mayor ángulo = más hombro sin necesitar mancuernas |
| Remo con banda elástica | 3 x 12-15 | Espalda media, postura (P2) | Único ejercicio de tracción horizontal posible con bandas |
| Face pull con banda | 3 x 15 | Deltoides posterior, postura (P2) | Contrarresta el volumen de empuje de arriba, salud de hombro |
| Zancada / sentadilla búlgara (peso corporal) | 3 x 12/lado | Pierna | Unilateral — compensa la ausencia de carga externa |
| Plancha | 3 x 30-45 seg | Core | Anti-extensión, coherente con `motor_pesos.md` (elbow-safe, sin carga) |

> **Nota lesión codo**: flexiones y plancha son isométricas/de empuje
> controlado, no cargan el codo en extensión bajo resistencia externa — no
> están excluidas por la restricción de "evitar extensión cargada" que sí
> aplica a ejercicios de tríceps aislado con peso (ver `lesiones.md`).

## 4. Progresión Dentro de la Rutina (si se repite varios días)
- Si "flexiones AMRAP-2" ya supera 20 reps limpias, pasar a variante más
  difícil (pies elevados) en vez de subir más reps — evita entrenar
  resistencia muscular en vez de hipertrofia.
- No hay progresión de carga real posible sin equipamiento — no forzarla.

## 5. Uso en el Sistema
1. `registrarAusencia_` (Codigo.gs) devuelve esta rutina como `rutina_casa`
   dentro de la respuesta, junto con el impacto en el plan.
2. La app la muestra en la confirmación de "Pausar plan (vacaciones)".
