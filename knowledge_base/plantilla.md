---
id: "[PREFIJO]-[NN]"
nombre: "Nombre Descriptivo"
fecha_modificacion: "DD/MM/YYYY"
estado: "BORRADOR | PLACEHOLDER | PROD_ACTUAL"
relacionados: ["ID-01", "ID-02"]
tags: ["tag1", "tag2"]
prioridad: "1-10 o 'soporte'" # Solo para evidencia/
---

# Título del Documento

> **Prioridad #N** — (Solo si es evidencia) Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Descripción breve de qué controla o documenta este archivo.

## 2. Variables / Datos
| Variable | Valor | Descripción |
|----------|-------|-------------|
| `NOMBRE_VAR` | [RELLENAR] | Uso en el sistema |

## 3. Reglas / Lógica (si aplica)
```yaml
REGLA_EJEMPLO:
  condicion: "descripción"
  accion: "qué hace el sistema"
```

## 4. Temas a Investigar (solo evidencia)
- [ ] Tema pendiente 1
- [ ] Tema pendiente 2

## 5. Papers / Referencias (solo evidencia)
| Título | Autor | Año | Hallazgo Clave |
|--------|-------|-----|----------------|
| — | — | — | — |

## 6. Síntesis para el Sistema
*Resumen de hallazgos aplicables*

## 7. Implicaciones para Reglas
*Qué reglas se derivan de este documento*

## 8. Uso en el Sistema
1. Cómo el backend o frontend usa esta información.
2. Qué otros archivos dependen de este.

---

## Guía de Prefijos de ID

| Prefijo | Uso |
|---------|-----|
| `SYS` | Sistema (manifest) |
| `USR` | Usuario (contexto personal) |
| `USR-PER` | Perfil de usuario |
| `USR-MET` | Métricas de usuario |
| `EVI` | Evidencia científica |
| `REG-ENT` | Reglas de entrenamiento |
| `REG-NUT` | Reglas de nutrición |
| `REG-LOG` | Reglas de lógica/backend |
| `REG-DEV` | Reglas de desarrollo |

## Estados

| Estado | Significado |
|--------|-------------|
| `BORRADOR` | En desarrollo, puede cambiar |
| `PLACEHOLDER` | Estructura lista, contenido pendiente |
| `PROD_ACTUAL` | Versión estable en uso |